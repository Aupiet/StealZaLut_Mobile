en_strings = '''    <string name="group_light">Light</string>
    <string name="group_tone">Tones</string>
    <string name="group_color">Color</string>
    <string name="group_texture">Texture</string>
    <string name="hint_double_tap_reset">Double-tap a slider to reset to 0</string>'''

fr_strings = '''    <string name="group_light">Lumière</string>
    <string name="group_tone">Tons</string>
    <string name="group_color">Couleur</string>
    <string name="group_texture">Texture</string>
    <string name="hint_double_tap_reset">Double-tap sur un curseur pour réinitialiser à 0</string>'''

es_strings = '''    <string name="group_light">Luz</string>
    <string name="group_tone">Tonos</string>
    <string name="group_color">Color</string>
    <string name="group_texture">Textura</string>
    <string name="hint_double_tap_reset">Doble toque en un deslizador para reiniciar a 0</string>'''

targets = [
    (r'app/src/main/res/values/strings.xml', en_strings),
    (r'app/src/main/res/values-fr/strings.xml', fr_strings),
    (r'app/src/main/res/values-es/strings.xml', es_strings)
]

for fpath, s in targets:
    with open(fpath, 'r', encoding='utf-8') as f:
        c = f.read()
    if 'group_light' not in c:
        c = c.replace('</resources>', s + '\n</resources>')
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(c)
        print(f"Added group strings to {fpath}")
    else:
        print(f"Already present in {fpath}")
