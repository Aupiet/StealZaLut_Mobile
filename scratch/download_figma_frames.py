import os
import urllib.request

urls = {
    "studio_step1_source.png": "https://www.figma.com/api/mcp/asset/77766b23-e2ca-4da7-a524-d8a74665cf78.png",
    "studio_step2_palettes.png": "https://www.figma.com/api/mcp/asset/b43e5c40-7d2a-413b-ae6d-28285f2aa835.png",
    "studio_step2_ref.png": "https://www.figma.com/api/mcp/asset/9a1a2ea2-ba62-4f17-9f0e-1e83565ff51f.png",
    "studio_step4_computing.png": "https://www.figma.com/api/mcp/asset/584719e0-a08d-4b6d-ac5a-3050cfbde369.png",
    "studio_step5_result.png": "https://www.figma.com/api/mcp/asset/0a92e6b1-85c3-4f85-95a2-0a8295ecf184.png",
    "palettes_extractor.png": "https://www.figma.com/api/mcp/asset/f09aae63-5a11-4009-a15f-f9aa609f2236.png",
    "profile_gallery.png": "https://www.figma.com/api/mcp/asset/d57ce51a-545a-4185-ae83-027a397a43d8.png",
    "profile_styles.png": "https://www.figma.com/api/mcp/asset/3fc3196f-c78b-40d4-b2bb-a259c6c5017a.png",
    "editor_save_preset.png": "https://www.figma.com/api/mcp/asset/a00aebe5-53bb-4cb0-8dd0-907a412e3f6b.png",
    "editor_dropdown_menu.png": "https://www.figma.com/api/mcp/asset/6a998e1f-1dfd-47ab-95ac-7a384d874357.png",
    "editor_curves.png": "https://www.figma.com/api/mcp/asset/94a88b42-8955-4d48-b9a6-e54f92e83358.png",
    "editor_hsl.png": "https://www.figma.com/api/mcp/asset/8a17d823-1a9e-451b-aa00-dffcda66a9db.png",
    "editor_3way.png": "https://www.figma.com/api/mcp/asset/1e2ff418-e5ee-457b-a8da-8c1192519398.png",
    "editor_detail.png": "https://www.figma.com/api/mcp/asset/9230bdb7-b2e4-41a0-aa7c-0d7ebf656401.png",
    "editor_effects.png": "https://www.figma.com/api/mcp/asset/53e75daf-cb8d-428c-a881-fa8ed7755143.png",
    "editor_calibration.png": "https://www.figma.com/api/mcp/asset/9337c0f7-10a9-4c29-8dc3-81170e264fcd.png",
    "editor_geometry.png": "https://www.figma.com/api/mcp/asset/f840974d-46d5-4b85-af67-8645a92b588b.png",
    "editor_optics.png": "https://www.figma.com/api/mcp/asset/dfa85d7b-4ebf-4ee7-b54c-e76a16b2aac4.png",
    "editor_presets_modal.png": "https://www.figma.com/api/mcp/asset/0f0a9da2-7889-4917-a466-9d719436c1a1.png",
}

out_dir = r"audits/fidelity/figma_frames"
os.makedirs(out_dir, exist_ok=True)

for name, url in urls.items():
    dest = os.path.join(out_dir, name)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req) as resp, open(dest, "wb") as f:
            f.write(resp.read())
        print(f"Downloaded {name} ({os.path.getsize(dest)} bytes)")
    except Exception as e:
        print(f"Failed {name}: {e}")
