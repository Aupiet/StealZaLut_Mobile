"""Colour-transfer engine backed by *color-matcher* and high-fidelity NumPy/SciPy algorithms.

This module provides professional color transfer between a source and reference
image with CIE LAB luminance preservation, contrast ratio clamping, Monge-Kantorovich
Linearization (MKL), Multi-Variate Gaussian Distribution (MVGD), channel-wise
Histogram Matching, and Reinhard statistics.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional, Union

import numpy as np
from numpy.typing import NDArray

from stealzalut.config import (
    DEFAULT_METHOD,
    DEFAULT_PRESERVE_LUMINANCE,
    MAX_STD_RATIO,
    MIN_STD_RATIO,
    SOFT_CLIP_HIGHLIGHT_THRESHOLD,
    SOFT_CLIP_SHADOW_THRESHOLD,
    SUPPORTED_METHODS,
)
from stealzalut.core.color_space import gamut_compress, oklab_to_rgb, rgb_to_oklab

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
OKLAB_SCALE_LIMITS: tuple[float, float] = (0.5, 2.0)


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
        out[mask_hi] = highlight_th + m_hi * np.tanh((x[mask_hi] - highlight_th) / m_hi)

    # Soft compression on shadows (x < shadow_th)
    m_lo = shadow_th
    mask_lo = x < shadow_th
    if np.any(mask_lo):
        out[mask_lo] = shadow_th - m_lo * np.tanh((shadow_th - x[mask_lo]) / m_lo)

    return np.clip(out, 0.0, 1.0)


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
    """Compute the Monge-Kantorovich Linear (MKL) transfer matrix."""
    n = cov_src.shape[0]
    eps = 1e-6
    cov_s = cov_src + eps * np.eye(n, dtype=np.float64)
    cov_r = cov_ref + eps * np.eye(n, dtype=np.float64)

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
    """Compute the Multi-Variate Gaussian Distribution (MVGD) transfer matrix."""
    n = cov_src.shape[0]
    eps = 1e-6
    cov_s = cov_src + eps * np.eye(n, dtype=np.float64)
    cov_r = cov_ref + eps * np.eye(n, dtype=np.float64)

    val_s, vec_s = np.linalg.eigh(cov_s)
    val_s = np.maximum(val_s, 0.0)
    inv_sqrt_s = vec_s @ np.diag(1.0 / np.sqrt(val_s + 1e-7)) @ vec_s.T

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
        out_lab[:, 0] = (
            clamped_preserve * t_flat[:, 0] + (1.0 - clamped_preserve) * out_lab[:, 0]
        )

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
    src_f = (
        (src_img.astype(np.float32) / 255.0)
        if src_img.dtype == np.uint8
        else src_img.astype(np.float32)
    )
    ref_f = (
        (ref_img.astype(np.float32) / 255.0)
        if ref_img.dtype == np.uint8
        else ref_img.astype(np.float32)
    )

    if target_img is None:
        t_f = src_f.copy()
    else:
        t_f = (
            (target_img.astype(np.float32) / 255.0)
            if target_img.dtype == np.uint8
            else target_img.astype(np.float32)
        )

    meth = method.lower()

    if meth == "zones":
        model = fit_transfer(src_img, ref_img, method="zones")
        result = model.apply(t_f.reshape(-1, 3)).reshape(t_f.shape)

    elif meth == "reinhard":
        result = _reinhard_lab_transfer(
            src_f=src_f,
            ref_f=ref_f,
            target_f=t_f,
            preserve_luminance=preserve_luminance,
        )

    elif meth == "oklab":
        src_lab = rgb_to_oklab(src_f.reshape(-1, 3)).astype(np.float32)
        ref_lab = rgb_to_oklab(ref_f.reshape(-1, 3)).astype(np.float32)
        t_lab = rgb_to_oklab(t_f.reshape(-1, 3)).astype(np.float32)

        mu_s = src_lab.mean(axis=0)
        mu_r = ref_lab.mean(axis=0)
        std_s = src_lab.std(axis=0) + 1e-6
        std_r = ref_lab.std(axis=0) + 1e-6
        scale = np.clip(std_r / std_s, MIN_STD_RATIO, MAX_STD_RATIO)
        out_lab = (t_lab - mu_s) * scale + mu_r

        clamped_preserve = float(np.clip(preserve_luminance, 0.0, 1.0))
        if clamped_preserve > 0.0:
            out_lab[:, 0] = (
                clamped_preserve * t_lab[:, 0] + (1.0 - clamped_preserve) * out_lab[:, 0]
            )

        out_lab = gamut_compress(out_lab)
        out_lab[:, 0] = np.clip(out_lab[:, 0], 0.0, 1.0)
        result = oklab_to_rgb(out_lab.reshape(t_f.shape)).astype(np.float32)

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

        t_mat = (
            _compute_mkl_matrix(cov_s, cov_r)
            if meth == "mkl"
            else _compute_mvgd_matrix(cov_s, cov_r)
        )
        res_flat = (t_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(
            np.float32
        )
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

        t_mat = (
            _compute_mkl_matrix(cov_s, cov_r)
            if meth == "hm-mkl-hm"
            else _compute_mvgd_matrix(cov_s, cov_r)
        )
        s_mkl = (
            (s_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(np.float32)
        ).reshape(s_hm1.shape)
        t_mkl = (
            (t_flat - mu_s.astype(np.float32)) @ t_mat.T + mu_r.astype(np.float32)
        ).reshape(t_hm1.shape)

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
        final_l = (
            clamped_preserve * t_lab[..., 0]
            + (1.0 - clamped_preserve) * res_lab[..., 0]
        )
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


class TransferModel:
    @property
    def method(self) -> str:
        raise NotImplementedError

    def apply(self, colors: NDArray[np.float64]) -> NDArray[np.float64]:
        raise NotImplementedError


@dataclass(frozen=True)
class AffineTransferModel(TransferModel):
    method_name: str = "mkl"
    src_mean: NDArray[np.float64] = np.array([])
    ref_mean: NDArray[np.float64] = np.array([])
    transform: NDArray[np.float64] = np.array([])

    def __post_init__(self) -> None:
        if self.src_mean.size == 0:
            raise ValueError("src_mean is required")
        if self.ref_mean.size == 0:
            raise ValueError("ref_mean is required")
        if self.transform.size == 0:
            raise ValueError("transform is required")

    def apply(self, colors: NDArray[np.float64]) -> NDArray[np.float64]:
        colors = np.asarray(colors, dtype=np.float64)
        if colors.ndim == 1:
            colors = colors.reshape(1, -1)
        return np.clip((colors - self.src_mean) @ self.transform.T + self.ref_mean, 0.0, 1.0)


@dataclass(frozen=True)
class HistogramTransferModel(TransferModel):
    src_values: tuple[NDArray[np.float64], NDArray[np.float64], NDArray[np.float64]]
    mapped_values: tuple[NDArray[np.float64], NDArray[np.float64], NDArray[np.float64]]

    @property
    def method(self) -> str:
        return "hm"

    @property
    def method_name(self) -> str:
        return self.method

    def apply(self, colors: NDArray[np.float64]) -> NDArray[np.float64]:
        colors = np.asarray(colors, dtype=np.float64)
        out = np.empty_like(colors)
        for ch in range(3):
            out[:, ch] = np.interp(colors[:, ch], self.src_values[ch], self.mapped_values[ch])
        return np.clip(out, 0.0, 1.0)


@dataclass(frozen=True)
class ReinhardTransferModel(TransferModel):
    mean_s: NDArray[np.float64]
    mean_r: NDArray[np.float64]
    std_ratios: NDArray[np.float64]

    @property
    def method(self) -> str:
        return "reinhard"

    def apply(self, colors: NDArray[np.float64]) -> NDArray[np.float64]:
        colors = np.asarray(colors, dtype=np.float64)
        if colors.ndim == 1:
            colors = colors.reshape(1, -1)
        out = (colors - self.mean_s) * self.std_ratios + self.mean_r
        return np.clip(out, 0.0, 1.0)


@dataclass(frozen=True)
class OklabTransferModel(TransferModel):
    src_mean: NDArray[np.float64]
    ref_mean: NDArray[np.float64]
    lightness_scale: float
    chroma_matrix: NDArray[np.float64]

    @property
    def method(self) -> str:
        return "oklab"

    @property
    def method_name(self) -> str:
        return self.method

    def apply(self, colors: NDArray[np.float64]) -> NDArray[np.float64]:
        lab = rgb_to_oklab(colors)
        out = np.empty_like(lab)
        out[:, 0] = self.ref_mean[0] + self.lightness_scale * (lab[:, 0] - self.src_mean[0])
        out[:, 1:] = self.ref_mean[1:] + (lab[:, 1:] - self.src_mean[1:]) @ self.chroma_matrix.T
        return np.clip(oklab_to_rgb(gamut_compress(out)), 0.0, 1.0)


def _bound_symmetric(matrix: NDArray[np.float64], lo: float, hi: float) -> NDArray[np.float64]:
    """Clip eigenvalues of a symmetric matrix to a bounded interval."""
    eigvals, eigvecs = np.linalg.eigh(0.5 * (matrix + matrix.T))
    result = eigvecs @ np.diag(np.clip(eigvals, lo, hi)) @ eigvecs.T
    return result.astype(np.float64)


def _fit_oklab(source: NDArray[np.uint8], reference: NDArray[np.uint8]) -> OklabTransferModel:
    flat_s = source.reshape(-1, 3).astype(np.float64) / 255.0
    flat_r = reference.reshape(-1, 3).astype(np.float64) / 255.0
    lab_s = rgb_to_oklab(flat_s)
    lab_r = rgb_to_oklab(flat_r)
    lo, hi = OKLAB_SCALE_LIMITS
    sigma_s = float(np.std(lab_s[:, 0]))
    sigma_r = float(np.std(lab_r[:, 0]))
    lightness_scale = float(np.clip(sigma_r / max(sigma_s, 1e-6), lo, hi))
    reg = 1e-8 * np.eye(2)
    cov_s = np.asarray(np.cov(lab_s[:, 1:], rowvar=False), dtype=np.float64) + reg
    cov_r = np.asarray(np.cov(lab_r[:, 1:], rowvar=False), dtype=np.float64) + reg
    chroma_matrix = _bound_symmetric(_compute_mkl_matrix(cov_s, cov_r), lo, hi)
    return OklabTransferModel(
        src_mean=lab_s.mean(axis=0),
        ref_mean=lab_r.mean(axis=0),
        lightness_scale=lightness_scale,
        chroma_matrix=chroma_matrix,
    )


def fit_transfer(
    source: NDArray[np.uint8],
    reference: NDArray[np.uint8],
    method: str = DEFAULT_METHOD,
) -> TransferModel:
    """Fit a transfer model to *source* and *reference* images."""
    chosen = method.lower()
    if chosen == "oklab":
        return _fit_oklab(source, reference)
    if chosen == "zones":
        from stealzalut.core.zone_transfer import fit_zone_transfer

        return fit_zone_transfer(source, reference)
    if chosen in ("mkl", "mvgd", "default"):
        s_flat = source.reshape(-1, 3).astype(np.float64) / 255.0
        r_flat = reference.reshape(-1, 3).astype(np.float64) / 255.0
        mu_s = s_flat.mean(axis=0)
        mu_r = r_flat.mean(axis=0)
        cov_s = np.cov(s_flat, rowvar=False)
        cov_r = np.cov(r_flat, rowvar=False)
        mat = _compute_mkl_matrix(cov_s, cov_r)
        return AffineTransferModel(
            method_name=chosen,
            src_mean=mu_s,
            ref_mean=mu_r,
            transform=mat,
        )
    if chosen == "reinhard":
        mean_s = source.reshape(-1, 3).astype(np.float64) / 255.0
        mean_r = reference.reshape(-1, 3).astype(np.float64) / 255.0
        std_s = np.std(mean_s, axis=0) + 1e-6
        std_r = np.std(mean_r, axis=0) + 1e-6
        return ReinhardTransferModel(
            mean_s=mean_s.mean(axis=0),
            mean_r=mean_r.mean(axis=0),
            std_ratios=std_r / std_s,
        )
    if chosen == "hm":
        s_flat = source.reshape(-1, 3).astype(np.float64) / 255.0
        r_flat = reference.reshape(-1, 3).astype(np.float64) / 255.0
        src_values = []
        mapped_values = []
        for ch in range(3):
            s_vals, s_cnts = np.unique(source[..., ch].ravel(), return_counts=True)
            r_vals, r_cnts = np.unique(reference[..., ch].ravel(), return_counts=True)
            s_cdf = np.cumsum(s_cnts).astype(np.float64) / source[..., ch].size
            r_cdf = np.cumsum(r_cnts).astype(np.float64) / reference[..., ch].size
            mapped = np.interp(s_cdf, r_cdf, r_vals) / 255.0
            src_values.append(s_vals.astype(np.float64) / 255.0)
            mapped_values.append(mapped)
        return HistogramTransferModel(
            src_values=(src_values[0], src_values[1], src_values[2]),
            mapped_values=(mapped_values[0], mapped_values[1], mapped_values[2]),
        )
    raise ValueError(f"Unsupported color transfer method '{method}'")
