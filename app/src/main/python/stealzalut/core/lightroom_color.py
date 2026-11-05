"""Colorimetric Processing Engine for Lightroom Classic Panels."""

from __future__ import annotations

import numpy as np
from numpy.typing import NDArray
from typing import List, Tuple

from stealzalut.core.color_space import linear_to_srgb, srgb_to_linear, smoothstep
from stealzalut.core.lightroom_models import (
    BasicSettings, CalibrationSettings, ColorGradingSettings,
    ColorMixerSettings, LightroomDevelopSettings, ToneCurveSettings,
)
from stealzalut.core.spline_interpolator import evaluate_monotone_cubic_spline

EPSILON: float = 1e-6


# ---------------------------------------------------------------------------
# 1. Basic Adjustments
# ---------------------------------------------------------------------------

def apply_basic_adjustments(rgb: NDArray[np.float32], s: BasicSettings) -> NDArray[np.float32]:
    """Applique les réglages de base du panneau 1."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    # A. Exposition en espace linéaire EV
    if abs(s.exposure) > 1e-4:
        lin = srgb_to_linear(out)
        lin *= (2.0 ** s.exposure)
        out = linear_to_srgb(lin)

    # B. Balance des blancs (Température & Teinte)
    if abs(s.temp) > 1e-4 or abs(s.tint) > 1e-4:
        r, g, b = out[..., 0], out[..., 1], out[..., 2]
        t = s.temp / 100.0
        ti = s.tint / 100.0
        r = r * (1.0 + 0.28 * t + 0.08 * ti)
        g = g * (1.0 - 0.18 * ti)
        b = b * (1.0 - 0.28 * t + 0.08 * ti)
        out = np.clip(np.stack([r, g, b], axis=-1), 0.0, 1.0)

    # C. Tonalité : Contrast, Highlights, Shadows, Whites, Blacks
    luma = 0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
    luma_3d = luma[..., np.newaxis]

    # Contrast sigmoïdal
    if abs(s.contrast) > 1e-4:
        centered = out - 0.5
        c_val = s.contrast / 100.0
        if c_val > 0.0:
            factor = 1.0 + c_val * 0.9
            norm = 2.0 * np.tanh(0.75 * factor)
            out = 0.5 + np.tanh(centered * 1.5 * factor) / norm
        else:
            out = 0.5 + centered * (1.0 + c_val * 0.6)
        out = np.clip(out, 0.0, 1.0)

    # Highlights & Shadows
    if abs(s.highlights) > 1e-4:
        h_weight = smoothstep(0.35, 0.95, luma_3d) ** 1.3
        out += (s.highlights / 100.0) * 0.32 * h_weight * out
        out = np.clip(out, 0.0, 1.0)

    if abs(s.shadows) > 1e-4:
        s_weight = (1.0 - smoothstep(0.05, 0.65, luma_3d)) ** 1.3
        out += (s.shadows / 100.0) * 0.30 * s_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    # Whites & Blacks (épaules et talons extrêmes)
    if abs(s.whites) > 1e-4:
        w_weight = smoothstep(0.7, 1.0, luma_3d) ** 2.0
        out += (s.whites / 100.0) * 0.25 * w_weight * (1.0 - out)
        out = np.clip(out, 0.0, 1.0)

    if abs(s.blacks) > 1e-4:
        b_weight = (1.0 - smoothstep(0.0, 0.3, luma_3d)) ** 2.0
        out += (s.blacks / 100.0) * 0.25 * b_weight * out
        out = np.clip(out, 0.0, 1.0)

    # D. Vibrance & Saturation
    if abs(s.vibrance) > 1e-4 or abs(s.saturation) > 1e-4:
        r, g, b = out[..., 0], out[..., 1], out[..., 2]
        max_c = np.maximum(np.maximum(r, g), b)
        min_c = np.minimum(np.minimum(r, g), b)
        delta = max_c - min_c
        sat = np.where(max_c > 1e-5, delta / np.maximum(max_c, 1e-5), 0.0)
        lum = 0.299 * r + 0.587 * g + 0.114 * b

        v_boost = (s.vibrance / 100.0) * (1.0 - sat) * 0.65
        s_boost = (s.saturation / 100.0) * 0.55
        total_boost = 1.0 + v_boost + s_boost

        out = np.stack([
            lum + (r - lum) * total_boost,
            lum + (g - lum) * total_boost,
            lum + (b - lum) * total_boost,
        ], axis=-1)
        out = np.clip(out, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# 2. Tone Curve
# ---------------------------------------------------------------------------

def apply_tone_curve(rgb: NDArray[np.float32], s: ToneCurveSettings) -> NDArray[np.float32]:
    """Applique la courbe des tonalités du panneau 2."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    if s.mode == "parametric":
        # Points pivots
        s_split = np.clip(s.shadow_split / 100.0, 0.05, 0.45)
        m_split = np.clip(s.midtone_split / 100.0, s_split + 0.05, 0.85)
        h_split = np.clip(s.highlight_split / 100.0, m_split + 0.05, 0.95)

        # Delta de déplacement sur chaque zone
        d_shadows = (s.shadows / 100.0) * 0.15
        d_darks = (s.darks / 100.0) * 0.18
        d_lights = (s.lights / 100.0) * 0.18
        d_highlights = (s.highlights / 100.0) * 0.15

        x_nodes = np.array([0.0, s_split, m_split, h_split, 1.0], dtype=np.float32)
        y_nodes = np.array([
            0.0,
            np.clip(s_split + d_shadows + 0.5 * d_darks, 0.01, 0.90),
            np.clip(m_split + 0.5 * d_darks + 0.5 * d_lights, 0.05, 0.95),
            np.clip(h_split + 0.5 * d_lights + d_highlights, 0.10, 0.99),
            1.0,
        ], dtype=np.float32)

        out = evaluate_monotone_cubic_spline(x_nodes, y_nodes, out)

    elif s.mode == "point":
        # Canal composite Global (RVB)
        if len(s.rgb_points) >= 2:
            pts = sorted(s.rgb_points, key=lambda p: p[0])
            x_pts = np.array([p[0] for p in pts], dtype=np.float32)
            y_pts = np.array([p[1] for p in pts], dtype=np.float32)
            out = evaluate_monotone_cubic_spline(x_pts, y_pts, out)

        # Canaux individuels R, G, B
        for ch, pts_list in enumerate([s.red_points, s.green_points, s.blue_points]):
            if len(pts_list) >= 2:
                pts = sorted(pts_list, key=lambda p: p[0])
                x_pts = np.array([p[0] for p in pts], dtype=np.float32)
                y_pts = np.array([p[1] for p in pts], dtype=np.float32)
                out[..., ch] = evaluate_monotone_cubic_spline(x_pts, y_pts, out[..., ch])

    return np.clip(out, 0.0, 1.0).astype(np.float32)


# ---------------------------------------------------------------------------
# 3. Color Mixer (TSL / HSL & Point Color)
# ---------------------------------------------------------------------------

def _rgb_to_hsl(rgb: NDArray[np.float32]) -> Tuple[NDArray[np.float32], NDArray[np.float32], NDArray[np.float32]]:
    """Convertit RGB [0, 1] vers H [0, 360], S [0, 1], L [0, 1]."""
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    cmax = np.maximum(np.maximum(r, g), b)
    cmin = np.minimum(np.minimum(r, g), b)
    delta = cmax - cmin

    lum = 0.5 * (cmax + cmin)
    sat = np.zeros_like(lum)
    denom = 1.0 - np.abs(2.0 * lum - 1.0)
    nonzero = (denom > EPSILON) & (delta > EPSILON)
    sat[nonzero] = delta[nonzero] / denom[nonzero]

    hue = np.zeros_like(lum)
    r_max = (cmax == r) & (delta > EPSILON)
    g_max = (cmax == g) & (delta > EPSILON)
    b_max = (cmax == b) & (delta > EPSILON)

    hue[r_max] = 60.0 * (((g[r_max] - b[r_max]) / delta[r_max]) % 6.0)
    hue[g_max] = 60.0 * (((b[g_max] - r[g_max]) / delta[g_max]) + 2.0)
    hue[b_max] = 60.0 * (((r[b_max] - g[b_max]) / delta[b_max]) + 4.0)
    hue = np.clip(hue % 360.0, 0.0, 360.0)

    return hue, sat, lum


def _hsl_to_rgb(h: NDArray[np.float32], s: NDArray[np.float32], l: NDArray[np.float32]) -> NDArray[np.float32]:
    """Convertit H [0, 360], S [0, 1], L [0, 1] vers RGB [0, 1]."""
    c = (1.0 - np.abs(2.0 * l - 1.0)) * s
    h_prime = h / 60.0
    x = c * (1.0 - np.abs((h_prime % 2.0) - 1.0))
    m = l - c / 2.0

    r, g, b = np.zeros_like(h), np.zeros_like(h), np.zeros_like(h)
    m0, m1, m2 = (h_prime < 1.0), (h_prime >= 1.0) & (h_prime < 2.0), (h_prime >= 2.0) & (h_prime < 3.0)
    m3, m4, m5 = (h_prime >= 3.0) & (h_prime < 4.0), (h_prime >= 4.0) & (h_prime < 5.0), (h_prime >= 5.0)

    r[m0], g[m0], b[m0] = c[m0], x[m0], 0.0
    r[m1], g[m1], b[m1] = x[m1], c[m1], 0.0
    r[m2], g[m2], b[m2] = 0.0, c[m2], x[m2]
    r[m3], g[m3], b[m3] = 0.0, x[m3], c[m3]
    r[m4], g[m4], b[m4] = x[m4], 0.0, c[m4]
    r[m5], g[m5], b[m5] = c[m5], 0.0, x[m5]

    return np.clip(np.stack([r + m, g + m, b + m], axis=-1), 0.0, 1.0).astype(np.float32)


def apply_color_mixer(rgb: NDArray[np.float32], s: ColorMixerSettings) -> NDArray[np.float32]:
    """Applique le mélangeur de couleurs TSL / HSL et Point Color."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    h, sat, lum = _rgb_to_hsl(out)

    if s.mode == "hsl":
        # 8 centres angulaires HSL
        centers = [0.0, 30.0, 60.0, 120.0, 180.0, 240.0, 280.0, 320.0]
        hues = [s.hue_red, s.hue_orange, s.hue_yellow, s.hue_green, s.hue_aqua, s.hue_blue, s.hue_purple, s.hue_magenta]
        sats = [s.sat_red, s.sat_orange, s.sat_yellow, s.sat_green, s.sat_aqua, s.sat_blue, s.sat_purple, s.sat_magenta]
        lums = [s.lum_red, s.lum_orange, s.lum_yellow, s.lum_green, s.lum_aqua, s.lum_blue, s.lum_purple, s.lum_magenta]

        total_dh = np.zeros_like(h)
        total_ds = np.zeros_like(sat)
        total_dl = np.zeros_like(lum)
        total_w = np.zeros_like(h)
        sigma = 22.0  # Largeur de fenêtre angulaire

        for c_angle, dh_val, ds_val, dl_val in zip(centers, hues, sats, lums):
            if abs(dh_val) < 1e-4 and abs(ds_val) < 1e-4 and abs(dl_val) < 1e-4:
                continue
            ang_diff = np.abs(h - c_angle)
            ang_diff = np.minimum(ang_diff, 360.0 - ang_diff)
            weight = np.exp(-0.5 * (ang_diff / sigma) ** 2)
            total_dh += weight * (dh_val / 100.0) * 30.0  # max +/- 30 deg hue rotation
            total_ds += weight * (ds_val / 100.0)
            total_dl += weight * (dl_val / 100.0) * 0.4
            total_w += weight

        norm = np.maximum(total_w, 1.0)
        h = (h + total_dh / norm) % 360.0
        sat = np.clip(sat + (total_ds / norm) * sat, 0.0, 1.0)
        lum = np.clip(lum + (total_dl / norm) * lum * (1.0 - lum) * 2.0, 0.0, 1.0)
        return _hsl_to_rgb(h, sat, lum)

    elif s.mode == "point_color" and s.point_active:
        # Distance dans l'espace HSL vers la cible
        target_rgb = np.array([s.point_target_r, s.point_target_g, s.point_target_b], dtype=np.float32)
        th, ts, tl = _rgb_to_hsl(target_rgb.reshape((1, 1, 3)))
        th, ts, tl = float(th[0, 0]), float(ts[0, 0]), float(tl[0, 0])

        dh = np.abs(h - th)
        dh = np.minimum(dh, 360.0 - dh) / 180.0
        ds = np.abs(sat - ts)
        dl = np.abs(lum - tl)
        dist = np.sqrt(1.5 * dh**2 + ds**2 + dl**2)

        radius = max(0.05, (s.point_range / 100.0) * 0.75)
        mask = 1.0 - smoothstep(0.0, radius, dist)

        h = (h + mask * (s.point_hue_shift / 100.0) * 45.0) % 360.0
        sat = np.clip(sat + mask * (s.point_sat_shift / 100.0) * sat, 0.0, 1.0)
        lum = np.clip(lum + mask * (s.point_lum_shift / 100.0) * 0.35, 0.0, 1.0)
        return _hsl_to_rgb(h, sat, lum)

    return out


# ---------------------------------------------------------------------------
# 4. Color Grading
# ---------------------------------------------------------------------------

def apply_color_grading(rgb: NDArray[np.float32], s: ColorGradingSettings) -> NDArray[np.float32]:
    """Applique les 3 roues chromatiques (Ombres, Moyens, Hautes) + Global."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)
    lum = 0.2126 * out[..., 0] + 0.7152 * out[..., 1] + 0.0722 * out[..., 2]
    lum_3d = lum[..., np.newaxis]

    # Balance & Blending
    pivot = 0.5 + 0.25 * (s.balance / 100.0)
    blend_width = max(0.1, (s.blending / 100.0) * 0.5)

    w_shad = np.clip((pivot - lum_3d) / blend_width, 0.0, 1.0) ** 1.5
    w_high = np.clip((lum_3d - pivot) / blend_width, 0.0, 1.0) ** 1.5
    w_mid = np.clip(1.0 - w_shad - w_high, 0.0, 1.0)

    def get_tint_vector(hue_deg: float, sat_percent: float) -> NDArray[np.float32]:
        if sat_percent < 1e-4:
            return np.zeros(3, dtype=np.float32)
        rad = np.radians(hue_deg)
        intensity = (sat_percent / 100.0) * 0.35
        # Vecteur chromatique RVB normalisé
        r_tint = np.cos(rad)
        g_tint = np.cos(rad - 2.0 * np.pi / 3.0)
        b_tint = np.cos(rad - 4.0 * np.pi / 3.0)
        return np.array([r_tint, g_tint, b_tint], dtype=np.float32) * intensity

    v_shad = get_tint_vector(s.shadow_hue, s.shadow_sat)
    v_mid = get_tint_vector(s.midtone_hue, s.midtone_sat)
    v_high = get_tint_vector(s.highlight_hue, s.highlight_sat)
    v_glob = get_tint_vector(s.global_hue, s.global_sat)

    # Teinte cumulée
    tint = (w_shad * v_shad) + (w_mid * v_mid) + (w_high * v_high) + v_glob
    out = np.clip(out + tint * out * (1.0 - out) * 4.0, 0.0, 1.0)

    # Curseurs de luminance des roues
    lum_delta = (
        w_shad * (s.shadow_lum / 100.0) * 0.3
        + w_mid * (s.midtone_lum / 100.0) * 0.3
        + w_high * (s.highlight_lum / 100.0) * 0.3
        + (s.global_lum / 100.0) * 0.25
    )
    out = np.clip(out + lum_delta * out * (1.0 - out) * 2.5, 0.0, 1.0)

    return out.astype(np.float32)


# ---------------------------------------------------------------------------
# 5. Calibration
# ---------------------------------------------------------------------------

def apply_calibration(rgb: NDArray[np.float32], s: CalibrationSettings) -> NDArray[np.float32]:
    """Applique l'étalonnage des primaires et la teinte des ombres (Panneau 10)."""
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    # Teinte des ombres (vert <-> magenta dans les noirs profonds)
    if abs(s.shadow_tint) > 1e-4:
        lum = 0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]
        shadow_mask = (1.0 - smoothstep(0.0, 0.25, lum))[..., np.newaxis]
        tint_val = (s.shadow_tint / 100.0) * 0.12
        # Magenta = +R +B, -G ; Vert = +G, -R -B
        out[..., 0] = np.clip(out[..., 0] + tint_val * shadow_mask[..., 0] * 0.5, 0.0, 1.0)
        out[..., 1] = np.clip(out[..., 1] - tint_val * shadow_mask[..., 0], 0.0, 1.0)
        out[..., 2] = np.clip(out[..., 2] + tint_val * shadow_mask[..., 0] * 0.5, 0.0, 1.0)

    # Rotation et saturation des primaires RVB en espace linéaire
    lin = srgb_to_linear(out)

    # Matrice de déviation des primaires
    dr_h = (s.red_hue / 100.0) * 0.20
    dr_s = (s.red_sat / 100.0) * 0.40

    dg_h = (s.green_hue / 100.0) * 0.20
    dg_s = (s.green_sat / 100.0) * 0.40

    db_h = (s.blue_hue / 100.0) * 0.20
    db_s = (s.blue_sat / 100.0) * 0.40

    m = np.eye(3, dtype=np.float32)
    # Red primary modifies G and B components
    m[1, 0] += dr_h * 0.15
    m[0, 0] += dr_s * 0.25
    # Green primary modifies R and B
    m[0, 1] += dg_h * 0.15
    m[1, 1] += dg_s * 0.25
    # Blue primary modifies R and G
    m[0, 2] += db_h * 0.15
    m[2, 2] += db_s * 0.25

    # Normalize rows to conserve neutral white
    row_sums = np.maximum(m.sum(axis=1, keepdims=True), EPSILON)
    m /= row_sums

    # Tensor contraction along channel axis
    lin_calibrated = np.tensordot(lin, m.T, axes=1)
    return linear_to_srgb(np.clip(lin_calibrated, 0.0, 1.0)).astype(np.float32)


# ---------------------------------------------------------------------------
# Pipeline Colorimétrique Intégré (Bypass toggles inclus)
# ---------------------------------------------------------------------------

def apply_full_lightroom_color_pipeline(
    rgb: NDArray[np.float32],
    settings: LightroomDevelopSettings,
) -> NDArray[np.float32]:
    """Exécute l'ensemble des panneaux colorimétriques dans l'ordre Lightroom Classic.

    Ordre natif : Étalonnage -> Réglages de base -> Courbe -> TSL -> Color Grading.
    Chaque section est contournée si son toggle 'Œil' correspondant est désactivé.
    """
    out = np.clip(rgb.astype(np.float32), 0.0, 1.0)

    # 1. Étalonnage (Calibration)
    if settings.is_calibration_enabled:
        out = apply_calibration(out, settings.calibration)

    # 2. Réglages de base (Basic)
    if settings.is_basic_enabled:
        out = apply_basic_adjustments(out, settings.basic)

    # 3. Courbe des tonalités (Tone Curve)
    if settings.is_tone_curve_enabled:
        out = apply_tone_curve(out, settings.tone_curve)

    # 4. Mélangeur de couleurs (Color Mixer)
    if settings.is_color_mixer_enabled:
        out = apply_color_mixer(out, settings.color_mixer)

    # 5. Color Grading
    if settings.is_color_grading_enabled:
        out = apply_color_grading(out, settings.color_grading)

    return np.clip(out, 0.0, 1.0).astype(np.float32)
