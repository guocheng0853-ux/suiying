"""随影 Whisper 字幕桥：收播放直链，下载→抽音轨→faster-whisper 转写→回 SRT。
单 worker 串行；令牌鉴权；仅本机文件清理，不碰其他目录。"""
import json,os,subprocess,threading,time,uuid,urllib.request
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer

ROOT=os.path.dirname(os.path.abspath(__file__))
MEDIA=os.path.join(ROOT,"media");os.makedirs(MEDIA,exist_ok=True)
TOKEN=os.environ.get("WHISPER_TOKEN","")
DEFAULT_MODEL=os.environ.get("WHISPER_MODEL","base")
FFMPEG=os.environ.get("WHISPER_FFMPEG","/usr/bin/ffmpeg")
MODELS=("tiny","base","small")
JOBS={};RUN_LOCK=threading.Lock();STATE_LOCK=threading.Lock()
_MODEL=None;_MODEL_NAME=""
_model_lock=threading.Lock()

def log(m):print(time.strftime("%m-%d %H:%M:%S"),m,flush=True)
def fmt_srt(t):
    h=int(t//3600);m=int(t%3600//60);s=int(t%60);ms=int(round((t-int(t))*1000))
    if ms==1000:ms=0;s+=1
    return "%02d:%02d:%02d,%03d"%(h,m,s,ms)
def get_model(name):
    global _MODEL,_MODEL_NAME
    with _model_lock:
        if _MODEL is None or _MODEL_NAME!=name:
            from faster_whisper import WhisperModel
            log("加载模型 "+name)
            _MODEL=WhisperModel(name,device="cpu",compute_type="int8");_MODEL_NAME=name
        return _MODEL

def run_job(job):
    media=os.path.join(MEDIA,job["id"]+".bin");wav=os.path.join(MEDIA,job["id"]+".wav")
    try:
        job["status"]="downloading"
        req=urllib.request.Request(job["url"],headers={"User-Agent":"suiying-whisper/1.0"})
        with urllib.request.urlopen(req,timeout=90) as r:
            total=int(r.headers.get("Content-Length") or 0);done=0;last=0
            with open(media,"wb") as f:
                while True:
                    chunk=r.read(1<<20)
                    if not chunk:break
                    done+=len(chunk);f.write(chunk)
                    if total and done-last>20*(1<<20):last=done;job["progress"]=min(49,done*49//max(1,total));log(job["id"]+" 下载 "+str(done))
        if not os.path.isfile(media) or os.path.getsize(media)<1024:raise RuntimeError("下载内容过小，可能地址不可用")
        job["status"]="transcribing";job["progress"]=50
        subprocess.run([FFMPEG,"-y","-loglevel","error","-i",media,"-vn","-ac","1","-ar","16000",wav],check=True)
        model=get_model(job.get("model") or DEFAULT_MODEL)
        segments,info=model.transcribe(wav,language=None,vad_filter=False,initial_prompt="以下是普通话的句子。")
        out=[]
        for i,seg in enumerate(segments,1):
            out.append("%d\n%s --> %s\n%s\n"%(i,fmt_srt(seg.start),fmt_srt(seg.end),seg.text.strip()))
            job["progress"]=50+int(min(49,seg.end/info.duration*49)) if info.duration else 50
        job["srt"]="\n".join(out);job["status"]="done";log(job["id"]+" 完成 "+str(len(out))+" 条")
    except Exception as e:
        job["status"]="failed";job["error"]=str(e)[:300];log(job["id"]+" 失败 "+repr(e))
    finally:
        for p in (media,wav):
            try:os.remove(p)
            except Exception:pass

class Handler(BaseHTTPRequestHandler):
    def log_message(self,*a):pass
    def _json(self,code,obj):self.send_response(code);self.send_header("Content-Type","application/json");self.end_headers();self.wfile.write(json.dumps(obj).encode())
    def _auth(self):return self.headers.get("X-Token","")==TOKEN and TOKEN!=""
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
        if not url.startswith(("http://","https://")):return self._json(400,{"error":"url must be http(s)"})
        model=str(body.get("model") or DEFAULT_MODEL)
        if model not in MODELS:model=DEFAULT_MODEL
        jid=uuid.uuid4().hex[:12];job={"id":jid,"url":url,"model":model,"status":"queued","progress":0,"created":time.time()}
        with STATE_LOCK:JOBS[jid]=job
        threading.Thread(target=lambda:RUN_LOCK and (RUN_LOCK.acquire(),run_job(job),RUN_LOCK.release()),daemon=True).start()
        log("任务 "+jid+" "+url[:80])
        self._json(200,{"job_id":jid})

if __name__=="__main__":
    port=int(os.environ.get("WHISPER_PORT","8765"))
    if TOKEN=="":raise SystemExit("WHISPER_TOKEN 未设置")
    log("whisper-srt 启动 :%d model=%s"%(port,DEFAULT_MODEL))
    ThreadingHTTPServer(("0.0.0.0",port),Handler).serve_forever()
