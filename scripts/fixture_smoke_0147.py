#!/usr/bin/env python3
"""Independent smoke for scripts/android_emby_fixture.py (0.1.147 helpers).

Proves fixture capability only — not Android client fixes. Exit 0 on pass.
"""
from __future__ import annotations

import json
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "scripts" / "android_emby_fixture.py"
VIDEO = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/fixture-0147/clip.mp4")


def http(method: str, url: str, body: dict | None = None, timeout: float = 5.0) -> tuple[int, object]:
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        raw = resp.read()
        return resp.status, json.loads(raw.decode()) if raw else None


def check(cond: bool, why: str) -> None:
    if not cond:
        raise AssertionError(why)


def peek_close(host: str, port: int, path: str, wait: float = 0.4) -> None:
    """Open a raw GET, read status/headers, then close during body delay."""
    with socket.create_connection((host, port), timeout=5) as sock:
        sock.sendall(f"GET {path} HTTP/1.1\r\nHost: {host}\r\nConnection: close\r\n\r\n".encode())
        buf = b""
        while b"\r\n\r\n" not in buf:
            chunk = sock.recv(4096)
            check(chunk, "fixture closed before headers")
            buf += chunk
        time.sleep(wait)


def main() -> None:
    check(VIDEO.is_file(), f"missing test video: {VIDEO}")
    proc = subprocess.Popen(
        [sys.executable, str(FIXTURE), "--video", str(VIDEO), "--port", "0"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    try:
        line = proc.stdout.readline()
        port = json.loads(line)["port"]
        base = f"http://127.0.0.1:{port}"

        # Default health / public info
        code, info = http("GET", f"{base}/System/Info/Public")
        check(code == 200 and info["ServerName"] == "Local recovery fixture", "default health public info")

        # Body-first-byte delay + client close observation
        http("POST", f"{base}/__fixture__/reset", {"mode": "health", "health_body_delay": 2, "catalog": "movies"})
        peek_close("127.0.0.1", port, "/System/Info/Public")
        time.sleep(0.2)
        _, state = http("GET", f"{base}/__fixture__/state")
        check(state["health_body_started"] >= 1, "body first byte observed")
        check(state["health_body_client_closed"] >= 1, "health_body_client_closed recorded")
        check(any(e.get("kind") == "health_body_client_closed" for e in state["events"]), "close event present")

        # configure keeps events / counters; missing params reset to defaults
        before_events = len(state["events"])
        before_closed = state["health_body_client_closed"]
        http("POST", f"{base}/__fixture__/configure", {"mode": "delayed", "delay": 3, "catalog": "series", "playback_conflicts": 1})
        _, state = http("GET", f"{base}/__fixture__/state")
        check(state["mode"] == "delayed" and state["catalog"] == "series", "configure mode/catalog")
        check(state["health_body_client_closed"] == before_closed, "configure preserves counters")
        check(len(state["events"]) == before_events + 1 and state["events"][-1]["kind"] == "configure", "configure event appended")
        check(state["health_body_delay"] == 0, "omitted params return to defaults")

        # Series catalog: Shows + two episodes
        _, views = http("GET", f"{base}/Users/fixture-user/Views")
        check(views["Items"][0]["CollectionType"] == "tvshows", "series catalog Shows folder")
        _, eps = http("GET", f"{base}/Shows/fixture-series/Episodes")
        check(eps["TotalRecordCount"] == 2 and eps["Items"][0]["Id"] == "fixture-episode-1", "two synthetic episodes")

        # One 409 then 200 on PlaybackInfo (non-diagnostic)
        body = json.dumps({"IsPlayback": True, "AutoOpenLiveStream": True}).encode()
        req = urllib.request.Request(f"{base}/Items/fixture-episode-1/PlaybackInfo", data=body, method="POST")
        req.add_header("Content-Type", "application/json")
        try:
            urllib.request.urlopen(req, timeout=5)
            raise AssertionError("expected first PlaybackInfo conflict 409")
        except urllib.error.HTTPError as e:
            check(e.code == 409, f"first conflict code={e.code}")
        with urllib.request.urlopen(req, timeout=5) as resp:
            check(resp.status == 200, "second PlaybackInfo recovers to 200")
            payload = json.loads(resp.read().decode())
            check(payload["MediaSources"][0]["Id"] == "fixture-media", "playback media source")

        _, state = http("GET", f"{base}/__fixture__/state")
        conflicts = [e for e in state["events"] if e.get("kind") == "playback_info" and e.get("response") == 409]
        oks = [e for e in state["events"] if e.get("kind") == "playback_info" and e.get("response") == 200]
        check(len(conflicts) == 1 and len(oks) >= 1, "409 then 200 events recorded")
        check(all("at_ms" in e for e in state["events"]), "events carry relative at_ms")

        # delayed playback close observation
        http("POST", f"{base}/__fixture__/reset", {"mode": "delayed", "delay": 5, "catalog": "movies"})
        auth = json.dumps({"Username": "fixture", "Pw": "fixture"}).encode()
        req = urllib.request.Request(f"{base}/Users/AuthenticateByName", data=auth, method="POST")
        req.add_header("Content-Type", "application/json")
        with urllib.request.urlopen(req, timeout=5) as resp:
            check(resp.status == 200, "synthetic login")
        play = json.dumps({"IsPlayback": True, "AutoOpenLiveStream": True}).encode()
        # First PlaybackInfo returns immediately (number=1); second waits.
        for _ in range(1):
            req = urllib.request.Request(f"{base}/Items/fixture-movie/PlaybackInfo", data=play, method="POST")
            req.add_header("Content-Type", "application/json")
            with urllib.request.urlopen(req, timeout=5) as resp:
                check(resp.status == 200, "first delayed PlaybackInfo")
        # Second: connect, then close while waiting
        with socket.create_connection(("127.0.0.1", port), timeout=5) as sock:
            sock.sendall(
                b"POST /Items/fixture-movie/PlaybackInfo HTTP/1.1\r\n"
                b"Host: 127.0.0.1\r\nContent-Type: application/json\r\n"
                + f"Content-Length: {len(play)}\r\nConnection: close\r\n\r\n".encode()
                + play
            )
            time.sleep(0.3)
        time.sleep(0.3)
        _, state = http("GET", f"{base}/__fixture__/state")
        check(state["playback_client_closed"] >= 1, "playback_client_closed recorded")

        print(json.dumps({
            "passed": True,
            "port": port,
            "checks": [
                "default_health",
                "health_body_client_closed",
                "configure_preserves_events",
                "series_catalog",
                "playback_conflict_then_ok",
                "playback_client_closed",
                "event_at_ms",
            ],
        }, ensure_ascii=False))
    finally:
        try:
            http("POST", f"http://127.0.0.1:{port}/__fixture__/shutdown", {}, timeout=2)
        except Exception:
            proc.kill()
        proc.wait(timeout=5)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(json.dumps({"passed": False, "error": str(e)}, ensure_ascii=False), file=sys.stderr)
        sys.exit(1)
