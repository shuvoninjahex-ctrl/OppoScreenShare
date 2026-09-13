import http.server, socketserver, threading, socket, qrcode, os, webbrowser

PORT=8765
HOST=socket.gethostbyname(socket.gethostname())
# If this is 127.0.0.1, replace HOST below with your Wi-Fi IPv4 from ipconfig.
if HOST.startswith("127."):
    HOST="192.168.1.100"

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/":
            html="""<!doctype html><meta name="viewport" content="width=device-width">
            <title>OPPO Screen Share</title><h2>OPPO Screen Share</h2>
            <p>Waiting for the phone…</p><img src="/view" style="max-width:100%;height:auto">
            """
            self.send_response(200); self.send_header("Content-Type","text/html"); self.end_headers()
            self.wfile.write(html.encode())
        elif self.path == "/view":
            self.send_response(200); self.send_header("Content-Type","multipart/x-mixed-replace; boundary=frame"); self.end_headers()
            while True:
                with frame_lock:
                    data=latest[0]
                if data:
                    try:
                        self.wfile.write(b"--frame\r\nContent-Type: image/jpeg\r\nContent-Length: "+str(len(data)).encode()+b"\r\n\r\n"+data+b"\r\n"); self.wfile.flush()
                    except: break
                threading.Event().wait(.05)
    def do_POST(self):
        if self.path != "/upload": self.send_error(404); return
        # Parse a multipart MJPEG stream from the phone.
        buf=b""
        self.send_response(200); self.end_headers()
        while True:
            chunk=self.rfile.read(4096)
            if not chunk: break
            buf += chunk
            while b"\r\n\r\n" in buf:
                head, rest=buf.split(b"\r\n\r\n",1)
                if b"Content-Length:" not in head: break
                try: n=int([x for x in head.split(b"\r\n") if x.lower().startswith(b"content-length:")][0].split(b":")[1])
                except: break
                if len(rest)<n+2: break
                data=rest[:n]; buf=rest[n+2:]
                with frame_lock: latest[0]=data

class ThreadingServer(socketserver.ThreadingMixIn, socketserver.TCPServer): allow_reuse_address=True

latest=[None]; frame_lock=threading.Lock()
srv=ThreadingServer(("0.0.0.0",PORT),Handler)
link=f"opposhare://connect?host={HOST}&port={PORT}"
img=qrcode.make(link); img.save("opposhare_qr.png")
print("Open on PC: http://127.0.0.1:%d" % PORT)
print("Scan this QR inside Oppo Screen Share:")
print(link)
print("QR saved as opposeshare_qr.png")
threading.Thread(target=lambda: webbrowser.open("http://127.0.0.1:%d"%PORT),daemon=True).start()
srv.serve_forever()
