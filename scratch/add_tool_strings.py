import re

en_strings = '''    <string name="tool_label_crop">Crop</string>
    <string name="tool_label_basic">Basic</string>
    <string name="tool_label_curves">Curves</string>
    <string name="tool_label_hsl">HSL</string>
    <string name="tool_label_3way">3-Way</string>
    <string name="tool_label_detail">Detail</string>
    <string name="tool_label_effects">Effects</string>
    <string name="tool_label_calibration">Calib.</string>
    <string name="tool_label_geometry">Geometry</string>
    <string name="tool_label_optics">Optics</string>'''

fr_strings = '''    <string name="tool_label_crop">Rogner</string>
    <string name="tool_label_basic">Base</string>
    <string name="tool_label_curves">Courbes</string>
    <string name="tool_label_hsl">TSL</string>
    <string name="tool_label_3way">3-Way</string>
    <string name="tool_label_detail">Détail</string>
    <string name="tool_label_effects">Effets</string>
    <string name="tool_label_calibration">Calib.</string>
    <string name="tool_label_geometry">Géométrie</string>
    <string name="tool_label_optics">Optique</string>'''

es_strings = '''    <string name="tool_label_crop">Recortar</string>
    <string name="tool_label_basic">Base</string>
    <string name="tool_label_curves">Curvas</string>
    <string name="tool_label_hsl">TSL</string>
    <string name="tool_label_3way">3-Way</string>
    <string name="tool_label_detail">Detalle</string>
    <string name="tool_label_effects">Efectos</string>
    <string name="tool_label_calibration">Calib.</string>
    <string name="tool_label_geometry">Geometría</string>
    <string name="tool_label_optics">Óptica</string>'''

targets = [
    (r'app/src/main/res/values/strings.xml', en_strings),
    (r'app/src/main/res/values-fr/strings.xml', fr_strings),
    (r'app/src/main/res/values-es/strings.xml', es_strings)
]

for fpath, s in targets:
    with open(fpath, 'r', encoding='utf-8') as f:
        c = f.read()
    if 'tool_label_crop' not in c:
        c = c.replace('</resources>', s + '\n</resources>')
        with open(fpath, 'w', encoding='utf-8') as f:
            f.write(c)
        print(f"Added tool labels to {fpath}")
    else:
        print(f"Already present in {fpath}")
