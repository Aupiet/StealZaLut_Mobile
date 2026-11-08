import os

def add_string_entry(path, key, val):
    with open(path, "r", encoding="utf-8") as f:
        c = f.read()
    if key in c:
        return
    tag = f'    <string name="{key}">{val}</string>\n'
    c = c.replace('</resources>', f'{tag}</resources>')
    with open(path, "w", encoding="utf-8") as f:
        f.write(c)
    print(f"Added {key} to {path}")

add_string_entry("app/src/main/res/values/strings.xml", "wizard_step_4_title_stage", "Interpolation Tetrahedral LUT 33x33x33")
add_string_entry("app/src/main/res/values-fr/strings.xml", "wizard_step_4_title_stage", "Interpolation Tetrahedral LUT 33x33x33")
add_string_entry("app/src/main/res/values-es/strings.xml", "wizard_step_4_title_stage", "Interpolación Tetraédrica LUT 33x33x33")
