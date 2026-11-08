with open('app/src/main/res/values-fr/strings.xml', 'r', encoding='utf-8') as f:
    fr = f.read()

fr = fr.replace("<string name=\"settings_theme_header\">Thème d'application</string>", "<string name=\"settings_theme_header\">Th\u00e8me d\\'application</string>")
fr = fr.replace("<string name=\"settings_icons_header\">Jeu d'icônes</string>", "<string name=\"settings_icons_header\">Jeu d\\'ic\u00f4nes</string>")

with open('app/src/main/res/values-fr/strings.xml', 'w', encoding='utf-8') as f:
    f.write(fr)

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt', 'r', encoding='utf-8') as f:
    step3 = f.read()

step3 = step3.replace('param_skin_prot_title_desc', 'param_skin_prot_desc')
step3 = step3.replace('param_neutral_lock_title_desc', 'param_neutral_lock_desc')

with open('app/src/main/java/com/stealzalut/mobile/ui/screens/studio/StudioStep3Parameters.kt', 'w', encoding='utf-8') as f:
    step3 = step3.replace('param_skin_prot_title_desc', 'param_skin_prot_desc')
    step3 = step3.replace('param_neutral_lock_title_desc', 'param_neutral_lock_desc')
    f.write(step3)

print("Both fixed successfully")
