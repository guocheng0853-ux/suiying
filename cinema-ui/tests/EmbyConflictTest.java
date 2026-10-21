package com.guocheng.mediapocket;
public class EmbyConflictTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(EmbyApi.serverReason("{\"Message\":\"已达到同时播放上限\"}").equals("已达到同时播放上限"),"json Message");
  check(EmbyApi.serverReason("<html><body><h1>409 Conflict</h1><p>设备数超限</p></body></html>").equals("409 Conflict 设备数超限"),"html stripped: "+EmbyApi.serverReason("<html><body><h1>409 Conflict</h1><p>设备数超限</p></body></html>"));
  check(EmbyApi.serverReason("see https://x.example/a?api_key=abc for help").contains("[地址]")&&!EmbyApi.serverReason("see https://x.example/a?api_key=abc").contains("api_key"),"url hidden");
  check(EmbyApi.serverReason("token 0123456789abcdef0123456789abcdef rejected").contains("[已隐藏]"),"token-like hidden");
  check(EmbyApi.serverReason("")==""&&EmbyApi.serverReason(null).isEmpty()&&EmbyApi.serverReason("{\"x\":1}").isEmpty(),"empty / json without message");
  StringBuilder big=new StringBuilder();for(int i=0;i<200;i++)big.append("错");check(EmbyApi.serverReason(big.toString()).length()<=81,"capped");
  check(EmbyApi.conflict(new java.io.IOException("HTTP 409：满了"))&&!EmbyApi.conflict(new java.io.IOException("HTTP 403")),"conflict detection");
  String m=EmbyApi.explain(new java.io.IOException("HTTP 409：已达到同时播放上限"));check(m.contains("HTTP 409")&&m.contains("已达到同时播放上限")&&m.contains("同时播放"),"409 explained: "+m);
  check(EmbyApi.explain(new java.io.IOException("HTTP 409")).contains("等一两分钟"),"409 without body");
  check(EmbyApi.explain(new java.io.IOException("HTTP 500：内部错误")).contains("内部错误"),"other codes keep reason");
  check(HttpRange.MAX_SKIP==64L*1024*1024&&HttpRange.plan(200,HttpRange.MAX_SKIP*2,HttpRange.UNSET,HttpRange.MAX_SKIP*4,HttpRange.UNSET).skip>HttpRange.MAX_SKIP,"skip cap constant and plan");
  check(!HttpRange.rejectIgnoredRange(HttpRange.MAX_SKIP+1,false),"open and moov may skip past 64MB");
  check(HttpRange.rejectIgnoredRange(HttpRange.MAX_SKIP+1,true)&&!HttpRange.rejectIgnoredRange(HttpRange.MAX_SKIP,true),"user seek rejects only above 64MB");
  check(OfflineHttp.UA.equals("MediaPocket/0.1.3"),"vlc and offline identity");
  check(OfflineHttp.BROWSER_UA.startsWith("Mozilla")&&!OfflineHttp.BROWSER_UA.equals(OfflineHttp.UA),"media3 default is a browser UA");
  System.out.println("PASS: Emby server reasons (json/html/sanitised/capped), 409 detection and wording, ignored-Range skip cap only for user seeks, VLC UA plus browser UA.");
 }
}
