"""Local Emby fixture for health/recovery acceptance; contains synthetic credentials only.

Run with --video /path/to/a/test.mp4 and reverse its printed TCP port to the device.
POST /__fixture__/reset with {"mode":"health|recover|fail|delayed","delay":20}.
GET /__fixture__/state returns counters; POST /__fixture__/shutdown stops the server.
Never point this fixture at a production account or expose it outside loopback.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs
from pathlib import Path
import argparse, json, threading, time, re

TOKEN = "fixture-token-only"
USER, MOVIE, MEDIA = "fixture-user", "fixture-movie", "fixture-media"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", required=True, type=Path)
    parser.add_argument("--port", type=int, default=0)
    args = parser.parse_args()
    video = args.video.read_bytes()
    state = {"mode": "health", "delay": 20, "health_delay": 0, "health_body_delay": 0, "auth_status": 200, "playback_info": 0,
             "diagnostic_info": 0, "sessions_started": 0, "media_ok": 0,
             "media_failed": 0, "health_public_requests": 0, "health_body_started": 0, "events": []}
    lock = threading.Lock()
    reset_at = [time.monotonic()]

    def source(route):
        return {"Id": MEDIA, "Container": "mp4", "SupportsDirectStream": True,
                "SupportsDirectPlay": True, "DirectStreamUrl": route + "?static=true",
                "RunTimeTicks": 400000000, "MediaStreams": [
                    {"Type": "Video", "Codec": "h264", "Index": 0, "Width": 320, "Height": 180},
                    {"Type": "Audio", "Codec": "aac", "Index": 1, "Language": "eng"}]}

    def movie():
        return {"Id": MOVIE, "Name": "Recovery fixture", "Type": "Movie",
                "RunTimeTicks": 400000000, "Overview": "Local acceptance test clip.",
                "UserData": {"PlaybackPositionTicks": 0, "Played": False},
                "MediaSources": [source("/Videos/fixture-movie/fresh.mp4")]}

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *unused):
            pass

        def handle(self):
            try:
                super().handle()
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                pass  # Cancelling the Android request may close this fixture connection.

        def output(self, value=None, status=200, body_delay=0):
            data = json.dumps(value).encode() if value is not None else b""
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            if data and self.command != "HEAD":
                try:
                    if body_delay:
                        self.wfile.write(data[:1])
                        self.wfile.flush()
                        with lock:
                            state["health_body_started"] += 1
                            state["events"].append({"kind": "health_body_started", "number": state["health_body_started"],
                                                    "at_ms": round((time.monotonic() - reset_at[0]) * 1000)})
                        time.sleep(body_delay)
                        self.wfile.write(data[1:])
                    else:
                        self.wfile.write(data)
                except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                    pass

        def do_HEAD(self):
            self.do_GET()

        def do_DELETE(self):
            self.output(status=204)

        def do_POST(self):
            path = urlsplit(self.path).path
            body = json.loads(self.rfile.read(int(self.headers.get("Content-Length", "0"))) or b"{}")
            if path == "/__fixture__/shutdown":
                self.output({"stopping": True})
                threading.Thread(target=self.server.shutdown, daemon=True).start()
                return
            if path == "/__fixture__/reset":
                assert body["mode"] in ("health", "recover", "fail", "delayed")
                with lock:
                    for key in ("playback_info", "diagnostic_info", "sessions_started", "media_ok", "media_failed", "health_public_requests", "health_body_started"):
                        state[key] = 0
                    state["events"] = []
                    reset_at[0] = time.monotonic()
                    state["mode"], state["delay"] = body["mode"], int(body.get("delay", 20))
                    state["health_delay"] = int(body.get("health_delay", 0))
                    state["health_body_delay"] = int(body.get("health_body_delay", 0))
                    state["auth_status"] = int(body.get("auth_status", 200))
                self.output({"ok": True})
                return
            if path.endswith("/Users/AuthenticateByName"):
                self.output({"AccessToken": TOKEN, "User": {"Id": USER, "Name": "fixture"}, "ServerId": "fixture-server"})
                return
            if path.endswith("/PlaybackInfo"):
                diagnostic = body.get("AutoOpenLiveStream") is False
                with lock:
                    key = "diagnostic_info" if diagnostic else "playback_info"
                    state[key] += 1
                    number, mode, delay = state[key], state["mode"], state["delay"]
                    state["events"].append({"kind": key, "number": number,
                                             "is_playback": body.get("IsPlayback"),
                                             "live": body.get("AutoOpenLiveStream")})
                if not diagnostic and mode == "delayed" and number >= 2:
                    time.sleep(delay)
                fresh = diagnostic or mode == "health" or (mode in ("recover", "delayed") and number >= 2)
                route = "/Videos/fixture-movie/" + ("fresh.mp4" if fresh else "old.mp4")
                self.output({"PlaySessionId": "fixture-session-" + str(number), "MediaSources": [source(route)]})
                return
            if path == "/Sessions/Playing":
                with lock:
                    state["sessions_started"] += 1
            self.output(status=204)

        def do_GET(self):
            parsed = urlsplit(self.path)
            path, query = parsed.path, parse_qs(parsed.query)
            if path == "/__fixture__/state":
                with lock:
                    snapshot = json.loads(json.dumps(state))
                self.output(snapshot)
                return
            if path.endswith("/System/Info/Public"):
                with lock:
                    delay = state["health_delay"]
                    body_delay = state["health_body_delay"]
                    state["health_public_requests"] += 1
                    state["events"].append({"kind": "health_public_request", "number": state["health_public_requests"],
                                            "at_ms": round((time.monotonic() - reset_at[0]) * 1000)})
                if delay:
                    time.sleep(delay)
                self.output({"ServerName": "Local recovery fixture", "Version": "4.9.0", "Id": "fixture-server"}, body_delay=body_delay)
                return
            if path.startswith("/Videos/"):
                authorized = query.get("api_key", [""])[0] == TOKEN
                if not authorized:
                    self.output(status=403)
                    return
                if not path.endswith("/fresh.mp4"):
                    with lock:
                        state["media_failed"] += 1
                    self.output(status=503)
                    return
                with lock:
                    state["media_ok"] += 1
                start, end = 0, len(video) - 1
                match = re.match(r"bytes=(\d+)-(\d*)", self.headers.get("Range", ""))
                if match:
                    start = int(match[1])
                    end = min(end, int(match[2])) if match[2] else end
                if start > end:
                    self.output(status=416)
                    return
                self.send_response(206 if match else 200)
                self.send_header("Content-Type", "video/mp4")
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("Content-Length", str(end - start + 1))
                if match:
                    self.send_header("Content-Range", f"bytes {start}-{end}/{len(video)}")
                self.end_headers()
                if self.command != "HEAD":
                    try:
                        self.wfile.write(video[start:end + 1])
                    except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                        pass
                return
            if path.endswith("/Views"):
                with lock:
                    code = state["auth_status"]
                self.output({"Items": [{"Id": "fixture-library", "Name": "Movies", "Type": "CollectionFolder", "CollectionType": "movies"}]}, code)
            elif path.endswith("/Items/fixture-library"):
                self.output({"Id": "fixture-library", "Name": "Movies", "Type": "CollectionFolder", "CollectionType": "movies"})
            elif path.endswith("/Items/" + MOVIE):
                self.output(movie())
            elif path.endswith("/Items/Latest"):
                self.output([movie()])
            elif path.endswith("/Items"):
                self.output({"Items": [movie()], "TotalRecordCount": 1})
            elif path.endswith(("/Genres", "/Items/Resume", "/Shows/NextUp")):
                self.output({"Items": []})
            else:
                self.output(status=404)

    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    server.daemon_threads = True
    print(json.dumps({"port": server.server_address[1]}), flush=True)
    try:
        server.serve_forever()
    finally:
        server.server_close()

if __name__ == "__main__":
    main()
