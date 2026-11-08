import os

OUTPUT_DIR = os.path.abspath("scripts/avant_svgs")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Amber Noir Tokens
C_BG = "#121417"
C_SURFACE = "#1A1D24"
C_SURFACE_VAR = "#262A34"
C_OUTLINE = "#383E4D"
C_OUTLINE_VAR = "#2E3440"
C_AMBER = "#FFB300"
C_AMBER_CONTAINER = "#3D2D00"
C_TEXT_PRIMARY = "#F0F2F5"
C_TEXT_SECONDARY = "#A0A7B5"
C_DARKROOM = "#0E0E11"
C_ERROR = "#EF4444"
C_SUCCESS = "#22C55E"
C_WHITE = "#FFFFFF"

def stepper_xml(step=1):
    items = ""
    for i in range(1, 6):
        x = 35 + (i - 1) * 80
        is_done = i < step
        is_cur = i == step
        fill = C_AMBER if (is_done or is_cur) else C_SURFACE_VAR
        text_fill = "#1E1400" if (is_done or is_cur) else C_TEXT_SECONDARY
        stroke = C_AMBER if is_cur else "none"
        symbol = "✓" if is_done else str(i)
        line = f'<line x1="{x+14}" y1="14" x2="{x+66}" y2="14" stroke="{C_OUTLINE}" stroke-width="2"/>' if i < 5 else ""
        items += f'''
        <g>
            {line}
            <circle cx="{x}" cy="14" r="12" fill="{fill}" stroke="{stroke}" stroke-width="2"/>
            <text x="{x}" y="18" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{text_fill}">{symbol}</text>
            <text x="{x}" y="36" text-anchor="middle" font-family="sans-serif" font-size="9" fill="{C_TEXT_SECONDARY}">Étape {i}</text>
        </g>
        '''
    return f'<g transform="translate(0, 10)">{items}</g>'

def base_frame(title, content, active_tab="studio", show_double_nav=False):
    nav_tabs = [
        ("studio", "Studio", '<path d="M12 4v4m0 4v8m-4-8h8m-8-6h8" stroke="{c}" stroke-width="2" stroke-linecap="round"/>'),
        ("editor", "Éditeur", '<path d="M14 4l6 6-10 10H4v-6L14 4z" stroke="{c}" stroke-width="1.8" fill="none"/>'),
        ("palettes", "Palettes", '<circle cx="8" cy="12" r="3" fill="{c}"/><circle cx="16" cy="9" r="3" fill="{c}"/><circle cx="16" cy="15" r="3" fill="{c}"/>'),
        ("profile", "Profil", '<rect x="5" y="5" width="14" height="14" rx="2" stroke="{c}" stroke-width="1.8" fill="none"/><circle cx="10" cy="10" r="2" fill="{c}"/>'),
        ("settings", "Paramètres", '<circle cx="12" cy="12" r="3" stroke="{c}" stroke-width="1.8" fill="none"/><path d="M12 2v3m0 14v3M2 12h3m14 12h3" stroke="{c}" stroke-width="1.8"/>')
    ]
    tab_items = ""
    for i, (tid, label, isvg) in enumerate(nav_tabs):
        cx = i * 78 + 39
        act = (tid == active_tab)
        c = C_AMBER if act else C_TEXT_SECONDARY
        pill = f'<rect x="{cx-24}" y="784" width="48" height="28" rx="14" fill="{C_AMBER_CONTAINER}"/>' if act else ''
        tab_items += f'''
        <g>
            {pill}
            <g transform="translate({cx-12}, 788)">{isvg.replace("{c}", c)}</g>
            <text x="{cx}" y="828" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="{"bold" if act else "normal"}" fill="{c}">{label}</text>
        </g>
        '''
    
    double_bar_alert = ""
    if show_double_nav:
        double_bar_alert = f'''
        <!-- ANNOTATION BUG FE-002: Double bar superposée -->
        <rect x="10" y="720" width="370" height="20" rx="4" fill="#B91C1C" opacity="0.9"/>
        <text x="195" y="734" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#FFFFFF">⚠️ BUG FE-002: Double barre de navigation superposée (140dp)</text>
        '''

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 844" width="390" height="844">
    <defs>
        <clipPath id="sc"><rect width="390" height="844" rx="28"/></clipPath>
        <linearGradient id="photoGrad" x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stop-color="#1A365D"/><stop offset="50%" stop-color="#E28743"/><stop offset="100%" stop-color="#B91C1C"/></linearGradient>
    </defs>
    <rect width="390" height="844" rx="28" fill="{C_BG}" stroke="#22252C" stroke-width="2"/>
    <g clip-path="url(#sc)">
        <!-- Status Bar -->
        <rect x="0" y="0" width="390" height="34" fill="{C_SURFACE}"/>
        <text x="24" y="22" font-family="sans-serif" font-size="12" font-weight="600" fill="{C_TEXT_PRIMARY}">09:41</text>
        <g transform="translate(324, 12)">
            <path d="M0 8h2v4H0zm4-3h2v7H4zm4-3h2v10H8zm4-3h2v13h-2z" fill="{C_TEXT_PRIMARY}"/>
            <path d="M18 3c-3 0-6 2-7 4l7 9 7-9c-1-2-4-4-7-4z" fill="{C_TEXT_PRIMARY}"/>
            <rect x="32" y="3" width="16" height="9" rx="2" fill="none" stroke="{C_TEXT_PRIMARY}" stroke-width="1.2"/>
            <rect x="34" y="5" width="10" height="5" rx="1" fill="{C_TEXT_PRIMARY}"/>
        </g>
        
        <!-- Frame Title Annotation Banner -->
        <rect x="0" y="34" width="390" height="22" fill="#201500"/>
        <text x="195" y="49" text-anchor="middle" font-family="monospace" font-size="9" font-weight="bold" fill="{C_AMBER}">[AVANT] {title}</text>

        <!-- Top App Bar -->
        <rect x="0" y="56" width="390" height="50" fill="{C_SURFACE}" stroke="{C_OUTLINE_VAR}" stroke-width="1"/>
        <text x="195" y="78" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">StealZaLUT Mobile</text>
        <text x="195" y="93" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Studio &amp; Éditeur Colorimétrique</text>
        <g transform="translate(16, 68)">
            <path d="M4 6h16M4 12h16M4 18h16" stroke="{C_AMBER}" stroke-width="2" stroke-linecap="round"/>
        </g>
        <g transform="translate(352, 68)">
            <circle cx="10" cy="10" r="10" fill="none" stroke="{C_AMBER}" stroke-width="1.8"/>
            <text x="10" y="14" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_AMBER}">?</text>
        </g>
        
        <!-- Screen Content -->
        <g transform="translate(0, 106)">
            {content}
        </g>
        
        <!-- Double Bar Alert if relevant -->
        {double_bar_alert}

        <!-- Bottom Navigation Bar -->
        <g id="NavigationBar">
            <rect x="0" y="776" width="390" height="68" fill="{C_SURFACE}" stroke="{C_OUTLINE_VAR}" stroke-width="1"/>
            {tab_items}
        </g>
    </g>
</svg>'''

screens = {}

# 1. Studio Step 1 Source
screens["Studio - Étape 1 Source [reconstruit]"] = base_frame(
    "Studio / Étape 1 Source",
    f'''{stepper_xml(1)}
    <g transform="translate(16, 70)">
        <text x="0" y="0" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">1. Image source (Cliché à étalonner)</text>
        <text x="0" y="18" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Sélectionnez la photo brute ou JPEG que vous souhaitez sublimer.</text>
        
        <!-- ImagePickerCard Empty -->
        <rect x="0" y="36" width="358" height="240" rx="12" fill="{C_SURFACE}" stroke="{C_AMBER}" stroke-width="2" stroke-dasharray="6,4"/>
        <circle cx="179" cy="130" r="32" fill="{C_AMBER_CONTAINER}"/>
        <path d="M167 130h24m-12-12v24" stroke="{C_AMBER}" stroke-width="3" stroke-linecap="round"/>
        <text x="179" y="180" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_AMBER}">Importer une photo source</text>
        <text x="179" y="200" text-anchor="middle" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">JPEG, PNG, DNG, RAW pris en charge</text>
        
        <!-- Next button disabled -->
        <rect x="0" y="310" width="358" height="52" rx="26" fill="{C_SURFACE_VAR}"/>
        <text x="179" y="342" text-anchor="middle" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_SECONDARY}">Suivant : Choisir le style →</text>
    </g>''',
    active_tab="studio"
)

# 2. Studio Step 2 Style - Palettes Cinéma
screens["Studio - Étape 2 Style (Palettes Cinéma) [reconstruit]"] = base_frame(
    "Studio / Étape 2 Style - Palettes Cinéma",
    f'''{stepper_xml(2)}
    <g transform="translate(16, 65)">
        <text x="0" y="0" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">2. Référence de style (Image B ou Palette)</text>
        
        <!-- Segmented Mode Selector -->
        <rect x="0" y="16" width="358" height="38" rx="19" fill="{C_SURFACE_VAR}"/>
        <rect x="2" y="18" width="176" height="34" rx="17" fill="{C_AMBER}"/>
        <text x="90" y="39" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E1400">Palettes cinéma (Presets)</text>
        <text x="268" y="39" text-anchor="middle" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Image de référence</text>
        
        <!-- Presets Cards List -->
        <g transform="translate(0, 68)">
            <!-- Preset 1 -->
            <rect x="0" y="0" width="358" height="74" rx="12" fill="{C_SURFACE}" stroke="{C_AMBER}" stroke-width="2"/>
            <rect x="12" y="12" width="50" height="50" rx="8" fill="url(#photoGrad)"/>
            <text x="74" y="32" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Teal &amp; Orange Hollywood</text>
            <text x="74" y="48" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Contraste chaud/froid classique des blockbusters</text>
            <g transform="translate(74, 54)">
                <rect x="0" y="0" width="20" height="8" rx="2" fill="#0D3B4C"/>
                <rect x="24" y="0" width="20" height="8" rx="2" fill="#E28743"/>
                <rect x="48" y="0" width="20" height="8" rx="2" fill="#E76F51"/>
                <rect x="72" y="0" width="20" height="8" rx="2" fill="#F4A261"/>
            </g>
            <circle cx="334" cy="37" r="10" fill="{C_AMBER}"/>
            <text x="334" y="41" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#1E1400">✓</text>
            
            <!-- Preset 2 -->
            <rect x="0" y="86" width="358" height="74" rx="12" fill="{C_SURFACE}" stroke="{C_OUTLINE_VAR}" stroke-width="1"/>
            <rect x="12" y="98" width="50" height="50" rx="8" fill="#3D3B3C"/>
            <text x="74" y="118" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Bleach Bypass Argentique</text>
            <text x="74" y="134" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Désaturation partielle et contraste tranché</text>
            
            <!-- Preset 3 -->
            <rect x="0" y="172" width="358" height="74" rx="12" fill="{C_SURFACE}" stroke="{C_OUTLINE_VAR}" stroke-width="1"/>
            <rect x="12" y="184" width="50" height="50" rx="8" fill="#B33924"/>
            <text x="74" y="204" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Kodachrome 64 Vintage</text>
            <text x="74" y="220" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Rouges saturés et grain chaud rétro</text>
        </g>
        
        <!-- Buttons row -->
        <g transform="translate(0, 330)">
            <rect x="0" y="0" width="110" height="48" rx="24" fill="{C_SURFACE_VAR}"/>
            <text x="55" y="28" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">← Retour</text>
            <rect x="120" y="0" width="238" height="48" rx="24" fill="{C_AMBER}"/>
            <text x="239" y="28" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="#1E1400">Étape 3 : Réglages →</text>
        </g>
    </g>''',
    active_tab="studio"
)

# 3. Studio Step 2 Style - Image Référence
screens["Studio - Étape 2 Style (Image Référence) [reconstruit]"] = base_frame(
    "Studio / Étape 2 Style - Image Référence",
    f'''{stepper_xml(2)}
    <g transform="translate(16, 65)">
        <text x="0" y="0" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">2. Référence de style (Image B ou Palette)</text>
        
        <!-- Segmented Mode Selector -->
        <rect x="0" y="16" width="358" height="38" rx="19" fill="{C_SURFACE_VAR}"/>
        <text x="90" y="39" text-anchor="middle" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Palettes cinéma (Presets)</text>
        <rect x="180" y="18" width="176" height="34" rx="17" fill="{C_AMBER}"/>
        <text x="268" y="39" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E1400">Image de référence</text>
        
        <!-- Loaded Reference Card -->
        <g transform="translate(0, 70)">
            <rect x="0" y="0" width="358" height="150" rx="12" fill="url(#photoGrad)"/>
            <rect x="0" y="110" width="358" height="40" fill="#000000" opacity="0.6"/>
            <text x="16" y="134" font-family="sans-serif" font-size="12" font-weight="bold" fill="#FFFFFF">Référence B : BladeRunner_2049_proxy.jpg</text>
            <rect x="280" y="118" width="66" height="24" rx="12" fill="{C_AMBER}"/>
            <text x="313" y="134" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#1E1400">Changer</text>
            
            <!-- Extracted Palette Swatches -->
            <text x="0" y="180" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">Palette extraite automatiquement :</text>
            <g transform="translate(0, 192)">
                <rect x="0" y="0" width="54" height="40" rx="6" fill="#0B1D28"/>
                <text x="27" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#0B1D28</text>
                <rect x="61" y="0" width="54" height="40" rx="6" fill="#E27D60"/>
                <text x="88" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#E27D60</text>
                <rect x="122" y="0" width="54" height="40" rx="6" fill="#E8A87C"/>
                <text x="149" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#E8A87C</text>
                <rect x="183" y="0" width="54" height="40" rx="6" fill="#C38D9E"/>
                <text x="210" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#C38D9E</text>
                <rect x="244" y="0" width="54" height="40" rx="6" fill="#41B3A3"/>
                <text x="271" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#41B3A3</text>
                <rect x="305" y="0" width="53" height="40" rx="6" fill="#F4D06F"/>
                <text x="331" y="52" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#F4D06F</text>
            </g>
        </g>
        
        <!-- Navigation Buttons -->
        <g transform="translate(0, 330)">
            <rect x="0" y="0" width="110" height="48" rx="24" fill="{C_SURFACE_VAR}"/>
            <text x="55" y="28" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">← Retour</text>
            <rect x="120" y="0" width="238" height="48" rx="24" fill="{C_AMBER}"/>
            <text x="239" y="28" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="#1E1400">Étape 3 : Réglages →</text>
        </g>
    </g>''',
    active_tab="studio"
)

# 4. Studio Step 3 Paramètres & Aperçu
screens["Studio - Étape 3 Paramètres & Aperçu [reconstruit]"] = base_frame(
    "Studio / Étape 3 Paramètres & Aperçu",
    f'''{stepper_xml(3)}
    <g transform="translate(16, 60)">
        <!-- Pinned Double View Curtain -->
        <rect x="0" y="0" width="358" height="150" rx="12" fill="url(#photoGrad)"/>
        <!-- Curtain wipe separator -->
        <line x1="179" y1="0" x2="179" y2="150" stroke="#FFFFFF" stroke-width="2"/>
        <circle cx="179" cy="75" r="16" fill="{C_AMBER}"/>
        <path d="M174 75l-4-4v8zm10 0l4-4v8z" fill="#1E1400"/>
        <rect x="10" y="10" width="60" height="20" rx="4" fill="#000000" opacity="0.6"/>
        <text x="40" y="24" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#FFFFFF">ORIGINAL</text>
        <rect x="288" y="10" width="60" height="20" rx="4" fill="{C_AMBER}" opacity="0.9"/>
        <text x="318" y="24" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#1E1400">ÉTALONNÉ</text>
        
        <!-- Algorithm Chips -->
        <g transform="translate(0, 160)">
            <text x="0" y="0" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_TEXT_PRIMARY}">Méthode de transfert :</text>
            <rect x="0" y="8" width="94" height="26" rx="13" fill="{C_AMBER}"/>
            <text x="47" y="25" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#1E1400">HM-MKL-HM</text>
            <rect x="100" y="8" width="70" height="26" rx="13" fill="{C_SURFACE_VAR}"/>
            <text x="135" y="25" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Reinhard</text>
            <rect x="176" y="8" width="56" height="26" rx="13" fill="{C_SURFACE_VAR}"/>
            <text x="204" y="25" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">MKL</text>
            <rect x="238" y="8" width="120" height="26" rx="13" fill="{C_SURFACE_VAR}"/>
            <text x="298" y="25" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Monge-Kantorovich</text>
        </g>
        
        <!-- Sliders -->
        <g transform="translate(0, 206)">
            <!-- Slider 1 -->
            <text x="0" y="0" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Intensité du transfert</text>
            <text x="358" y="0" text-anchor="end" font-family="monospace" font-size="11" font-weight="bold" fill="{C_AMBER}">100 %</text>
            <rect x="0" y="6" width="358" height="6" rx="3" fill="{C_SURFACE_VAR}"/>
            <rect x="0" y="6" width="358" height="6" rx="3" fill="{C_AMBER}"/>
            <circle cx="358" cy="9" r="8" fill="{C_AMBER}"/>
            
            <!-- Slider 2 -->
            <text x="0" y="32" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Protection des tons chair</text>
            <text x="358" y="32" text-anchor="end" font-family="monospace" font-size="11" font-weight="bold" fill="{C_AMBER}">50 %</text>
            <rect x="0" y="38" width="358" height="6" rx="3" fill="{C_SURFACE_VAR}"/>
            <rect x="0" y="38" width="179" height="6" rx="3" fill="{C_AMBER}"/>
            <circle cx="179" cy="41" r="8" fill="{C_AMBER}"/>
        </g>
        
        <!-- Action Buttons -->
        <g transform="translate(0, 280)">
            <rect x="0" y="0" width="140" height="44" rx="22" fill="{C_SURFACE_VAR}"/>
            <text x="70" y="26" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_TEXT_PRIMARY}">Exporter .CUBE</text>
            <rect x="150" y="0" width="208" height="44" rx="22" fill="{C_AMBER}"/>
            <text x="254" y="26" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="#1E1400">Calculer la LUT (HD) ⚡</text>
        </g>
    </g>''',
    active_tab="studio"
)

# 5. Studio Step 4 Calcul LUT
screens["Studio - Étape 4 Calcul LUT [reconstruit]"] = base_frame(
    "Studio / Étape 4 Calcul de la LUT",
    f'''{stepper_xml(4)}
    <g transform="translate(16, 80)">
        <rect x="0" y="0" width="358" height="340" rx="16" fill="{C_SURFACE}" stroke="{C_OUTLINE_VAR}" stroke-width="1"/>
        
        <!-- Large Central Spinner -->
        <circle cx="179" cy="100" r="44" fill="none" stroke="{C_SURFACE_VAR}" stroke-width="6"/>
        <path d="M179 56 A44 44 0 0 1 223 100" fill="none" stroke="{C_AMBER}" stroke-width="6" stroke-linecap="round"/>
        <text x="179" y="106" text-anchor="middle" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_AMBER}">68 %</text>
        
        <text x="179" y="170" text-anchor="middle" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Traitement colorimétrique Chaquopy</text>
        <text x="179" y="190" text-anchor="middle" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Application de l'algorithme HM-MKL-HM sur 50 MP...</text>
        
        <!-- Stage Checklist -->
        <g transform="translate(30, 220)">
            <circle cx="12" cy="12" r="8" fill="{C_SUCCESS}"/>
            <text x="12" y="15" text-anchor="middle" font-size="10" font-weight="bold" fill="#FFFFFF">✓</text>
            <text x="28" y="16" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">1. Analyse statistique du proxy couleur (512px)</text>
            
            <circle cx="12" cy="40" r="8" fill="{C_AMBER}"/>
            <text x="12" y="43" text-anchor="middle" font-size="10" font-weight="bold" fill="#1E1400">⏳</text>
            <text x="28" y="44" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_AMBER}">2. Rapprochement chromatique Monge-Kantorovich</text>
            
            <circle cx="12" cy="68" r="8" fill="{C_SURFACE_VAR}"/>
            <text x="28" y="72" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">3. Interpolation tétraédrique 3D LUT (33³)</text>
        </g>
    </g>''',
    active_tab="studio"
)

# 6. Studio Step 5 Résultat & Export
screens["Studio - Étape 5 Résultat & Export [reconstruit]"] = base_frame(
    "Studio / Étape 5 Résultat & Export",
    f'''{stepper_xml(5)}
    <g transform="translate(16, 60)">
        <!-- Graded Image Preview -->
        <rect x="0" y="0" width="358" height="180" rx="12" fill="url(#photoGrad)"/>
        <rect x="12" y="12" width="70" height="22" rx="4" fill="{C_AMBER}"/>
        <text x="47" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="#1E1400">RÉSULTAT</text>
        <rect x="256" y="12" width="90" height="22" rx="4" fill="#000000" opacity="0.6"/>
        <text x="301" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="#FFFFFF">Hold: Comparer</text>
        
        <!-- Dosage slider -->
        <g transform="translate(0, 195)">
            <text x="0" y="0" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Dosage du style final :</text>
            <text x="358" y="0" text-anchor="end" font-family="monospace" font-size="11" font-weight="bold" fill="{C_AMBER}">100 %</text>
            <rect x="0" y="8" width="358" height="6" rx="3" fill="{C_SURFACE_VAR}"/>
            <rect x="0" y="8" width="358" height="6" rx="3" fill="{C_AMBER}"/>
            <circle cx="358" cy="11" r="8" fill="{C_AMBER}"/>
        </g>
        
        <!-- Primary Action: Save to Gallery -->
        <rect x="0" y="235" width="358" height="48" rx="24" fill="{C_AMBER}"/>
        <text x="179" y="264" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="#1E1400">Enregistrer l'image dans la galerie</text>
        
        <!-- Secondary Actions Row -->
        <g transform="translate(0, 292)">
            <rect x="0" y="0" width="172" height="42" rx="10" fill="{C_SURFACE_VAR}"/>
            <text x="86" y="25" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_TEXT_PRIMARY}">Exporter .CUBE</text>
            <rect x="186" y="0" width="172" height="42" rx="10" fill="{C_SURFACE_VAR}"/>
            <text x="272" y="25" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_TEXT_PRIMARY}">Partager la LUT</text>
        </g>
        
        <!-- Retoucher dans l'éditeur -->
        <rect x="0" y="344" width="358" height="42" rx="10" fill="{C_AMBER_CONTAINER}" stroke="{C_AMBER}" stroke-width="1"/>
        <text x="179" y="369" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_AMBER}">🎨 Retoucher dans l'éditeur avancé</text>
    </g>''',
    active_tab="studio"
)

# 7. Éditeur État vide
screens["Éditeur - État vide [reconstruit]"] = base_frame(
    "Éditeur / État vide",
    f'''<rect x="0" y="0" width="390" height="670" fill="{C_DARKROOM}"/>
    <g transform="translate(24, 180)">
        <rect x="0" y="0" width="342" height="260" rx="16" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <circle cx="171" cy="70" r="32" fill="{C_SURFACE_VAR}"/>
        <!-- Photo Camera icon -->
        <path d="M157 65h6l3-5h10l3 5h6a4 4 0 0 1 4 4v18a4 4 0 0 1-4 4h-28a4 4 0 0 1-4-4V69a4 4 0 0 1 4-4z" stroke="{C_AMBER}" stroke-width="2" fill="none"/>
        <circle cx="171" cy="78" r="6" stroke="{C_AMBER}" stroke-width="2" fill="none"/>
        
        <text x="171" y="135" text-anchor="middle" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Chambre noire professionnelle</text>
        <text x="171" y="156" text-anchor="middle" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Aucune photo chargée pour la retouche.</text>
        
        <!-- Button Photo Picker (FE-030) -->
        <rect x="41" y="180" width="260" height="48" rx="24" fill="{C_AMBER}"/>
        <text x="171" y="209" text-anchor="middle" font-family="sans-serif" font-size="14" font-weight="bold" fill="#1E1400">Choisir une photo</text>
    </g>''',
    active_tab="editor"
)

# 8. Éditeur Viewport & Barre d'outils
screens["Éditeur - Viewport & Barre d'outils [reconstruit]"] = base_frame(
    "Éditeur / Viewport & Barre d'outils",
    f'''<rect x="0" y="0" width="390" height="670" fill="{C_DARKROOM}"/>
    
    <!-- Loaded Photo in Darkroom Viewport -->
    <rect x="16" y="10" width="358" height="440" rx="8" fill="url(#photoGrad)"/>
    
    <!-- Floating Top Strip -->
    <g transform="translate(24, 20)">
        <rect x="0" y="0" width="110" height="30" rx="15" fill="#000000" opacity="0.6"/>
        <circle cx="14" cy="15" r="4" fill="{C_TEXT_SECONDARY}"/>
        <text x="26" y="19" font-family="sans-serif" font-size="11" fill="#FFFFFF">Aucune LUT</text>
        
        <!-- Right icons: Histogram, AutoEdit -->
        <rect x="230" y="0" width="100" height="30" rx="15" fill="#000000" opacity="0.6"/>
        <text x="250" y="19" font-family="sans-serif" font-size="11" fill="{C_AMBER}">Auto ⚡</text>
        <text x="300" y="19" font-family="sans-serif" font-size="11" fill="#FFFFFF">RGB 📊</text>
    </g>
    
    <!-- RGB Histogram Compact Overlay -->
    <g transform="translate(240, 60)">
        <rect x="0" y="0" width="124" height="60" rx="6" fill="#000000" opacity="0.75" stroke="{C_OUTLINE}" stroke-width="1"/>
        <path d="M4 54 Q30 20 60 40 T120 54" fill="none" stroke="#FF5252" stroke-width="1.5"/>
        <path d="M4 54 Q45 10 80 45 T120 54" fill="none" stroke="#69F0AE" stroke-width="1.5"/>
        <path d="M4 54 Q20 40 70 15 T120 54" fill="none" stroke="#448AFF" stroke-width="1.5"/>
    </g>
    
    <!-- Retractable Drawer Handle -->
    <rect x="175" y="580" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>

    <!-- EditorToolbar Pills (Horizontal) -->
    <g transform="translate(10, 590)">
        <rect x="0" y="0" width="370" height="46" rx="8" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <!-- Basic (Selected) -->
        <rect x="4" y="5" width="60" height="36" rx="6" fill="{C_AMBER_CONTAINER}"/>
        <text x="34" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="{C_AMBER}">Basic</text>
        <circle cx="56" cy="12" r="3" fill="{C_AMBER}"/>
        
        <!-- Courbes -->
        <rect x="68" y="5" width="60" height="36" rx="6" fill="none"/>
        <text x="98" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Courbes</text>
        
        <!-- TSL -->
        <rect x="132" y="5" width="50" height="36" rx="6" fill="none"/>
        <text x="157" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">TSL</text>
        
        <!-- 3-Way -->
        <rect x="186" y="5" width="56" height="36" rx="6" fill="none"/>
        <text x="214" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">3-Way</text>
        
        <!-- Détail -->
        <rect x="246" y="5" width="54" height="36" rx="6" fill="none"/>
        <text x="273" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Détail</text>
        
        <!-- Effets -->
        <rect x="304" y="5" width="54" height="36" rx="6" fill="none"/>
        <text x="331" y="27" text-anchor="middle" font-family="sans-serif" font-size="10" fill="{C_TEXT_SECONDARY}">Effets</text>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 9. Éditeur Menu Actions Déroulant
screens["Éditeur - Menu Actions Déroulant [reconstruit]"] = base_frame(
    "Éditeur / Menu Actions Déroulant",
    f'''<rect x="0" y="0" width="390" height="670" fill="{C_DARKROOM}"/>
    <rect x="16" y="10" width="358" height="400" rx="8" fill="url(#photoGrad)"/>
    
    <!-- Dropdown Menu Popped Open -->
    <g transform="translate(16, 20)">
        <rect x="0" y="0" width="220" height="230" rx="12" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1.5" filter="drop-shadow(0 8px 16px rgba(0,0,0,0.5))"/>
        
        <text x="40" y="30" font-family="sans-serif" font-size="13" font-weight="600" fill="{C_AMBER}">⚡ Auto-Edit (IA)</text>
        <line x1="12" y1="42" x2="208" y2="42" stroke="{C_OUTLINE_VAR}"/>
        
        <text x="40" y="66" font-family="sans-serif" font-size="13" fill="{C_TEXT_PRIMARY}">🎨 Presets &amp; LUTs...</text>
        <line x1="12" y1="78" x2="208" y2="78" stroke="{C_OUTLINE_VAR}"/>
        
        <text x="40" y="102" font-family="sans-serif" font-size="13" fill="{C_TEXT_PRIMARY}">💾 Enregistrer preset</text>
        <line x1="12" y1="114" x2="208" y2="114" stroke="{C_OUTLINE_VAR}"/>
        
        <text x="40" y="138" font-family="sans-serif" font-size="13" fill="{C_TEXT_PRIMARY}">📥 Exporter photo HD</text>
        <line x1="12" y1="150" x2="208" y2="150" stroke="{C_OUTLINE_VAR}"/>
        
        <text x="40" y="174" font-family="sans-serif" font-size="13" fill="{C_TEXT_PRIMARY}">📦 Exporter .CUBE LUT</text>
        <line x1="12" y1="186" x2="208" y2="186" stroke="{C_OUTLINE_VAR}"/>
        
        <text x="40" y="210" font-family="sans-serif" font-size="13" font-weight="600" fill="{C_ERROR}">↺ Réinitialiser tout</text>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 10. Éditeur Panneau Réglages de base
screens["Éditeur - Panneau Réglages de base [reconstruit]"] = base_frame(
    "Éditeur / Panneau Réglages de base",
    f'''<!-- Photo Viewport (Upper Half) -->
    <rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    
    <!-- Drawer Container (Lower Half) -->
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        
        <!-- Header: Title, Bypass, Reset, Collapse -->
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Réglages de base</text>
        <circle cx="300" cy="32" r="14" fill="{C_SURFACE}"/>
        <text x="300" y="36" text-anchor="middle" font-size="12" fill="{C_AMBER}">👁</text>
        <circle cx="334" cy="32" r="14" fill="{C_SURFACE}"/>
        <text x="334" y="36" text-anchor="middle" font-size="12" fill="{C_TEXT_SECONDARY}">↺</text>
        <circle cx="368" cy="32" r="14" fill="{C_SURFACE}"/>
        <text x="368" y="36" text-anchor="middle" font-size="12" fill="{C_TEXT_SECONDARY}">▼</text>
        
        <!-- Sliders list -->
        <g transform="translate(20, 56)">
            <!-- Slider 1 -->
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Exposition</text>
            <rect x="300" y="0" width="50" height="18" rx="4" fill="{C_SURFACE}"/>
            <text x="325" y="13" text-anchor="middle" font-family="monospace" font-size="10" font-weight="bold" fill="{C_AMBER}">+0.50</text>
            <rect x="0" y="20" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="20" width="60" height="4" fill="{C_AMBER}"/>
            <circle cx="235" cy="22" r="7" fill="{C_AMBER}"/>
            
            <!-- Slider 2 -->
            <text x="0" y="44" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Contraste</text>
            <rect x="300" y="34" width="50" height="18" rx="4" fill="{C_SURFACE}"/>
            <text x="325" y="47" text-anchor="middle" font-family="monospace" font-size="10" font-weight="bold" fill="{C_AMBER}">+15</text>
            <rect x="0" y="54" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="54" width="40" height="4" fill="{C_AMBER}"/>
            <circle cx="215" cy="56" r="7" fill="{C_AMBER}"/>
            
            <!-- Slider 3 -->
            <text x="0" y="78" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Hautes lumières</text>
            <rect x="300" y="68" width="50" height="18" rx="4" fill="{C_SURFACE}"/>
            <text x="325" y="81" text-anchor="middle" font-family="monospace" font-size="10" font-weight="bold" fill="{C_AMBER}">-20</text>
            <rect x="0" y="88" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="125" y="88" width="50" height="4" fill="{C_AMBER}"/>
            <circle cx="125" cy="90" r="7" fill="{C_AMBER}"/>
            
            <!-- Slider 4 -->
            <text x="0" y="112" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Ombres</text>
            <rect x="300" y="102" width="50" height="18" rx="4" fill="{C_SURFACE}"/>
            <text x="325" y="115" text-anchor="middle" font-family="monospace" font-size="10" font-weight="bold" fill="{C_AMBER}">+30</text>
            <rect x="0" y="122" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="122" width="65" height="4" fill="{C_AMBER}"/>
            <circle cx="240" cy="124" r="7" fill="{C_AMBER}"/>
            
            <!-- Slider 5 -->
            <text x="0" y="146" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Température de couleur</text>
            <rect x="300" y="136" width="50" height="18" rx="4" fill="{C_SURFACE}"/>
            <text x="325" y="149" text-anchor="middle" font-family="monospace" font-size="10" font-weight="bold" fill="{C_AMBER}">5500K</text>
            <rect x="0" y="156" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="156" width="40" height="4" fill="{C_AMBER}"/>
            <circle cx="215" cy="158" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 11. Éditeur Panneau Courbe des tonalités
screens["Éditeur - Panneau Courbe des tonalités [reconstruit]"] = base_frame(
    "Éditeur / Panneau Courbe des tonalités",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Courbe des tonalités</text>
        
        <!-- Channels Pills (RGB, Rouge, Vert, Bleu) -->
        <g transform="translate(20, 50)">
            <rect x="0" y="0" width="60" height="28" rx="14" fill="{C_AMBER}"/>
            <text x="30" y="18" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E1400">RGB</text>
            <rect x="68" y="0" width="60" height="28" rx="14" fill="{C_SURFACE}"/>
            <text x="98" y="18" text-anchor="middle" font-family="sans-serif" font-size="11" fill="#FF5252">Rouge</text>
            <rect x="136" y="0" width="60" height="28" rx="14" fill="{C_SURFACE}"/>
            <text x="166" y="18" text-anchor="middle" font-family="sans-serif" font-size="11" fill="#69F0AE">Vert</text>
            <rect x="204" y="0" width="60" height="28" rx="14" fill="{C_SURFACE}"/>
            <text x="234" y="18" text-anchor="middle" font-family="sans-serif" font-size="11" fill="#448AFF">Bleu</text>
        </g>
        
        <!-- Cubic Spline Canvas Grid -->
        <g transform="translate(20, 90)">
            <rect x="0" y="0" width="350" height="180" rx="10" fill="{C_DARKROOM}" stroke="{C_OUTLINE}" stroke-width="1"/>
            <!-- Grid lines -->
            <line x1="87" y1="0" x2="87" y2="180" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            <line x1="175" y1="0" x2="175" y2="180" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            <line x1="262" y1="0" x2="262" y2="180" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            <line x1="0" y1="45" x2="350" y2="45" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            <line x1="0" y1="90" x2="350" y2="90" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            <line x1="0" y1="135" x2="350" y2="135" stroke="{C_OUTLINE_VAR}" stroke-dasharray="3,3"/>
            
            <!-- Diagonal Reference -->
            <line x1="0" y1="180" x2="350" y2="0" stroke="#444" stroke-width="1"/>
            
            <!-- Spline Curve S-curve -->
            <path d="M 0 180 C 80 180, 100 130, 175 90 C 250 50, 270 10, 350 0" fill="none" stroke="{C_WHITE}" stroke-width="2.5"/>
            <!-- Control points -->
            <circle cx="80" cy="140" r="5" fill="{C_AMBER}"/>
            <circle cx="175" cy="90" r="5" fill="{C_AMBER}"/>
            <circle cx="270" cy="40" r="5" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 12. Éditeur Panneau Mélangeur TSL
screens["Éditeur - Panneau Mélangeur TSL [reconstruit]"] = base_frame(
    "Éditeur / Panneau Mélangeur TSL",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Mélangeur de couleurs (TSL 8 canaux)</text>
        
        <!-- 8 Color Channel Dots -->
        <g transform="translate(20, 50)">
            <circle cx="14" cy="14" r="12" fill="#E53935"/>
            <circle cx="56" cy="14" r="14" fill="#FB8C00" stroke="{C_WHITE}" stroke-width="2"/>
            <circle cx="98" cy="14" r="12" fill="#FDD835"/>
            <circle cx="140" cy="14" r="12" fill="#43A047"/>
            <circle cx="182" cy="14" r="12" fill="#00ACC1"/>
            <circle cx="224" cy="14" r="12" fill="#1E88E5"/>
            <circle cx="266" cy="14" r="12" fill="#5E35B1"/>
            <circle cx="308" cy="14" r="12" fill="#D81B60"/>
        </g>
        
        <!-- Sliders for selected channel (Orange) -->
        <g transform="translate(20, 95)">
            <text x="0" y="10" font-family="sans-serif" font-size="12" font-weight="bold" fill="#FB8C00">Canal Orange (Teints de peau) :</text>
            
            <text x="0" y="35" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Teinte</text>
            <text x="350" y="35" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">0</text>
            <rect x="0" y="42" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="44" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="70" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Saturation</text>
            <text x="350" y="70" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+18</text>
            <rect x="0" y="77" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="77" width="40" height="4" fill="{C_AMBER}"/>
            <circle cx="215" cy="79" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="105" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Luminance</text>
            <text x="350" y="105" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+5</text>
            <rect x="0" y="112" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="175" y="112" width="15" height="4" fill="{C_AMBER}"/>
            <circle cx="190" cy="114" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 13. Éditeur Panneau Étalonnage 3-Way
screens["Éditeur - Panneau Étalonnage 3-Way [reconstruit]"] = base_frame(
    "Éditeur / Panneau Étalonnage 3-Way",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="34" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Étalonnage des couleurs (Color Grading 3-Way)</text>
        
        <!-- 3 Color Wheels (Ombres, Tons Moyens, Hautes Lumières) -->
        <g transform="translate(20, 50)">
            <!-- Wheel 1: Ombres -->
            <g transform="translate(30, 50)">
                <circle cx="0" cy="0" r="40" fill="url(#photoGrad)" stroke="{C_OUTLINE}" stroke-width="2"/>
                <circle cx="-10" cy="12" r="4" fill="#FFFFFF" stroke="#000000" stroke-width="1.5"/>
                <text x="0" y="55" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="{C_TEXT_PRIMARY}">Ombres</text>
            </g>
            <!-- Wheel 2: Tons moyens -->
            <g transform="translate(145, 50)">
                <circle cx="0" cy="0" r="40" fill="url(#photoGrad)" stroke="{C_OUTLINE}" stroke-width="2"/>
                <circle cx="0" cy="0" r="4" fill="#FFFFFF" stroke="#000000" stroke-width="1.5"/>
                <text x="0" y="55" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="{C_TEXT_PRIMARY}">Tons moyens</text>
            </g>
            <!-- Wheel 3: Hautes lumières -->
            <g transform="translate(260, 50)">
                <circle cx="0" cy="0" r="40" fill="url(#photoGrad)" stroke="{C_OUTLINE}" stroke-width="2"/>
                <circle cx="15" cy="-8" r="4" fill="#FFFFFF" stroke="#000000" stroke-width="1.5"/>
                <text x="0" y="55" text-anchor="middle" font-family="sans-serif" font-size="10" font-weight="bold" fill="{C_TEXT_PRIMARY}">Hautes lum.</text>
            </g>
        </g>
        
        <!-- Blending & Balance sliders -->
        <g transform="translate(20, 185)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Fusion (Blending)</text>
            <text x="350" y="10" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">50</text>
            <rect x="0" y="16" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="18" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="38" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Balance</text>
            <text x="350" y="38" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">0</text>
            <rect x="0" y="44" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="46" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 14. Éditeur Panneau Détail
screens["Éditeur - Panneau Détail [reconstruit]"] = base_frame(
    "Éditeur / Panneau Détail & Netteté",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Détail &amp; Netteté</text>
        
        <g transform="translate(20, 56)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Netteté (Sharpening)</text>
            <text x="350" y="10" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">40</text>
            <rect x="0" y="18" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <rect x="0" y="18" width="90" height="4" fill="{C_AMBER}"/>
            <circle cx="90" cy="20" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="44" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Rayon</text>
            <text x="350" y="44" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">1.0</text>
            <rect x="0" y="52" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="110" cy="54" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="78" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Réduction du bruit</text>
            <text x="350" y="78" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">10</text>
            <rect x="0" y="86" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="35" cy="88" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="112" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Réduction du bruit couleur</text>
            <text x="350" y="112" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">25</text>
            <rect x="0" y="120" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="85" cy="122" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 15. Éditeur Panneau Effets
screens["Éditeur - Panneau Effets [reconstruit]"] = base_frame(
    "Éditeur / Panneau Effets & Texture",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Effets &amp; Texture</text>
        
        <g transform="translate(20, 56)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Texture</text>
            <text x="350" y="10" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+10</text>
            <rect x="0" y="18" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="195" cy="20" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="44" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Clarté (Clarity)</text>
            <text x="350" y="44" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+8</text>
            <rect x="0" y="52" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="190" cy="54" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="78" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Correction du voile (Dehaze)</text>
            <text x="350" y="78" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+5</text>
            <rect x="0" y="86" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="185" cy="88" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="112" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Vignettage</text>
            <text x="350" y="112" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">-15</text>
            <rect x="0" y="120" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="140" cy="122" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 16. Éditeur Panneau Calibration capteur
screens["Éditeur - Panneau Calibration capteur [reconstruit]"] = base_frame(
    "Éditeur / Panneau Calibration capteur",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="36" font-family="sans-serif" font-size="15" font-weight="bold" fill="{C_TEXT_PRIMARY}">Étalonnage (Calibration capteur)</text>
        
        <g transform="translate(20, 56)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Teinte des ombres</text>
            <text x="350" y="10" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">0</text>
            <rect x="0" y="18" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="20" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="44" font-family="sans-serif" font-size="11" fill="#FF5252">Primaire Rouge — Saturation</text>
            <text x="350" y="44" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+10</text>
            <rect x="0" y="52" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="195" cy="54" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="78" font-family="sans-serif" font-size="11" fill="#69F0AE">Primaire Vert — Saturation</text>
            <text x="350" y="78" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">0</text>
            <rect x="0" y="86" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="88" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="112" font-family="sans-serif" font-size="11" fill="#448AFF">Primaire Bleu — Saturation</text>
            <text x="350" y="112" text-anchor="end" font-family="monospace" font-size="10" fill="{C_AMBER}">+15</text>
            <rect x="0" y="120" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="205" cy="122" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 17. Éditeur Panneau Géométrie & Transform [Orphelin FE-009]
screens["Éditeur - Panneau Géométrie & Transform [reconstruit]"] = base_frame(
    "Éditeur / Panneau Géométrie [Orphelin FE-009]",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="32" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Géométrie &amp; Perspective</text>
        <rect x="220" y="18" width="150" height="20" rx="4" fill="#B91C1C"/>
        <text x="295" y="32" text-anchor="middle" font-family="sans-serif" font-size="9" font-weight="bold" fill="#FFFFFF">⚠️ FE-009 : Code orphelin</text>
        
        <g transform="translate(20, 50)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Perspective Verticale</text>
            <rect x="0" y="16" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="18" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="40" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Perspective Horizontale</text>
            <rect x="0" y="46" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="48" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="70" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Rotation (±45°)</text>
            <rect x="0" y="76" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="78" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="100" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Échelle (Zoom géométrique)</text>
            <rect x="0" y="106" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="108" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 18. Éditeur Panneau Corrections optiques [Orphelin FE-009]
screens["Éditeur - Panneau Corrections optiques [reconstruit]"] = base_frame(
    "Éditeur / Panneau Corrections optiques [Orphelin FE-009]",
    f'''<rect x="16" y="0" width="358" height="240" rx="8" fill="url(#photoGrad)"/>
    <g transform="translate(0, 250)">
        <rect x="0" y="0" width="390" height="380" rx="20" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="32" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Corrections optiques</text>
        <rect x="220" y="18" width="150" height="20" rx="4" fill="#B91C1C"/>
        <text x="295" y="32" text-anchor="middle" font-family="sans-serif" font-size="9" font-weight="bold" fill="#FFFFFF">⚠️ FE-009 : Code orphelin</text>
        
        <g transform="translate(20, 50)">
            <text x="0" y="10" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Distorsion (Barillet / Coussinet)</text>
            <rect x="0" y="16" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="18" r="7" fill="{C_AMBER}"/>
            
            <text x="0" y="44" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_TEXT_PRIMARY}">Supprimer l'aberration chromatique</text>
            <!-- Switch ON -->
            <rect x="300" y="32" width="44" height="22" rx="11" fill="{C_AMBER}"/>
            <circle cx="332" cy="43" r="8" fill="#1E1400"/>
            
            <text x="0" y="80" font-family="sans-serif" font-size="11" fill="{C_TEXT_PRIMARY}">Vignettage de l'objectif</text>
            <rect x="0" y="86" width="350" height="4" rx="2" fill="{C_SURFACE}"/>
            <circle cx="175" cy="88" r="7" fill="{C_AMBER}"/>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 19. Éditeur Modal Presets & LUTs
screens["Éditeur - Modal Presets & LUTs [reconstruit]"] = base_frame(
    "Éditeur / Modal Presets & LUTs",
    f'''<rect x="0" y="0" width="390" height="670" fill="#000000" opacity="0.6"/>
    <!-- Bottom Sheet -->
    <g transform="translate(0, 260)">
        <rect x="0" y="0" width="390" height="420" rx="20" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="20" y="38" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Bibliothèque de Presets &amp; LUTs</text>
        
        <!-- Import Buttons -->
        <g transform="translate(20, 55)">
            <rect x="0" y="0" width="168" height="38" rx="8" fill="{C_SURFACE_VAR}"/>
            <text x="84" y="24" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_AMBER}">+ Importer .cube</text>
            <rect x="182" y="0" width="168" height="38" rx="8" fill="{C_SURFACE_VAR}"/>
            <text x="266" y="24" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="{C_AMBER}">+ Importer preset</text>
        </g>
        
        <!-- Preset Item 1 -->
        <g transform="translate(20, 110)">
            <rect x="0" y="0" width="350" height="64" rx="10" fill="{C_SURFACE_VAR}"/>
            <text x="16" y="26" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">Cinematic Teal 2026</text>
            <text x="16" y="44" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Basic + TSL + 3-Way Color Grading</text>
            <rect x="260" y="16" width="76" height="32" rx="16" fill="{C_AMBER}"/>
            <text x="298" y="36" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E1400">Appliquer</text>
        </g>
        
        <!-- Preset Item 2 -->
        <g transform="translate(20, 185)">
            <rect x="0" y="0" width="350" height="64" rx="10" fill="{C_SURFACE_VAR}"/>
            <text x="16" y="26" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">Fuji Film Look Soft</text>
            <text x="16" y="44" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Courbe S-Curve + Grain fin 8%</text>
            <rect x="260" y="16" width="76" height="32" rx="16" fill="{C_AMBER}"/>
            <text x="298" y="36" text-anchor="middle" font-family="sans-serif" font-size="11" font-weight="bold" fill="#1E1400">Appliquer</text>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 20. Éditeur Dialogue Enregistrer Preset
screens["Éditeur - Dialogue Enregistrer Preset [reconstruit]"] = base_frame(
    "Éditeur / Dialogue Enregistrer Preset",
    f'''<rect x="0" y="0" width="390" height="670" fill="#000000" opacity="0.7"/>
    <!-- Modal Dialog -->
    <g transform="translate(30, 220)">
        <rect x="0" y="0" width="330" height="200" rx="16" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <text x="24" y="36" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Enregistrer le préréglage</text>
        <text x="24" y="58" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Donnez un nom mémorable à vos réglages actuels :</text>
        
        <!-- Input TextField -->
        <rect x="24" y="74" width="282" height="44" rx="8" fill="{C_SURFACE_VAR}" stroke="{C_AMBER}" stroke-width="1.5"/>
        <text x="36" y="101" font-family="sans-serif" font-size="13" fill="{C_TEXT_PRIMARY}">Sunset Noir Studio 2026</text>
        
        <!-- Buttons -->
        <g transform="translate(130, 142)">
            <text x="40" y="26" text-anchor="middle" font-family="sans-serif" font-size="13" fill="{C_TEXT_SECONDARY}">Annuler</text>
            <rect x="90" y="6" width="94" height="36" rx="18" fill="{C_AMBER}"/>
            <text x="137" y="29" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="#1E1400">Sauvegarder</text>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 21. Éditeur Dialogue Confirmation Sortie
screens["Éditeur - Dialogue Confirmation Sortie [reconstruit]"] = base_frame(
    "Éditeur / Dialogue Confirmation Sortie",
    f'''<rect x="0" y="0" width="390" height="670" fill="#000000" opacity="0.7"/>
    <g transform="translate(30, 200)">
        <rect x="0" y="0" width="330" height="230" rx="16" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <text x="24" y="36" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Quitter l'éditeur ?</text>
        <text x="24" y="60" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Des modifications n'ont pas été enregistrées comme preset. Que souhaitez-vous faire ?</text>
        
        <!-- Action Options -->
        <g transform="translate(24, 90)">
            <rect x="0" y="0" width="282" height="36" rx="8" fill="{C_AMBER}"/>
            <text x="141" y="23" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="#1E1400">Sauvegarder le brouillon</text>
            
            <rect x="0" y="44" width="282" height="36" rx="8" fill="{C_SURFACE_VAR}"/>
            <text x="141" y="67" text-anchor="middle" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">Quitter en conservant l'image</text>
            
            <text x="141" y="112" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_ERROR}">Fermer l'image définitivement</text>
        </g>
    </g>''',
    active_tab="editor",
    show_double_nav=True
)

# 22. Palettes Extracteur & Nuancier
screens["Palettes - Extracteur & Nuancier [reconstruit]"] = base_frame(
    "Palettes / Extracteur & Nuancier",
    f'''<g transform="translate(16, 10)">
        <!-- Header banner -->
        <rect x="0" y="0" width="358" height="60" rx="10" fill="{C_SURFACE_VAR}"/>
        <text x="16" y="24" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_AMBER}">Extracteur de nuances cinéma</text>
        <text x="16" y="42" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Extrayez la signature chromatique de n'importe quelle photo.</text>
        
        <!-- Image with photo -->
        <g transform="translate(0, 72)">
            <rect x="0" y="0" width="358" height="150" rx="12" fill="url(#photoGrad)"/>
            
            <!-- 6 Color Swatches -->
            <g transform="translate(0, 162)">
                <rect x="0" y="0" width="54" height="50" rx="6" fill="#1A365D"/>
                <text x="27" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#1A365D</text>
                
                <rect x="61" y="0" width="54" height="50" rx="6" fill="#2E5B88"/>
                <text x="88" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#2E5B88</text>
                
                <rect x="122" y="0" width="54" height="50" rx="6" fill="#E28743"/>
                <text x="149" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#E28743</text>
                
                <rect x="183" y="0" width="54" height="50" rx="6" fill="#E76F51"/>
                <text x="210" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#E76F51</text>
                
                <rect x="244" y="0" width="54" height="50" rx="6" fill="#F4A261"/>
                <text x="271" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#F4A261</text>
                
                <rect x="305" y="0" width="53" height="50" rx="6" fill="#B91C1C"/>
                <text x="331" y="66" text-anchor="middle" font-family="monospace" font-size="9" fill="{C_TEXT_SECONDARY}">#B91C1C</text>
            </g>
        </g>
        
        <!-- Save Palette Form -->
        <g transform="translate(0, 310)">
            <rect x="0" y="0" width="358" height="42" rx="8" fill="{C_SURFACE_VAR}" stroke="{C_OUTLINE}" stroke-width="1"/>
            <text x="16" y="26" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Nom de la palette (ex. Blade Runner Sunset)</text>
            <rect x="0" y="52" width="358" height="42" rx="21" fill="{C_AMBER}"/>
            <text x="179" y="78" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="#1E1400">Enregistrer dans le Profil</text>
        </g>
    </g>''',
    active_tab="palettes"
)

# 23. Profil Portfolio Photos
screens["Profil - Portfolio Photos [reconstruit]"] = base_frame(
    "Profil / Portfolio Photos",
    f'''<g transform="translate(16, 10)">
        <!-- Header User -->
        <circle cx="36" cy="36" r="28" fill="{C_AMBER}"/>
        <text x="36" y="42" text-anchor="middle" font-family="sans-serif" font-size="18" font-weight="bold" fill="#1E1400">AP</text>
        <text x="80" y="28" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Audric Piet</text>
        <text x="80" y="46" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">14 Photos retouchées • 8 Presets 3D LUT</text>
        
        <!-- Primary Tabs: Photos (Active) vs Styles -->
        <g transform="translate(0, 75)">
            <rect x="0" y="0" width="179" height="36" fill="none"/>
            <text x="90" y="22" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">Photos (14)</text>
            <rect x="20" y="32" width="140" height="3" fill="{C_AMBER}"/>
            
            <rect x="179" y="0" width="179" height="36" fill="none"/>
            <text x="268" y="22" text-anchor="middle" font-family="sans-serif" font-size="13" fill="{C_TEXT_SECONDARY}">Styles / Presets (8)</text>
        </g>
        
        <!-- Instagram Style 3-Column Grid -->
        <g transform="translate(0, 125)">
            <!-- Row 1 -->
            <rect x="0" y="0" width="114" height="114" rx="4" fill="url(#photoGrad)"/>
            <rect x="122" y="0" width="114" height="114" rx="4" fill="#2E5B88"/>
            <rect x="244" y="0" width="114" height="114" rx="4" fill="#B91C1C"/>
            <!-- Row 2 -->
            <rect x="0" y="122" width="114" height="114" rx="4" fill="#E28743"/>
            <rect x="122" y="122" width="114" height="114" rx="4" fill="#3D3B3C"/>
            <rect x="244" y="122" width="114" height="114" rx="4" fill="#4CA1AF"/>
            <!-- Row 3 -->
            <rect x="0" y="244" width="114" height="114" rx="4" fill="#1A365D"/>
            <rect x="122" y="244" width="114" height="114" rx="4" fill="#C0392B"/>
            <rect x="244" y="244" width="114" height="114" rx="4" fill="#F4A261"/>
        </g>
    </g>''',
    active_tab="profile"
)

# 24. Profil Bibliothèque de Styles
screens["Profil - Bibliothèque de Styles [reconstruit]"] = base_frame(
    "Profil / Bibliothèque de Styles",
    f'''<g transform="translate(16, 10)">
        <circle cx="36" cy="36" r="28" fill="{C_AMBER}"/>
        <text x="36" y="42" text-anchor="middle" font-family="sans-serif" font-size="18" font-weight="bold" fill="#1E1400">AP</text>
        <text x="80" y="28" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Audric Piet</text>
        <text x="80" y="46" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">14 Photos retouchées • 8 Presets 3D LUT</text>
        
        <!-- Primary Tabs: Photos vs Styles (Active) -->
        <g transform="translate(0, 75)">
            <rect x="0" y="0" width="179" height="36" fill="none"/>
            <text x="90" y="22" text-anchor="middle" font-family="sans-serif" font-size="13" fill="{C_TEXT_SECONDARY}">Photos (14)</text>
            
            <rect x="179" y="0" width="179" height="36" fill="none"/>
            <text x="268" y="22" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">Styles / Presets (8)</text>
            <rect x="199" y="32" width="140" height="3" fill="{C_AMBER}"/>
        </g>
        
        <!-- Preset Cards -->
        <g transform="translate(0, 125)">
            <!-- Card 1 -->
            <rect x="0" y="0" width="358" height="80" rx="10" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
            <rect x="12" y="12" width="56" height="56" rx="6" fill="url(#photoGrad)"/>
            <text x="80" y="32" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Tokyo Neon Nights</text>
            <text x="80" y="50" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">LUT 33³ • Méthode HM-MKL-HM</text>
            <rect x="270" y="24" width="76" height="32" rx="6" fill="{C_SURFACE_VAR}"/>
            <text x="308" y="44" text-anchor="middle" font-family="sans-serif" font-size="11" fill="{C_AMBER}">Exporter</text>
            
            <!-- Card 2 -->
            <rect x="0" y="92" width="358" height="80" rx="10" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
            <rect x="12" y="104" width="56" height="56" rx="6" fill="#C0392B"/>
            <text x="80" y="124" font-family="sans-serif" font-size="14" font-weight="bold" fill="{C_TEXT_PRIMARY}">Portra 160 Warmth</text>
            <text x="80" y="142" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">LUT 65³ • Grain fin argentique</text>
            <rect x="270" y="116" width="76" height="32" rx="6" fill="{C_SURFACE_VAR}"/>
            <text x="308" y="136" text-anchor="middle" font-family="sans-serif" font-size="11" fill="{C_AMBER}">Exporter</text>
        </g>
    </g>''',
    active_tab="profile"
)

# 25. Profil Dialogue Détail Image
screens["Profil - Dialogue Détail Image [reconstruit]"] = base_frame(
    "Profil / Dialogue Détail Image",
    f'''<rect x="0" y="0" width="390" height="670" fill="#000000" opacity="0.8"/>
    <g transform="translate(24, 80)">
        <rect x="0" y="0" width="342" height="480" rx="16" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <text x="20" y="32" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Détail de la photo</text>
        <circle cx="318" cy="28" r="12" fill="{C_SURFACE_VAR}"/>
        <text x="318" y="32" text-anchor="middle" font-size="12" fill="{C_TEXT_SECONDARY}">✕</text>
        
        <!-- Large Photo Preview -->
        <rect x="16" y="48" width="310" height="240" rx="8" fill="url(#photoGrad)"/>
        
        <!-- Technical Metadata -->
        <g transform="translate(16, 305)">
            <text x="0" y="15" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_TEXT_PRIMARY}">Métadonnées de prise de vue :</text>
            <text x="0" y="35" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Résolution : 8192 × 5464 px (45 MP) • RAW DNG</text>
            <text x="0" y="52" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Boîtier : Sony A7R V • Objectif : 35mm F1.4 GM</text>
            <text x="0" y="69" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Exposition : 1/500s • f/2.8 • ISO 100 • Espace sRGB</text>
            <text x="0" y="86" font-family="sans-serif" font-size="11" fill="{C_AMBER}">LUT appliquée : Teal &amp; Orange Blockbuster (100%)</text>
        </g>
        
        <!-- Action Buttons -->
        <g transform="translate(16, 420)">
            <rect x="0" y="0" width="145" height="42" rx="21" fill="{C_AMBER}"/>
            <text x="72" y="26" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="#1E1400">Partager</text>
            
            <rect x="165" y="0" width="145" height="42" rx="21" fill="{C_SURFACE_VAR}"/>
            <text x="237" y="26" text-anchor="middle" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_ERROR}">Supprimer</text>
        </g>
    </g>''',
    active_tab="profile"
)

# 26. Paramètres Préférences & Thèmes
screens["Paramètres - Préférences & Thèmes [reconstruit]"] = base_frame(
    "Paramètres / Préférences & Thèmes",
    f'''<g transform="translate(16, 10)">
        <!-- Guide Banner -->
        <rect x="0" y="0" width="358" height="56" rx="10" fill="{C_AMBER_CONTAINER}" stroke="{C_AMBER}" stroke-width="1"/>
        <text x="16" y="24" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">Guide colorimétrique &amp; Métriques</text>
        <text x="16" y="42" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Consulter l'aide sur Delta E, PSNR et les tables 3D LUT →</text>
        
        <!-- Themes Section -->
        <g transform="translate(0, 70)">
            <text x="0" y="10" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">Apparence &amp; Thèmes visuels :</text>
            
            <!-- 4 Themes Grid -->
            <!-- Theme 1 Amber Noir (Active) -->
            <rect x="0" y="20" width="172" height="64" rx="8" fill="{C_SURFACE}" stroke="{C_AMBER}" stroke-width="2"/>
            <circle cx="24" cy="52" r="10" fill="{C_AMBER}"/>
            <text x="44" y="48" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_TEXT_PRIMARY}">Amber Noir</text>
            <text x="44" y="62" font-family="sans-serif" font-size="10" fill="{C_AMBER}">Actif ✓</text>
            
            <!-- Theme 2 Cyan Studio -->
            <rect x="186" y="20" width="172" height="64" rx="8" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
            <circle cx="210" cy="52" r="10" fill="#00E5FF"/>
            <text x="230" y="55" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">Cyan Studio</text>
            
            <!-- Theme 3 OLED Pure Black -->
            <rect x="0" y="92" width="172" height="64" rx="8" fill="#000000" stroke="{C_OUTLINE}" stroke-width="1"/>
            <circle cx="24" cy="124" r="10" fill="#FFC107"/>
            <text x="44" y="127" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">OLED Black</text>
            
            <!-- Theme 4 Daylight Light -->
            <rect x="186" y="92" width="172" height="64" rx="8" fill="#F8F9FA" stroke="{C_OUTLINE}" stroke-width="1"/>
            <circle cx="210" cy="124" r="10" fill="#D97706"/>
            <text x="230" y="127" font-family="sans-serif" font-size="12" fill="#1F2937">Daylight (Clair)</text>
        </g>
        
        <!-- Engine Defaults -->
        <g transform="translate(0, 245)">
            <text x="0" y="10" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_TEXT_PRIMARY}">Paramètres du moteur :</text>
            <rect x="0" y="20" width="358" height="42" rx="8" fill="{C_SURFACE_VAR}"/>
            <text x="16" y="45" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">Taille de grille 3D LUT :</text>
            <text x="340" y="45" text-anchor="end" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_AMBER}">33 × 33 × 33 (Standard)</text>
            
            <rect x="0" y="70" width="358" height="42" rx="8" fill="{C_SURFACE_VAR}"/>
            <text x="16" y="95" font-family="sans-serif" font-size="12" fill="{C_TEXT_PRIMARY}">Algorithme par défaut :</text>
            <text x="340" y="95" text-anchor="end" font-family="sans-serif" font-size="12" font-weight="bold" fill="{C_AMBER}">HM-MKL-HM</text>
        </g>
        
        <!-- Cache cleanup -->
        <g transform="translate(0, 375)">
            <rect x="0" y="0" width="358" height="42" rx="21" fill="{C_SURFACE_VAR}"/>
            <text x="179" y="26" text-anchor="middle" font-family="sans-serif" font-size="12" fill="{C_TEXT_SECONDARY}">Vider le cache d'images (14.2 Mo)</text>
        </g>
    </g>''',
    active_tab="settings"
)

# 27. Transversal BottomSheet Métriques
screens["Transversal - BottomSheet Métriques [reconstruit]"] = base_frame(
    "Transversal / BottomSheet Métriques",
    f'''<rect x="0" y="0" width="390" height="670" fill="#000000" opacity="0.75"/>
    <g transform="translate(0, 240)">
        <rect x="0" y="0" width="390" height="440" rx="20" fill="{C_SURFACE}" stroke="{C_OUTLINE}" stroke-width="1"/>
        <rect x="175" y="8" width="40" height="4" rx="2" fill="{C_TEXT_SECONDARY}"/>
        <text x="24" y="38" font-family="sans-serif" font-size="16" font-weight="bold" fill="{C_TEXT_PRIMARY}">Comprendre les métriques couleur</text>
        
        <g transform="translate(24, 60)">
            <text x="0" y="16" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">1. Delta E 2000 (ΔE₀₀)</text>
            <text x="0" y="34" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Mesure la différence perçue par l'œil humain entre deux couleurs. Un score &lt; 2.0 est indécelable à l'œil nu.</text>
            
            <text x="0" y="74" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">2. PSNR (Peak Signal-to-Noise Ratio)</text>
            <text x="0" y="92" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Exprimé en décibels (dB). Évalue la fidélité de reconstruction. Une valeur supérieure à 35 dB indique une qualité studio sans artefact.</text>
            
            <text x="0" y="132" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">3. 3D LUT (.CUBE)</text>
            <text x="0" y="150" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Table de correspondance tridimensionnelle standardisée dans l'industrie cinématographique (DaVinci Resolve, Premiere Pro, Lightroom).</text>
            
            <text x="0" y="190" font-family="sans-serif" font-size="13" font-weight="bold" fill="{C_AMBER}">4. Algorithme HM-MKL-HM</text>
            <text x="0" y="208" font-family="sans-serif" font-size="11" fill="{C_TEXT_SECONDARY}">Combinaison d'un histogram matching initial et du transport optimal Monge-Kantorovich pour un transfert chromatique sans dérive spectrale.</text>
        </g>
        
        <rect x="24" y="320" width="342" height="44" rx="22" fill="{C_AMBER}"/>
        <text x="195" y="347" text-anchor="middle" font-family="sans-serif" font-size="13" font-weight="bold" fill="#1E1400">J'ai compris</text>
    </g>''',
    active_tab="studio"
)

# Generate all 27 SVGs
for name, xml in screens.items():
    safe_name = name.replace(" / ", " - ").replace("/", "-")
    file_path = os.path.join(OUTPUT_DIR, f"{safe_name}.svg")
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(xml)
    print(f"Generated: {safe_name}.svg ({len(xml)} bytes)")

print(f"\nSuccessfully generated {len(screens)} SVG files in {OUTPUT_DIR}")

