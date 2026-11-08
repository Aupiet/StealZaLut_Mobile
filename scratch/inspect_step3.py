import sys
import io
import re

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

with open(r'C:\Users\audri\.gemini\antigravity\brain\5a39b74a-39fa-42e9-9add-1eb49261e417\.system_generated\steps\468\output.txt', 'r', encoding='utf-8') as f:
    text = f.read()

texts = re.findall(r'>([^<]+)<', text)
clean_texts = [t.strip() for t in texts if t.strip()]

print(f"Total texts in 12:244 (Studio Step 3): {len(clean_texts)}")
for i, t in enumerate(clean_texts):
    print(f"{i:2d}: {t}")
