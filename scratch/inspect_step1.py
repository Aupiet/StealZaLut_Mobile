import sys

sys.stdout.reconfigure(encoding='utf-8')

with open("scripts/apres_sections/01_Section_Studio_Apres.svg", "r", encoding="utf-8") as f:
    lines = f.readlines()

recording = False
for line in lines:
    if "[APRÈS] Studio / Étape 1 Source" in line:
        recording = True
    elif "[APRÈS] Studio / Étape 2 Palettes" in line:
        recording = False
    if recording:
        print(line, end="")
