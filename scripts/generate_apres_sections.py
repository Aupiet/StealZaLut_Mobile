import os
import glob
import re

os.makedirs("scripts/apres_sections", exist_ok=True)

# Helper to extract the inner content of an SVG (excluding outer <svg> and <defs>)
def get_svg_inner(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Strip <svg ...> and </svg>
    content = re.sub(r'<svg[^>]*>', '', content)
    content = content.replace('</svg>', '')
    return content

# Master section builder
def build_section_svg(section_title, screens_data, total_width, total_height=980):
    # screens_data: list of (screen_title, svg_path, x_pos)
    
    inner_defs = """
    <style>
      @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&amp;family=Playfair+Display:ital,wght@0,600;0,700;1,600&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap');
      text { font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
      .mono { font-family: 'JetBrains Mono', monospace; }
      .serif { font-family: 'Playfair Display', Georgia, serif; }
      .title { font-weight: 700; fill: #FFFFFF; }
      .subtitle { font-weight: 500; fill: #94A3B8; font-size: 13px; }
      .section-banner { fill: #161B22; stroke: #E5A93C; stroke-width: 1.5; rx: 16; }
    </style>
    <linearGradient id="photoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1E293B" /><stop offset="50%" stop-color="#0F172A" /><stop offset="100%" stop-color="#090D16" />
    </linearGradient>
    <linearGradient id="warmSunset" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#F59E0B" /><stop offset="40%" stop-color="#D97706" /><stop offset="80%" stop-color="#991B1B" /><stop offset="100%" stop-color="#1E1B4B" />
    </linearGradient>
    <linearGradient id="goldAccent" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FBBF24" /><stop offset="100%" stop-color="#D97706" />
    </linearGradient>
    """

    svg_screens = []
    for title, path, x in screens_data:
        inner = get_svg_inner(path)
        # remove internal <defs>...</defs> to avoid conflicts
        inner_cleaned = re.sub(r'<defs>.*?</defs>', '', inner, flags=re.DOTALL)
        svg_screens.append(f"""
        <!-- Frame: {title} -->
        <g id="{title.replace(' ', '_').replace('/', '_')}" transform="translate({x}, 90)">
          <!-- Screen Border Container -->
          <rect width="390" height="844" rx="20" fill="none" stroke="#30363D" stroke-width="2" />
          {inner_cleaned}
        </g>
        """)

    full_svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {total_width} {total_height}" width="{total_width}" height="{total_height}">
  <defs>
    {inner_defs}
  </defs>

  <!-- Section Canvas Background -->
  <rect width="{total_width}" height="{total_height}" fill="#08090C" />

  <!-- Section Top Header Banner -->
  <g transform="translate(40, 20)">
    <rect width="{total_width - 80}" height="54" class="section-banner" />
    <circle cx="28" cy="27" r="12" fill="#E5A93C" />
    <path d="M24 27 L27 30 L33 24" stroke="#000000" stroke-width="2" fill="none" stroke-linecap="round" />
    <text x="50" y="33" class="serif title" font-size="20">{section_title}</text>
    <rect x="{total_width - 240}" y="14" width="180" height="26" rx="6" fill="#10B981" fill-opacity="0.15" stroke="#10B981" stroke-width="1" />
    <text x="{total_width - 150}" y="31" text-anchor="middle" font-size="11" font-weight="700" fill="#10B981">SECTION APRÈS • VALIDÉ</text>
  </g>

  {''.join(svg_screens)}
</svg>"""
    return full_svg

# Section 1: Studio (6 screens)
s1_screens = [
    ("Studio - Étape 1 Source", "scripts/apres_svgs/Studio - Etape 1 Source.svg", 40),
    ("Studio - Étape 2 Palettes", "scripts/apres_svgs/Studio - Etape 2 Palettes.svg", 470),
    ("Studio - Étape 2 Réf", "scripts/apres_svgs/Studio - Etape 2 Ref.svg", 900),
    ("Studio - Étape 3 Paramètres", "scripts/apres_svgs/Studio - Etape 3 Params.svg", 1330),
    ("Studio - Étape 4 Calcul", "scripts/apres_svgs/Studio - Etape 4 Calcul.svg", 1760),
    ("Studio - Étape 5 Résultat", "scripts/apres_svgs/Studio - Etape 5 Resultat.svg", 2190)
]
svg_sec1 = build_section_svg("02 · Après / Section 1 : Parcours Studio Colorimétrique", s1_screens, 2620)
with open("scripts/apres_sections/01_Section_Studio_Apres.svg", "w", encoding="utf-8") as f:
    f.write(svg_sec1)
print("Generated 01_Section_Studio_Apres.svg (2620x980)")

# Section 2: Editeur Part 1 (7 screens)
s2_screens = [
    ("Éditeur - État Vide", "scripts/apres_svgs/Editeur - Etat vide.svg", 40),
    ("Éditeur - Viewport", "scripts/apres_svgs/Editeur - Viewport.svg", 470),
    ("Éditeur - Menu Dropdown", "scripts/apres_svgs/Editeur - Dropdown Menu.svg", 900),
    ("Éditeur - Outil 0 Recadrage", "scripts/apres_svgs/Editeur - Outil 0 Recadrage.svg", 1330),
    ("Éditeur - Outil 1 Basic", "scripts/apres_svgs/Editeur - Outil 1 Basic.svg", 1760),
    ("Éditeur - Outil 2 Courbes", "scripts/apres_svgs/Editeur - Outil 2 Courbes.svg", 2190),
    ("Éditeur - Outil 3 TSL", "scripts/apres_svgs/Editeur - Outil 3 TSL.svg", 2620)
]
svg_sec2 = build_section_svg("02 · Après / Section 2 : Éditeur Pro — Viewport & Outils Principaux", s2_screens, 3050)
with open("scripts/apres_sections/02_Section_Editeur_Part1_Apres.svg", "w", encoding="utf-8") as f:
    f.write(svg_sec2)
print("Generated 02_Section_Editeur_Part1_Apres.svg (3050x980)")

# Section 3: Editeur Part 2 (8 screens: 3-Way, Detail, Effets, Calibr, Geom, Optique, Presets, Export HD, Exit)
s3_screens = [
    ("Éditeur - Outil 4 3-Way", "scripts/apres_svgs/Editeur - Outil 4 3-Way.svg", 40),
    ("Éditeur - Outil 5 Détail", "scripts/apres_svgs/Editeur - Outil 5 Detail.svg", 470),
    ("Éditeur - Outil 6 Effets", "scripts/apres_svgs/Editeur - Outil 6 Effets.svg", 900),
    ("Éditeur - Outil 7 Calibration", "scripts/apres_svgs/Editeur - Outil 7 Calibration.svg", 1330),
    ("Éditeur - Outil 8 Géométrie", "scripts/apres_svgs/Editeur - Outil 8 Transform.svg", 1760),
    ("Éditeur - Outil 9 Optique", "scripts/apres_svgs/Editeur - Outil 9 Lens.svg", 2190),
    ("Éditeur - Presets Modal", "scripts/apres_svgs/Editeur - Presets Modal.svg", 2620),
    ("Éditeur - Dialogue Export HD", "scripts/apres_svgs/Editeur - Dialogue Export HD.svg", 3050),
    ("Éditeur - Confirmation Quitter", "scripts/apres_svgs/Editeur - Exit Confirm.svg", 3480)
]
svg_sec3 = build_section_svg("02 · Après / Section 3 : Éditeur Pro — Outils Avancés, Presets & Export HD", s3_screens, 3910)
with open("scripts/apres_sections/03_Section_Editeur_Part2_Apres.svg", "w", encoding="utf-8") as f:
    f.write(svg_sec3)
print("Generated 03_Section_Editeur_Part2_Apres.svg (3910x980)")

# Section 4: Palettes, Profil & Parametres (5 screens)
s4_screens = [
    ("Palettes - Extracteur Nuances", "scripts/apres_svgs/Palettes - Extracteur Nuances.svg", 40),
    ("Profil - Galerie Photos", "scripts/apres_svgs/Profil - Photos.svg", 470),
    ("Profil - Styles & LUTs", "scripts/apres_svgs/Profil - Styles.svg", 900),
    ("Éditeur - Enregistrer Preset", "scripts/apres_svgs/Editeur - Save Preset.svg", 1330),
    ("Paramètres - Thèmes & Système", "scripts/apres_svgs/Parametres - Themes.svg", 1760)
]
svg_sec4 = build_section_svg("02 · Après / Section 4 : Palettes, Profil Créatif & Paramètres Système", s4_screens, 2190)
with open("scripts/apres_sections/04_Section_Palettes_Profil_Settings_Apres.svg", "w", encoding="utf-8") as f:
    f.write(svg_sec4)
print("Generated 04_Section_Palettes_Profil_Settings_Apres.svg (2190x980)")
