"""Local Emby fixture for health/recovery acceptance; contains synthetic credentials only.

Run with --video /path/to/a/test.mp4 and reverse its printed TCP port to the device.
POST /__fixture__/reset with {"mode":"health|recover|fail|delayed","delay":20}.
GET /__fixture__/state returns counters; POST /__fixture__/shutdown stops the server.
Never point this fixture at a production account or expose it outside loopback.
"""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit, parse_qs
from pathlib import Path
import argparse, json, threading, time, re, select, socket

TOKEN = "fixture-token-only"
USER, MOVIE, MEDIA = "fixture-user", "fixture-movie", "fixture-media"
SERIES, SEASON = "fixture-series", "fixture-season"
EPISODES = ("fixture-episode-1", "fixture-episode-2")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", required=True, type=Path)
    parser.add_argument("--port", type=int, default=0)
    args = parser.parse_args()
    video = args.video.read_bytes()
    state = {"mode": "health", "delay": 20, "health_delay": 0, "health_body_delay": 0, "auth_status": 200, "playback_info": 0,
             "diagnostic_info": 0, "sessions_started": 0, "media_ok": 0,
             "media_failed": 0, "health_public_requests": 0, "health_body_started": 0,
             "health_body_client_closed": 0, "playback_client_closed": 0,
             "sessions_stopped": 0, "sessions_progress": 0,
             "catalog": "movies", "playback_conflicts": 0, "events": []}
    lock = threading.Lock()
    reset_at = [time.monotonic()]
    generation = [0]

    def event(kind, **values):
        state["events"].append({"kind": kind, "at_ms": round((time.monotonic() - reset_at[0]) * 1000), **values})

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

    def series():
        return {"Id": SERIES, "Name": "Recovery series", "Type": "Series", "Overview": "Synthetic two-episode series."}

    def season():
        return {"Id": SEASON, "SeriesId": SERIES, "Name": "Season 1", "Type": "Season", "IndexNumber": 1}

    def episode(index):
        item = movie()
        item.update(Id=EPISODES[index - 1], Name="Fixture episode " + str(index), Type="Episode",
                    SeriesId=SERIES, SeriesName="Recovery series", SeasonId=SEASON,
                    ParentIndexNumber=1, IndexNumber=index)
        return item

    def library():
        with lock:
            shows = state["catalog"] == "series"
        return {"Id": "fixture-library", "Name": "Shows" if shows else "Movies", "Type": "CollectionFolder",
                "CollectionType": "tvshows" if shows else "movies"}

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *unused):
            pass

        def handle(self):
            try:
                super().handle()
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                pass  # Cancelling the Android request may close this fixture connection.

        def wait_peer(self, seconds, kind, number, epoch):
            """Peek only: observe EOF/reset without consuming any future request bytes."""
            end = time.monotonic() + seconds
            while time.monotonic() < end:
                try:
                    ready, _, _ = select.select([self.connection], [], [], max(0, min(.05, end - time.monotonic())))
                    if not ready:
                        continue
                    if self.connection.recv(1, socket.MSG_PEEK):
                        time.sleep(.02)
                        continue
                    reason = "eof"
                except (ConnectionResetError, ConnectionAbortedError, OSError):
                    reason = "reset"
                with lock:
                    if epoch == generation[0]:
                        state[kind] += 1
                        event(kind, number=number, reason=reason)
                return True
            return False

        def output(self, value=None, status=200, body_delay=0, health_request=None):
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
                            number, epoch = health_request
                            if epoch == generation[0]:
                                state["health_body_started"] += 1
                                event("health_body_started", number=number)
                        if self.wait_peer(body_delay, "health_body_client_closed", number, epoch):
                            return
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
            if path in ("/__fixture__/reset", "/__fixture__/configure"):
                assert body["mode"] in ("health", "recover", "fail", "delayed")
                assert body.get("catalog", "movies") in ("movies", "series")
                with lock:
                    if path.endswith("/reset"):
                        for key in ("playback_info", "diagnostic_info", "sessions_started", "media_ok", "media_failed", "health_public_requests", "health_body_started", "health_body_client_closed", "playback_client_closed", "sessions_stopped", "sessions_progress"):
                            state[key] = 0
                        state["events"] = []
                        reset_at[0] = time.monotonic()
                        generation[0] += 1
                    state["mode"], state["delay"] = body["mode"], int(body.get("delay", 20))
                    state["health_delay"] = int(body.get("health_delay", 0))
                    state["health_body_delay"] = int(body.get("health_body_delay", 0))
                    state["auth_status"] = int(body.get("auth_status", 200))
                    state["catalog"] = body.get("catalog", "movies")
                    state["playback_conflicts"] = max(0, int(body.get("playback_conflicts", 0)))
                    if path.endswith("/configure"):
                        event("configure")
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
                    epoch = generation[0]
                    conflict = not diagnostic and state["playback_conflicts"] > 0
                    if conflict:
                        state["playback_conflicts"] -= 1
                    event(key, number=number, item=path.split("/")[-2], is_playback=body.get("IsPlayback"),
                          live=body.get("AutoOpenLiveStream"), response=409 if conflict else 200)
                if conflict:
                    self.output({"Message": "Synthetic busy playback session"}, 409)
                    return
                if not diagnostic and mode == "delayed" and number >= 2:
                    if self.wait_peer(delay, "playback_client_closed", number, epoch):
                        return
                fresh = diagnostic or mode == "health" or (mode in ("recover", "delayed") and number >= 2)
                route = "/Videos/fixture-movie/" + ("fresh.mp4" if fresh else "old.mp4")
                self.output({"PlaySessionId": "fixture-session-" + str(number), "MediaSources": [source(route)]})
                return
            if path == "/Sessions/Playing":
                with lock:
                    state["sessions_started"] += 1
                    event("session_started", item=body.get("ItemId"), session=body.get("PlaySessionId"))
            elif path == "/Sessions/Playing/Stopped":
                with lock:
                    state["sessions_stopped"] += 1
                    event("session_stopped", item=body.get("ItemId"), session=body.get("PlaySessionId"))
            elif path == "/Sessions/Playing/Progress":
                with lock:
                    state["sessions_progress"] += 1
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
                    number, epoch = state["health_public_requests"], generation[0]
                    event("health_public_request", number=number)
                if delay:
                    time.sleep(delay)
                self.output({"ServerName": "Local recovery fixture", "Version": "4.9.0", "Id": "fixture-server"}, body_delay=body_delay, health_request=(number, epoch))
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
                self.output({"Items": [library()]}, code)
            elif path.endswith("/Items/fixture-library"):
                self.output(library())
            elif path.endswith("/Items/" + SERIES):
                self.output(series())
            elif path.endswith("/Items/" + SEASON):
                self.output(season())
            elif path.split("/")[-1] in EPISODES:
                self.output(episode(EPISODES.index(path.split("/")[-1]) + 1))
            elif path.endswith("/Shows/" + SERIES + "/Seasons"):
                self.output({"Items": [season()], "TotalRecordCount": 1})
            elif path.endswith("/Shows/" + SERIES + "/Episodes"):
                self.output({"Items": [episode(1), episode(2)], "TotalRecordCount": 2})
            elif path.endswith("/Items/" + MOVIE):
                self.output(movie())
            elif path.endswith("/Items/Latest"):
                self.output([series() if library()["CollectionType"] == "tvshows" else movie()])
            elif path.endswith("/Items"):
                self.output({"Items": [series() if library()["CollectionType"] == "tvshows" else movie()], "TotalRecordCount": 1})
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
