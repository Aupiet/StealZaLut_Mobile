import sys
import urllib.request

if len(sys.argv) < 3:
    print("Usage: upload_section.py <file_path> <submit_url>")
    sys.exit(1)

file_path = sys.argv[1]
url = sys.argv[2]
filename = file_path.split("/")[-1].split("\\")[-1]

with open(file_path, "rb") as f:
    data = f.read()

boundary = "----FigmaFormBoundary7MA4YWxkTrZu0gW"
body = bytearray()
body.extend(f"--{boundary}\r\n".encode())
body.extend(f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'.encode())
body.extend(b"Content-Type: image/svg+xml\r\n\r\n")
body.extend(data)
body.extend(f"\r\n--{boundary}--\r\n".encode())

req = urllib.request.Request(url, data=body, headers={"Content-Type": f"multipart/form-data; boundary={boundary}"}, method="POST")
try:
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode())
except Exception as e:
    print(f"Error uploading {filename}: {e}")
