import re

# 1. Update strings.xml (values, values-fr, values-es)
def update_strings():
    entries_en = {
        'profile_title': 'Creative Gallery',
        'profile_subtitle': 'Saved photos, custom looks, and export 3D LUTs',
        'settings_title': 'Preferences &amp; Themes',
        'settings_subtitle': 'Modular design system personalization',
        'settings_theme_header': 'Application Theme',
        'settings_icons_header': 'Icon Set',
    }
    entries_fr = {
        'profile_title': 'Galerie Créative',
        'profile_subtitle': 'Photos sauvegardées et presets LUT 3D',
        'settings_title': 'Préférences &amp; Thèmes',
        'settings_subtitle': 'Personnalisation du design system modulaire',
        'settings_theme_header': "Thème d'application",
        'settings_icons_header': "Jeu d'icônes",
    }
    entries_es = {
        'profile_title': 'Galería Creativa',
        'profile_subtitle': 'Fotos guardadas y ajustes preestablecidos LUT 3D',
        'settings_title': 'Preferencias y Temas',
        'settings_subtitle': 'Personalización del sistema de diseño modular',
        'settings_theme_header': 'Tema de la aplicación',
        'settings_icons_header': 'Conjunto de iconos',
    }

    files = [
        ('app/src/main/res/values/strings.xml', entries_en),
        ('app/src/main/res/values-fr/strings.xml', entries_fr),
        ('app/src/main/res/values-es/strings.xml', entries_es),
    ]

    for path, entries in files:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        added = []
        for k, v in entries.items():
            if f'name="{k}"' not in content:
                added.append(f'    <string name="{k}">{v}</string>')
        if added:
            content = content.replace('</resources>', '\n'.join(added) + '\n</resources>')
            with open(path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated {path} with {len(added)} strings")

# 2. Fix ApplyThemeScreen.kt
def fix_apply_theme():
    path = 'app/src/main/java/com/stealzalut/mobile/ui/screens/ApplyThemeScreen.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # ensure import
    if 'import androidx.compose.ui.res.stringResource' not in content:
        content = content.replace('import androidx.compose.runtime.Composable', 'import androidx.compose.runtime.Composable\nimport androidx.compose.ui.res.stringResource')
    
    # replace STEP_4_COMPUTING with STEP_4_COMPUTE
    content = content.replace('AppConstants.STEP_4_COMPUTING', 'AppConstants.STEP_4_COMPUTE')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed ApplyThemeScreen.kt")

# 3. Fix SettingsScreen.kt
def fix_settings():
    path = 'app/src/main/java/com/stealzalut/mobile/ui/screens/SettingsScreen.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'import androidx.compose.ui.unit.dp' not in content:
        content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed SettingsScreen.kt")

# 4. Fix EditorCropOverlay.kt
def fix_crop_overlay():
    path = 'app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorCropOverlay.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # ensure imports
    if 'import androidx.compose.foundation.horizontalScroll' not in content:
        content = content.replace('import androidx.compose.foundation.clickable', 'import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.layout.defaultMinSize\nimport androidx.compose.ui.unit.dp')
    
    # fix the broken references
    content = content.replace('.androidx.compose.foundation.horizontalScroll(ratioScrollState)', '.horizontalScroll(ratioScrollState)')
    content = content.replace('.androidx.compose.foundation.layout.defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)', '.defaultMinSize(minWidth = 58.dp, minHeight = AppDimens.MinInteractiveTarget)')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed EditorCropOverlay.kt")

# 5. Fix StudioStep3Parameters.kt
def fix_step3():
    path = 'app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Let's inspect the slider usages in StudioStep3Parameters.kt
    # We replace lines around SzlSlider with correct call
    target_intensity = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.wizard_step_3_intensity_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(intensity * 100f).roundToInt()} %",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    SzlSlider(
                        value = intensity * 100f,
                        onValueChange = { onIntensityChange(it / 100f) },
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        label = stringResource(R.string.wizard_step_3_intensity_title),
                        unit = "%",
                        showButtons = false
                    )"""

    replacement_intensity = """                    SzlSlider(
                        title = stringResource(R.string.wizard_step_3_intensity_title),
                        value = intensity,
                        onValueChange = onIntensityChange,
                        min = 0f,
                        max = 1f,
                        step = 0.01f,
                        defaultValue = 1f
                    )"""

    target_smooth = """                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.param_smooth_sigma),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(preserveLuminance * 100f).roundToInt()} %",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    SzlSlider(
                        value = preserveLuminance * 100f,
                        onValueChange = { onPreserveLuminanceChange(it / 100f) },
                        min = 0f,
                        max = 100f,
                        step = 1f,
                        label = stringResource(R.string.param_smooth_sigma),
                        unit = "%",
                        showButtons = false
                    )"""

    replacement_smooth = """                    SzlSlider(
                        title = stringResource(R.string.param_smooth_title),
                        value = preserveLuminance,
                        onValueChange = onPreserveLuminanceChange,
                        min = 0f,
                        max = 1f,
                        step = 0.01f,
                        defaultValue = 0.8f
                    )"""

    content = content.replace(target_intensity, replacement_intensity)
    content = content.replace(target_smooth, replacement_smooth)
    content = content.replace('R.string.param_skin_protection', 'R.string.param_skin_prot_title')
    content = content.replace('R.string.param_skin_protection_desc', 'R.string.param_skin_prot_desc')
    content = content.replace('R.string.param_neutral_lock', 'R.string.param_neutral_lock_title')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed StudioStep3Parameters.kt")

if __name__ == '__main__':
    update_strings()
    fix_apply_theme()
    fix_settings()
    fix_crop_overlay()
    fix_step3()
