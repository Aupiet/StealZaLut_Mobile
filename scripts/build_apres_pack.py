import os

os.makedirs("scripts/apres_svgs", exist_ok=True)

def header(title):
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 844" width="390" height="844">
  <defs>
    <style>
      @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&amp;family=Playfair+Display:ital,wght@0,600;0,700;1,600&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap');
      text {{ font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }}
      .mono {{ font-family: 'JetBrains Mono', monospace; }}
      .serif {{ font-family: 'Playfair Display', Georgia, serif; }}
      .title {{ font-weight: 700; fill: #FFFFFF; }}
      .subtitle {{ font-weight: 500; fill: #94A3B8; font-size: 13px; }}
      .card {{ fill: #161B22; stroke: #262C36; stroke-width: 1; rx: 18; }}
      .chip {{ rx: 16; font-size: 12px; font-weight: 600; }}
      .btn-primary {{ fill: #E5A93C; rx: 14; }}
      .btn-secondary {{ fill: #21262D; stroke: #30363D; stroke-width: 1; rx: 14; }}
    </style>
    <linearGradient id="photoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#1E293B" />
      <stop offset="50%" stop-color="#0F172A" />
      <stop offset="100%" stop-color="#090D16" />
    </linearGradient>
    <linearGradient id="warmSunset" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#F59E0B" />
      <stop offset="40%" stop-color="#D97706" />
      <stop offset="80%" stop-color="#991B1B" />
      <stop offset="100%" stop-color="#1E1B4B" />
    </linearGradient>
    <linearGradient id="goldAccent" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FBBF24" />
      <stop offset="100%" stop-color="#D97706" />
    </linearGradient>
  </defs>

  <!-- Background -->
  <rect width="390" height="844" fill="#0D0F12" />

  <!-- Status Bar (34px) -->
  <g id="statusBar">
    <text x="32" y="23" font-size="14" font-weight="600" fill="#FFFFFF">09:41</text>
    <g transform="translate(322, 12)">
      <path d="M0 12 A6 6 0 0 1 12 12" stroke="#FFFFFF" stroke-width="1.5" fill="none" />
      <path d="M2 9 A4 4 0 0 1 10 9" stroke="#FFFFFF" stroke-width="1.5" fill="none" />
      <circle cx="6" cy="13" r="1" fill="#FFFFFF" />
      <rect x="20" y="2" width="22" height="12" rx="3" stroke="#FFFFFF" stroke-width="1" fill="none" />
      <rect x="22" y="4" width="14" height="8" rx="1.5" fill="#FFFFFF" />
      <path d="M43 6 L44 6 L44 10 L43 10 Z" fill="#FFFFFF" />
    </g>
  </g>

  <!-- Mode Tag -->
  <g transform="translate(16, 40)">
    <rect width="358" height="26" rx="7" fill="#10B981" fill-opacity="0.12" stroke="#10B981" stroke-width="1" />
    <circle cx="14" cy="13" r="4" fill="#10B981" />
    <text x="24" y="17" font-size="11" font-weight="700" fill="#10B981">[APRÈS] {title}</text>
    <text x="345" y="17" text-anchor="end" class="mono" font-size="10" fill="#10B981">CHIC &amp; MODULAIRE</text>
  </g>
"""

def bottom_nav(active_tab="studio"):
    def tab(name, icon_svg, label, x):
        is_active = (name == active_tab)
        bg = f'<rect x="0" y="0" width="54" height="46" rx="14" fill="#E5A93C" fill-opacity="0.12" />' if is_active else ''
        color = '#E5A93C' if is_active else '#94A3B8'
        return f"""
        <g transform="translate({x}, 8)">
          {bg}
          {icon_svg.replace('COLOR', color)}
          <text x="27" y="38" text-anchor="middle" font-size="11" font-weight="{ '700' if is_active else '500' }" fill="{color}">{label}</text>
        </g>
        """
    s_ic = '<path d="M27 10 L35 22 L19 22 Z" fill="COLOR" />'
    e_ic = '<path d="M22 14 L32 14 M27 9 L27 19 M19 23 L35 23" stroke="COLOR" stroke-width="2" stroke-linecap="round" />'
    p_ic = '<circle cx="23" cy="15" r="5" fill="COLOR" fill-opacity="0.5" /><circle cx="31" cy="15" r="5" fill="COLOR" />'
    pr_ic = '<circle cx="27" cy="13" r="4.5" stroke="COLOR" stroke-width="1.8" fill="none" /><path d="M19 23 C19 19 23 19 27 19 C31 19 35 19 35 23" stroke="COLOR" stroke-width="1.8" fill="none" stroke-linecap="round" />'
    st_ic = '<circle cx="27" cy="16" r="5.5" stroke="COLOR" stroke-width="1.8" fill="none" />'

    return f"""
    <!-- Main Bottom Nav (Clean, chic) -->
    <g id="mainBottomNav" transform="translate(0, 756)">
      <rect width="390" height="88" fill="#12161E" stroke="#21262D" stroke-width="1" />
      {tab("studio", s_ic, "Studio", 12)}
      {tab("editor", e_ic, "Éditeur", 88)}
      {tab("palettes", p_ic, "Palettes", 164)}
      {tab("profil", pr_ic, "Profil", 240)}
      {tab("settings", st_ic, "Réglages", 316)}
    </g>
    """

def editor_toolbar(active_tool_idx=1):
    tools = [
        ("Recadrer", '<rect x="11" y="6" width="16" height="16" rx="3" fill="none" stroke="COLOR" stroke-width="1.8" />'),
        ("Base", '<circle cx="19" cy="14" r="8" fill="none" stroke="COLOR" stroke-width="1.8" />'),
        ("Courbes", '<path d="M11 20 Q19 8 27 14" stroke="COLOR" stroke-width="2" fill="none" stroke-linecap="round" />'),
        ("TSL", '<circle cx="15" cy="14" r="5" fill="COLOR" fill-opacity="0.4" /><circle cx="23" cy="14" r="5" fill="COLOR" />'),
        ("3-Way", '<circle cx="19" cy="14" r="7" fill="none" stroke="COLOR" stroke-width="2" />'),
        ("Détail", '<polygon points="19,7 26,20 12,20" fill="none" stroke="COLOR" stroke-width="1.8" />'),
        ("Effets", '<circle cx="17" cy="13" r="3" fill="COLOR" /><circle cx="23" cy="10" r="1.5" fill="COLOR" />'),
        ("Calibr.", '<circle cx="19" cy="14" r="8" stroke="COLOR" stroke-width="1.5" fill="none" stroke-dasharray="3 3" />'),
        ("Géom.", '<rect x="10" y="8" width="18" height="12" rx="2" fill="none" stroke="COLOR" stroke-width="1.8" />'),
        ("Optique", '<circle cx="19" cy="14" r="7.5" fill="none" stroke="COLOR" stroke-width="1.8" />')
    ]
    items = []
    start = max(0, min(active_tool_idx - 2, len(tools) - 5))
    visible = tools[start:start+5]
    for i, (name, ic) in enumerate(visible):
        global_idx = start + i
        is_act = (global_idx == active_tool_idx)
        x = 12 + i * 73
        c = '#E5A93C' if is_act else '#94A3B8'
        bg = '<rect x="0" y="2" width="68" height="48" rx="12" fill="#E5A93C" fill-opacity="0.15" stroke="#E5A93C" stroke-width="1" />' if is_act else ''
        dot = '<circle cx="34" cy="48" r="2.5" fill="#E5A93C" />' if is_act else ''
        rendered_ic = ic.replace('COLOR', c)
        items.append(f"""
        <g transform="translate({x}, 0)">
          {bg}
          <g transform="translate(15, 4)">{rendered_ic}</g>
          <text x="34" y="38" text-anchor="middle" font-size="12" font-weight="{'700' if is_act else '500'}" fill="{c}">{name}</text>
          {dot}
        </g>
        """)

    return f"""
    <!-- Editor Single Toolbar (Fixed FE-002, FE-009, FE-023) -->
    <g id="editorToolbar" transform="translate(0, 772)">
      <rect width="390" height="72" fill="#12161E" stroke="#21262D" stroke-width="1" />
      {''.join(items)}
    </g>
    """

EDITOR_TOP_BAR = """
  <!-- Top Action Bar -->
  <g transform="translate(16, 74)">
    <rect width="358" height="44" rx="14" fill="#161B22" stroke="#262C36" />
    <g transform="translate(8, 6)">
      <rect width="32" height="32" rx="8" fill="#21262D" />
      <path d="M12 18 A6 6 0 1 1 18 24 M10 20 L12 18 L16 18" stroke="#E5A93C" stroke-width="2" fill="none" stroke-linecap="round" />
    </g>
    <g transform="translate(48, 6)">
      <rect width="32" height="32" rx="8" fill="#21262D" />
      <path d="M20 18 A6 6 0 1 0 14 24 M22 20 L20 18 L16 18" stroke="#94A3B8" stroke-width="2" fill="none" stroke-linecap="round" />
    </g>
    <g transform="translate(135, 6)">
      <rect width="88" height="32" rx="10" fill="#21262D" stroke="#30363D" />
      <text x="44" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#E2E8F0">Comparer</text>
    </g>
    <g transform="translate(268, 6)">
      <rect width="32" height="32" rx="8" fill="#21262D" />
      <circle cx="16" cy="11" r="1.5" fill="#CBD5E1" />
      <circle cx="16" cy="16" r="1.5" fill="#CBD5E1" />
      <circle cx="16" cy="21" r="1.5" fill="#CBD5E1" />
    </g>
    <g transform="translate(306, 6)">
      <rect width="44" height="32" rx="10" fill="#E5A93C" />
      <path d="M22 10 L22 22 M16 16 L22 10 L28 16" stroke="#000000" stroke-width="2" stroke-linecap="round" fill="none" />
    </g>
  </g>
"""

HISTOGRAM_CHIC = """
  <!-- RGB Histogram with FE-017 Clipping Warnings -->
  <g transform="translate(24, 126)">
    <rect width="130" height="54" rx="10" fill="#0D1117" fill-opacity="0.85" stroke="#30363D" />
    <circle cx="10" cy="12" r="4" fill="#2979FF" />
    <path d="M18 46 Q35 15 55 35 Q80 10 110 46" fill="#FFFFFF" fill-opacity="0.15" stroke="#94A3B8" stroke-width="1.2" />
    <path d="M20 46 Q40 20 60 40 Q85 18 108 46" fill="none" stroke="#EF4444" stroke-width="1" stroke-opacity="0.8" />
    <path d="M19 46 Q42 25 65 38 Q90 15 112 46" fill="none" stroke="#10B981" stroke-width="1" stroke-opacity="0.8" />
    <path d="M22 46 Q45 18 70 32 Q95 22 115 46" fill="none" stroke="#38BDF8" stroke-width="1" stroke-opacity="0.8" />
    <circle cx="120" cy="12" r="4" fill="#FF1744" />
  </g>
"""

def save(fname, svg):
    with open(f"scripts/apres_svgs/{fname}.svg", "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Saved {fname}.svg")

# --- 1. Studio Etape 1 Source ---
s1 = f"""{header("Studio / Étape 1 Source")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Studio Colorimétrique</text>
    <text x="0" y="44" class="subtitle">Transférez la signature chromatique d'un film</text>
  </g>
  <g transform="translate(20, 138)">
    <rect width="350" height="42" rx="12" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 12)">
      <circle cx="10" cy="10" r="10" fill="#E5A93C" /><text x="10" y="14" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">1</text>
      <line x1="24" y1="10" x2="65" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="78" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="78" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">2</text>
      <line x1="91" y1="10" x2="132" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="145" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="145" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">3</text>
      <line x1="158" y1="10" x2="199" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="212" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="212" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">4</text>
      <line x1="225" y1="10" x2="266" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="279" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="279" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">5</text>
    </g>
  </g>
  <g transform="translate(20, 196)">
    <rect width="350" height="340" rx="20" fill="url(#photoGrad)" stroke="#30363D" />
    <circle cx="175" cy="140" r="44" fill="#E5A93C" fill-opacity="0.12" stroke="#E5A93C" stroke-width="1.5" />
    <path d="M165 148 L175 132 L185 148 Z" fill="#E5A93C" />
    <text x="175" y="210" text-anchor="middle" class="title" font-size="17">Photo source prête</text>
    <text x="175" y="232" text-anchor="middle" class="subtitle" font-size="13">portrait_paris_sunset.dng</text>
    <g transform="translate(45, 260)">
      <rect x="0" y="0" width="75" height="26" rx="13" fill="#161B22" stroke="#30363D" /><text x="37" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">ISO 100</text>
      <rect x="85" y="0" width="75" height="26" rx="13" fill="#161B22" stroke="#30363D" /><text x="122" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">f/1.8</text>
      <rect x="170" y="0" width="90" height="26" rx="13" fill="#161B22" stroke="#30363D" /><text x="215" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">1/250s</text>
    </g>
  </g>
  <g transform="translate(20, 552)">
    <rect width="350" height="52" rx="16" fill="#21262D" stroke="#30363D" />
    <text x="175" y="32" text-anchor="middle" font-size="14" font-weight="600" fill="#FFFFFF">Changer de photo source</text>
  </g>
  <g transform="translate(20, 616)">
    <rect width="350" height="56" rx="18" fill="url(#goldAccent)" />
    <text x="175" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Continuer vers Référence (2/5) →</text>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 1 Source", s1)

# --- 2. Studio Etape 2 Palettes ---
s2_palettes = f"""{header("Studio / Étape 2 Palettes")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Sélection Référence</text>
    <text x="0" y="44" class="subtitle">Choisissez un nuancier cinéma extrait</text>
  </g>
  <g transform="translate(20, 138)">
    <rect width="350" height="42" rx="12" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 12)">
      <circle cx="10" cy="10" r="10" fill="#10B981" /><text x="10" y="14" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">✓</text>
      <line x1="24" y1="10" x2="65" y2="10" stroke="#E5A93C" stroke-width="2" />
      <circle cx="78" cy="10" r="10" fill="#E5A93C" /><text x="78" y="14" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">2</text>
      <line x1="91" y1="10" x2="132" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="145" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="145" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">3</text>
      <line x1="158" y1="10" x2="199" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="212" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="212" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">4</text>
      <line x1="225" y1="10" x2="266" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="279" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="279" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">5</text>
    </g>
  </g>
  <!-- Segmented Button: Nuancier vs Image Ref -->
  <g transform="translate(20, 196)">
    <rect width="350" height="42" rx="14" fill="#161B22" stroke="#262C36" />
    <rect x="4" y="4" width="168" height="34" rx="11" fill="#E5A93C" />
    <text x="88" y="25" text-anchor="middle" font-size="13" font-weight="700" fill="#000000">Nuanciers Cinéma</text>
    <text x="260" y="25" text-anchor="middle" font-size="13" font-weight="500" fill="#94A3B8">Photo de Référence</text>
  </g>
  <!-- Palette Cards -->
  <g transform="translate(20, 254)">
    <rect width="350" height="120" rx="18" fill="#161B22" stroke="#E5A93C" stroke-width="1.5" />
    <text x="20" y="30" class="title" font-size="15">Blade Runner 2049 (Orange &amp; Teal)</text>
    <text x="20" y="48" class="subtitle" font-size="12">Atmosphère brumeuse néon crépusculaire</text>
    <g transform="translate(20, 64)">
      <rect x="0" y="0" width="46" height="38" rx="8" fill="#1A365D" />
      <rect x="52" y="0" width="46" height="38" rx="8" fill="#2E5B88" />
      <rect x="104" y="0" width="46" height="38" rx="8" fill="#E28743" />
      <rect x="156" y="0" width="46" height="38" rx="8" fill="#E76F51" />
      <rect x="208" y="0" width="46" height="38" rx="8" fill="#F4A261" />
      <rect x="260" y="0" width="46" height="38" rx="8" fill="#B91C1C" />
    </g>
  </g>
  <g transform="translate(20, 390)">
    <rect width="350" height="120" rx="18" fill="#161B22" stroke="#262C36" />
    <text x="20" y="30" class="title" font-size="15">Grand Budapest Hotel (Pastel Vintage)</text>
    <text x="20" y="48" class="subtitle" font-size="12">Tons roses feutrés, cyan doux et crème</text>
    <g transform="translate(20, 64)">
      <rect x="0" y="0" width="46" height="38" rx="8" fill="#F4ACB7" />
      <rect x="52" y="0" width="46" height="38" rx="8" fill="#FFCAD4" />
      <rect x="104" y="0" width="46" height="38" rx="8" fill="#B5E2FA" />
      <rect x="156" y="0" width="46" height="38" rx="8" fill="#EDDEA4" />
      <rect x="208" y="0" width="46" height="38" rx="8" fill="#F7A072" />
      <rect x="260" y="0" width="46" height="38" rx="8" fill="#8AC926" />
    </g>
  </g>
  <g transform="translate(20, 616)">
    <rect width="350" height="56" rx="18" fill="url(#goldAccent)" />
    <text x="175" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Continuer vers Paramètres (3/5) →</text>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 2 Palettes", s2_palettes)

# --- 3. Studio Etape 2 Ref ---
s2_ref = f"""{header("Studio / Étape 2 Réf")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Photo de Référence</text>
    <text x="0" y="44" class="subtitle">Image maître dont le style sera extrait</text>
  </g>
  <g transform="translate(20, 138)">
    <rect width="350" height="42" rx="12" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 12)">
      <circle cx="10" cy="10" r="10" fill="#10B981" /><text x="10" y="14" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">✓</text>
      <line x1="24" y1="10" x2="65" y2="10" stroke="#E5A93C" stroke-width="2" />
      <circle cx="78" cy="10" r="10" fill="#E5A93C" /><text x="78" y="14" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">2</text>
      <line x1="91" y1="10" x2="132" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="145" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="145" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">3</text>
      <line x1="158" y1="10" x2="199" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="212" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="212" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">4</text>
      <line x1="225" y1="10" x2="266" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="279" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="279" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">5</text>
    </g>
  </g>
  <g transform="translate(20, 196)">
    <rect width="350" height="42" rx="14" fill="#161B22" stroke="#262C36" />
    <text x="88" y="25" text-anchor="middle" font-size="13" font-weight="500" fill="#94A3B8">Nuanciers Cinéma</text>
    <rect x="178" y="4" width="168" height="34" rx="11" fill="#E5A93C" />
    <text x="262" y="25" text-anchor="middle" font-size="13" font-weight="700" fill="#000000">Photo de Référence</text>
  </g>
  <g transform="translate(20, 254)">
    <rect width="350" height="330" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
    <rect x="0" y="240" width="350" height="90" rx="20" fill="#0D1117" fill-opacity="0.85" />
    <text x="20" y="270" class="title" font-size="16">blade_runner_still_04.jpg</text>
    <text x="20" y="292" class="subtitle" font-size="12">12 450 nuances échantillonnées</text>
  </g>
  <g transform="translate(20, 616)">
    <rect width="350" height="56" rx="18" fill="url(#goldAccent)" />
    <text x="175" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Continuer vers Paramètres (3/5) →</text>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 2 Ref", s2_ref)

# --- 4. Studio Etape 3 Params ---
s3_params = f"""{header("Studio / Étape 3 Paramètres")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Ajustement du Transfert</text>
    <text x="0" y="44" class="subtitle">Dosage et préservation des carnations</text>
  </g>
  <g transform="translate(20, 138)">
    <rect width="350" height="42" rx="12" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 12)">
      <circle cx="10" cy="10" r="10" fill="#10B981" /><text x="10" y="14" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">✓</text>
      <line x1="24" y1="10" x2="65" y2="10" stroke="#10B981" stroke-width="2" />
      <circle cx="78" cy="10" r="10" fill="#10B981" /><text x="78" y="14" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">✓</text>
      <line x1="91" y1="10" x2="132" y2="10" stroke="#E5A93C" stroke-width="2" />
      <circle cx="145" cy="10" r="10" fill="#E5A93C" /><text x="145" y="14" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">3</text>
      <line x1="158" y1="10" x2="199" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="212" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="212" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">4</text>
      <line x1="225" y1="10" x2="266" y2="10" stroke="#30363D" stroke-width="2" />
      <circle cx="279" cy="10" r="9" fill="#21262D" stroke="#30363D" /><text x="279" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">5</text>
    </g>
  </g>
  <g transform="translate(20, 196)">
    <rect width="350" height="390" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(20, 24)">
      <!-- Slider 1: Force -->
      <text x="0" y="18" class="title" font-size="14">Intensité du transfert</text>
      <text x="310" y="18" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">80 %</text>
      <rect x="0" y="32" width="310" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="32" width="248" height="6" rx="3" fill="#E5A93C" />
      <circle cx="248" cy="35" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <!-- Slider 2: Correspondance -->
      <text x="0" y="80" class="title" font-size="14">Précision chromatique</text>
      <text x="310" y="80" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">65 %</text>
      <rect x="0" y="94" width="310" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="94" width="201" height="6" rx="3" fill="#E5A93C" />
      <circle cx="201" cy="97" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <!-- Switch Skin Tone -->
      <g transform="translate(0, 140)">
        <rect width="310" height="60" rx="14" fill="#0D1117" stroke="#262C36" />
        <text x="16" y="26" class="title" font-size="13">Protéger les tons de peau</text>
        <text x="16" y="44" class="subtitle" font-size="11">Masque automatique des visages</text>
        <rect x="250" y="16" width="46" height="28" rx="14" fill="#E5A93C" />
        <circle cx="282" cy="30" r="10" fill="#000000" />
      </g>

      <!-- Switch Grain -->
      <g transform="translate(0, 215)">
        <rect width="310" height="60" rx="14" fill="#0D1117" stroke="#262C36" />
        <text x="16" y="26" class="title" font-size="13">Émulation grain argentique 35mm</text>
        <text x="16" y="44" class="subtitle" font-size="11">Structure organique subtile</text>
        <rect x="250" y="16" width="46" height="28" rx="14" fill="#E5A93C" />
        <circle cx="282" cy="30" r="10" fill="#000000" />
      </g>
    </g>
  </g>
  <g transform="translate(20, 616)">
    <rect width="350" height="56" rx="18" fill="url(#goldAccent)" />
    <text x="175" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Générer le Rendu LUT (4/5) →</text>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 3 Params", s3_params)

# --- 5. Studio Etape 4 Calcul ---
s4_calc = f"""{header("Studio / Étape 4 Calcul")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Génération en cours</text>
    <text x="0" y="44" class="subtitle">Calcul colorimétrique haute fidélité 64-bit</text>
  </g>
  <g transform="translate(20, 180)">
    <rect width="350" height="400" rx="24" fill="#161B22" stroke="#262C36" />
    <!-- Progress Ring -->
    <g transform="translate(175, 150)">
      <circle cx="0" cy="0" r="75" stroke="#21262D" stroke-width="10" fill="none" />
      <circle cx="0" cy="0" r="75" stroke="#E5A93C" stroke-width="10" stroke-linecap="round" fill="none" stroke-dasharray="471" stroke-dashoffset="120" />
      <text x="0" y="12" text-anchor="middle" class="mono" font-size="32" font-weight="700" fill="#FFFFFF">74 %</text>
    </g>
    <text x="175" y="270" text-anchor="middle" class="title" font-size="16">Interpolation Tetrahedral LUT 33x33x33</text>
    <text x="175" y="295" text-anchor="middle" class="subtitle" font-size="13">Étape 3 sur 4 : Conservation des luminances</text>
    <!-- Progress Mini Bar -->
    <g transform="translate(45, 330)">
      <rect width="260" height="6" rx="3" fill="#21262D" />
      <rect width="192" height="6" rx="3" fill="#10B981" />
    </g>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 4 Calcul", s4_calc)

# --- 6. Studio Etape 5 Resultat ---
s5_res = f"""{header("Studio / Étape 5 Résultat")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Chef-d'œuvre Colorimétrique</text>
    <text x="0" y="44" class="subtitle">Signature Blade Runner 2049 appliquée</text>
  </g>
  <!-- Split comparison view -->
  <g transform="translate(20, 138)">
    <rect width="350" height="420" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
    <!-- Split handle -->
    <line x1="175" y1="0" x2="175" y2="420" stroke="#FFFFFF" stroke-width="2" />
    <circle cx="175" cy="210" r="18" fill="#E5A93C" />
    <path d="M168 210 L172 206 M168 210 L172 214 M182 210 L178 206 M182 210 L178 214" stroke="#000000" stroke-width="2" stroke-linecap="round" />
    <!-- Badges -->
    <rect x="20" y="20" width="65" height="24" rx="8" fill="#000000" fill-opacity="0.6" />
    <text x="52" y="36" text-anchor="middle" font-size="11" font-weight="700" fill="#FFFFFF">AVANT</text>
    <rect x="265" y="20" width="65" height="24" rx="8" fill="#E5A93C" />
    <text x="297" y="36" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">APRÈS</text>
  </g>
  <!-- Actions -->
  <g transform="translate(20, 580)">
    <rect width="350" height="54" rx="16" fill="url(#goldAccent)" />
    <text x="175" y="33" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Ouvrir dans l'Éditeur Pro →</text>
  </g>
  <g transform="translate(20, 646)">
    <rect width="170" height="46" rx="14" fill="#21262D" stroke="#30363D" />
    <text x="85" y="28" text-anchor="middle" font-size="13" font-weight="600" fill="#FFFFFF">Exporter .cube</text>
    <rect x="180" y="0" width="170" height="46" rx="14" fill="#21262D" stroke="#30363D" />
    <text x="265" y="28" text-anchor="middle" font-size="13" font-weight="600" fill="#FFFFFF">Enregistrer Preset</text>
  </g>
{bottom_nav("studio")}
</svg>"""
save("Studio - Etape 5 Resultat", s5_res)

# --- 7. Editeur Etat Vide ---
ed_vide = f"""{header("Éditeur / État vide")}
  <!-- Empty State Hero Card with Photographic Warmth -->
  <g transform="translate(20, 110)">
    <rect width="350" height="580" rx="24" fill="#161B22" stroke="#262C36" />
    <g transform="translate(175, 120)">
      <circle cx="0" cy="0" r="54" fill="#E5A93C" fill-opacity="0.12" stroke="#E5A93C" stroke-width="1.5" />
      <rect x="-24" y="-18" width="48" height="36" rx="8" stroke="#E5A93C" stroke-width="2.5" fill="none" />
      <circle cx="0" cy="0" r="8" stroke="#E5A93C" stroke-width="2" fill="none" />
      <circle cx="14" cy="-10" r="3" fill="#E5A93C" />
    </g>
    <text x="175" y="225" text-anchor="middle" class="serif title" font-size="22">Sublimez vos photographies</text>
    <text x="175" y="252" text-anchor="middle" class="subtitle" font-size="14">Développez vos fichiers RAW ou JPEG</text>
    <text x="175" y="272" text-anchor="middle" class="subtitle" font-size="14">avec la puissance de l'émulation argentique.</text>
    <!-- Pro Tips Card (Method 8, 14) -->
    <g transform="translate(25, 310)">
      <rect width="300" height="110" rx="14" fill="#0D1117" stroke="#21262D" />
      <text x="20" y="28" class="title" font-size="12" fill="#E5A93C">CONSEIL PHOTO DU LABO</text>
      <text x="20" y="52" class="subtitle" font-size="12">Pour un rendu cinéma chaleureux, combinez</text>
      <text x="20" y="70" class="subtitle" font-size="12">l'abaissement des hautes lumières et un léger</text>
      <text x="20" y="88" class="subtitle" font-size="12">virage sépia dans le panneau 3-Way.</text>
    </g>
    <!-- CTA Button 48dp+ -->
    <g transform="translate(25, 455)">
      <rect width="300" height="56" rx="18" fill="url(#goldAccent)" />
      <text x="150" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Choisir une photo (PickVisualMedia)</text>
    </g>
  </g>
{bottom_nav("editor")}
</svg>"""
save("Editeur - Etat vide", ed_vide)

# --- 8. Editeur Viewport ---
ed_viewport = f"""{header("Éditeur / Viewport")}
{EDITOR_TOP_BAR}
  <!-- Hero Picture (NO DOUBLE BOTTOM BAR! Full height) -->
  <g transform="translate(16, 126)">
    <rect width="358" height="634" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
{HISTOGRAM_CHIC}
{editor_toolbar(1)}
</svg>"""
save("Editeur - Viewport", ed_viewport)

# --- 9. Editeur Dropdown Menu ---
ed_dropdown = f"""{header("Éditeur / Menu Actions Dropdown")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="634" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
{HISTOGRAM_CHIC}
  <!-- Dropdown Popup -->
  <g transform="translate(150, 124)">
    <rect width="220" height="210" rx="16" fill="#161B22" stroke="#30363D" filter="drop-shadow(0 10 20 rgba(0,0,0,0.5))" />
    <g transform="translate(16, 20)">
      <text x="0" y="16" font-size="13" font-weight="600" fill="#FFFFFF">Afficher l'histogramme</text>
      <line x1="0" y1="30" x2="188" y2="30" stroke="#21262D" />
      <text x="0" y="54" font-size="13" font-weight="600" fill="#FFFFFF">Métadonnées EXIF</text>
      <line x1="0" y1="68" x2="188" y2="68" stroke="#21262D" />
      <text x="0" y="92" font-size="13" font-weight="600" fill="#FFFFFF">Sauvegarder en Preset</text>
      <line x1="0" y1="106" x2="188" y2="106" stroke="#21262D" />
      <text x="0" y="130" font-size="13" font-weight="600" fill="#EF4444">Réinitialiser les réglages</text>
      <line x1="0" y1="144" x2="188" y2="144" stroke="#21262D" />
      <text x="0" y="168" font-size="13" font-weight="600" fill="#94A3B8">Aide des raccourcis</text>
    </g>
  </g>
{editor_toolbar(1)}
</svg>"""
save("Editeur - Dropdown Menu", ed_dropdown)

# --- 10. Editeur Outil 0 Recadrage (FE-005) ---
ed_crop = f"""{header("Éditeur / Outil 0 Recadrage")}
{EDITOR_TOP_BAR}
  <!-- Photo with Crop Overlay (FE-005) -->
  <g transform="translate(16, 126)">
    <rect width="358" height="420" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
    <!-- Darkening veil -->
    <rect width="358" height="420" rx="20" fill="#000000" fill-opacity="0.65" />
    <!-- Active Crop Rect 4:5 -->
    <rect x="50" y="30" width="258" height="320" fill="none" stroke="#FFFFFF" stroke-width="2" />
    <!-- Thirds grid -->
    <line x1="136" y1="30" x2="136" y2="350" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="3 3" />
    <line x1="222" y1="30" x2="222" y2="350" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="3 3" />
    <line x1="50" y1="136" x2="308" y2="136" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="3 3" />
    <line x1="50" y1="242" x2="308" y2="242" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="3 3" />
    <!-- Corner handles 48dp target -->
    <circle cx="50" cy="30" r="8" fill="#E5A93C" />
    <circle cx="308" cy="30" r="8" fill="#E5A93C" />
    <circle cx="50" cy="350" r="8" fill="#E5A93C" />
    <circle cx="308" cy="350" r="8" fill="#E5A93C" />
  </g>
  <!-- Bottom Panel for Crop -->
  <g transform="translate(16, 560)">
    <rect width="358" height="200" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <text x="0" y="18" class="title" font-size="14">Ratio de cadrage</text>
      <!-- Ratio Pills -->
      <g transform="translate(0, 32)">
        <rect x="0" y="0" width="55" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="27" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Libre</text>
        <rect x="62" y="0" width="65" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="94" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Original</text>
        <rect x="134" y="0" width="55" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="161" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">1:1</text>
        <rect x="196" y="0" width="55" height="32" rx="16" fill="#E5A93C" /><text x="223" y="21" text-anchor="middle" font-size="12" font-weight="700" fill="#000000">4:5</text>
        <rect x="258" y="0" width="65" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="290" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">16:9</text>
      </g>
      <!-- Straighten angle -->
      <g transform="translate(0, 85)">
        <text x="0" y="16" class="title" font-size="13">Redressement horizon</text>
        <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+1.8°</text>
        <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" />
        <rect x="163" y="28" width="40" height="6" rx="3" fill="#E5A93C" />
        <circle cx="203" cy="31" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
      </g>
    </g>
  </g>
{editor_toolbar(0)}
</svg>"""
save("Editeur - Outil 0 Recadrage", ed_crop)

# --- 11. Editeur Outil 1 Basic ---
ed_basic = f"""{header("Éditeur / Outil 1 Basic")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
{HISTOGRAM_CHIC}
  <!-- Basic adjustments panel with double-tap reset hints -->
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <!-- Row 1: Expo -->
      <text x="0" y="16" class="title" font-size="13">Exposition</text>
      <text x="326" y="16" text-anchor="end" class="mono" font-size="12" font-weight="700" fill="#E5A93C">+0.35 EV</text>
      <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="163" y="28" width="55" height="6" rx="3" fill="#E5A93C" />
      <circle cx="218" cy="31" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <!-- Row 2: Contraste -->
      <text x="0" y="66" class="title" font-size="13">Contraste</text>
      <text x="326" y="66" text-anchor="end" class="mono" font-size="12" font-weight="700" fill="#E5A93C">+12</text>
      <rect x="0" y="78" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="163" y="78" width="35" height="6" rx="3" fill="#E5A93C" />
      <circle cx="198" cy="81" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <!-- Row 3: Hautes Lum -->
      <text x="0" y="116" class="title" font-size="13">Hautes Lumières</text>
      <text x="326" y="116" text-anchor="end" class="mono" font-size="12" font-weight="700" fill="#38BDF8">-15</text>
      <rect x="0" y="128" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="120" y="128" width="43" height="6" rx="3" fill="#38BDF8" />
      <circle cx="120" cy="131" r="10" fill="#FFFFFF" stroke="#38BDF8" stroke-width="3" />

      <!-- Row 4: Ombres -->
      <text x="0" y="166" class="title" font-size="13">Ombres</text>
      <text x="326" y="166" text-anchor="end" class="mono" font-size="12" font-weight="700" fill="#E5A93C">+20</text>
      <rect x="0" y="178" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="163" y="178" width="58" height="6" rx="3" fill="#E5A93C" />
      <circle cx="221" cy="181" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <text x="163" y="222" text-anchor="middle" class="subtitle" font-size="11">Double-tap sur un curseur pour réinitialiser à 0</text>
    </g>
  </g>
{editor_toolbar(1)}
</svg>"""
save("Editeur - Outil 1 Basic", ed_basic)

# --- 12. Editeur Outil 2 Courbes ---
ed_courbes = f"""{header("Éditeur / Outil 2 Courbes")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="320" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <!-- Curves Panel -->
  <g transform="translate(16, 460)">
    <rect width="358" height="300" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 16)">
      <!-- Channel Selector -->
      <g transform="translate(20, 0)">
        <rect x="0" y="0" width="65" height="28" rx="14" fill="#FFFFFF" /><text x="32" y="19" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">RGB</text>
        <rect x="75" y="0" width="65" height="28" rx="14" fill="#21262D" /><text x="107" y="19" text-anchor="middle" font-size="11" font-weight="700" fill="#EF4444">Rouge</text>
        <rect x="150" y="0" width="65" height="28" rx="14" fill="#21262D" /><text x="182" y="19" text-anchor="middle" font-size="11" font-weight="700" fill="#10B981">Vert</text>
        <rect x="225" y="0" width="65" height="28" rx="14" fill="#21262D" /><text x="257" y="19" text-anchor="middle" font-size="11" font-weight="700" fill="#38BDF8">Bleu</text>
      </g>
      <!-- Curve Graph Box -->
      <g transform="translate(20, 42)">
        <rect width="286" height="200" rx="12" fill="#0D1117" stroke="#30363D" />
        <line x1="71" y1="0" x2="71" y2="200" stroke="#21262D" stroke-dasharray="2 2" />
        <line x1="143" y1="0" x2="143" y2="200" stroke="#21262D" stroke-dasharray="2 2" />
        <line x1="214" y1="0" x2="214" y2="200" stroke="#21262D" stroke-dasharray="2 2" />
        <line x1="0" y1="66" x2="286" y2="66" stroke="#21262D" stroke-dasharray="2 2" />
        <line x1="0" y1="133" x2="286" y2="133" stroke="#21262D" stroke-dasharray="2 2" />
        <!-- S-Curve -->
        <path d="M0 200 C80 200 90 140 143 100 C190 60 200 0 286 0" fill="none" stroke="#E5A93C" stroke-width="3" />
        <!-- Control points -->
        <circle cx="71" cy="165" r="7" fill="#FFFFFF" stroke="#E5A93C" stroke-width="2.5" />
        <circle cx="143" cy="100" r="7" fill="#FFFFFF" stroke="#E5A93C" stroke-width="2.5" />
        <circle cx="214" cy="35" r="7" fill="#FFFFFF" stroke="#E5A93C" stroke-width="2.5" />
      </g>
    </g>
  </g>
{editor_toolbar(2)}
</svg>"""
save("Editeur - Outil 2 Courbes", ed_courbes)

# --- 13. Editeur Outil 3 TSL ---
ed_tsl = f"""{header("Éditeur / Outil 3 TSL")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <!-- TSL Panel -->
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 16)">
      <!-- 8 Chromatic Color Pills -->
      <g transform="translate(6, 0)">
        <circle cx="15" cy="15" r="12" fill="#EF4444" stroke="#FFFFFF" stroke-width="2.5" />
        <circle cx="52" cy="15" r="10" fill="#F97316" />
        <circle cx="89" cy="15" r="10" fill="#EAB308" />
        <circle cx="126" cy="15" r="10" fill="#22C55E" />
        <circle cx="163" cy="15" r="10" fill="#06B6D4" />
        <circle cx="200" cy="15" r="10" fill="#3B82F6" />
        <circle cx="237" cy="15" r="10" fill="#A855F7" />
        <circle cx="274" cy="15" r="10" fill="#EC4899" />
      </g>
      <!-- Teinte / Saturation / Luminance Sliders for Red -->
      <g transform="translate(0, 48)">
        <text x="0" y="16" class="title" font-size="13">Teinte (Rouge)</text>
        <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+5</text>
        <rect x="0" y="26" width="326" height="6" rx="3" fill="#21262D" />
        <rect x="163" y="26" width="20" height="6" rx="3" fill="#E5A93C" />
        <circle cx="183" cy="29" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

        <text x="0" y="66" class="title" font-size="13">Saturation</text>
        <text x="326" y="66" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+18</text>
        <rect x="0" y="76" width="326" height="6" rx="3" fill="#21262D" />
        <rect x="163" y="76" width="45" height="6" rx="3" fill="#E5A93C" />
        <circle cx="208" cy="79" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

        <text x="0" y="116" class="title" font-size="13">Luminance</text>
        <text x="326" y="116" text-anchor="end" class="mono" font-size="12" fill="#38BDF8">-10</text>
        <rect x="0" y="126" width="326" height="6" rx="3" fill="#21262D" />
        <rect x="133" y="126" width="30" height="6" rx="3" fill="#38BDF8" />
        <circle cx="133" cy="129" r="10" fill="#FFFFFF" stroke="#38BDF8" stroke-width="3" />
      </g>
    </g>
  </g>
{editor_toolbar(3)}
</svg>"""
save("Editeur - Outil 3 TSL", ed_tsl)

# --- 14. Editeur Outil 4 3-Way ---
ed_3way = f"""{header("Éditeur / Outil 4 3-Way")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="320" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <!-- 3-Way Color Wheels Panel -->
  <g transform="translate(16, 460)">
    <rect width="358" height="300" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 18)">
      <!-- Wheel 1: Ombres -->
      <g transform="translate(10, 0)">
        <text x="45" y="16" text-anchor="middle" class="title" font-size="13">Ombres</text>
        <circle cx="45" cy="65" r="40" fill="#0D1117" stroke="#30363D" stroke-width="2" />
        <circle cx="45" cy="65" r="3" fill="#64748B" />
        <circle cx="35" cy="55" r="6" fill="#38BDF8" stroke="#FFFFFF" stroke-width="1.5" />
        <text x="45" y="122" text-anchor="middle" class="mono" font-size="11" fill="#38BDF8">Teal 195°</text>
      </g>
      <!-- Wheel 2: Tons Moyens -->
      <g transform="translate(118, 0)">
        <text x="45" y="16" text-anchor="middle" class="title" font-size="13">Tons Moyens</text>
        <circle cx="45" cy="65" r="40" fill="#0D1117" stroke="#30363D" stroke-width="2" />
        <circle cx="45" cy="65" r="3" fill="#64748B" />
        <circle cx="48" cy="60" r="6" fill="#E5A93C" stroke="#FFFFFF" stroke-width="1.5" />
        <text x="45" y="122" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">Warm 42°</text>
      </g>
      <!-- Wheel 3: Hautes Lumières -->
      <g transform="translate(226, 0)">
        <text x="45" y="16" text-anchor="middle" class="title" font-size="13">Hautes Lum.</text>
        <circle cx="45" cy="65" r="40" fill="#0D1117" stroke="#30363D" stroke-width="2" />
        <circle cx="45" cy="65" r="3" fill="#64748B" />
        <circle cx="55" cy="50" r="6" fill="#F59E0B" stroke="#FFFFFF" stroke-width="1.5" />
        <text x="45" y="122" text-anchor="middle" class="mono" font-size="11" fill="#F59E0B">Gold 35°</text>
      </g>
      <!-- Master Balance Slider -->
      <g transform="translate(10, 160)">
        <text x="0" y="16" class="title" font-size="13">Balance globale</text>
        <text x="306" y="16" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+10</text>
        <rect x="0" y="26" width="306" height="6" rx="3" fill="#21262D" />
        <rect x="153" y="26" width="30" height="6" rx="3" fill="#E5A93C" />
        <circle cx="183" cy="29" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
      </g>
    </g>
  </g>
{editor_toolbar(4)}
</svg>"""
save("Editeur - Outil 4 3-Way", ed_3way)

# --- 15. Editeur Outil 5 Detail ---
ed_detail = f"""{header("Éditeur / Outil 5 Détail")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <text x="0" y="16" class="title" font-size="13">Netteté / Piqué</text>
      <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">40</text>
      <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="28" width="130" height="6" rx="3" fill="#E5A93C" />
      <circle cx="130" cy="31" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <text x="0" y="66" class="title" font-size="13">Rayon du contour</text>
      <text x="326" y="66" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">1.0 px</text>
      <rect x="0" y="78" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="78" width="108" height="6" rx="3" fill="#E5A93C" />
      <circle cx="108" cy="81" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <text x="0" y="116" class="title" font-size="13">Réduction de bruit luminance</text>
      <text x="326" y="116" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">25</text>
      <rect x="0" y="128" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="128" width="81" height="6" rx="3" fill="#E5A93C" />
      <circle cx="81" cy="131" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <text x="0" y="166" class="title" font-size="13">Réduction de bruit chromatique</text>
      <text x="326" y="166" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">30</text>
      <rect x="0" y="178" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="178" width="98" height="6" rx="3" fill="#E5A93C" />
      <circle cx="98" cy="181" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
    </g>
  </g>
{editor_toolbar(5)}
</svg>"""
save("Editeur - Outil 5 Detail", ed_detail)

# --- 16. Editeur Outil 6 Effets ---
ed_effets = f"""{header("Éditeur / Outil 6 Effets")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <text x="0" y="16" class="title" font-size="13">Vignettage optique</text>
      <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#38BDF8">-20</text>
      <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="110" y="28" width="53" height="6" rx="3" fill="#38BDF8" />
      <circle cx="110" cy="31" r="10" fill="#FFFFFF" stroke="#38BDF8" stroke-width="3" />

      <text x="0" y="66" class="title" font-size="13">Grain argentique</text>
      <text x="326" y="66" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">35</text>
      <rect x="0" y="78" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="0" y="78" width="114" height="6" rx="3" fill="#E5A93C" />
      <circle cx="114" cy="81" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />

      <text x="0" y="116" class="title" font-size="13">Correction du voile (Dehaze)</text>
      <text x="326" y="116" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+15</text>
      <rect x="0" y="128" width="326" height="6" rx="3" fill="#21262D" />
      <rect x="163" y="128" width="40" height="6" rx="3" fill="#E5A93C" />
      <circle cx="203" cy="131" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
    </g>
  </g>
{editor_toolbar(6)}
</svg>"""
save("Editeur - Outil 6 Effets", ed_effets)

# --- 17. Editeur Outil 7 Calibration ---
ed_calib = f"""{header("Éditeur / Outil 7 Calibration")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <text x="0" y="16" class="title" font-size="13">Primaire Rouge (Teinte)</text>
      <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+10</text>
      <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" /><rect x="163" y="28" width="30" height="6" rx="3" fill="#EF4444" /><circle cx="193" cy="31" r="10" fill="#FFFFFF" stroke="#EF4444" stroke-width="3" />

      <text x="0" y="66" class="title" font-size="13">Primaire Verte (Teinte)</text>
      <text x="326" y="66" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">-5</text>
      <rect x="0" y="78" width="326" height="6" rx="3" fill="#21262D" /><rect x="145" y="78" width="18" height="6" rx="3" fill="#10B981" /><circle cx="145" cy="81" r="10" fill="#FFFFFF" stroke="#10B981" stroke-width="3" />

      <text x="0" y="116" class="title" font-size="13">Primaire Bleue (Saturation)</text>
      <text x="326" y="116" text-anchor="end" class="mono" font-size="12" fill="#E5A93C">+25</text>
      <rect x="0" y="128" width="326" height="6" rx="3" fill="#21262D" /><rect x="163" y="128" width="60" height="6" rx="3" fill="#38BDF8" /><circle cx="223" cy="131" r="10" fill="#FFFFFF" stroke="#38BDF8" stroke-width="3" />
    </g>
  </g>
{editor_toolbar(7)}
</svg>"""
save("Editeur - Outil 7 Calibration", ed_calib)

# --- 18. Editeur Outil 8 Transform (FE-009 Géométrie) ---
ed_trans = f"""{header("Éditeur / Outil 8 Géométrie (FE-009)")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <text x="0" y="16" class="title" font-size="13">Perspective verticale</text>
      <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#10B981">-8°</text>
      <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" /><rect x="135" y="28" width="28" height="6" rx="3" fill="#10B981" /><circle cx="135" cy="31" r="10" fill="#FFFFFF" stroke="#10B981" stroke-width="3" />

      <text x="0" y="66" class="title" font-size="13">Perspective horizontale</text>
      <text x="326" y="66" text-anchor="end" class="mono" font-size="12" fill="#10B981">0°</text>
      <rect x="0" y="78" width="326" height="6" rx="3" fill="#21262D" /><circle cx="163" cy="81" r="10" fill="#FFFFFF" stroke="#10B981" stroke-width="3" />

      <text x="0" y="116" class="title" font-size="13">Rotation géométrique</text>
      <text x="326" y="116" text-anchor="end" class="mono" font-size="12" fill="#10B981">+2.5°</text>
      <rect x="0" y="128" width="326" height="6" rx="3" fill="#21262D" /><rect x="163" y="128" width="25" height="6" rx="3" fill="#10B981" /><circle cx="188" cy="131" r="10" fill="#FFFFFF" stroke="#10B981" stroke-width="3" />
    </g>
  </g>
{editor_toolbar(8)}
</svg>"""
save("Editeur - Outil 8 Transform", ed_trans)

# --- 19. Editeur Outil 9 Lens (FE-009 Optique) ---
ed_lens = f"""{header("Éditeur / Outil 9 Optique (FE-009)")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="350" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <g transform="translate(16, 490)">
    <rect width="358" height="270" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 20)">
      <!-- Toggle profile -->
      <g transform="translate(0, 0)">
        <rect width="326" height="56" rx="14" fill="#0D1117" stroke="#262C36" />
        <text x="16" y="24" class="title" font-size="13">Correction du profil d'objectif</text>
        <text x="16" y="42" class="subtitle" font-size="11">Sony FE 24-70mm F2.8 GM II détecté</text>
        <rect x="265" y="14" width="46" height="28" rx="14" fill="#10B981" />
        <circle cx="297" cy="28" r="10" fill="#000000" />
      </g>
      <!-- Distortion -->
      <g transform="translate(0, 75)">
        <text x="0" y="16" class="title" font-size="13">Correction de distorsion manuelle</text>
        <text x="326" y="16" text-anchor="end" class="mono" font-size="12" fill="#10B981">+5</text>
        <rect x="0" y="28" width="326" height="6" rx="3" fill="#21262D" /><rect x="163" y="28" width="20" height="6" rx="3" fill="#10B981" /><circle cx="183" cy="31" r="10" fill="#FFFFFF" stroke="#10B981" stroke-width="3" />
      </g>
      <!-- Chromatic Aberration -->
      <g transform="translate(0, 135)">
        <rect width="326" height="56" rx="14" fill="#0D1117" stroke="#262C36" />
        <text x="16" y="24" class="title" font-size="13">Supprimer l'aberration chromatique</text>
        <text x="16" y="42" class="subtitle" font-size="11">Suppression des franges pourpres/vertes</text>
        <rect x="265" y="14" width="46" height="28" rx="14" fill="#10B981" />
        <circle cx="297" cy="28" r="10" fill="#000000" />
      </g>
    </g>
  </g>
{editor_toolbar(9)}
</svg>"""
save("Editeur - Outil 9 Lens", ed_lens)

# --- 20. Editeur Presets Modal ---
ed_presets = f"""{header("Éditeur / Presets Modal")}
{EDITOR_TOP_BAR}
  <g transform="translate(16, 126)">
    <rect width="358" height="634" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <!-- Modal Overlay Backdrop -->
  <rect width="390" height="844" fill="#000000" fill-opacity="0.65" />
  <!-- Bottom Sheet Presets -->
  <g transform="translate(0, 320)">
    <rect width="390" height="524" rx="28" fill="#161B22" stroke="#30363D" />
    <rect x="175" y="12" width="40" height="4" rx="2" fill="#4B5563" />
    <text x="24" y="45" class="serif title" font-size="20">Bibliothèque de Presets</text>
    <text x="366" y="45" text-anchor="end" class="subtitle" font-size="13" fill="#E5A93C">+ Créer</text>
    <!-- Preset Grid -->
    <g transform="translate(24, 70)">
      <!-- Card 1 -->
      <g transform="translate(0, 0)">
        <rect width="162" height="150" rx="16" fill="#21262D" stroke="#E5A93C" stroke-width="1.5" />
        <rect x="6" y="6" width="150" height="90" rx="12" fill="url(#warmSunset)" />
        <text x="12" y="118" class="title" font-size="13">Cinéma Sunset</text>
        <text x="12" y="136" class="subtitle" font-size="11">Teal &amp; Orange vif</text>
      </g>
      <!-- Card 2 -->
      <g transform="translate(180, 0)">
        <rect width="162" height="150" rx="16" fill="#21262D" stroke="#30363D" />
        <rect x="6" y="6" width="150" height="90" rx="12" fill="url(#photoGrad)" />
        <text x="12" y="118" class="title" font-size="13">Moody Noir 400</text>
        <text x="12" y="136" class="subtitle" font-size="11">Contraste argentique</text>
      </g>
      <!-- Card 3 -->
      <g transform="translate(0, 170)">
        <rect width="162" height="150" rx="16" fill="#21262D" stroke="#30363D" />
        <rect x="6" y="6" width="150" height="90" rx="12" fill="#1E293B" />
        <text x="12" y="118" class="title" font-size="13">Pastel Wes</text>
        <text x="12" y="136" class="subtitle" font-size="11">Tons roses &amp; crème</text>
      </g>
      <!-- Card 4 -->
      <g transform="translate(180, 170)">
        <rect width="162" height="150" rx="16" fill="#21262D" stroke="#30363D" />
        <rect x="6" y="6" width="150" height="90" rx="12" fill="#0F172A" />
        <text x="12" y="118" class="title" font-size="13">Cyan Pro Studio</text>
        <text x="12" y="136" class="subtitle" font-size="11">Étalonnage clinique</text>
      </g>
    </g>
  </g>
</svg>"""
save("Editeur - Presets Modal", ed_presets)

# --- 21. Editeur Save Preset ---
ed_save_p = f"""{header("Éditeur / Enregistrer Preset")}
  <rect width="390" height="844" fill="#000000" fill-opacity="0.7" />
  <g transform="translate(24, 210)">
    <rect width="342" height="424" rx="24" fill="#161B22" stroke="#30363D" />
    <g transform="translate(24, 28)">
      <text x="0" y="20" class="serif title" font-size="20">Enregistrer le Preset</text>
      <text x="0" y="42" class="subtitle" font-size="13">Capture l'état actuel de votre retouche</text>
      
      <!-- Input -->
      <g transform="translate(0, 64)">
        <text x="0" y="14" class="subtitle" font-size="12">Nom du style</text>
        <rect x="0" y="24" width="294" height="48" rx="12" fill="#0D1117" stroke="#E5A93C" stroke-width="1.5" />
        <text x="16" y="54" class="title" font-size="14">Paris Golden Hour 2026</text>
      </g>

      <!-- Category Selector -->
      <g transform="translate(0, 155)">
        <text x="0" y="14" class="subtitle" font-size="12">Catégorie</text>
        <g transform="translate(0, 24)">
          <rect x="0" y="0" width="85" height="32" rx="16" fill="#E5A93C" /><text x="42" y="21" text-anchor="middle" font-size="12" font-weight="700" fill="#000000">Portrait</text>
          <rect x="95" y="0" width="85" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="137" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Paysage</text>
          <rect x="190" y="0" width="95" height="32" rx="16" fill="#21262D" stroke="#30363D" /><text x="237" y="21" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Argentique</text>
        </g>
      </g>

      <!-- Buttons 48dp+ -->
      <g transform="translate(0, 250)">
        <rect width="294" height="52" rx="16" fill="url(#goldAccent)" />
        <text x="147" y="32" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Enregistrer dans le Profil</text>

        <rect y="64" width="294" height="46" rx="14" fill="#21262D" stroke="#30363D" />
        <text x="147" y="93" text-anchor="middle" font-size="14" font-weight="600" fill="#94A3B8">Annuler</text>
      </g>
    </g>
  </g>
</svg>"""
save("Editeur - Save Preset", ed_save_p)

# --- 22. Editeur Dialogue Export HD (FE-013) ---
ed_export = f"""{header("Éditeur / Dialogue Export HD (FE-013)")}
  <rect width="390" height="844" fill="#000000" fill-opacity="0.75" />
  <!-- Export Modal Dialog (FE-013) -->
  <g transform="translate(24, 170)">
    <rect width="342" height="504" rx="24" fill="#161B22" stroke="#E5A93C" stroke-width="1.5" />
    <g transform="translate(24, 28)">
      <text x="0" y="20" class="serif title" font-size="22">Exporter la Photo</text>
      <text x="0" y="42" class="subtitle" font-size="13">Rendu master haute résolution sans perte</text>

      <!-- Format selector -->
      <g transform="translate(0, 68)">
        <text x="0" y="14" class="subtitle" font-size="12">Format de fichier</text>
        <g transform="translate(0, 24)">
          <rect x="0" y="0" width="140" height="42" rx="12" fill="#E5A93C" />
          <text x="70" y="26" text-anchor="middle" font-size="13" font-weight="700" fill="#000000">JPEG (Chic)</text>
          <rect x="150" y="0" width="144" height="42" rx="12" fill="#21262D" stroke="#30363D" />
          <text x="222" y="26" text-anchor="middle" font-size="13" font-weight="600" fill="#CBD5E1">PNG (Lossless)</text>
        </g>
      </g>

      <!-- Quality Slider -->
      <g transform="translate(0, 160)">
        <text x="0" y="16" class="title" font-size="13">Qualité de compression</text>
        <text x="294" y="16" text-anchor="end" class="mono" font-size="13" font-weight="700" fill="#E5A93C">95 %</text>
        <rect x="0" y="28" width="294" height="6" rx="3" fill="#21262D" />
        <rect x="0" y="28" width="279" height="6" rx="3" fill="#E5A93C" />
        <circle cx="279" cy="31" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
      </g>

      <!-- Resolution & Weight Info Card -->
      <g transform="translate(0, 226)">
        <rect width="294" height="74" rx="14" fill="#0D1117" stroke="#262C36" />
        <g transform="translate(16, 20)">
          <text x="0" y="14" class="subtitle" font-size="12">Dimensions</text>
          <text x="0" y="36" class="mono title" font-size="14">3840 × 2160 (4K)</text>

          <text x="180" y="14" class="subtitle" font-size="12">Poids estimé</text>
          <text x="180" y="36" class="mono" font-size="14" font-weight="700" fill="#10B981">~ 4.2 Mo</text>
        </g>
      </g>

      <!-- CTA Export Buttons -->
      <g transform="translate(0, 330)">
        <rect width="294" height="54" rx="16" fill="url(#goldAccent)" />
        <text x="147" y="33" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Exporter maintenant (SAF)</text>

        <rect y="64" width="294" height="46" rx="14" fill="#21262D" stroke="#30363D" />
        <text x="147" y="93" text-anchor="middle" font-size="14" font-weight="600" fill="#94A3B8">Annuler</text>
      </g>
    </g>
  </g>
</svg>"""
save("Editeur - Dialogue Export HD", ed_export)

# --- 23. Editeur Exit Confirm ---
ed_exit = f"""{header("Éditeur / Quitter sans enregistrer")}
  <rect width="390" height="844" fill="#000000" fill-opacity="0.75" />
  <g transform="translate(24, 260)">
    <rect width="342" height="324" rx="24" fill="#161B22" stroke="#30363D" />
    <g transform="translate(24, 28)">
      <circle cx="24" cy="24" r="24" fill="#EF4444" fill-opacity="0.15" />
      <text x="24" y="32" text-anchor="middle" font-size="20" fill="#EF4444">!</text>
      <g transform="translate(60, 10)">
        <text x="0" y="18" class="title" font-size="17">Quitter l'éditeur ?</text>
        <text x="0" y="36" class="subtitle" font-size="12">Vos modifications non sauvegardées</text>
        <text x="0" y="52" class="subtitle" font-size="12">seront conservées dans la session (FE-015).</text>
      </g>
      <g transform="translate(0, 100)">
        <rect width="294" height="50" rx="14" fill="url(#goldAccent)" />
        <text x="147" y="31" text-anchor="middle" font-size="14" font-weight="700" fill="#000000">Enregistrer &amp; Quitter</text>

        <rect y="60" width="294" height="46" rx="14" fill="#21262D" stroke="#30363D" />
        <text x="147" y="89" text-anchor="middle" font-size="14" font-weight="600" fill="#CBD5E1">Continuer l'édition</text>

        <rect y="116" width="294" height="42" rx="14" fill="none" />
        <text x="147" y="142" text-anchor="middle" font-size="13" font-weight="600" fill="#EF4444">Ignorer et fermer</text>
      </g>
    </g>
  </g>
</svg>"""
save("Editeur - Exit Confirm", ed_exit)

# --- 24. Palettes Extracteur Nuances ---
palettes = f"""{header("Palettes / Extracteur Nuances")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Extracteur de Nuances</text>
    <text x="0" y="44" class="subtitle">Créez des nuanciers maîtres depuis vos photos</text>
  </g>
  <g transform="translate(20, 138)">
    <rect width="350" height="230" rx="20" fill="url(#warmSunset)" stroke="#30363D" />
  </g>
  <!-- Extracted Palette Swatches Card -->
  <g transform="translate(20, 386)">
    <rect width="350" height="230" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(20, 20)">
      <text x="0" y="16" class="title" font-size="15">Nuances Clés Extraites (6 Dominantes)</text>
      <text x="0" y="34" class="subtitle" font-size="12">Harmonie : Complémentaire divisée</text>
      <g transform="translate(0, 52)">
        <rect x="0" y="0" width="46" height="52" rx="10" fill="#1A365D" />
        <text x="23" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#1A36</text>

        <rect x="52" y="0" width="46" height="52" rx="10" fill="#2E5B88" />
        <text x="75" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#2E5B</text>

        <rect x="104" y="0" width="46" height="52" rx="10" fill="#E28743" />
        <text x="127" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#E287</text>

        <rect x="156" y="0" width="46" height="52" rx="10" fill="#E76F51" />
        <text x="179" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#E76F</text>

        <rect x="208" y="0" width="46" height="52" rx="10" fill="#F4A261" />
        <text x="231" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#F4A2</text>

        <rect x="260" y="0" width="46" height="52" rx="10" fill="#B91C1C" />
        <text x="283" y="70" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#B91C</text>
      </g>
      <g transform="translate(0, 140)">
        <rect width="310" height="46" rx="14" fill="#21262D" stroke="#30363D" />
        <text x="155" y="28" text-anchor="middle" font-size="13" font-weight="600" fill="#E5A93C">Enregistrer dans ma collection</text>
      </g>
    </g>
  </g>
  <g transform="translate(20, 630)">
    <rect width="350" height="54" rx="16" fill="url(#goldAccent)" />
    <text x="175" y="33" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Échantillonner une nouvelle image</text>
  </g>
{bottom_nav("palettes")}
</svg>"""
save("Palettes - Extracteur Nuances", palettes)

# --- 25. Profil Photos ---
pr_photos = f"""{header("Profil / Galerie Photos")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Galerie Créative</text>
    <text x="0" y="44" class="subtitle">14 chefs-d'œuvre développés avec StealZaLUT</text>
  </g>
  <!-- Filter tabs -->
  <g transform="translate(20, 134)">
    <rect x="0" y="0" width="110" height="34" rx="17" fill="#E5A93C" /><text x="55" y="22" text-anchor="middle" font-size="12" font-weight="700" fill="#000000">Toutes (14)</text>
    <rect x="120" y="0" width="100" height="34" rx="17" fill="#21262D" stroke="#30363D" /><text x="170" y="22" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Portraits</text>
    <rect x="230" y="0" width="100" height="34" rx="17" fill="#21262D" stroke="#30363D" /><text x="280" y="22" text-anchor="middle" font-size="12" font-weight="600" fill="#CBD5E1">Paysages</text>
  </g>
  <!-- Grid -->
  <g transform="translate(20, 184)">
    <rect x="0" y="0" width="170" height="230" rx="18" fill="url(#warmSunset)" stroke="#30363D" />
    <rect x="10" y="10" width="75" height="22" rx="11" fill="#000000" fill-opacity="0.6" />
    <text x="47" y="25" text-anchor="middle" font-size="10" font-weight="700" fill="#E5A93C">Blade 2049</text>

    <rect x="180" y="0" width="170" height="230" rx="18" fill="url(#photoGrad)" stroke="#30363D" />
    <rect x="190" y="10" width="75" height="22" rx="11" fill="#000000" fill-opacity="0.6" />
    <text x="227" y="25" text-anchor="middle" font-size="10" font-weight="700" fill="#FFFFFF">Moody B&amp;W</text>

    <rect x="0" y="245" width="170" height="230" rx="18" fill="#1E293B" stroke="#30363D" />
    <rect x="180" y="245" width="170" height="230" rx="18" fill="#0F172A" stroke="#30363D" />
  </g>
{bottom_nav("profil")}
</svg>"""
save("Profil - Photos", pr_photos)

# --- 26. Profil Styles ---
pr_styles = f"""{header("Profil / Styles & LUTs")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Styles &amp; Profils</text>
    <text x="0" y="44" class="subtitle">Presets personnels et fichiers 3D LUT sauvegardés</text>
  </g>
  <g transform="translate(20, 138)">
    <!-- Style Card 1 -->
    <rect width="350" height="96" rx="18" fill="#161B22" stroke="#E5A93C" stroke-width="1.5" />
    <g transform="translate(16, 16)">
      <rect x="0" y="0" width="64" height="64" rx="14" fill="url(#warmSunset)" />
      <text x="76" y="24" class="title" font-size="15">Paris Sunset Gold</text>
      <text x="76" y="42" class="subtitle" font-size="12">Créé le 8 nov. 2026 • 8 photos éditées</text>
      <rect x="76" y="48" width="65" height="18" rx="9" fill="#E5A93C" fill-opacity="0.15" />
      <text x="108" y="61" text-anchor="middle" class="mono" font-size="10" font-weight="700" fill="#E5A93C">PRESET</text>
    </g>
  </g>
  <g transform="translate(20, 248)">
    <rect width="350" height="96" rx="18" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 16)">
      <rect x="0" y="0" width="64" height="64" rx="14" fill="url(#photoGrad)" />
      <text x="76" y="24" class="title" font-size="15">Nordic Winter Cold</text>
      <text x="76" y="42" class="subtitle" font-size="12">Créé le 2 nov. 2026 • 3 photos éditées</text>
      <rect x="76" y="48" width="55" height="18" rx="9" fill="#38BDF8" fill-opacity="0.15" />
      <text x="103" y="61" text-anchor="middle" class="mono" font-size="10" font-weight="700" fill="#38BDF8">.CUBE</text>
    </g>
  </g>
{bottom_nav("profil")}
</svg>"""
save("Profil - Styles", pr_styles)

# --- 27. Parametres Themes ---
settings_th = f"""{header("Paramètres / Thèmes & Système")}
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Préférences &amp; Thèmes</text>
    <text x="0" y="44" class="subtitle">Personnalisation du design system modulaire</text>
  </g>
  <!-- Theme Selection Section -->
  <g transform="translate(20, 138)">
    <rect width="350" height="340" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(20, 20)">
      <text x="0" y="16" class="title" font-size="15">Thème d'application (FE-020, FE-021)</text>
      
      <!-- Option 1: Système -->
      <g transform="translate(0, 32)">
        <rect width="310" height="46" rx="12" fill="#0D1117" stroke="#30363D" />
        <circle cx="24" cy="23" r="8" fill="#38BDF8" />
        <text x="44" y="28" font-size="13" font-weight="600" fill="#FFFFFF">Système par défaut (Auto Jour/Nuit)</text>
      </g>

      <!-- Option 2: AmberNoir (Active) -->
      <g transform="translate(0, 86)">
        <rect width="310" height="46" rx="12" fill="#0D1117" stroke="#E5A93C" stroke-width="1.5" />
        <circle cx="24" cy="23" r="8" fill="#E5A93C" />
        <text x="44" y="28" font-size="13" font-weight="700" fill="#E5A93C">AmberNoir (Signature)</text>
        <circle cx="286" cy="23" r="10" fill="#E5A93C" />
        <text x="286" y="27" text-anchor="middle" font-size="10" font-weight="700" fill="#000000">✓</text>
      </g>

      <!-- Option 3: CyanStudio -->
      <g transform="translate(0, 140)">
        <rect width="310" height="46" rx="12" fill="#0D1117" stroke="#30363D" />
        <circle cx="24" cy="23" r="8" fill="#26C6DA" />
        <text x="44" y="28" font-size="13" font-weight="600" fill="#FFFFFF">CyanStudio (Pro Lab)</text>
      </g>

      <!-- Option 4: OledBlack -->
      <g transform="translate(0, 194)">
        <rect width="310" height="46" rx="12" fill="#0D1117" stroke="#30363D" />
        <circle cx="24" cy="23" r="8" fill="#FF6D00" />
        <text x="44" y="28" font-size="13" font-weight="600" fill="#FFFFFF">OledBlack (Contraste Pur)</text>
      </g>

      <!-- Option 5: Daylight -->
      <g transform="translate(0, 248)">
        <rect width="310" height="46" rx="12" fill="#0D1117" stroke="#30363D" />
        <circle cx="24" cy="23" r="8" fill="#C85A17" />
        <text x="44" y="28" font-size="13" font-weight="600" fill="#FFFFFF">Daylight (Clair Chaud)</text>
      </g>
    </g>
  </g>
  <!-- Icon Set Section -->
  <g transform="translate(20, 495)">
    <rect width="350" height="150" rx="20" fill="#161B22" stroke="#262C36" />
    <g transform="translate(20, 20)">
      <text x="0" y="16" class="title" font-size="15">Jeu d'icônes (SzlIconSet)</text>
      <g transform="translate(0, 32)">
        <rect width="310" height="42" rx="12" fill="#0D1117" stroke="#E5A93C" stroke-width="1.5" />
        <text x="16" y="26" font-size="13" font-weight="700" fill="#E5A93C">Rounded Icons (Chic &amp; Doux)</text>
        <circle cx="286" cy="21" r="9" fill="#E5A93C" />
        <text x="286" y="25" text-anchor="middle" font-size="9" font-weight="700" fill="#000000">✓</text>
      </g>
      <g transform="translate(0, 82)">
        <rect width="310" height="42" rx="12" fill="#0D1117" stroke="#30363D" />
        <text x="16" y="26" font-size="13" font-weight="600" fill="#CBD5E1">Outlined Icons (Précision Studio)</text>
      </g>
    </g>
  </g>
{bottom_nav("settings")}
</svg>"""
save("Parametres - Themes", settings_th)

print("All 27 Après screens generated successfully in scripts/apres_svgs/.")
