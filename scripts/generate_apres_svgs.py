import os

os.makedirs("scripts/apres_svgs", exist_ok=True)

# Common SVG elements & styles
SVG_HEADER = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 844" width="390" height="844">
  <defs>
    <style>
      @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&amp;family=Playfair+Display:ital,wght@0,600;0,700;1,600&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap');
      text { font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
      .mono { font-family: 'JetBrains Mono', monospace; }
      .serif { font-family: 'Playfair Display', Georgia, serif; }
      .title { font-weight: 700; fill: #FFFFFF; }
      .subtitle { font-weight: 500; fill: #94A3B8; font-size: 13px; }
      .card { fill: #161B22; stroke: #262C36; stroke-width: 1; rx: 18; }
      .chip { rx: 16; font-size: 12px; font-weight: 600; }
      .btn-primary { fill: #E5A93C; rx: 14; }
      .btn-secondary { fill: #21262D; stroke: #30363D; stroke-width: 1; rx: 14; }
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
    <linearGradient id="cyanAccent" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#26C6DA" />
      <stop offset="100%" stop-color="#00838F" />
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
"""

STATUS_TAG = """  <!-- Mode Badge -->
  <g transform="translate(16, 42)">
    <rect width="358" height="26" rx="6" fill="#10B981" fill-opacity="0.12" stroke="#10B981" stroke-width="1" />
    <circle cx="14" cy="13" r="4" fill="#10B981" />
    <text x="24" y="17" font-size="11" font-weight="700" fill="#10B981">[APRÈS] {title}</text>
    <text x="345" y="17" text-anchor="end" class="mono" font-size="10" fill="#10B981">CHIC &amp; MODULAIRE</text>
  </g>
"""

BOTTOM_NAV = """  <!-- Main Navigation Bar (Clean, chic 68px) -->
  <g id="mainBottomNav" transform="translate(0, 756)">
    <rect width="390" height="88" fill="#12161E" stroke="#21262D" stroke-width="1" />
    
    <!-- Tab 1: Studio -->
    <g transform="translate(15, 8)">
      {studio_active}
      <path d="M25 10 L33 22 L17 22 Z" fill="{studio_icon_color}" />
      <text x="25" y="36" text-anchor="middle" font-size="11" font-weight="600" fill="{studio_text_color}">Studio</text>
    </g>

    <!-- Tab 2: Éditeur -->
    <g transform="translate(92, 8)">
      {editor_active}
      <path d="M20 14 L28 14 M24 10 L24 18 M16 22 L32 22" stroke="{editor_icon_color}" stroke-width="2" stroke-linecap="round" />
      <text x="24" y="36" text-anchor="middle" font-size="11" font-weight="600" fill="{editor_text_color}">Éditeur</text>
    </g>

    <!-- Tab 3: Palettes -->
    <g transform="translate(170, 8)">
      {palettes_active}
      <circle cx="20" cy="14" r="5" fill="{palettes_icon_color}" fill-opacity="0.6" />
      <circle cx="28" cy="14" r="5" fill="{palettes_icon_color}" />
      <text x="24" y="36" text-anchor="middle" font-size="11" font-weight="600" fill="{palettes_text_color}">Palettes</text>
    </g>

    <!-- Tab 4: Profil -->
    <g transform="translate(248, 8)">
      {profil_active}
      <circle cx="24" cy="12" r="4" stroke="{profil_icon_color}" stroke-width="1.8" fill="none" />
      <path d="M16 22 C16 18 20 18 24 18 C28 18 32 18 32 22" stroke="{profil_icon_color}" stroke-width="1.8" fill="none" stroke-linecap="round" />
      <text x="24" y="36" text-anchor="middle" font-size="11" font-weight="600" fill="{profil_text_color}">Profil</text>
    </g>

    <!-- Tab 5: Réglages -->
    <g transform="translate(325, 8)">
      {settings_active}
      <circle cx="24" cy="15" r="5" stroke="{settings_icon_color}" stroke-width="1.8" fill="none" />
      <text x="24" y="36" text-anchor="middle" font-size="11" font-weight="600" fill="{settings_text_color}">Réglages</text>
    </g>
  </g>
"""

def make_bottom_nav(active_tab="studio"):
    def tab_state(name):
        if name == active_tab:
            return (
                '<rect x="0" y="0" width="50" height="44" rx="12" fill="#E5A93C" fill-opacity="0.12" />',
                '#E5A93C',
                '#E5A93C'
            )
        else:
            return ('', '#94A3B8', '#94A3B8')
    
    s_act, s_ic, s_tx = tab_state("studio")
    e_act, e_ic, e_tx = tab_state("editor")
    p_act, p_ic, p_tx = tab_state("palettes")
    pr_act, pr_ic, pr_tx = tab_state("profil")
    set_act, set_ic, set_tx = tab_state("settings")

    return BOTTOM_NAV.format(
        studio_active=s_act, studio_icon_color=s_ic, studio_text_color=s_tx,
        editor_active=e_act, editor_icon_color=e_ic, editor_text_color=e_tx,
        palettes_active=p_act, palettes_icon_color=p_ic, palettes_text_color=p_tx,
        profil_active=pr_act, profil_icon_color=pr_ic, profil_text_color=pr_tx,
        settings_active=set_act, settings_icon_color=set_ic, settings_text_color=set_tx
    )

def save_svg(name, content):
    with open(f"scripts/apres_svgs/{name}.svg", "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Generated scripts/apres_svgs/{name}.svg")

print("Framework for Après SVGs ready.")
