import urllib.request
import json

url = 'https://mcp.figma.com/mcp/upload/751f4e92-2b01-4e5c-ac23-ddf504d9efd0/submit?scaleMode=FILL'
filename = 'Design System - Tokens, Composants & Iconographie.svg'
with open('scripts/design_system_board.svg', 'rb') as f:
    data = f.read()

boundary = '----FigmaFormBoundary7MA4YWxkTrZu0gW'
body = bytearray()
body.extend(f'--{boundary}\r\n'.encode())
body.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode())
body.extend(b'Content-Type: image/svg+xml\r\n\r\n')
body.extend(data)
body.extend(f'\r\n--{boundary}--\r\n'.encode())

req = urllib.request.Request(url, data=body, headers={'Content-Type': f'multipart/form-data; boundary={boundary}'}, method='POST')
try:
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode())
except Exception as e:
    print(f"Error: {e}")
