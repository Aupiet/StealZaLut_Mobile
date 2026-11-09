"""Lightroom Classic Develop Settings Models and Specifications.

Defines typed dataclasses for the 10 Adobe Lightroom Classic develop panels:
1. Basic Adjustments (Balance des blancs, Tonalité, Présence)
2. Tone Curve (Paramétrique & Courbe à points RGB/R/G/B)
3. Color Mixer / TSL & Point Color (8 plages TSL + Pipette Point Color)
4. Color Grading (3 Roues chromatiques + Global + Blending & Balance)
5. Detail (Netteté & Réduction du bruit)
6. Lens Corrections (Distorsion, Aberration chromatique, Vignettage)
7. Transform (Distorsion géométrique, Perspective, Échelle)
8. Lens Blur (Flou de profondeur synthétique & Styles de Bokeh)
9. Effects (Vignette post-recadrage & Grain photographique)
10. Calibration (Étalonnage des primaires RVB & Teinte des ombres)
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from typing import Any, Dict, List, Optional, Tuple


@dataclass
class BasicSettings:
    """Panneau 1 : Réglages de base (Basic Adjustments)."""

    # Balance des blancs
    temp: float = 0.0  # [-100.0, +100.0] (Bleu <-> Jaune)
    tint: float = 0.0  # [-100.0, +100.0] (Vert <-> Magenta)
    # Tonalité
    exposure: float = 0.0  # [-5.0, +5.0] EV
    contrast: float = 0.0  # [-100.0, +100.0]
    highlights: float = 0.0  # [-100.0, +100.0]
    shadows: float = 0.0  # [-100.0, +100.0]
    whites: float = 0.0  # [-100.0, +100.0]
    blacks: float = 0.0  # [-100.0, +100.0]
    # Présence
    texture: float = 0.0  # [-100.0, +100.0]
    clarity: float = 0.0  # [-100.0, +100.0]
    dehaze: float = 0.0  # [-100.0, +100.0]
    vibrance: float = 0.0  # [-100.0, +100.0]
    saturation: float = 0.0  # [-100.0, +100.0]


@dataclass
class ToneCurveSettings:
    """Panneau 2 : Courbe des tonalités (Tone Curve)."""

    mode: str = "parametric"  # "parametric" | "point"
    # Parametric curve controls
    highlights: float = 0.0  # [-100.0, +100.0]
    lights: float = 0.0  # [-100.0, +100.0]
    darks: float = 0.0  # [-100.0, +100.0]
    shadows: float = 0.0  # [-100.0, +100.0]
    # Split pivots [0.0, 100.0]
    shadow_split: float = 25.0
    midtone_split: float = 50.0
    highlight_split: float = 75.0
    # Point curve control points (x, y) in [0.0, 1.0]
    rgb_points: List[Tuple[float, float]] = field(
        default_factory=lambda: [(0.0, 0.0), (1.0, 1.0)]
    )
    red_points: List[Tuple[float, float]] = field(
        default_factory=lambda: [(0.0, 0.0), (1.0, 1.0)]
    )
    green_points: List[Tuple[float, float]] = field(
        default_factory=lambda: [(0.0, 0.0), (1.0, 1.0)]
    )
    blue_points: List[Tuple[float, float]] = field(
        default_factory=lambda: [(0.0, 0.0), (1.0, 1.0)]
    )


@dataclass
class ColorMixerSettings:
    """Panneau 3 : Mélangeur de couleurs (Color Mixer / TSL & Point Color)."""

    mode: str = "hsl"  # "hsl" | "point_color"
    # HSL 8 color ranges: [Red, Orange, Yellow, Green, Aqua, Blue, Purple, Magenta]
    # Hue shift [-100.0, +100.0]
    hue_red: float = 0.0
    hue_orange: float = 0.0
    hue_yellow: float = 0.0
    hue_green: float = 0.0
    hue_aqua: float = 0.0
    hue_blue: float = 0.0
    hue_purple: float = 0.0
    hue_magenta: float = 0.0
    # Saturation shift [-100.0, +100.0]
    sat_red: float = 0.0
    sat_orange: float = 0.0
    sat_yellow: float = 0.0
    sat_green: float = 0.0
    sat_aqua: float = 0.0
    sat_blue: float = 0.0
    sat_purple: float = 0.0
    sat_magenta: float = 0.0
    # Luminance shift [-100.0, +100.0]
    lum_red: float = 0.0
    lum_orange: float = 0.0
    lum_yellow: float = 0.0
    lum_green: float = 0.0
    lum_aqua: float = 0.0
    lum_blue: float = 0.0
    lum_purple: float = 0.0
    lum_magenta: float = 0.0
    # Point Color
    point_active: bool = False
    point_target_r: float = 0.5
    point_target_g: float = 0.5
    point_target_b: float = 0.5
    point_hue_shift: float = 0.0  # [-100.0, +100.0]
    point_sat_shift: float = 0.0  # [-100.0, +100.0]
    point_lum_shift: float = 0.0  # [-100.0, +100.0]
    point_range: float = 50.0  # [0.0, 100.0]


@dataclass
class ColorGradingSettings:
    """Panneau 4 : Color Grading (3 Roues chromatiques + Global)."""

    # Shadows
    shadow_hue: float = 0.0  # [0.0, 360.0] degrees
    shadow_sat: float = 0.0  # [0.0, 100.0] %
    shadow_lum: float = 0.0  # [-100.0, +100.0]
    # Midtones
    midtone_hue: float = 0.0  # [0.0, 360.0]
    midtone_sat: float = 0.0  # [0.0, 100.0]
    midtone_lum: float = 0.0  # [-100.0, +100.0]
    # Highlights
    highlight_hue: float = 0.0  # [0.0, 360.0]
    highlight_sat: float = 0.0  # [0.0, 100.0]
    highlight_lum: float = 0.0  # [-100.0, +100.0]
    # Global
    global_hue: float = 0.0  # [0.0, 360.0]
    global_sat: float = 0.0  # [0.0, 100.0]
    global_lum: float = 0.0  # [-100.0, +100.0]
    # Tuning
    blending: float = 50.0  # [0.0, 100.0] (default 50)
    balance: float = 0.0  # [-100.0, +100.0] (default 0)


@dataclass
class DetailSettings:
    """Panneau 5 : Détail (Netteté & Réduction du bruit)."""

    # Sharpening
    sharpen_amount: float = 0.0  # [0.0, 150.0]
    sharpen_radius: float = 1.0  # [0.5, 3.0]
    sharpen_detail: float = 25.0  # [0.0, 100.0]
    sharpen_masking: float = 0.0  # [0.0, 100.0]
    # Noise Reduction
    nr_luminance: float = 0.0  # [0.0, 100.0]
    nr_lum_detail: float = 50.0  # [0.0, 100.0]
    nr_color: float = 0.0  # [0.0, 100.0]


@dataclass
class LensCorrectionsSettings:
    """Panneau 6 : Corrections de l'objectif (Lens Corrections)."""

    distortion: float = 0.0  # [-100.0, +100.0] (Barillet <-> Coussinet)
    remove_ca: bool = False  # Aberration chromatique auto
    ca_red_cyan: float = 0.0  # [-100.0, +100.0]
    ca_blue_yellow: float = 0.0  # [-100.0, +100.0]
    vignette_amount: float = 0.0  # [-100.0, +100.0]
    vignette_midpoint: float = 50.0  # [0.0, 100.0]


@dataclass
class TransformSettings:
    """Panneau 7 : Transformation géométrique (Transform)."""

    vertical: float = 0.0  # [-100.0, +100.0]
    horizontal: float = 0.0  # [-100.0, +100.0]
    rotate: float = 0.0  # [-45.0, +45.0] deg
    aspect: float = 0.0  # [-100.0, +100.0]
    scale: float = 100.0  # [50.0, 150.0] %
    offset_x: float = 0.0  # [-100.0, +100.0]
    offset_y: float = 0.0  # [-100.0, +100.0]


@dataclass
class LensBlurSettings:
    """Panneau 8 : Flou de l'objectif (Lens Blur)."""

    enabled: bool = False
    blur_amount: float = 0.0  # [0.0, 100.0]
    bokeh_style: str = "circle"  # "circle" | "bubble" | "blade5" | "blade7" | "ring"
    bokeh_boost: float = 0.0  # [0.0, 100.0]
    focal_range: float = 50.0  # [0.0, 100.0]


@dataclass
class EffectsSettings:
    """Panneau 9 : Effets (Post-crop Vignette & Grain)."""

    # Vignette
    vignette_amount: float = 0.0  # [-100.0, +100.0]
    vignette_midpoint: float = 50.0  # [0.0, 100.0]
    vignette_roundness: float = 0.0  # [-100.0, +100.0]
    vignette_feather: float = 50.0  # [0.0, 100.0]
    vignette_highlights: float = 0.0  # [0.0, 100.0]
    # Grain
    grain_amount: float = 0.0  # [0.0, 100.0]
    grain_size: float = 25.0  # [0.0, 100.0]
    grain_roughness: float = 50.0  # [0.0, 100.0]


@dataclass
class CropSettings:
    """Adobe Camera Raw Crop & Straighten specification (crs:Crop*)."""

    left: float = 0.0  # [0.0, 1.0]
    top: float = 0.0  # [0.0, 1.0]
    right: float = 1.0  # [0.0, 1.0]
    bottom: float = 1.0  # [0.0, 1.0]
    angle: float = 0.0  # Straighten angle in degrees [-45.0, +45.0]

    @property
    def has_crop(self) -> bool:
        return (
            self.left > 1e-4
            or self.top > 1e-4
            or (1.0 - self.right) > 1e-4
            or (1.0 - self.bottom) > 1e-4
            or abs(self.angle) > 1e-4
        )


@dataclass
class CalibrationSettings:
    """Panneau 10 : Étalonnage (Calibration)."""

    shadow_tint: float = 0.0  # [-100.0, +100.0] (Vert <-> Magenta)
    red_hue: float = 0.0  # [-100.0, +100.0]
    red_sat: float = 0.0  # [-100.0, +100.0]
    green_hue: float = 0.0  # [-100.0, +100.0]
    green_sat: float = 0.0  # [-100.0, +100.0]
    blue_hue: float = 0.0  # [-100.0, +100.0]
    blue_sat: float = 0.0  # [-100.0, +100.0]


@dataclass
class LightroomDevelopSettings:
    """Conteneur complet des réglages de développement Adobe Lightroom Classic."""

    # Toggle Bypass Œil pour chacune des 10 sections
    is_basic_enabled: bool = True
    is_tone_curve_enabled: bool = True
    is_color_mixer_enabled: bool = True
    is_color_grading_enabled: bool = True
    is_detail_enabled: bool = True
    is_lens_corrections_enabled: bool = True
    is_transform_enabled: bool = True
    is_lens_blur_enabled: bool = True
    is_effects_enabled: bool = True
    is_calibration_enabled: bool = True

    # 10 configurations dédiées
    basic: BasicSettings = field(default_factory=BasicSettings)
    tone_curve: ToneCurveSettings = field(default_factory=ToneCurveSettings)
    color_mixer: ColorMixerSettings = field(default_factory=ColorMixerSettings)
    color_grading: ColorGradingSettings = field(default_factory=ColorGradingSettings)
    detail: DetailSettings = field(default_factory=DetailSettings)
    lens_corrections: LensCorrectionsSettings = field(
        default_factory=LensCorrectionsSettings
    )
    transform: TransformSettings = field(default_factory=TransformSettings)
    lens_blur: LensBlurSettings = field(default_factory=LensBlurSettings)
    effects: EffectsSettings = field(default_factory=EffectsSettings)
    calibration: CalibrationSettings = field(default_factory=CalibrationSettings)
    crop: CropSettings = field(default_factory=CropSettings)

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Optional[Dict[str, Any]]) -> LightroomDevelopSettings:
        if not data:
            return cls()

        def parse_sub(sub_cls, key):
            val = data.get(key)
            if isinstance(val, dict):
                # Filter valid fields
                valid_fields = set(sub_cls.__dataclass_fields__.keys())
                filtered = {k: v for k, v in val.items() if k in valid_fields}
                return sub_cls(**filtered)
            return sub_cls()

        return cls(
            is_basic_enabled=bool(data.get("is_basic_enabled", True)),
            is_tone_curve_enabled=bool(data.get("is_tone_curve_enabled", True)),
            is_color_mixer_enabled=bool(data.get("is_color_mixer_enabled", True)),
            is_color_grading_enabled=bool(data.get("is_color_grading_enabled", True)),
            is_detail_enabled=bool(data.get("is_detail_enabled", True)),
            is_lens_corrections_enabled=bool(
                data.get("is_lens_corrections_enabled", True)
            ),
            is_transform_enabled=bool(data.get("is_transform_enabled", True)),
            is_lens_blur_enabled=bool(data.get("is_lens_blur_enabled", True)),
            is_effects_enabled=bool(data.get("is_effects_enabled", True)),
            is_calibration_enabled=bool(data.get("is_calibration_enabled", True)),
            basic=parse_sub(BasicSettings, "basic"),
            tone_curve=parse_sub(ToneCurveSettings, "tone_curve"),
            color_mixer=parse_sub(ColorMixerSettings, "color_mixer"),
            color_grading=parse_sub(ColorGradingSettings, "color_grading"),
            detail=parse_sub(DetailSettings, "detail"),
            lens_corrections=parse_sub(LensCorrectionsSettings, "lens_corrections"),
            transform=parse_sub(TransformSettings, "transform"),
            lens_blur=parse_sub(LensBlurSettings, "lens_blur"),
            effects=parse_sub(EffectsSettings, "effects"),
            calibration=parse_sub(CalibrationSettings, "calibration"),
            crop=parse_sub(CropSettings, "crop"),
        )
