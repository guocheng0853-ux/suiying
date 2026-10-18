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
  System.out.println("PASS: whisper job body/urls/status parsing and chinese labels.");
 }
}
