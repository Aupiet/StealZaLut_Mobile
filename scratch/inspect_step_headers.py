import os
from PIL import Image

frames = [
    "studio_step1_source.png",
    "studio_step2_palettes.png",
    "studio_step2_ref.png",
    "studio_step3_params.png",
    "studio_step4_computing.png",
    "studio_step5_result.png"
]

for f in frames:
    p = os.path.join("audits/fidelity/figma_frames", f)
    if os.path.exists(p):
        im = Image.open(p)
        print(f"{f}: size={im.size}")
