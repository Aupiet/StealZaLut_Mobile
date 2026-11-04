"""Colour-transfer engine backed by *color-matcher* and high-fidelity NumPy/SciPy algorithms.

This module provides professional color transfer between a source and reference
image with CIE LAB luminance preservation, contrast ratio clamping, Monge-Kantorovich
Linearization (MKL), Multi-Variate Gaussian Distribution (MVGD), channel-wise
Histogram Matching, and Reinhard statistics.
"""

from __future__ import annotations

from typing import Any, Dict, Optional, Tuple, Union

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import (
    CHROMA_BOOST_DEFAULT,
    DEFAULT_LUMINANCE_BINS,
    DEFAULT_METHOD,
    DEFAULT_PRESERVE_LUMINANCE,
    EPSILON,
    LUMINANCE_ALIGN_WEIGHT,
    MAX_CHROMA_BOOST,
    MAX_STD_RATIO,
    MIN_CHROMA_BOOST,
    MIN_STD_RATIO,
    SHADOW_ROLLOFF_L_THRESHOLD,
    SOFT_CLIP_HIGHLIGHT_THRESHOLD,
    SOFT_CLIP_SHADOW_THRESHOLD,
    SUPPORTED_METHODS,
    VIVID_CHROMA_PERCENTILE,
)

# Standard D65 sRGB to XYZ transformation matrix (IEC 61966-2-1)
_M_SRGB_TO_XYZ: NDArray[np.float32] = np.array(
    [
        [0.4124564, 0.3575761, 0.1804375],
        [0.2126729, 0.7151522, 0.0721750],
        [0.0193339, 0.1191920, 0.9503041],
    ],
    dtype=np.float32,
)

# Standard XYZ to sRGB inverse matrix
_M_XYZ_TO_SRGB: NDArray[np.float32] = np.array(
    [
        [3.2404542, -1.5371385, -0.4985314],
        [-0.9692660, 1.8760108, 0.0415560],
        [0.0556434, -0.2040259, 1.0572252],
    ],
    dtype=np.float32,
)

# CIE 1931 2° D65 standard illuminant reference white
_D65_WHITE: NDArray[np.float32] = np.array(
    [0.95047, 1.00000, 1.08883], dtype=np.float32
)

_CIE_DELTA: float = 6.0 / 29.0
_CIE_DELTA_CUBED: float = _CIE_DELTA**3
_CIE_3_DELTA_SQUARED: float = 3.0 * (_CIE_DELTA**2)


def rgb_to_lab(rgb: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert sRGB array in [0.0, 1.0] to CIE L*a*b*.

    Args:
        rgb: Float32 array of shape (..., 3) with values in [0.0, 1.0].

    Returns:
        Float32 array of shape (..., 3) with L in [0.0, 100.0],
        a and b in approximately [-128.0, 127.0].
    """
    rgb_clipped = np.clip(rgb, 0.0, 1.0)
    # Linearize sRGB gamma
    mask = rgb_clipped > 0.04045
    rgb_lin = np.where(
        mask,
        ((rgb_clipped + 0.055) / 1.055) ** 2.4,
        rgb_clipped / 12.92,
    )

    # sRGB -> XYZ
    xyz = rgb_lin @ _M_SRGB_TO_XYZ.T
    xyz_n = xyz / _D65_WHITE

    # Nonlinear transformation for CIE LAB
    f_xyz = np.where(
        xyz_n > _CIE_DELTA_CUBED,
        np.cbrt(xyz_n),
        xyz_n / _CIE_3_DELTA_SQUARED + (4.0 / 29.0),
    )

    l_channel = 116.0 * f_xyz[..., 1] - 16.0
    a_channel = 500.0 * (f_xyz[..., 0] - f_xyz[..., 1])
    b_channel = 200.0 * (f_xyz[..., 1] - f_xyz[..., 2])

    return np.stack([l_channel, a_channel, b_channel], axis=-1)


def lab_to_rgb(lab: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convert CIE L*a*b* array to sRGB in [0.0, 1.0].

    Args:
        lab: Float32 array of shape (..., 3).

    Returns:
        Float32 array of shape (..., 3) with sRGB values in [0.0, 1.0].
    """
    fy = (lab[..., 0] + 16.0) / 116.0
    fx = fy + lab[..., 1] / 500.0
    fz = fy - lab[..., 2] / 200.0

    f_xyz = np.stack([fx, fy, fz], axis=-1)
    xyz_n = np.where(
        f_xyz > _CIE_DELTA,
        f_xyz**3,
        _CIE_3_DELTA_SQUARED * (f_xyz - 4.0 / 29.0),
    )
    xyz = xyz_n * _D65_WHITE

    # XYZ -> linear sRGB
    rgb_lin = xyz @ _M_XYZ_TO_SRGB.T
    rgb_lin = np.clip(rgb_lin, 0.0, None)

    # Apply sRGB gamma
    rgb = np.where(
        rgb_lin > 0.0031308,
        1.055 * (rgb_lin ** (1.0 / 2.4)) - 0.055,
        12.92 * rgb_lin,
    )
    return np.clip(rgb, 0.0, 1.0)


def apply_soft_clip(
    x: NDArray[np.float32],
    shadow_th: float = SOFT_CLIP_SHADOW_THRESHOLD,
    highlight_th: float = SOFT_CLIP_HIGHLIGHT_THRESHOLD,
) -> NDArray[np.float32]:
    """Apply a smooth C1-continuous soft-knee compression using np.tanh.

    Protects shadow details (deepest 5%) and highlight roll-off (brightest 5%)
    to avoid harsh clipping and posterisation.
    """
    out = x.copy()

    # Soft compression on highlights (x > highlight_th)
    m_hi = 1.0 - highlight_th
    mask_hi = x > highlight_th
    if np.any(mask_hi):
        out[mask_hi] = highlight_th + m_hi * np.tanh(
            (x[mask_hi] - highlight_th) / m_hi
        )

    # Soft compression on shadows (x < shadow_th)
    m_lo = shadow_th
    mask_lo = x < shadow_th
    if np.any(mask_lo):
        out[mask_lo] = shadow_th - m_lo * np.tanh((shadow_th - x[mask_lo]) / m_lo)

    return np.clip(out, 0.0, 1.0)


def luminance_binned_chroma_transfer(
    src_lab: NDArray[np.float32],
    ref_lab: NDArray[np.float32],
    num_bins: int = DEFAULT_LUMINANCE_BINS,
    vivid_percentile: float = VIVID_CHROMA_PERCENTILE,
    chroma_boost: float = CHROMA_BOOST_DEFAULT,
    shadow_th: float = SHADOW_ROLLOFF_L_THRESHOLD,
    luminance_align_weight: float = LUMINANCE_ALIGN_WEIGHT,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
) -> NDArray[np.float32]:
    """Transfer chroma across luminance-partitioned bins with vividness selection."""
    src_shape = src_lab.shape
    src_flat = src_lab.reshape(-1, 3)
    ref_flat = ref_lab.reshape(-1, 3)

    l_src = src_flat[:, 0]
    a_src = src_flat[:, 1]
    b_src = src_flat[:, 2]

    l_ref = ref_flat[:, 0]
    a_ref = ref_flat[:, 1]
    b_ref = ref_flat[:, 2]

    chroma_ref = np.sqrt(a_ref**2 + b_ref**2)

    bin_centers = np.linspace(15.0, 85.0, num_bins, dtype=np.float32)
    step = float(bin_centers[1] - bin_centers[0]) if num_bins > 1 else 30.0
    sigma_l = step * 0.75

    w_src_list = []
    a_trans_bins = []
    b_trans_bins = []

    boost = float(np.clip(chroma_boost, MIN_CHROMA_BOOST, MAX_CHROMA_BOOST))

    global_mu_a_ref = float(np.mean(a_ref))
    global_mu_b_ref = float(np.mean(b_ref))
    global_std_a_ref = float(np.std(a_ref))
    global_std_b_ref = float(np.std(b_ref))

    for center in bin_centers:
        w_s = np.exp(-0.5 * ((l_src - center) / sigma_l) ** 2)
        w_r = np.exp(-0.5 * ((l_ref - center) / sigma_l) ** 2)
        w_src_list.append(w_s)

        active_ref = w_r > 0.10
        if np.count_nonzero(active_ref) >= 16:
            ref_bin_chroma = chroma_ref[active_ref]
            th_chroma = float(np.percentile(ref_bin_chroma, vivid_percentile))
            vivid_mask = active_ref & (chroma_ref >= th_chroma)
            w_eff_r = w_r * vivid_mask.astype(np.float32)
        else:
            w_eff_r = w_r

        sum_w_r = float(np.sum(w_eff_r))
        if sum_w_r > float(EPSILON):
            mu_a_r = float(np.sum(w_eff_r * a_ref) / sum_w_r)
            mu_b_r = float(np.sum(w_eff_r * b_ref) / sum_w_r)
            std_a_r = float(np.sqrt(max(0.0, float(np.sum(w_eff_r * (a_ref - mu_a_r) ** 2) / sum_w_r))))
            std_b_r = float(np.sqrt(max(0.0, float(np.sum(w_eff_r * (b_ref - mu_b_r) ** 2) / sum_w_r))))
        else:
            mu_a_r = global_mu_a_ref
            mu_b_r = global_mu_b_ref
            std_a_r = global_std_a_ref
            std_b_r = global_std_b_ref

        sum_w_s = float(np.sum(w_s))
        if sum_w_s > float(EPSILON):
            mu_a_s = float(np.sum(w_s * a_src) / sum_w_s)
            mu_b_s = float(np.sum(w_s * b_src) / sum_w_s)
            std_a_s = float(np.sqrt(max(0.0, float(np.sum(w_s * (a_src - mu_a_s) ** 2) / sum_w_s))))
            std_b_s = float(np.sqrt(max(0.0, float(np.sum(w_s * (b_src - mu_b_s) ** 2) / sum_w_s))))
        else:
            mu_a_s = float(np.mean(a_src))
            mu_b_s = float(np.mean(b_src))
            std_a_s = float(np.std(a_src))
            std_b_s = float(np.std(b_src))

        scale_a = float(np.clip((std_a_r / (std_a_s + float(EPSILON))) * boost, MIN_STD_RATIO, MAX_STD_RATIO))
        scale_b = float(np.clip((std_b_r / (std_b_s + float(EPSILON))) * boost, MIN_STD_RATIO, MAX_STD_RATIO))

        a_trans_bins.append((a_src - mu_a_s) * scale_a + mu_a_r)
        b_trans_bins.append((b_src - mu_b_s) * scale_b + mu_b_r)

    w_src_stack = np.stack(w_src_list, axis=0)
    w_src_norm = w_src_stack / (np.sum(w_src_stack, axis=0, keepdims=True) + float(EPSILON))

    a_trans_stack = np.stack(a_trans_bins, axis=0)
    b_trans_stack = np.stack(b_trans_bins, axis=0)

    a_trans = np.sum(w_src_norm * a_trans_stack, axis=0)
    b_trans = np.sum(w_src_norm * b_trans_stack, axis=0)

    t_shadow = np.clip(l_src / (shadow_th + float(EPSILON)), 0.0, 1.0)
    shadow_roll = (t_shadow ** 2.5).astype(np.float32)
    a_chroma = a_src + shadow_roll * (a_trans - a_src)
    b_chroma = b_src + shadow_roll * (b_trans - b_src)

    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
    if clamped_preserve < 1.0 and luminance_align_weight > 0.0:
        percentiles = np.linspace(0.0, 100.0, 21, dtype=np.float32)
        src_pct = np.percentile(l_src, percentiles)
        ref_pct = np.percentile(l_ref, percentiles)
        if float(src_pct[-1] - src_pct[0]) > float(EPSILON):
            l_aligned = np.interp(l_src, src_pct, ref_pct).astype(np.float32)
            l_soft = l_src + luminance_align_weight * (l_aligned - l_src)
            l_final = clamped_preserve * l_src + (1.0 - clamped_preserve) * l_soft
        else:
            l_final = l_src.copy()
    else:
        l_final = l_src.copy()

    l_final = np.nan_to_num(np.clip(l_final, 0.0, 100.0), nan=50.0, posinf=100.0, neginf=0.0)
    a_chroma = np.nan_to_num(a_chroma, nan=0.0, posinf=127.0, neginf=-128.0)
    b_chroma = np.nan_to_num(b_chroma, nan=0.0, posinf=127.0, neginf=-128.0)

    result_flat = np.stack([l_final, a_chroma, b_chroma], axis=-1)
    return result_flat.reshape(src_shape).astype(np.float32)


def _numpy_statistical_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.uint8]:
    """Pure NumPy statistical Reinhard color transfer in CIE LAB space."""
    res_f = fit_and_apply_transfer(
        src_img=src_img,
        ref_img=ref_img,
        target_img=src_img,
        method="reinhard",
        preserve_luminance=preserve_luminance,
        soft_clip=soft_clip,
    )
    return (np.clip(res_f, 0.0, 1.0) * 255.0).astype(np.uint8)


# ---------------------------------------------------------------------------
# Core Color Transfer Algorithms (Desktop Parity)
# ---------------------------------------------------------------------------

def _histogram_match_1d(
    src: NDArray[np.float32],
    ref: NDArray[np.float32],
    target: Optional[NDArray[np.float32]] = None,
) -> NDArray[np.float32]:
    """Channel-wise 1D Histogram matching for normalized sRGB images [0.0, 1.0].

    Computes empirical cumulative distribution functions (CDFs) of *src* and *ref*,
    then maps *target* (or *src* if target is None) through the quantile mapping.
    """
    s_f = np.clip(src.astype(np.float32), 0.0, 1.0)
    r_f = np.clip(ref.astype(np.float32), 0.0, 1.0)
    t_f = s_f if target is None else np.clip(target.astype(np.float32), 0.0, 1.0)

    out = np.empty_like(t_f)
    qs = np.linspace(0.0, 1.0, 256)

    for ch in range(3):
        s_c = s_f[..., ch].ravel()
        r_c = r_f[..., ch].ravel()
        t_c = t_f[..., ch].ravel()

        s_vals = np.quantile(s_c, qs)
        r_vals = np.quantile(r_c, qs)

        # Map target through the learned CDF correspondence
        t_mapped = np.interp(t_c, s_vals, r_vals)
        out[..., ch] = t_mapped.reshape(t_f[..., ch].shape)

    return np.clip(out, 0.0, 1.0).astype(np.float32)


def _compute_mkl_matrix(
    cov_src: NDArray[np.float64],
    cov_ref: NDArray[np.float64],
) -> NDArray[np.float32]:
    """Compute the 3x3 Monge-Kantorovich Linear (MKL) transfer matrix."""
    eps = 1e-6
    cov_s = cov_src + eps * np.eye(3, dtype=np.float64)
    cov_r = cov_ref + eps * np.eye(3, dtype=np.float64)

    eig_val_r, eig_vec_r = np.linalg.eigh(cov_s)
    eig_val_r = np.maximum(eig_val_r, 0.0)
    val_r = np.diag(np.sqrt(eig_val_r))
    inv_r = np.diag(1.0 / (np.diag(val_r) + 1e-7))

    mat_c = val_r @ eig_vec_r.T @ cov_r @ eig_vec_r @ val_r
    eig_val_c, eig_vec_c = np.linalg.eigh(mat_c)
    eig_val_c = np.maximum(eig_val_c, 0.0)
    val_c = np.diag(np.sqrt(eig_val_c))

    t_mat = eig_vec_r @ inv_r @ eig_vec_c @ val_c @ eig_vec_c.T @ inv_r @ eig_vec_r.T
    return t_mat.astype(np.float32)


def _compute_mvgd_matrix(
    cov_src: NDArray[np.float64],
    cov_ref: NDArray[np.float64],
) -> NDArray[np.float32]:
    """Compute the 3x3 Multi-Variate Gaussian Distribution (MVGD) transfer matrix."""
    eps = 1e-6
    cov_s = cov_src + eps * np.eye(3, dtype=np.float64)
    cov_r = cov_ref + eps * np.eye(3, dtype=np.float64)

    # Diagonalize cov_s
    val_s, vec_s = np.linalg.eigh(cov_s)
    val_s = np.maximum(val_s, 0.0)
    inv_sqrt_s = vec_s @ np.diag(1.0 / np.sqrt(val_s + 1e-7)) @ vec_s.T

    # Diagonalize cov_r
    val_r, vec_r = np.linalg.eigh(cov_r)
    val_r = np.maximum(val_r, 0.0)
    sqrt_r = vec_r @ np.diag(np.sqrt(val_r)) @ vec_r.T

    t_mat = sqrt_r @ inv_sqrt_s
    return t_mat.astype(np.float32)


def _reinhard_lab_transfer(
    src_f: NDArray[np.float32],
    ref_f: NDArray[np.float32],
    target_f: Optional[NDArray[np.float32]] = None,
    preserve_luminance: float = 0.0,
) -> NDArray[np.float32]:
    """Reinhard color transfer in true CIE L*a*b* space (D65)."""
    src_lab = rgb_to_lab(src_f).reshape(-1, 3)
    ref_lab = rgb_to_lab(ref_f).reshape(-1, 3)

    t_f = src_f if target_f is None else target_f
    t_lab = rgb_to_lab(t_f)
    t_shape = t_lab.shape
    t_flat = t_lab.reshape(-1, 3)

    mu_s = np.mean(src_lab, axis=0)
    std_s = np.std(src_lab, axis=0)
    mu_r = np.mean(ref_lab, axis=0)
    std_r = np.std(ref_lab, axis=0)

    std_ratio = np.clip(std_r / (std_s + 1e-6), MIN_STD_RATIO, MAX_STD_RATIO)
    out_lab = (t_flat - mu_s) * std_ratio + mu_r

    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
    if clamped_preserve > 0.0:
        out_lab[:, 0] = clamped_preserve * t_flat[:, 0] + (1.0 - clamped_preserve) * out_lab[:, 0]

    out_lab[:, 0] = np.clip(out_lab[:, 0], 0.0, 100.0)
    out_rgb = lab_to_rgb(out_lab.reshape(t_shape))
    return np.clip(out_rgb, 0.0, 1.0).astype(np.float32)


def fit_and_apply_transfer(
    src_img: NDArray[Union[np.uint8, np.float32]],
    ref_img: NDArray[Union[np.uint8, np.float32]],
    target_img: Optional[NDArray[Union[np.uint8, np.float32]]] = None,
    method: str = DEFAULT_METHOD,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = False,
) -> NDArray[np.float32]:
    """Unified solver fitting color transfer between *src* and *ref* and applying to *target*.

    If *target_img* is None, applies directly to *src_img*.
    If *target_img* is a 3D identity lattice (N^3, 3), evaluates the transfer across
    the entire RGB cube to produce an Adobe-compliant 3D LUT.
    """
    src_f = (src_img.astype(np.float32) / 255.0) if src_img.dtype == np.uint8 else src_img.astype(np.float32)
    ref_f = (ref_img.astype(np.float32) / 255.0) if ref_img.dtype == np.uint8 else ref_img.astype(np.float32)

    if target_img is None:
        t_f = src_f.copy()
    else:
        t_f = (target_img.astype(np.float32) / 255.0) if target_img.dtype == np.uint8 else target_img.astype(np.float32)

    meth = method.lower()

    if meth == "reinhard":
        result = _reinhard_lab_transfer(
            src_f=src_f,
            ref_f=ref_f,
            target_f=t_f,
            preserve_luminance=preserve_luminance,
        )

    elif meth == "hm":
        result = _histogram_match_1d(src=src_f, ref=ref_f, target=t_f)

    elif meth in ("mkl", "mvgd"):
        s_flat = src_f.reshape(-1, 3).astype(np.float64)
        r_flat = ref_f.reshape(-1, 3).astype(np.float64)
        t_flat = t_f.reshape(-1, 3).astype(np.float32)

        mu_s = s_flat.mean(axis=0)
        mu_r = r_flat.mean(axis=0)
        cov_s = np.cov(s_flat, rowvar=False)
        cov_r = np.cov(r_flat, rowvar=False)

        t_mat = _compute_mkl_matrix(cov_s, cov_r) if meth == "mkl" else _compute_mvgd_matrix(cov_s, cov_r)
        res_flat = (t_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(np.float32)
        result = res_flat.reshape(t_f.shape)

    elif meth in ("hm-mkl-hm", "hm-mvgd-hm"):
        # 1. Pre-HM: Match histogram of source to reference
        s_hm1 = _histogram_match_1d(src=src_f, ref=ref_f, target=src_f)
        t_hm1 = _histogram_match_1d(src=src_f, ref=ref_f, target=t_f)

        # 2. Covariance transfer (MKL or MVGD)
        s_flat = s_hm1.reshape(-1, 3).astype(np.float64)
        r_flat = ref_f.reshape(-1, 3).astype(np.float64)
        t_flat = t_hm1.reshape(-1, 3).astype(np.float32)

        mu_s = s_flat.mean(axis=0)
        mu_r = r_flat.mean(axis=0)
        cov_s = np.cov(s_flat, rowvar=False)
        cov_r = np.cov(r_flat, rowvar=False)

        t_mat = _compute_mkl_matrix(cov_s, cov_r) if meth == "hm-mkl-hm" else _compute_mvgd_matrix(cov_s, cov_r)
        s_mkl = ((s_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(np.float32)).reshape(s_hm1.shape)
        t_mkl = ((t_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(np.float32)).reshape(t_hm1.shape)

        # 3. Post-HM: Re-match histogram to reference
        result = _histogram_match_1d(src=s_mkl, ref=ref_f, target=t_mkl)

    else:
        # Default fallback to Reinhard
        result = _reinhard_lab_transfer(
            src_f=src_f,
            ref_f=ref_f,
            target_f=t_f,
            preserve_luminance=preserve_luminance,
        )

    # Optional CIE LAB Luminance Preservation
    clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
    if clamped_preserve > 0.0 and meth != "reinhard":
        t_lab = rgb_to_lab(t_f)
        res_lab = rgb_to_lab(np.clip(result, 0.0, 1.0))
        final_l = clamped_preserve * t_lab[..., 0] + (1.0 - clamped_preserve) * res_lab[..., 0]
        blended_lab = np.stack([final_l, res_lab[..., 1], res_lab[..., 2]], axis=-1)
        result = lab_to_rgb(blended_lab)

    if soft_clip:
        result = apply_soft_clip(result)
    else:
        result = np.clip(result, 0.0, 1.0)

    return result.astype(np.float32)


def compute_color_transfer(
    src_img: NDArray[np.uint8],
    ref_img: NDArray[np.uint8],
    method: str = DEFAULT_METHOD,
    preserve_luminance: float = DEFAULT_PRESERVE_LUMINANCE,
    soft_clip: bool = True,
) -> NDArray[np.uint8]:
    """Transfer the colour distribution of *ref_img* onto *src_img*.

    Returns an RGB uint8 array with the exact spatial dimensions of *src_img*.
    """
    if method not in SUPPORTED_METHODS:
        raise ValueError(
            f"Unsupported method '{method}'. "
            f"Choose from: {list(SUPPORTED_METHODS.keys())}"
        )

    res_f = fit_and_apply_transfer(
        src_img=src_img,
        ref_img=ref_img,
        target_img=src_img,
        method=method,
        preserve_luminance=preserve_luminance,
        soft_clip=soft_clip,
    )
    return (np.clip(res_f, 0.0, 1.0) * 255.0).astype(np.uint8)
