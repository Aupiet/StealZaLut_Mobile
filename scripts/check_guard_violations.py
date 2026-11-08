import os
import re

target_dirs = [
    "app/src/main/java/com/stealzalut/mobile/ui/screens",
    "app/src/main/java/com/stealzalut/mobile/ui/components"
]

color_pattern = re.compile(r'Color\(0x[0-9a-fA-F]+\)')
dp_pattern = re.compile(r'\b\d+(\.\d+)?\.dp\b')
sp_pattern = re.compile(r'\b\d+(\.\d+)?\.sp\b')
hardcoded_text_pattern = re.compile(r'Text\(\s*"[^"]{2,}"[\s,)](?!\s*\/\/\s*GUARD_ALLOW)')
content_desc_pattern = re.compile(r'contentDescription\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)')
user_msg_pattern = re.compile(r'userMessage\s*=\s*"[^"]{2,}"(?!\s*\/\/\s*GUARD_ALLOW)')
icons_pattern = re.compile(r'\bIcons\.(Default|Filled|Outlined|AutoMirrored|Rounded|Sharp)\b(?!\s*\/\/\s*GUARD_ALLOW)')
font_family_pattern = re.compile(r'\bFontFamily\([^)]*\)(?!\s*\/\/\s*GUARD_ALLOW)')
raw_font_size_pattern = re.compile(r'\bfontSize\s*=\s*\d+(?!\s*\/\/\s*GUARD_ALLOW)')

violations = []

for d in target_dirs:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith(".kt"):
                filepath = os.path.join(root, f)
                with open(filepath, "r", encoding="utf-8") as file:
                    lines = file.readlines()
                for idx, line in enumerate(lines):
                    line_num = idx + 1
                    trimmed = line.strip()
                    if trimmed.startswith("//") or trimmed.startswith("/*") or trimmed.startswith("*"):
                        continue
                    
                    if color_pattern.search(line):
                        violations.append(f"{f}:{line_num} [Color(0x)] -> {trimmed}")
                    if dp_pattern.search(line):
                        violations.append(f"{f}:{line_num} [.dp] -> {trimmed}")
                    if sp_pattern.search(line):
                        violations.append(f"{f}:{line_num} [.sp] -> {trimmed}")
                    if hardcoded_text_pattern.search(line):
                        violations.append(f"{f}:{line_num} [Text(\"\")] -> {trimmed}")
                    if content_desc_pattern.search(line):
                        violations.append(f"{f}:{line_num} [contentDescription] -> {trimmed}")
                    if user_msg_pattern.search(line):
                        violations.append(f"{f}:{line_num} [userMessage] -> {trimmed}")
                    if icons_pattern.search(line):
                        violations.append(f"{f}:{line_num} [Icons.*] -> {trimmed}")
                    if font_family_pattern.search(line):
                        violations.append(f"{f}:{line_num} [FontFamily] -> {trimmed}")
                    if raw_font_size_pattern.search(line):
                        violations.append(f"{f}:{line_num} [fontSize] -> {trimmed}")

print(f"Total violations found: {len(violations)}")
for v in violations:
    print(v)
