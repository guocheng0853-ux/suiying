package com.guocheng.mediapocket;
public class WhisperClientTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args){
  check(WhisperClient.trim(" http://srv:8765/ ").equals("http://srv:8765"),"base url trimmed");
  check(WhisperClient.jobUrl("http://srv:8765").equals("http://srv:8765/job")&&WhisperClient.srtUrl("http://srv:8765/","abc").equals("http://srv:8765/srt/abc"),"job and srt urls");
  org.json.JSONObject body=new org.json.JSONObject(WhisperClient.jobBody("https://x/v.mp4",""));
  check(body.optString("url").equals("https://x/v.mp4")&&body.optString("model").equals("base"),"job body defaults to base model");
  check(WhisperClient.jobId("{\"job_id\":\"abc123\"}").equals("abc123"),"job id parsed");
  check(WhisperClient.status("{\"status\":\"transcribing\",\"progress\":60}").equals("transcribing"),"status parsed");
  check(WhisperClient.error("{\"error\":\"下载失败\"}").equals("下载失败"),"error parsed");
  check(WhisperClient.statusLabel("queued").equals("排队中")&&WhisperClient.statusLabel("downloading").equals("云端下载视频")&&WhisperClient.statusLabel("transcribing").equals("语音识别中"),"status labels are chinese");
  check(WhisperClient.safe("not-json").optString("status","").isEmpty(),"broken json is safe");
  for(String u:new String[]{"http://127.0.0.1:5244/a.mkv","http://192.168.1.8:8096/Videos/1/stream","http://10.0.0.2/a","http://172.20.1.1/a","http://localhost/a","http://[::1]/a","http://nas.local/a","http://169.254.169.254/x"})if(!WhisperClient.unreachableFromCloud(u))throw new AssertionError("should be local: "+u);
  for(String u:new String[]{"https://xiaoya.example.com/d/a.mkv","http://8.8.8.8/a","http://172.32.0.1/a","https://emby.example.org:8920/Videos/1/stream?api_key=x"})if(WhisperClient.unreachableFromCloud(u))throw new AssertionError("should be public: "+u);
  System.out.println("PASS: whisper job body/urls/status parsing and chinese labels.");
 }
}
