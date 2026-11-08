import os

def patch_file(path, replacements):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    for old, new in replacements:
        content = content.replace(old, new)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Patched {path}")

# FR
patch_file("app/src/main/res/values-fr/strings.xml", [
    (
        '<string name="wizard_step_3_subtitle">Aperçu temps réel du style appliqué sur l\'image source</string>',
        '<string name="wizard_step_3_subtitle">Dosage et préservation des carnations</string>'
    ),
    (
        '<string name="wizard_step_3_btn_run">Générer le rendu 3D LUT →</string>',
        '<string name="wizard_step_3_btn_run">Générer le Rendu LUT (4/5) →</string>'
    ),
    (
        '<string name="wizard_step_1_btn_next">Continuer →</string>',
        '<string name="wizard_step_1_btn_next">Continuer vers Référence (2/5) →</string>'
    ),
    (
        '<string name="wizard_step_2_btn_next">Continuer →</string>',
        '<string name="wizard_step_2_btn_next">Continuer vers Paramètres (3/5) →</string>'
    ),
    (
        '<string name="btn_edit_in_editor">Retoucher dans l\'Éditeur Pro</string>',
        '<string name="btn_edit_in_editor">Ouvrir dans l\'Éditeur Pro →</string>'
    ),
    (
        '<string name="wizard_step_5_title">Chef-d\'œuvre Colorimétrique</string>',
        '<string name="wizard_step_5_title">Chef-d\'œuvre Colorimétrique</string>'
    ),
    (
        '<string name="wizard_step_5_subtitle">Signature Blade Runner 2049 appliquée</string>',
        '<string name="wizard_step_5_subtitle">Signature Blade Runner 2049 appliquée</string>'
    ),
    (
        '<string name="profile_title">Profil Créateur</string>',
        '<string name="profile_title">Galerie Créative</string>'
    ),
    (
        '<string name="profile_subtitle">Statistiques colorimétriques et créations sauvegardées</string>',
        '<string name="profile_subtitle">14 chefs-d\'œuvre développés avec StealZaLUT</string>'
    ),
    (
        '<string name="settings_title">Paramètres Système</string>',
        '<string name="settings_title">Préférences &amp; Thèmes</string>'
    ),
    (
        '<string name="settings_subtitle">Préférences de calcul, sauvegarde et interface</string>',
        '<string name="settings_subtitle">Personnalisation du design system modulaire</string>'
    ),
    (
        '<string name="settings_theme_header">Thème de l\'interface</string>',
        '<string name="settings_theme_header">Thème d\'application</string>'
    ),
    (
        '<string name="settings_icons_header">Jeu d\'icônes</string>',
        '<string name="settings_icons_header">Jeu d\'icônes</string>'
    ),
    (
        '<string name="palettes_header_title">Laboratoire Nuanciers</string>',
        '<string name="palettes_header_title">Extracteur de Nuances</string>'
    ),
    (
        '<string name="palettes_header_desc">Extrayez des palettes harmonieuses à partir de vos photos de référence</string>',
        '<string name="palettes_header_desc">Créez des nuanciers maîtres depuis vos photos</string>'
    )
])

# EN
patch_file("app/src/main/res/values/strings.xml", [
    (
        '<string name="wizard_step_3_subtitle">Real-time preview of style applied to source image</string>',
        '<string name="wizard_step_3_subtitle">Dosage and skin tones preservation</string>'
    ),
    (
        '<string name="wizard_step_3_btn_run">Generate 3D LUT Render →</string>',
        '<string name="wizard_step_3_btn_run">Generate LUT Render (4/5) →</string>'
    ),
    (
        '<string name="wizard_step_1_btn_next">Continue →</string>',
        '<string name="wizard_step_1_btn_next">Continue to Reference (2/5) →</string>'
    ),
    (
        '<string name="wizard_step_2_btn_next">Continue →</string>',
        '<string name="wizard_step_2_btn_next">Continue to Parameters (3/5) →</string>'
    ),
    (
        '<string name="btn_edit_in_editor">Touch up in Pro Editor</string>',
        '<string name="btn_edit_in_editor">Open in Pro Editor →</string>'
    ),
    (
        '<string name="profile_title">Creator Profile</string>',
        '<string name="profile_title">Creative Gallery</string>'
    ),
    (
        '<string name="profile_subtitle">Colorimetric metrics and saved creations</string>',
        '<string name="profile_subtitle">14 masterpieces developed with StealZaLUT</string>'
    ),
    (
        '<string name="settings_title">System Settings</string>',
        '<string name="settings_title">Preferences &amp; Themes</string>'
    ),
    (
        '<string name="settings_subtitle">Computation, storage, and interface preferences</string>',
        '<string name="settings_subtitle">Modular design system personalization</string>'
    ),
    (
        '<string name="settings_theme_header">Interface Theme</string>',
        '<string name="settings_theme_header">Application Theme</string>'
    ),
    (
        '<string name="palettes_header_title">Palette Laboratory</string>',
        '<string name="palettes_header_title">Color Swatch Extractor</string>'
    ),
    (
        '<string name="palettes_header_desc">Extract harmonious palettes from your reference photos</string>',
        '<string name="palettes_header_desc">Create master palettes from your photos</string>'
    )
])

# ES
patch_file("app/src/main/res/values-es/strings.xml", [
    (
        '<string name="wizard_step_3_subtitle">Vista previa en tiempo real del estilo aplicado</string>',
        '<string name="wizard_step_3_subtitle">Dosificación y preservación de tonos de piel</string>'
    ),
    (
        '<string name="wizard_step_3_btn_run">Generar render 3D LUT →</string>',
        '<string name="wizard_step_3_btn_run">Generar Render LUT (4/5) →</string>'
    ),
    (
        '<string name="wizard_step_1_btn_next">Continuar →</string>',
        '<string name="wizard_step_1_btn_next">Continuar a Referencia (2/5) →</string>'
    ),
    (
        '<string name="wizard_step_2_btn_next">Continuar →</string>',
        '<string name="wizard_step_2_btn_next">Continuar a Parámetros (3/5) →</string>'
    ),
    (
        '<string name="btn_edit_in_editor">Retocar en Editor Pro</string>',
        '<string name="btn_edit_in_editor">Abrir en Editor Pro →</string>'
    ),
    (
        '<string name="profile_title">Perfil de Creador</string>',
        '<string name="profile_title">Galería Creativa</string>'
    ),
    (
        '<string name="profile_subtitle">Estadísticas colorimétricas y creaciones guardadas</string>',
        '<string name="profile_subtitle">14 obras maestras desarrolladas con StealZaLUT</string>'
    ),
    (
        '<string name="settings_title">Ajustes del Sistema</string>',
        '<string name="settings_title">Preferencias y Temas</string>'
    ),
    (
        '<string name="settings_subtitle">Preferencias de cálculo, almacenamiento e interfaz</string>',
        '<string name="settings_subtitle">Personalización del design system modular</string>'
    ),
    (
        '<string name="settings_theme_header">Tema de la interfaz</string>',
        '<string name="settings_theme_header">Tema de aplicación</string>'
    ),
    (
        '<string name="palettes_header_title">Laboratorio de Paletas</string>',
        '<string name="palettes_header_title">Extractor de Matices</string>'
    ),
    (
        '<string name="palettes_header_desc">Extrae paletas armoniosas a partir de tus fotos de referencia</string>',
        '<string name="palettes_header_desc">Crea paletas maestras desde tus fotos</string>'
    )
])
