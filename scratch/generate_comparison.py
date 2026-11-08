import os
import sys
from PIL import Image, ImageDraw, ImageFont

def make_side_by_side(figma_path, emu_path, output_path, title="FRAME COMPARISON"):
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    
    figma_img = Image.open(figma_path).convert("RGB")
    emu_img = Image.open(emu_path).convert("RGB")
    
    target_height = 1200
    
    # Scale Figma to target_height preserving aspect ratio
    f_w, f_h = figma_img.size
    f_scaled_w = int(f_w * (target_height / f_h))
    figma_resized = figma_img.resize((f_scaled_w, target_height), Image.Resampling.LANCZOS)
    
    # Scale Emulator to target_height preserving aspect ratio
    e_w, e_h = emu_img.size
    e_scaled_w = int(e_w * (target_height / e_h))
    emu_resized = emu_img.resize((e_scaled_w, target_height), Image.Resampling.LANCZOS)
    
    header_height = 80
    border_gap = 20
    total_width = f_scaled_w + e_scaled_w + border_gap * 3
    total_height = target_height + header_height + border_gap * 2
    
    composite = Image.new("RGB", (total_width, total_height), color=(18, 18, 20))
    draw = ImageDraw.Draw(composite)
    
    # Draw header text
    try:
        font_title = ImageFont.truetype("arial.ttf", 28)
        font_label = ImageFont.truetype("arial.ttf", 20)
    except:
        font_title = font_label = ImageFont.load_default()
        
    draw.text((border_gap, 25), title, fill=(245, 158, 11), font=font_title)
    
    # Paste Figma
    x_figma = border_gap
    y_figma = header_height + border_gap
    composite.paste(figma_resized, (x_figma, y_figma))
    
    # Figma label
    draw.text((x_figma + 10, header_height - 5), "FIGMA (VÉRITÉ)", fill=(34, 197, 94), font=font_label)
    
    # Paste Emu
    x_emu = x_figma + f_scaled_w + border_gap
    y_emu = y_figma
    composite.paste(emu_resized, (x_emu, y_emu))
    
    # Emu label
    draw.text((x_emu + 10, header_height - 5), "ÉMULATEUR (LIVE)", fill=(59, 130, 246), font=font_label)
    
    composite.save(output_path, quality=95)
    print(f"Generated side-by-side: {output_path}")

if __name__ == "__main__":
    if len(sys.argv) >= 4:
        title = sys.argv[4] if len(sys.argv) > 4 else "COMPARISON"
        make_side_by_side(sys.argv[1], sys.argv[2], sys.argv[3], title)
