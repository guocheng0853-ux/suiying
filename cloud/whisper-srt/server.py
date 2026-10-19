"""随影 Whisper 字幕桥：收播放直链，ffmpeg 直接从直链抽音轨→faster-whisper 转写→回 SRT。
单 worker 串行；令牌鉴权；仅本机文件清理，不碰其他目录。

安全（0.1.91）：
- 默认只监听 127.0.0.1（WHISPER_HOST 可改），对外请用 Caddy 等反代加 HTTPS，令牌不要走明文 http。
- 只接受解析到公网地址的 http(s) 直链，拒绝回环、内网、链路本地（含云厂商元数据 169.254.169.254），避免被当跳板访问本机/内网服务。
- 不再把整部视频下载到磁盘：ffmpeg 边读边抽 16kHz 单声道音轨，只落一个小 wav。
- 排队任务最多 WHISPER_MAX_QUEUE 个（默认 3），完成的任务 6 小时后清掉；令牌用常量时间比较。
"""
import hmac,ipaddress,json,os,socket,subprocess,threading,time,uuid
from urllib.parse import urlparse
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer

ROOT=os.path.dirname(os.path.abspath(__file__))
MEDIA=os.path.join(ROOT,"media");os.makedirs(MEDIA,exist_ok=True)
TOKEN=os.environ.get("WHISPER_TOKEN","")
DEFAULT_MODEL=os.environ.get("WHISPER_MODEL","base")
FFMPEG=os.environ.get("WHISPER_FFMPEG","/usr/bin/ffmpeg")
MODELS=("tiny","base","small")
HOST=os.environ.get("WHISPER_HOST","127.0.0.1")
MAX_QUEUE=int(os.environ.get("WHISPER_MAX_QUEUE","3"))
MAX_SECONDS=int(os.environ.get("WHISPER_MAX_SECONDS",str(4*3600)))
KEEP_SECONDS=6*3600
JOBS={};RUN_LOCK=threading.Lock();STATE_LOCK=threading.Lock()
_MODEL=None;_MODEL_NAME=""
_model_lock=threading.Lock()

def log(m):print(time.strftime("%m-%d %H:%M:%S"),m,flush=True)
def fmt_srt(t):
    h=int(t//3600);m=int(t%3600//60);s=int(t%60);ms=int(round((t-int(t))*1000))
    if ms==1000:ms=0;s+=1
    return "%02d:%02d:%02d,%03d"%(h,m,s,ms)
def public_url(url):
    """只放行解析结果全部是公网地址的 http(s) 直链。返回 None 表示可以，否则返回原因。"""
    try:u=urlparse(url)
    except Exception:return "地址无效"
    if u.scheme not in ("http","https") or not u.hostname:return "url must be http(s)"
    try:infos=socket.getaddrinfo(u.hostname,u.port or (443 if u.scheme=="https" else 80),proto=socket.IPPROTO_TCP)
    except Exception:return "解析不到这个主机"
    for info in infos:
        ip=ipaddress.ip_address(info[4][0].split("%")[0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast or ip.is_unspecified:
            return "不支持内网/本机地址（云端访问不到，也不允许）"
    return None
def prune():
    now=time.time()
    with STATE_LOCK:
        for k in [k for k,j in JOBS.items() if j["status"] in ("done","failed") and now-j.get("finished",now)>KEEP_SECONDS]:JOBS.pop(k,None)
def pending():
    with STATE_LOCK:return sum(1 for j in JOBS.values() if j["status"] in ("queued","downloading","transcribing"))
def get_model(name):
    global _MODEL,_MODEL_NAME
    with _model_lock:
        if _MODEL is None or _MODEL_NAME!=name:
            from faster_whisper import WhisperModel
            log("加载模型 "+name)
            _MODEL=WhisperModel(name,device="cpu",compute_type="int8");_MODEL_NAME=name
        return _MODEL

def run_job(job):
    wav=os.path.join(MEDIA,job["id"]+".wav")
    try:
        job["status"]="downloading";job["progress"]=5
        # ffmpeg 直接读直链（支持 Range/跳转），只输出音轨；-t 限制最长时长，避免超长流占满磁盘
        subprocess.run([FFMPEG,"-y","-loglevel","error","-user_agent","suiying-whisper/1.0","-reconnect","1","-reconnect_streamed","1","-reconnect_delay_max","10","-i",job["url"],"-vn","-sn","-dn","-ac","1","-ar","16000","-t",str(MAX_SECONDS),wav],check=True,timeout=MAX_SECONDS+600)
        if not os.path.isfile(wav) or os.path.getsize(wav)<1024:raise RuntimeError("没有取到音轨，可能地址不可用")
        job["status"]="transcribing";job["progress"]=50
        model=get_model(job.get("model") or DEFAULT_MODEL)
        segments,info=model.transcribe(wav,language=None,vad_filter=False,initial_prompt="以下是普通话的句子。")
        out=[]
        for i,seg in enumerate(segments,1):
            out.append("%d\n%s --> %s\n%s\n"%(i,fmt_srt(seg.start),fmt_srt(seg.end),seg.text.strip()))
            job["progress"]=50+int(min(49,seg.end/info.duration*49)) if info.duration else 50
        job["srt"]="\n".join(out);job["status"]="done";log(job["id"]+" 完成 "+str(len(out))+" 条")
    except subprocess.CalledProcessError:
        job["status"]="failed";job["error"]="ffmpeg 读取或抽音轨失败（地址需要登录/请求头，或不是视频）";log(job["id"]+" 失败 ffmpeg")
    except Exception as e:
        job["status"]="failed";job["error"]=str(e)[:300];log(job["id"]+" 失败 "+repr(e)[:200])
    finally:
        job["finished"]=time.time();job.pop("url",None)
        try:os.remove(wav)
        except Exception:pass

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*a):pass
    def _json(self,code,obj):self.send_response(code);self.send_header("Content-Type","application/json");self.end_headers();self.wfile.write(json.dumps(obj).encode())
    def _auth(self):return TOKEN!="" and hmac.compare_digest(self.headers.get("X-Token","").encode(),TOKEN.encode())
    def do_GET(self):
        p=self.path
        if p=="/health":return self._json(200,{"ok":True})
        if not self._auth():return self._json(401,{"error":"token"})
        if p.startswith("/job/"):
            job=JOBS.get(p[5:]);return self._json(404,{"error":"no job"}) if job is None else self._json(200,{k:job.get(k) for k in ("status","progress","error")})
        if p.startswith("/srt/"):
            job=JOBS.get(p[5:])
            if job is None:return self._json(404,{"error":"no job"})
            if job.get("status")!="done":return self._json(409,{"error":job.get("status","?")})
            body=job["srt"].encode();self.send_response(200);self.send_header("Content-Type","text/plain; charset=utf-8");self.send_header("Content-Length",str(len(body)));self.end_headers();self.wfile.write(body);return
        self._json(404,{"error":"nf"})
    def do_POST(self):
        if self.path!="/job" or not self._auth():return self._json(401,{"error":"token"})
        try:
            n=int(self.headers.get("Content-Length") or 0);body=json.loads(self.rfile.read(min(n,1<<16)) or b"{}")
        except Exception:return self._json(400,{"error":"bad json"})
        url=str(body.get("url",""))
        why=public_url(url)
        if why:return self._json(400,{"error":why})
        prune()
        if pending()>=MAX_QUEUE:return self._json(429,{"error":"任务太多，请等前面的完成"})
        model=str(body.get("model") or DEFAULT_MODEL)
        if model not in MODELS:model=DEFAULT_MODEL
        jid=uuid.uuid4().hex[:12];job={"id":jid,"url":url,"model":model,"status":"queued","progress":0,"created":time.time()}
        with STATE_LOCK:JOBS[jid]=job
        threading.Thread(target=lambda:RUN_LOCK and (RUN_LOCK.acquire(),run_job(job),RUN_LOCK.release()),daemon=True).start()
        log("任务 "+jid+" "+(urlparse(url).hostname or "?"))
        self._json(200,{"job_id":jid})

if __name__=="__main__":
    port=int(os.environ.get("WHISPER_PORT","8765"))
    if len(TOKEN)<24:raise SystemExit("WHISPER_TOKEN 未设置或太短（至少 24 位随机字符）")
    log("whisper-srt 启动 %s:%d model=%s"%(HOST,port,DEFAULT_MODEL))
    ThreadingHTTPServer((HOST,port),Handler).serve_forever()
