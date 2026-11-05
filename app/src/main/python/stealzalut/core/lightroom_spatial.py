"""Spatial and Geometric Processing Engine for Lightroom Classic Panels.

Handles 2D spatial image operations using pure NumPy and SciPy:
- Presence (Texture, Clarity, Dehaze from Panel 1)
- Detail (Panel 5 : Sharpening with Edge Masking & Noise Reduction)
- Lens Corrections (Panel 6 : Distortion, Chromatic Aberration, Lens Vignetting)
- Transform (Panel 7 : Perspective Homography, Rotation, Scale, Offsets)
- Lens Blur (Panel 8 : Depth-aware synthetic blur, Bokeh styles, Highlight Boost)
- Effects (Panel 9 : Post-crop Vignette & Photographic Film Grain)
"""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray

from stealzalut.core.color_space import oklab_to_rgb, rgb_to_oklab, smoothstep
from stealzalut.core.lightroom_models import (
    BasicSettings,
    DetailSettings,
    EffectsSettings,
    LensBlurSettings,
    LensCorrectionsSettings,
    LightroomDevelopSettings,
    TransformSettings,
)
from stealzalut.core.spatial_filters import (
    filter_2d,
    gaussian_blur_2d,
    laplacian_2d,
    min_filter_2d,
    remap_2d,
    resize_2d,
    sobel_magnitude_2d,
)

EPSILON: float = 1e-6


# ---------------------------------------------------------------------------
# 1. Presence (Texture, Clarity, Dehaze)
# ---------------------------------------------------------------------------

def apply_presence_spatial(rgb: NDArray[np.float32], s: BasicSettings) -> NDArray[np.float32]:
    """Applique les filtres spatiaux de Présence : Texture, Clarté, Dehaze."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]
    if min(h, w) < 16:
        return out

    # A. Texture : Rehaussement de fréquences moyennes (Passe-bande DoG)
    if abs(s.texture) > 1e-4:
        blur_fine = gaussian_blur_2d(out, 1.2)
        blur_medium = gaussian_blur_2d(out, 3.5)
        mid_freq = blur_fine - blur_medium
        out += (s.texture / 100.0) * 0.45 * mid_freq
        out = np.clip(out, 0.0, 1.0)

    # B. Clarté : Contraste local centré sur les tons moyens
    if abs(s.clarity) > 1e-4:
        sigma_clarity = max(3.0, min(h, w) / 40.0)
        blur_clarity = gaussian_blur_2d(out, sigma_clarity)
        high_pass = out - blur_clarity
        lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
        midtone_w = np.clip(1.0 - 4.0 * (lum - 0.5) ** 2, 0.0, 1.0)[..., np.newaxis]
        out += (s.clarity / 100.0) * 0.40 * high_pass * midtone_w
        out = np.clip(out, 0.0, 1.0)

    # C. Correction du voile (Dehaze) : Dark Channel Prior
    if abs(s.dehaze) > 1e-4:
        d_val = s.dehaze / 100.0
        dark_ch = np.min(out, axis=2)
        k_size = max(5, int(min(h, w) / 30) | 1)
        dark_min = min_filter_2d(dark_ch, k_size)
        trans = np.clip(1.0 - 0.95 * dark_min, 0.1, 1.0)[..., np.newaxis]
        atm_light = np.percentile(out, 99.5, axis=(0, 1))

        if d_val > 0.0:
            out = (out - atm_light) / np.maximum(trans ** (d_val * 0.8), 0.15) + atm_light
        else:
            fog = np.abs(d_val) * (1.0 - trans) * 0.6
            out = out * (1.0 - fog) + atm_light * fog
        out = np.clip(out, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# 2. Detail (Netteté & Réduction du Bruit)
# ---------------------------------------------------------------------------

def apply_detail_spatial(rgb: NDArray[np.float32], s: DetailSettings) -> NDArray[np.float32]:
    """Applique la netteté avec masquage de contours et le débruitage via OKLab."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]
    if min(h, w) < 16:
        return out

    # A. Réduction du bruit (Luminance & Couleur)
    if s.nr_luminance > 1e-4 or s.nr_color > 1e-4:
        oklab = rgb_to_oklab(out)
        l_ch = oklab[..., 0]
        a_ch = oklab[..., 1]
        b_ch = oklab[..., 2]

        if s.nr_luminance > 1e-4:
            sigma_s = 1.0 + (s.nr_luminance / 100.0) * 3.5
            l_blur = gaussian_blur_2d(l_ch, sigma_s)
            diff = np.abs(l_ch - l_blur)
            threshold = 0.04 + 0.12 * (1.0 - s.nr_lum_detail / 100.0)
            edge_weight = smoothstep(threshold * 0.5, threshold * 1.5, diff)
            alpha_l = (s.nr_luminance / 100.0) * 0.85
            oklab[..., 0] = (1.0 - alpha_l * (1.0 - edge_weight)) * l_ch + alpha_l * (1.0 - edge_weight) * l_blur

        if s.nr_color > 1e-4:
            sigma_c = 1.0 + (s.nr_color / 100.0) * 4.0
            oklab[..., 1] = gaussian_blur_2d(a_ch, sigma_c)
            oklab[..., 2] = gaussian_blur_2d(b_ch, sigma_c)

        out = np.clip(oklab_to_rgb(oklab), 0.0, 1.0)

    # B. Netteté (Sharpening avec rayon, détail et masque de contours)
    if s.sharpen_amount > 1e-4:
        rad = max(0.5, float(s.sharpen_radius))
        blur = gaussian_blur_2d(out, rad)
        high_pass = out - blur

        if s.sharpen_masking > 1e-4:
            gray = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
            grad_mag = sobel_magnitude_2d(gray)
            thresh = (s.sharpen_masking / 100.0) * 0.4
            mask = smoothstep(thresh * 0.5, thresh + 0.1, grad_mag)[..., np.newaxis]
        else:
            mask = np.ones_like(high_pass[..., :1])

        detail_weight = 0.5 + 0.5 * (s.sharpen_detail / 100.0)
        gain = (s.sharpen_amount / 100.0) * 0.70 * detail_weight
        out += gain * high_pass * mask
        out = np.clip(out, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# 3. Lens Corrections
# ---------------------------------------------------------------------------

def apply_lens_corrections_spatial(rgb: NDArray[np.float32], s: LensCorrectionsSettings) -> NDArray[np.float32]:
    """Applique les corrections de distorsion, aberrations chromatiques et vignettage."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]

    # A. Distorsion géométrique (modèle radial polynomial Brown-Conrady)
    if abs(s.distortion) > 1e-4:
        cx, cy = w / 2.0, h / 2.0
        k = (s.distortion / 100.0) * 0.25
        grid_y, grid_x = np.indices((h, w), dtype=np.float32)
        norm_x = (grid_x - cx) / cx
        norm_y = (grid_y - cy) / cy
        r2 = norm_x**2 + norm_y**2
        radial_factor = 1.0 + k * r2

        map_x = cx + norm_x * radial_factor * cx
        map_y = cy + norm_y * radial_factor * cy
        out = remap_2d(out, map_x, map_y)

    # B. Aberrations chromatiques (décalage radial des canaux R et B par rapport au vert G)
    if s.remove_ca or abs(s.ca_red_cyan) > 1e-4 or abs(s.ca_blue_yellow) > 1e-4:
        shift_r = (s.ca_red_cyan / 100.0) * 2.0 if not s.remove_ca else 0.8
        shift_b = (s.ca_blue_yellow / 100.0) * 2.0 if not s.remove_ca else -0.8

        cx, cy = w / 2.0, h / 2.0
        grid_y, grid_x = np.indices((h, w), dtype=np.float32)

        scale_r = 1.0 + (shift_r / max(w, h))
        scale_b = 1.0 + (shift_b / max(w, h))

        map_xr = np.clip(cx + (grid_x - cx) * scale_r, 0, w - 1)
        map_yr = np.clip(cy + (grid_y - cy) * scale_r, 0, h - 1)
        map_xb = np.clip(cx + (grid_x - cx) * scale_b, 0, w - 1)
        map_yb = np.clip(cy + (grid_y - cy) * scale_b, 0, h - 1)

        out[..., 0] = remap_2d(out[..., 0], map_xr, map_yr)
        out[..., 2] = remap_2d(out[..., 2], map_xb, map_yb)

    # C. Vignettage optique de l'objectif
    if abs(s.vignette_amount) > 1e-4:
        cx, cy = w / 2.0, h / 2.0
        grid_y, grid_x = np.indices((h, w), dtype=np.float32)
        norm_r = np.sqrt(((grid_x - cx) / cx) ** 2 + ((grid_y - cy) / cy) ** 2) / np.sqrt(2.0)
        mid = np.clip(s.vignette_midpoint / 100.0, 0.1, 0.9)
        vig_factor = 1.0 + (s.vignette_amount / 100.0) * smoothstep(mid * 0.5, 1.0, norm_r)[..., np.newaxis]
        out = np.clip(out * vig_factor, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# 4. Transform (Homographie / Perspective géométrique)
# ---------------------------------------------------------------------------

def apply_transform_spatial(rgb: NDArray[np.float32], s: TransformSettings) -> NDArray[np.float32]:
    """Applique les redressements de perspective, rotation, échelle et translation."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]

    if (
        abs(s.vertical) < 1e-4
        and abs(s.horizontal) < 1e-4
        and abs(s.rotate) < 1e-4
        and abs(s.aspect) < 1e-4
        and abs(s.scale - 100.0) < 1e-4
        and abs(s.offset_x) < 1e-4
        and abs(s.offset_y) < 1e-4
    ):
        return out

    cx, cy = w / 2.0, h / 2.0
    scale_factor = np.clip(s.scale / 100.0, 0.5, 2.0)
    theta = np.radians(-s.rotate)

    grid_y, grid_x = np.indices((h, w), dtype=np.float32)
    x1 = grid_x - cx - (s.offset_x / 100.0) * cx
    y1 = grid_y - cy - (s.offset_y / 100.0) * cy

    x2 = (x1 * np.cos(-theta) - y1 * np.sin(-theta)) / scale_factor
    y2 = (x1 * np.sin(-theta) + y1 * np.cos(-theta)) / scale_factor

    vert = (s.vertical / 100.0) * 0.25
    horiz = (s.horizontal / 100.0) * 0.25
    aspect = (s.aspect / 100.0) * 0.25

    ny = y2 / max(cy, 1.0)
    nx = x2 / max(cx, 1.0)
    denom_y = np.maximum(1.0 + horiz * nx, 0.2)
    denom_x = np.maximum(1.0 + vert * ny, 0.2)

    src_x = cx + (x2 / max(1.0 + aspect, 0.1)) / denom_x
    src_y = cy + y2 / denom_y

    transformed = remap_2d(out, src_x.astype(np.float32), src_y.astype(np.float32))
    return np.clip(transformed, 0.0, 1.0).astype(np.float32)


# ---------------------------------------------------------------------------
# 5. Lens Blur (Flou de l'objectif & Styles de Bokeh)
# ---------------------------------------------------------------------------

def apply_lens_blur_spatial(rgb: NDArray[np.float32], s: LensBlurSettings) -> NDArray[np.float32]:
    """Applique le flou synthétique de profondeur avec style de bokeh et boost spéculaire."""
    if not s.enabled or s.blur_amount < 1e-4:
        return rgb.copy()

    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]

    grid_y, _ = np.indices((h, w), dtype=np.float32)
    center_y = (s.focal_range / 100.0) * h
    depth_dist = np.abs(grid_y - center_y) / max(h, 1.0)

    k_radius = max(3, int((s.blur_amount / 100.0) * 15))
    k_size = 2 * k_radius + 1
    ky, kx = np.indices((k_size, k_size), dtype=np.float32) - k_radius
    kr = np.sqrt(kx**2 + ky**2)

    if s.bokeh_style == "bubble":
        kernel = np.where(kr <= k_radius, np.clip((kr / k_radius) ** 2, 0.2, 1.0), 0.0)
    elif s.bokeh_style == "ring":
        kernel = np.where((kr <= k_radius) & (kr >= k_radius * 0.6), 1.0, 0.0)
    elif s.bokeh_style in ("blade5", "blade7"):
        n_blades = 5 if s.bokeh_style == "blade5" else 7
        angle = np.arctan2(ky, kx)
        r_poly = k_radius * np.cos(np.pi / n_blades) / np.cos((angle % (2.0 * np.pi / n_blades)) - np.pi / n_blades)
        kernel = np.where(kr <= r_poly, 1.0, 0.0)
    else:
        kernel = np.where(kr <= k_radius, 1.0, 0.0)

    k_sum = np.sum(kernel)
    if k_sum > EPSILON:
        kernel /= k_sum
    else:
        kernel = np.ones((k_size, k_size), dtype=np.float32) / (k_size * k_size)

    if s.bokeh_boost > 1e-4:
        boost_mask = smoothstep(0.7, 1.0, 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2])
        out += (s.bokeh_boost / 100.0) * 0.35 * boost_mask[..., np.newaxis]

    blurred = filter_2d(out, kernel)
    blur_weight = smoothstep(0.08, 0.40, depth_dist)[..., np.newaxis]
    res = (1.0 - blur_weight) * out + blur_weight * blurred
    return np.clip(res, 0.0, 1.0).astype(np.float32)


# ---------------------------------------------------------------------------
# 6. Effects (Vignette Post-Recadrage & Grain)
# ---------------------------------------------------------------------------

def apply_effects_spatial(rgb: NDArray[np.float32], s: EffectsSettings) -> NDArray[np.float32]:
    """Applique la vignette après recadrage et le grain photographique réaliste."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, w = out.shape[:2]

    # A. Vignette après recadrage (Post-Crop Vignette)
    if abs(s.vignette_amount) > 1e-4:
        cx, cy = w / 2.0, h / 2.0
        grid_y, grid_x = np.indices((h, w), dtype=np.float32)

        roundness = s.vignette_roundness / 100.0
        nx = np.abs((grid_x - cx) / cx)
        ny = np.abs((grid_y - cy) / cy)
        p = 2.0 + roundness * 1.5
        norm_r = (nx**p + ny**p) ** (1.0 / p)

        mid = np.clip(s.vignette_midpoint / 100.0, 0.05, 0.95)
        feather = np.clip(s.vignette_feather / 100.0, 0.05, 0.95)
        vig_falloff = smoothstep(mid * (1.0 - 0.5 * feather), mid + 0.5 * feather, norm_r)[..., np.newaxis]

        lum = 0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
        hl_protection = (s.vignette_highlights / 100.0) * smoothstep(0.6, 1.0, lum)[..., np.newaxis]

        vig_mask = vig_falloff * (1.0 - hl_protection)
        if s.vignette_amount < 0.0:
            out *= (1.0 + (s.vignette_amount / 100.0) * vig_mask)
        else:
            out = out + (s.vignette_amount / 100.0) * (1.0 - out) * vig_mask
        out = np.clip(out, 0.0, 1.0)

    # B. Grain photographique argentique
    if s.grain_amount > 1e-4:
        lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
        grain_weight = np.clip(4.0 * lum * (1.0 - lum), 0.0, 1.0)[..., np.newaxis]

        scale_size = max(1, int(1 + (s.grain_size / 100.0) * 4))
        small_h = max(4, h // scale_size)
        small_w = max(4, w // scale_size)

        rng = np.random.RandomState(42)
        raw_noise = rng.normal(0.0, 0.10 * (s.grain_amount / 100.0), size=(small_h, small_w, 3)).astype(np.float32)

        if s.grain_roughness > 50.0:
            k = laplacian_2d(raw_noise)
            raw_noise += (s.grain_roughness - 50.0) / 50.0 * 0.3 * k

        noise_resized = resize_2d(raw_noise, w, h)
        out = np.clip(out + noise_resized * grain_weight, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# Pipeline Spatial Intégré (Bypass toggles inclus)
# ---------------------------------------------------------------------------

def apply_full_lightroom_spatial_pipeline(
    rgb: NDArray[np.float32],
    settings: LightroomDevelopSettings,
) -> NDArray[np.float32]:
    """Exécute les opérations spatiales et géométriques avec contournement par section.

    Ordre natif : Lens Corrections -> Transform -> Presence -> Detail -> Lens Blur -> Effects.
    """
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    if settings.is_lens_corrections_enabled:
        out = apply_lens_corrections_spatial(out, settings.lens_corrections)

    if settings.is_transform_enabled:
        out = apply_transform_spatial(out, settings.transform)

    if settings.is_basic_enabled:
        out = apply_presence_spatial(out, settings.basic)

    if settings.is_detail_enabled:
        out = apply_detail_spatial(out, settings.detail)

    if settings.is_lens_blur_enabled:
        out = apply_lens_blur_spatial(out, settings.lens_blur)

    if settings.is_effects_enabled:
        out = apply_effects_spatial(out, settings.effects)

    return np.clip(out, 0.0, 1.0).astype(np.float32)
