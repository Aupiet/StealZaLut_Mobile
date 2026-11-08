import os

svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 1350" width="1600" height="1350">
  <defs>
    <style>
      @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&amp;family=Playfair+Display:ital,wght@0,600;0,700;1,600&amp;family=JetBrains+Mono:wght@500;600&amp;display=swap');
      text { font-family: 'Plus Jakarta Sans', -apple-system, sans-serif; }
      .mono { font-family: 'JetBrains Mono', monospace; }
      .serif { font-family: 'Playfair Display', Georgia, serif; }
      .title { font-weight: 700; fill: #FFFFFF; }
      .subtitle { font-weight: 500; fill: #94A3B8; }
      .card { fill: #161B22; stroke: #30363D; stroke-width: 1; rx: 16; }
      .inner-card { fill: #0D1117; stroke: #21262D; stroke-width: 1; rx: 12; }
      .section-hdr { font-weight: 700; fill: #E5A93C; font-size: 18px; letter-spacing: 0.5px; }
      .badge { rx: 6; font-size: 11px; font-weight: 600; }
    </style>
    <linearGradient id="goldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#F59E0B" />
      <stop offset="100%" stop-color="#D97706" />
    </linearGradient>
    <linearGradient id="cyanGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#26C6DA" />
      <stop offset="100%" stop-color="#00838F" />
    </linearGradient>
    <linearGradient id="oledGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FF6D00" />
      <stop offset="100%" stop-color="#DD2C00" />
    </linearGradient>
    <linearGradient id="dayGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#C85A17" />
      <stop offset="100%" stop-color="#9C3D06" />
    </linearGradient>
  </defs>

  <!-- Background Canvas -->
  <rect width="1600" height="1350" fill="#0A0C10" />

  <!-- Top Hero Header -->
  <g transform="translate(60, 50)">
    <rect width="1480" height="110" rx="20" fill="#12161F" stroke="#21262D" stroke-width="1.5" />
    <circle cx="50" cy="55" r="28" fill="#E5A93C" fill-opacity="0.15" stroke="#E5A93C" stroke-width="1.5" />
    <path d="M42 63 L50 43 L58 63 Z" fill="#E5A93C" />
    <circle cx="50" cy="48" r="3" fill="#12161F" />
    <text x="96" y="48" class="title serif" font-size="28">StealZaLUT Mobile — Design System Modulaire</text>
    <text x="96" y="74" class="subtitle" font-size="14">Architecture 3 Couches : Primitives • Sémantiques • Composants Szl* (Figma &lt;=&gt; Jetpack Compose 1:1)</text>
    <rect x="1320" y="38" width="130" height="34" rx="17" fill="#E5A93C" fill-opacity="0.15" stroke="#E5A93C" stroke-width="1" />
    <text x="1385" y="60" text-anchor="middle" fill="#E5A93C" font-size="12" font-weight="700">VERSION 2.0</text>
  </g>

  <!-- COLUMN 1: THEMES & COLOR SYSTEM (x=60, width=460) -->
  <g transform="translate(60, 180)">
    <rect width="460" height="1120" class="card" />
    <text x="30" y="45" class="section-hdr">01 · THÈMES &amp; COULEURS SÉMANTIQUES</text>
    <text x="30" y="70" class="subtitle" font-size="13">Abstraction ThemeSpec unifiée (4 identités interchangeables)</text>

    <!-- Theme 1: AmberNoir (Signature) -->
    <g transform="translate(30, 95)">
      <rect width="400" height="130" class="inner-card" />
      <rect x="16" y="16" width="36" height="36" rx="8" fill="#E5A93C" />
      <text x="64" y="32" class="title" font-size="15">AmberNoir (Défaut Dark)</text>
      <text x="64" y="48" class="subtitle" font-size="12">Atmosphère chambre noire chic &amp; feutrée</text>
      
      <!-- Swatches -->
      <g transform="translate(16, 68)">
        <rect x="0" y="0" width="68" height="42" rx="6" fill="#0D0F12" stroke="#30363D" />
        <text x="34" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#0D0F12</text>
        <text x="34" y="37" text-anchor="middle" font-size="9" fill="#64748B">Surface</text>

        <rect x="74" y="0" width="68" height="42" rx="6" fill="#161B22" stroke="#30363D" />
        <text x="108" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#161B22</text>
        <text x="108" y="37" text-anchor="middle" font-size="9" fill="#64748B">Card</text>

        <rect x="148" y="0" width="68" height="42" rx="6" fill="#E5A93C" />
        <text x="182" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#E5A93C</text>
        <text x="182" y="37" text-anchor="middle" font-size="9" fill="#000000" font-weight="600">Accent</text>

        <rect x="222" y="0" width="68" height="42" rx="6" fill="#2979FF" />
        <text x="256" y="24" text-anchor="middle" class="mono" font-size="10" fill="#FFFFFF">#2979FF</text>
        <text x="256" y="37" text-anchor="middle" font-size="9" fill="#FFFFFF">Secondary</text>

        <rect x="296" y="0" width="72" height="42" rx="6" fill="#FFFFFF" />
        <text x="332" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#FFFFFF</text>
        <text x="332" y="37" text-anchor="middle" font-size="9" fill="#000000">onSurface</text>
      </g>
    </g>

    <!-- Theme 2: CyanStudio -->
    <g transform="translate(30, 240)">
      <rect width="400" height="130" class="inner-card" />
      <rect x="16" y="16" width="36" height="36" rx="8" fill="#26C6DA" />
      <text x="64" y="32" class="title" font-size="15">CyanStudio (Pro Lab)</text>
      <text x="64" y="48" class="subtitle" font-size="12">Contraste technique moderne haute précision</text>
      
      <g transform="translate(16, 68)">
        <rect x="0" y="0" width="68" height="42" rx="6" fill="#101418" stroke="#30363D" />
        <text x="34" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#101418</text>
        <text x="34" y="37" text-anchor="middle" font-size="9" fill="#64748B">Surface</text>

        <rect x="74" y="0" width="68" height="42" rx="6" fill="#182026" stroke="#30363D" />
        <text x="108" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#182026</text>
        <text x="108" y="37" text-anchor="middle" font-size="9" fill="#64748B">Card</text>

        <rect x="148" y="0" width="68" height="42" rx="6" fill="#26C6DA" />
        <text x="182" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#26C6DA</text>
        <text x="182" y="37" text-anchor="middle" font-size="9" fill="#000000" font-weight="600">Accent</text>

        <rect x="222" y="0" width="68" height="42" rx="6" fill="#80DEEA" />
        <text x="256" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#80DEEA</text>
        <text x="256" y="37" text-anchor="middle" font-size="9" fill="#000000">Secondary</text>

        <rect x="296" y="0" width="72" height="42" rx="6" fill="#ECEFF1" />
        <text x="332" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#ECEFF1</text>
        <text x="332" y="37" text-anchor="middle" font-size="9" fill="#000000">onSurface</text>
      </g>
    </g>

    <!-- Theme 3: OledBlack -->
    <g transform="translate(30, 385)">
      <rect width="400" height="130" class="inner-card" />
      <rect x="16" y="16" width="36" height="36" rx="8" fill="#FF6D00" />
      <text x="64" y="32" class="title" font-size="15">OledBlack (Contraste Infini)</text>
      <text x="64" y="48" class="subtitle" font-size="12">Fond noir pur #000000 &amp; orange vif dynamique</text>
      
      <g transform="translate(16, 68)">
        <rect x="0" y="0" width="68" height="42" rx="6" fill="#000000" stroke="#30363D" />
        <text x="34" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#000000</text>
        <text x="34" y="37" text-anchor="middle" font-size="9" fill="#64748B">Surface</text>

        <rect x="74" y="0" width="68" height="42" rx="6" fill="#121212" stroke="#30363D" />
        <text x="108" y="24" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">#121212</text>
        <text x="108" y="37" text-anchor="middle" font-size="9" fill="#64748B">Card</text>

        <rect x="148" y="0" width="68" height="42" rx="6" fill="#FF6D00" />
        <text x="182" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#FF6D00</text>
        <text x="182" y="37" text-anchor="middle" font-size="9" fill="#000000" font-weight="600">Accent</text>

        <rect x="222" y="0" width="68" height="42" rx="6" fill="#FF9E80" />
        <text x="256" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#FF9E80</text>
        <text x="256" y="37" text-anchor="middle" font-size="9" fill="#000000">Secondary</text>

        <rect x="296" y="0" width="72" height="42" rx="6" fill="#FFFFFF" />
        <text x="332" y="24" text-anchor="middle" class="mono" font-size="10" fill="#000000">#FFFFFF</text>
        <text x="332" y="37" text-anchor="middle" font-size="9" fill="#000000">onSurface</text>
      </g>
    </g>

    <!-- Theme 4: Daylight (Chic Light) -->
    <g transform="translate(30, 530)">
      <rect width="400" height="130" class="inner-card" />
      <rect x="16" y="16" width="36" height="36" rx="8" fill="#C85A17" />
      <text x="64" y="32" class="title" font-size="15">Daylight (Clair Chaud)</text>
      <text x="64" y="48" class="subtitle" font-size="12">Ivoire texturé &amp; terre cuite toscane chaleureuse</text>
      
      <g transform="translate(16, 68)">
        <rect x="0" y="0" width="68" height="42" rx="6" fill="#FBF9F5" stroke="#E2E8F0" />
        <text x="34" y="24" text-anchor="middle" class="mono" font-size="10" fill="#1E293B">#FBF9F5</text>
        <text x="34" y="37" text-anchor="middle" font-size="9" fill="#475569">Surface</text>

        <rect x="74" y="0" width="68" height="42" rx="6" fill="#F2EFE9" stroke="#E2E8F0" />
        <text x="108" y="24" text-anchor="middle" class="mono" font-size="10" fill="#1E293B">#F2EFE9</text>
        <text x="108" y="37" text-anchor="middle" font-size="9" fill="#475569">Card</text>

        <rect x="148" y="0" width="68" height="42" rx="6" fill="#C85A17" />
        <text x="182" y="24" text-anchor="middle" class="mono" font-size="10" fill="#FFFFFF">#C85A17</text>
        <text x="182" y="37" text-anchor="middle" font-size="9" fill="#FFFFFF" font-weight="600">Accent</text>

        <rect x="222" y="0" width="68" height="42" rx="6" fill="#E07A5F" />
        <text x="256" y="24" text-anchor="middle" class="mono" font-size="10" fill="#FFFFFF">#E07A5F</text>
        <text x="256" y="37" text-anchor="middle" font-size="9" fill="#FFFFFF">Secondary</text>

        <rect x="296" y="0" width="72" height="42" rx="6" fill="#1C1917" />
        <text x="332" y="24" text-anchor="middle" class="mono" font-size="10" fill="#FFFFFF">#1C1917</text>
        <text x="332" y="37" text-anchor="middle" font-size="9" fill="#E2E8F0">onSurface</text>
      </g>
    </g>

    <!-- Specialized Photographic Tokens -->
    <g transform="translate(30, 680)">
      <rect width="400" height="410" class="inner-card" />
      <text x="20" y="32" class="title" font-size="14">Tokens Métier Photo (SzlColors)</text>
      <text x="20" y="50" class="subtitle" font-size="11">Rôles sémantiques indispensables à l&#39;édition</text>

      <g transform="translate(20, 70)">
        <rect x="0" y="0" width="28" height="28" rx="6" fill="#2979FF" />
        <text x="40" y="18" class="title" font-size="12">clippingShadows</text>
        <text x="190" y="18" class="mono" font-size="11" fill="#94A3B8">#2979FF</text>
        <text x="260" y="18" class="subtitle" font-size="11">Alerte sous-exposition (0)</text>

        <rect x="0" y="40" width="28" height="28" rx="6" fill="#FF1744" />
        <text x="40" y="58" class="title" font-size="12">clippingHighlights</text>
        <text x="190" y="58" class="mono" font-size="11" fill="#94A3B8">#FF1744</text>
        <text x="260" y="58" class="subtitle" font-size="11">Alerte sur-exposition (255)</text>

        <rect x="0" y="80" width="28" height="28" rx="6" fill="#000000" fill-opacity="0.6" stroke="#4B5563" />
        <text x="40" y="98" class="title" font-size="12">cropOverlay</text>
        <text x="190" y="98" class="mono" font-size="11" fill="#94A3B8">#99000000</text>
        <text x="260" y="98" class="subtitle" font-size="11">Voile assombrissant</text>

        <rect x="0" y="120" width="28" height="28" rx="6" fill="#FFFFFF" />
        <text x="40" y="138" class="title" font-size="12">cropHandle</text>
        <text x="190" y="138" class="mono" font-size="11" fill="#94A3B8">#FFFFFFFF</text>
        <text x="260" y="138" class="subtitle" font-size="11">Coins tactiles 48dp</text>

        <rect x="0" y="160" width="28" height="28" rx="6" fill="#FFFFFF" fill-opacity="0.3" stroke="#9CA3AF" />
        <text x="40" y="178" class="title" font-size="12">cropGrid</text>
        <text x="190" y="178" class="mono" font-size="11" fill="#94A3B8">#4DFFFFFF</text>
        <text x="260" y="178" class="subtitle" font-size="11">Grille des tiers</text>

        <rect x="0" y="200" width="28" height="28" rx="6" fill="#E5A93C" />
        <text x="40" y="218" class="title" font-size="12">toolActiveDot</text>
        <text x="190" y="218" class="mono" font-size="11" fill="#94A3B8">#E5A93C</text>
        <text x="260" y="218" class="subtitle" font-size="11">Indicateur outil modifié</text>

        <rect x="0" y="240" width="28" height="28" rx="6" fill="#21262D" stroke="#30363D" />
        <text x="40" y="258" class="title" font-size="12">exportTrack</text>
        <text x="190" y="258" class="mono" font-size="11" fill="#94A3B8">#21262D</text>
        <text x="260" y="258" class="subtitle" font-size="11">Fond jauge de progression</text>

        <rect x="0" y="280" width="28" height="28" rx="6" fill="#10B981" />
        <text x="40" y="298" class="title" font-size="12">exportProgress</text>
        <text x="190" y="298" class="mono" font-size="11" fill="#94A3B8">#10B981</text>
        <text x="260" y="298" class="subtitle" font-size="11">Barre tuiles export</text>
      </g>
    </g>
  </g>

  <!-- COLUMN 2: TYPOGRAPHY & SHAPES/SPACING (x=550, width=460) -->
  <g transform="translate(550, 180)">
    <rect width="460" height="1120" class="card" />
    <text x="30" y="45" class="section-hdr">02 · TYPOGRAPHIE &amp; GÉOMÉTRIE</text>
    <text x="30" y="70" class="subtitle" font-size="13">Échelle typographique et métriques tactiles strictes</text>

    <!-- FontSpec Families -->
    <g transform="translate(30, 95)">
      <rect width="400" height="180" class="inner-card" />
      <text x="20" y="28" class="title" font-size="14">FontSpec Abstraction (3 familles max)</text>
      
      <g transform="translate(20, 45)">
        <text x="0" y="22" class="serif" font-size="20" fill="#E5A93C">Playfair Display Serif</text>
        <text x="240" y="20" class="mono" font-size="11" fill="#64748B">FontSpec.title</text>
        <text x="0" y="38" class="subtitle" font-size="11">Titres prestigieux, écran Studio résultat &amp; branding chic</text>

        <text x="0" y="72" font-size="17" font-weight="600" fill="#FFFFFF">Plus Jakarta Sans</text>
        <text x="240" y="70" class="mono" font-size="11" fill="#64748B">FontSpec.body</text>
        <text x="0" y="88" class="subtitle" font-size="11">Haute lisibilité mobile, panneaux d&#39;outils &amp; dialogues</text>

        <text x="0" y="118" class="mono" font-size="14" font-weight="600" fill="#38BDF8">JetBrains Mono 120.00</text>
        <text x="240" y="116" class="mono" font-size="11" fill="#64748B">FontSpec.mono</text>
        <text x="0" y="132" class="subtitle" font-size="11">Valeurs numériques précises, pourcentages, coordonnées</text>
      </g>
    </g>

    <!-- Type Scales with strict minimum >= 12sp -->
    <g transform="translate(30, 295)">
      <rect width="400" height="375" class="inner-card" />
      <text x="20" y="28" class="title" font-size="14">Échelle Typographique (SzlTypography)</text>
      <text x="20" y="44" class="subtitle" font-size="11">Règle FE-023 : Strictement &gt;= 12 sp (fin du 10 sp illisible)</text>

      <g transform="translate(20, 65)">
        <!-- Display Large -->
        <text x="0" y="26" class="serif" font-size="28" font-weight="700" fill="#FFFFFF">Display Large</text>
        <text x="280" y="24" class="mono" font-size="11" fill="#E5A93C">32 sp • 700</text>

        <!-- Title Large -->
        <text x="0" y="65" font-size="20" font-weight="700" fill="#FFFFFF">Title Large (Écrans)</text>
        <text x="280" y="63" class="mono" font-size="11" fill="#E5A93C">22 sp • 700</text>

        <!-- Title Medium -->
        <text x="0" y="102" font-size="16" font-weight="600" fill="#F1F5F9">Title Medium (Sections)</text>
        <text x="280" y="100" class="mono" font-size="11" fill="#94A3B8">18 sp • 600</text>

        <!-- Body Large -->
        <text x="0" y="138" font-size="15" font-weight="400" fill="#CBD5E1">Body Large (Courant long)</text>
        <text x="280" y="136" class="mono" font-size="11" fill="#94A3B8">16 sp • 400</text>

        <!-- Body Medium -->
        <text x="0" y="172" font-size="13" font-weight="400" fill="#94A3B8">Body Medium (Explications)</text>
        <text x="280" y="170" class="mono" font-size="11" fill="#94A3B8">14 sp • 400</text>

        <!-- Label Large -->
        <text x="0" y="206" font-size="13" font-weight="700" fill="#E5A93C">LABEL LARGE (BOUTONS)</text>
        <text x="280" y="204" class="mono" font-size="11" fill="#E5A93C">14 sp • 700</text>

        <!-- Label Medium -->
        <text x="0" y="238" font-size="12" font-weight="600" fill="#E2E8F0">Label Medium (Onglets/Pills)</text>
        <text x="280" y="236" class="mono" font-size="11" fill="#94A3B8">13 sp • 600</text>

        <!-- Label Small (Fixing FE-023) -->
        <g transform="translate(0, 258)">
          <rect x="-8" y="0" width="375" height="36" rx="6" fill="#10B981" fill-opacity="0.1" stroke="#10B981" stroke-width="1" />
          <text x="6" y="22" font-size="12" font-weight="600" fill="#10B981">Label Small (Légendes outils)</text>
          <text x="260" y="22" class="mono" font-size="11" font-weight="700" fill="#10B981">12 sp (FIX FE-023)</text>
        </g>
      </g>
    </g>

    <!-- Spacing & Radii (AppDimens) -->
    <g transform="translate(30, 690)">
      <rect width="400" height="400" class="inner-card" />
      <text x="20" y="28" class="title" font-size="14">Espacements &amp; Cibles Tactiles (AppDimens)</text>
      
      <!-- Min Interactive Target 48dp banner -->
      <g transform="translate(20, 45)">
        <rect width="360" height="52" rx="10" fill="#E5A93C" fill-opacity="0.12" stroke="#E5A93C" stroke-width="1.5" />
        <circle cx="26" cy="26" r="16" fill="#E5A93C" />
        <path d="M22 26 L25 29 L31 22" stroke="#000000" stroke-width="2.5" fill="none" stroke-linecap="round" />
        <text x="52" y="23" font-size="13" font-weight="700" fill="#FFFFFF">MinInteractiveTarget = 48.dp</text>
        <text x="52" y="39" class="subtitle" font-size="11">Norme WCAG AA respectée (était 44.dp - Fix FE-019)</text>
      </g>

      <!-- Spacing scale -->
      <g transform="translate(20, 115)">
        <text x="0" y="16" class="title" font-size="12">Échelle d&#39;espacements standardisés</text>
        <g transform="translate(0, 28)">
          <rect x="0" y="0" width="30" height="18" rx="4" fill="#1E293B" /><text x="15" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">0</text>
          <rect x="36" y="0" width="30" height="18" rx="4" fill="#1E293B" /><text x="51" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">2</text>
          <rect x="72" y="0" width="30" height="18" rx="4" fill="#1E293B" /><text x="87" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">4</text>
          <rect x="108" y="0" width="30" height="18" rx="4" fill="#1E293B" /><text x="123" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">8</text>
          <rect x="144" y="0" width="32" height="18" rx="4" fill="#1E293B" /><text x="160" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">12</text>
          <rect x="182" y="0" width="32" height="18" rx="4" fill="#E5A93C" fill-opacity="0.3" stroke="#E5A93C" /><text x="198" y="13" text-anchor="middle" class="mono" font-size="10" fill="#E5A93C">16</text>
          <rect x="220" y="0" width="32" height="18" rx="4" fill="#1E293B" /><text x="236" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">20</text>
          <rect x="258" y="0" width="32" height="18" rx="4" fill="#1E293B" /><text x="274" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">24</text>
          <rect x="296" y="0" width="32" height="18" rx="4" fill="#1E293B" /><text x="312" y="13" text-anchor="middle" class="mono" font-size="10" fill="#94A3B8">32</text>
        </g>
      </g>

      <!-- Corner Radii -->
      <g transform="translate(20, 190)">
        <text x="0" y="16" class="title" font-size="12">Rayons de courbure chic &amp; ludiques</text>
        <g transform="translate(0, 30)">
          <rect x="0" y="0" width="70" height="50" rx="4" fill="#161B22" stroke="#30363D" />
          <text x="35" y="28" text-anchor="middle" class="mono" font-size="11" fill="#FFFFFF">4 dp</text>
          <text x="35" y="42" text-anchor="middle" class="subtitle" font-size="9">Xs</text>

          <rect x="80" y="0" width="70" height="50" rx="8" fill="#161B22" stroke="#30363D" />
          <text x="115" y="28" text-anchor="middle" class="mono" font-size="11" fill="#FFFFFF">8 dp</text>
          <text x="115" y="42" text-anchor="middle" class="subtitle" font-size="9">Sm (Badges)</text>

          <rect x="160" y="0" width="70" height="50" rx="12" fill="#161B22" stroke="#30363D" />
          <text x="195" y="28" text-anchor="middle" class="mono" font-size="11" fill="#FFFFFF">12 dp</text>
          <text x="195" y="42" text-anchor="middle" class="subtitle" font-size="9">Md (Cartes)</text>

          <rect x="240" y="0" width="70" height="50" rx="20" fill="#161B22" stroke="#E5A93C" />
          <text x="275" y="28" text-anchor="middle" class="mono" font-size="11" fill="#E5A93C">20 dp</text>
          <text x="275" y="42" text-anchor="middle" class="subtitle" font-size="9">Lg (Modales)</text>
        </g>
      </g>
    </g>
  </g>

  <!-- COLUMN 3: SZL* COMPONENTS & ICON SETS (x=1040, width=500) -->
  <g transform="translate(1040, 180)">
    <rect width="500" height="1120" class="card" />
    <text x="30" y="45" class="section-hdr">03 · COMPOSANTS SZL* &amp; ICONOGRAPHIE</text>
    <text x="30" y="70" class="subtitle" font-size="13">Composants réutilisables &amp; jeux d&#39;icônes sémantiques</text>

    <!-- Component 1: SzlSlider with Double Tap Reset -->
    <g transform="translate(30, 95)">
      <rect width="440" height="145" class="inner-card" />
      <g transform="translate(20, 20)">
        <text x="0" y="16" class="title" font-size="13">SzlSlider (Double-Tap Reset &amp; Valeur Active)</text>
        <rect x="330" y="2" width="65" height="20" rx="10" fill="#E5A93C" fill-opacity="0.15" />
        <text x="362" y="16" text-anchor="middle" class="mono" font-size="11" font-weight="700" fill="#E5A93C">+0.45 EV</text>

        <!-- Slider Bar -->
        <g transform="translate(0, 45)">
          <text x="0" y="16" class="subtitle" font-size="12">Exposition</text>
          <rect x="90" y="6" width="305" height="6" rx="3" fill="#21262D" />
          <rect x="90" y="6" width="185" height="6" rx="3" fill="#E5A93C" />
          <circle cx="275" cy="9" r="10" fill="#FFFFFF" stroke="#E5A93C" stroke-width="3" />
        </g>

        <!-- Feature tags -->
        <g transform="translate(0, 90)">
          <rect x="0" y="0" width="125" height="22" rx="4" fill="#1E293B" />
          <text x="62" y="15" text-anchor="middle" font-size="10" fill="#94A3B8">Double-tap -&gt; 0.0</text>

          <rect x="135" y="0" width="135" height="22" rx="4" fill="#1E293B" />
          <text x="202" y="15" text-anchor="middle" font-size="10" fill="#94A3B8">Cible tactile &gt;= 48dp</text>

          <rect x="280" y="0" width="115" height="22" rx="4" fill="#10B981" fill-opacity="0.15" />
          <text x="337" y="15" text-anchor="middle" font-size="10" fill="#10B981" font-weight="600">FE-006 Undo/Redo</text>
        </g>
      </g>
    </g>

    <!-- Component 2: SzlBipolarSlider (Color Temp) -->
    <g transform="translate(30, 255)">
      <rect width="440" height="120" class="inner-card" />
      <g transform="translate(20, 20)">
        <text x="0" y="16" class="title" font-size="13">SzlBipolarSlider (Centre neutre)</text>
        <rect x="330" y="2" width="65" height="20" rx="10" fill="#38BDF8" fill-opacity="0.15" />
        <text x="362" y="16" text-anchor="middle" class="mono" font-size="11" font-weight="700" fill="#38BDF8">-15 K</text>

        <g transform="translate(0, 45)">
          <text x="0" y="16" class="subtitle" font-size="12">Température</text>
          <!-- Track with cold/warm gradient -->
          <rect x="90" y="6" width="305" height="6" rx="3" fill="#21262D" />
          <circle cx="242" cy="9" r="3" fill="#64748B" />
          <rect x="190" y="6" width="52" height="6" rx="3" fill="#38BDF8" />
          <circle cx="190" cy="9" r="10" fill="#FFFFFF" stroke="#38BDF8" stroke-width="3" />
        </g>
      </g>
    </g>

    <!-- Component 3: Crop Overlay Specs (FE-005) -->
    <g transform="translate(30, 390)">
      <rect width="440" height="180" class="inner-card" />
      <g transform="translate(20, 20)">
        <text x="0" y="16" class="title" font-size="13">EditorCropOverlay (Outil #1 Recadrage FE-005)</text>
        <text x="0" y="34" class="subtitle" font-size="11">Ratios 1:1, 4:5, 16:9, Original + Poignées de coin 48dp</text>

        <!-- Mini Crop Box Preview -->
        <g transform="translate(10, 50)">
          <rect width="180" height="90" rx="4" fill="#000000" fill-opacity="0.6" stroke="#4B5563" />
          <rect x="25" y="10" width="130" height="70" fill="none" stroke="#FFFFFF" stroke-width="1.5" />
          <!-- Thirds grid -->
          <line x1="68" y1="10" x2="68" y2="80" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="2 2" />
          <line x1="112" y1="10" x2="112" y2="80" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="2 2" />
          <line x1="25" y1="33" x2="155" y2="33" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="2 2" />
          <line x1="25" y1="57" x2="155" y2="57" stroke="#FFFFFF" stroke-opacity="0.4" stroke-dasharray="2 2" />
          <!-- Corner handles -->
          <circle cx="25" cy="10" r="4" fill="#E5A93C" />
          <circle cx="155" cy="10" r="4" fill="#E5A93C" />
          <circle cx="25" cy="80" r="4" fill="#E5A93C" />
          <circle cx="155" cy="80" r="4" fill="#E5A93C" />
        </g>

        <!-- Ratios pills -->
        <g transform="translate(210, 52)">
          <rect x="0" y="0" width="55" height="24" rx="12" fill="#E5A93C" />
          <text x="27" y="16" text-anchor="middle" font-size="11" font-weight="700" fill="#000000">1:1</text>

          <rect x="65" y="0" width="55" height="24" rx="12" fill="#21262D" stroke="#30363D" />
          <text x="92" y="16" text-anchor="middle" font-size="11" font-weight="600" fill="#CBD5E1">4:5</text>

          <rect x="130" y="0" width="55" height="24" rx="12" fill="#21262D" stroke="#30363D" />
          <text x="157" y="16" text-anchor="middle" font-size="11" font-weight="600" fill="#CBD5E1">16:9</text>

          <rect x="0" y="32" width="90" height="24" rx="12" fill="#21262D" stroke="#30363D" />
          <text x="45" y="48" text-anchor="middle" font-size="11" font-weight="600" fill="#CBD5E1">Original</text>

          <rect x="100" y="32" width="85" height="24" rx="12" fill="#21262D" stroke="#30363D" />
          <text x="142" y="48" text-anchor="middle" font-size="11" font-weight="600" fill="#CBD5E1">Libre</text>
        </g>
      </g>
    </g>

    <!-- Semantic Icons Set (SzlIconSet Comparison) -->
    <g transform="translate(30, 585)">
      <rect width="440" height="505" class="inner-card" />
      <g transform="translate(20, 20)">
        <text x="0" y="16" class="title" font-size="14">SzlIconSet (Jeux d&#39;icônes interchangeables)</text>
        <text x="0" y="34" class="subtitle" font-size="11">Remplacement global en 1 ligne dans ThemeSpec</text>

        <!-- Table Header -->
        <g transform="translate(0, 50)">
          <text x="10" y="16" class="subtitle" font-size="11" font-weight="700">RÔLE SÉMANTIQUE</text>
          <text x="160" y="16" class="subtitle" font-size="11" font-weight="700">ROUNDED (Chic/Doux)</text>
          <text x="300" y="16" class="subtitle" font-size="11" font-weight="700">OUTLINED (Studio)</text>
          <line x1="0" y1="24" x2="400" y2="24" stroke="#21262D" />
        </g>

        <!-- Icon Rows -->
        <g transform="translate(0, 85)">
          <!-- ToolCrop -->
          <text x="10" y="18" class="mono" font-size="11" fill="#E5A93C">ToolCrop</text>
          <rect x="195" y="2" width="22" height="22" rx="4" fill="#E5A93C" fill-opacity="0.2" stroke="#E5A93C" />
          <rect x="335" y="2" width="22" height="22" rx="0" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolBasic -->
          <text x="10" y="50" class="mono" font-size="11" fill="#E5A93C">ToolBasic</text>
          <circle cx="206" cy="46" r="10" fill="#E5A93C" fill-opacity="0.2" stroke="#E5A93C" />
          <circle cx="346" cy="46" r="10" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolCurve -->
          <text x="10" y="82" class="mono" font-size="11" fill="#E5A93C">ToolCurve</text>
          <path d="M196 82 Q206 70 216 78" stroke="#E5A93C" stroke-width="2.5" fill="none" stroke-linecap="round" />
          <path d="M336 82 Q346 70 356 78" stroke="#CBD5E1" stroke-width="1.5" fill="none" />

          <!-- ToolColorMixer -->
          <text x="10" y="114" class="mono" font-size="11" fill="#E5A93C">ToolColorMixer</text>
          <circle cx="202" cy="110" r="6" fill="#E5A93C" fill-opacity="0.5" />
          <circle cx="210" cy="110" r="6" fill="#2979FF" fill-opacity="0.5" />
          <circle cx="342" cy="110" r="6" fill="none" stroke="#CBD5E1" stroke-width="1.5" />
          <circle cx="350" cy="110" r="6" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolGrading -->
          <text x="10" y="146" class="mono" font-size="11" fill="#E5A93C">ToolGrading</text>
          <circle cx="206" cy="142" r="10" fill="#E5A93C" fill-opacity="0.2" stroke="#E5A93C" stroke-width="2" />
          <circle cx="346" cy="142" r="10" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolDetail -->
          <text x="10" y="178" class="mono" font-size="11" fill="#E5A93C">ToolDetail</text>
          <polygon points="206,170 214,184 198,184" fill="#E5A93C" fill-opacity="0.3" stroke="#E5A93C" />
          <polygon points="346,170 354,184 338,184" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolEffects -->
          <text x="10" y="210" class="mono" font-size="11" fill="#E5A93C">ToolEffects</text>
          <circle cx="206" cy="206" r="4" fill="#E5A93C" />
          <circle cx="214" cy="202" r="2" fill="#E5A93C" />
          <circle cx="346" cy="206" r="4" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolGeometry (FE-009) -->
          <text x="10" y="242" class="mono" font-size="11" fill="#10B981">ToolGeometry</text>
          <rect x="196" y="232" width="20" height="16" rx="4" fill="#10B981" fill-opacity="0.2" stroke="#10B981" />
          <rect x="336" y="232" width="20" height="16" rx="0" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- ToolLens (FE-009) -->
          <text x="10" y="274" class="mono" font-size="11" fill="#10B981">ToolLens</text>
          <circle cx="206" cy="270" r="9" fill="#10B981" fill-opacity="0.2" stroke="#10B981" />
          <circle cx="346" cy="270" r="9" fill="none" stroke="#CBD5E1" stroke-width="1.5" />

          <!-- Export -->
          <text x="10" y="306" class="mono" font-size="11" fill="#E5A93C">Export</text>
          <path d="M206 300 L206 312 M200 306 L206 300 L212 306" stroke="#E5A93C" stroke-width="2" stroke-linecap="round" fill="none" />
          <path d="M346 300 L346 312 M340 306 L346 300 L352 306" stroke="#CBD5E1" stroke-width="1.5" stroke-linecap="round" fill="none" />

          <!-- Undo / Redo -->
          <text x="10" y="338" class="mono" font-size="11" fill="#E5A93C">Undo / Redo</text>
          <path d="M200 334 A6 6 0 1 1 206 340 M198 338 L200 334 L204 334" stroke="#E5A93C" stroke-width="2" fill="none" stroke-linecap="round" />
          <path d="M340 334 A6 6 0 1 1 346 340 M338 338 L340 334 L344 334" stroke="#CBD5E1" stroke-width="1.5" fill="none" stroke-linecap="round" />

          <!-- Compare (Before/After) -->
          <text x="10" y="370" class="mono" font-size="11" fill="#E5A93C">Compare</text>
          <rect x="196" y="360" width="10" height="18" fill="#E5A93C" />
          <rect x="206" y="360" width="10" height="18" fill="none" stroke="#E5A93C" stroke-width="1.5" />
          <rect x="336" y="360" width="10" height="18" fill="none" stroke="#CBD5E1" stroke-width="1.5" />
          <rect x="346" y="360" width="10" height="18" fill="#CBD5E1" />
        </g>
      </g>
    </g>
  </g>

</svg>
"""

with open("scripts/design_system_board.svg", "w", encoding="utf-8") as f:
    f.write(svg_content)

print("Generated scripts/design_system_board.svg successfully.")
