import os
import glob
import json
import urllib.request
import urllib.parse
import mimetypes

urls = [
  "https://mcp.figma.com/mcp/upload/0b2b44cb-5d1f-4c57-9d3a-b252d5de2b53/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/883cac6f-79e3-4faf-86db-9655745bf52d/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/13fda753-0947-4353-be3f-f5db6bd95b39/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/52910285-6320-41c8-b63c-2bbc25f68713/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/aad64334-df0e-4e16-bfe0-fdb4e87b548b/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/8af2a885-d047-4297-80be-1c277adef737/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/b8ece594-1ba4-4b66-a94c-4e6203573692/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/ff600615-a80e-41ce-86d3-a797eaaab6ca/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/8fb942ac-fcde-4b6d-9b8a-8271ac68e00c/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/9ebb50c6-dff2-49a6-9b21-d9ad68068844/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/d4a7ba8b-c6d3-43fd-be71-fcb3ddfb16dd/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/2806ca9d-5ef8-4733-941b-12fa30e740a5/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/03fe20c4-9a5d-4ea8-9b2f-d5f6bb128798/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/8144114d-3909-47b6-bc31-19d1768a251e/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/fad90533-8880-40b0-acb6-2ec4adc8dd5b/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/82685672-0fca-4d5b-bd67-9afda290538a/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/15bac720-f96c-46d2-a14a-5035f85d6ed0/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/ed444e2d-4e1c-47c6-8fe9-9065a2130d4b/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/caaf9bc4-0b1e-489b-bd83-75f4a6bf9684/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/510567e7-db3c-41be-b9cc-47268c49bf29/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/14357635-1a7d-471f-a864-06176ff1d535/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/ee0a5a38-c0fc-44b8-8b3b-297ba5083053/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/7b4eb5d3-cc7d-4336-831c-ec1ad7291ab2/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/72f655b5-8272-400f-8b37-1aa0c7e8cbb5/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/b7a0802f-cb64-4634-886e-ce8f23cf0727/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/63955088-42e1-4c9d-8492-1c2e5ecf3840/submit?scaleMode=FILL&currentPageId=0%3A1",
  "https://mcp.figma.com/mcp/upload/39c74398-24f7-4d25-b76c-d70be50671fc/submit?scaleMode=FILL&currentPageId=0%3A1"
]

svg_files = sorted(glob.glob("scripts/avant_svgs/*.svg"))
print(f"Found {len(svg_files)} SVG files and {len(urls)} URLs")

results = []
for i, file_path in enumerate(svg_files):
    url = urls[i]
    filename = os.path.basename(file_path)
    
    # Read file bytes
    with open(file_path, "rb") as f:
        data = f.read()
    
    # Build multipart request
    boundary = "----FigmaFormBoundary7MA4YWxkTrZu0gW"
    body = bytearray()
    body.extend(f"--{boundary}\r\n".encode())
    body.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode())
    body.extend(b"Content-Type: image/svg+xml\r\n\r\n")
    body.extend(data)
    body.extend(f"\r\n--{boundary}--\r\n".encode())
    
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}"
        },
        method="POST"
    )
    
    try:
        with urllib.request.urlopen(req) as resp:
            resp_body = resp.read().decode()
            res = json.loads(resp_body)
            print(f"[{i+1}/{len(svg_files)}] Uploaded {filename} -> NodeId: {res.get('placedOnNodeId')}")
            results.append((filename, res.get('placedOnNodeId')))
    except Exception as e:
        print(f"[{i+1}/{len(svg_files)}] Error uploading {filename}: {e}")

print(f"\nAll uploads completed. Successfully placed {len(results)} frames on Figma canvas.")
