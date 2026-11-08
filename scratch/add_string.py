def add_key(path, new_line):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if 'name="wizard_step_1_ready"' in content:
        return
    target = 'name="wizard_step_1_change">'
    idx = content.find(target)
    if idx != -1:
        end_line = content.find('\n', idx)
        content = content[:end_line+1] + '    ' + new_line + '\n' + content[end_line+1:]
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Added to {path}")

add_key("app/src/main/res/values/strings.xml", '<string name="wizard_step_1_ready">Source photo ready</string>')
add_key("app/src/main/res/values-fr/strings.xml", '<string name="wizard_step_1_ready">Photo source prête</string>')
add_key("app/src/main/res/values-es/strings.xml", '<string name="wizard_step_1_ready">Foto de origen lista</string>')
