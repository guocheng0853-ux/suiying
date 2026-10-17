package com.guocheng.mediapocket;
/** 调试文本脱敏（无 Android 依赖，便于在 JVM 上测试）：网址只留主机名，常见令牌参数打码。 */
final class DebugText {
 private DebugText(){}
 static String sanitize(String s){if(s==null)return "";return s.replaceAll("(?i)\\b(https?|rtsp|rtmp|magnet):(//)?([^/\\s?#\"']*)[^\\s\"']*","$1:$2$3/…").replaceAll("(?i)(token|key|auth|sign|pwd|password|cookie)=([^&\\s]+)","$1=…");}
}
