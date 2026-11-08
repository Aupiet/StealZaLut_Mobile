with open('app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorColorGradingPanel.kt', 'r', encoding='utf-8') as f:
    ecgp = f.read()

# Replace missing strings and imports
ecgp = ecgp.replace('ColorCardReference.NeutralSwatch', 'ColorCardReference.GrayMiddle1')
ecgp = ecgp.replace('stringResource(R.string.color_grading_tab_shadows)', 'stringResource(R.string.wheel_shadows)')
ecgp = ecgp.replace('stringResource(R.string.color_grading_tab_midtones)', 'stringResource(R.string.wheel_midtones)')
ecgp = ecgp.replace('stringResource(R.string.color_grading_tab_highlights)', 'stringResource(R.string.wheel_highlights)')
ecgp = ecgp.replace('stringResource(R.string.color_grading_tab_global)', 'stringResource(R.string.wheel_global)')
ecgp = ecgp.replace('stringResource(R.string.color_grading_tab_blending)', 'stringResource(R.string.slider_blending)')

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/editor/EditorColorGradingPanel.kt', 'w', encoding='utf-8') as f:
    f.write(ecgp)
print("Updated EditorColorGradingPanel.kt")

# Update SettingsScreen.kt
with open('app/src/main/java/com/stealzalut/mobile/ui/screens/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    settings_code = f.read()

settings_code = settings_code.replace('settings.maxExportDimension', 'settings.maxExportDim')
settings_code = settings_code.replace('maxExportDimension = dim', 'maxExportDim = dim')
settings_code = settings_code.replace('settings.proxyDimension', 'settings.analysisProxyDim')
settings_code = settings_code.replace('proxyDimension = dim', 'analysisProxyDim = dim')
settings_code = settings_code.replace('method.displayName', 'stringResource(method.displayNameRes)')
settings_code = settings_code.replace('size.label', 'stringResource(size.labelRes)')
settings_code = settings_code.replace('settings.language == code', '(uiState.currentLanguageTag ?: "fr") == code')

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/SettingsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(settings_code)
print("Updated SettingsScreen.kt")

# Update StudioStep3Parameters.kt
with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt', 'r', encoding='utf-8') as f:
    step3 = f.read()

step3 = step3.replace('showSplitHandle = true,', '')

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt', 'w', encoding='utf-8') as f:
    f.write(step3)
print("Updated StudioStep3Parameters.kt")
