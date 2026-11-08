import os

os.makedirs("scripts/apres_svgs", exist_ok=True)

COMMON_DEFS = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 844" width="390" height="844">
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
"""

def make_bottom_nav(active_tab="studio"):
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

    studio_icon = '<path d="M27 10 L35 22 L19 22 Z" fill="COLOR" />'
    editor_icon = '<path d="M22 14 L32 14 M27 9 L27 19 M19 23 L35 23" stroke="COLOR" stroke-width="2" stroke-linecap="round" />'
    palettes_icon = '<circle cx="23" cy="15" r="5" fill="COLOR" fill-opacity="0.5" /><circle cx="31" cy="15" r="5" fill="COLOR" />'
    profil_icon = '<circle cx="27" cy="13" r="4.5" stroke="COLOR" stroke-width="1.8" fill="none" /><path d="M19 23 C19 19 23 19 27 19 C31 19 35 19 35 23" stroke="COLOR" stroke-width="1.8" fill="none" stroke-linecap="round" />'
    settings_icon = '<circle cx="27" cy="16" r="5.5" stroke="COLOR" stroke-width="1.8" fill="none" />'

    return f"""
    <!-- Main Bottom Nav (Clean, chic) -->
    <g id="mainBottomNav" transform="translate(0, 756)">
      <rect width="390" height="88" fill="#12161E" stroke="#21262D" stroke-width="1" />
      {tab("studio", studio_icon, "Studio", 12)}
      {tab("editor", editor_icon, "Éditeur", 88)}
      {tab("palettes", palettes_icon, "Palettes", 164)}
      {tab("profil", profil_icon, "Profil", 240)}
      {tab("settings", settings_icon, "Réglages", 316)}
    </g>
    """

def wrap_screen(title, body_svg, nav_tab="studio", show_nav=True):
    nav = make_bottom_nav(nav_tab) if show_nav else ""
    return f"""{COMMON_DEFS}
  <!-- Mode Tag -->
  <g transform="translate(16, 40)">
    <rect width="358" height="26" rx="7" fill="#10B981" fill-opacity="0.12" stroke="#10B981" stroke-width="1" />
    <circle cx="14" cy="13" r="4" fill="#10B981" />
    <text x="24" y="17" font-size="11" font-weight="700" fill="#10B981">[APRÈS] {title}</text>
    <text x="345" y="17" text-anchor="end" class="mono" font-size="10" fill="#10B981">CHIC &amp; MODULAIRE</text>
  </g>

{body_svg}

{nav}
</svg>
"""

# 1. Studio Etape 1
s1 = """
  <!-- Header -->
  <g transform="translate(20, 78)">
    <text x="0" y="24" class="serif title" font-size="24">Studio Colorimétrique</text>
    <text x="0" y="44" class="subtitle">Transférez la signature chromatique d'un film</text>
  </g>

  <!-- Step Progress Indicator (FE-013) -->
  <g transform="translate(20, 138)">
    <rect width="350" height="42" rx="12" fill="#161B22" stroke="#262C36" />
    <g transform="translate(16, 12)">
      <!-- Step 1 active -->
      <circle cx="10" cy="10" r="10" fill="#E5A93C" />
      <text x="10" y="14" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">1</text>
      <line x1="24" y1="10" x2="65" y2="10" stroke="#30363D" stroke-width="2" />
      
      <!-- Step 2 -->
      <circle cx="78" cy="10" r="9" fill="#21262D" stroke="#30363D" />
      <text x="78" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">2</text>
      <line x1="91" y1="10" x2="132" y2="10" stroke="#30363D" stroke-width="2" />

      <!-- Step 3 -->
      <circle cx="145" cy="10" r="9" fill="#21262D" stroke="#30363D" />
      <text x="145" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">3</text>
      <line x1="158" y1="10" x2="199" y2="10" stroke="#30363D" stroke-width="2" />

      <!-- Step 4 -->
      <circle cx="212" cy="10" r="9" fill="#21262D" stroke="#30363D" />
      <text x="212" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">4</text>
      <line x1="225" y1="10" x2="266" y2="10" stroke="#30363D" stroke-width="2" />

      <!-- Step 5 -->
      <circle cx="279" cy="10" r="9" fill="#21262D" stroke="#30363D" />
      <text x="279" y="14" text-anchor="middle" font-size="10" font-weight="600" fill="#94A3B8">5</text>
    </g>
  </g>

  <!-- Photo Hero Card -->
  <g transform="translate(20, 196)">
    <rect width="350" height="340" rx="20" fill="url(#photoGrad)" stroke="#30363D" />
    <circle cx="175" cy="140" r="44" fill="#E5A93C" fill-opacity="0.12" stroke="#E5A93C" stroke-width="1.5" />
    <path d="M165 148 L175 132 L185 148 Z" fill="#E5A93C" />
    <circle cx="175" cy="138" r="3" fill="#0D0F12" />
    <text x="175" y="210" text-anchor="middle" class="title" font-size="17">Photo source sélectionnée</text>
    <text x="175" y="232" text-anchor="middle" class="subtitle" font-size="13">portrait_paris_sunset.dng</text>
    
    <!-- EXIF chips -->
    <g transform="translate(45, 260)">
      <rect x="0" y="0" width="75" height="26" rx="13" fill="#161B22" stroke="#30363D" />
      <text x="37" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">ISO 100</text>
      
      <rect x="85" y="0" width="75" height="26" rx="13" fill="#161B22" stroke="#30363D" />
      <text x="122" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">f/1.8</text>

      <rect x="170" y="0" width="90" height="26" rx="13" fill="#161B22" stroke="#30363D" />
      <text x="215" y="17" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">1/250s</text>
    </g>
  </g>

  <!-- Change Photo / PickPhoto Button -->
  <g transform="translate(20, 552)">
    <rect width="350" height="52" rx="16" fill="#21262D" stroke="#30363D" />
    <text x="175" y="32" text-anchor="middle" font-size="14" font-weight="600" fill="#FFFFFF">Changer de photo source</text>
  </g>

  <!-- CTA Next Step (48dp height minimum) -->
  <g transform="translate(20, 616)">
    <rect width="350" height="56" rx="18" fill="url(#goldAccent)" />
    <text x="175" y="34" text-anchor="middle" font-size="15" font-weight="700" fill="#000000">Continuer vers Référence (2/5) →</text>
  </g>
"""

with open("scripts/apres_svgs/Studio - Etape 1 Source.svg", "w", encoding="utf-8") as f:
    f.write(wrap_screen("Studio / Étape 1 Source", s1, "studio"))

print("Script template ready. Adding next screens...")
