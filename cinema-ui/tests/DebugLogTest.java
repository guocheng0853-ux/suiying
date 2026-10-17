package com.guocheng.mediapocket;
public class DebugLogTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  String s=DebugText.sanitize("加载失败 https://media.example.com/0123456789abcdeffedcba9876543210/tvbox/my_ext.json?x=1 与 http://10.0.0.2:5244/d/电影/a.mkv?sign=abc");
  check(s.contains("https://media.example.com/…")&&!s.contains("0123456789abcdef")&&!s.contains("my_ext"),"path token removed: "+s);
  check(s.contains("http://10.0.0.2:5244/…")&&!s.contains("sign=abc")&&!s.contains("电影"),"second url host only: "+s);
  check(DebugText.sanitize("magnet:?xt=urn:btih:0123&tr=udp://t:1").startsWith("magnet:")&&!DebugText.sanitize("magnet:?xt=urn:btih:0123").contains("0123"),"magnet stripped");
  check(DebugText.sanitize("Cookie: token=abcdef; other").contains("token=…"),"token param masked");
  check(DebugText.sanitize("P2P · 206 人连接 · 2604.6 KB/s · 等待片段 3631").equals("P2P · 206 人连接 · 2604.6 KB/s · 等待片段 3631"),"plain status untouched");
  System.out.println("PASS: debug log keeps only hosts for URLs (path tokens, signed params, magnet hashes removed), masks token-like params, leaves status text intact.");
 }
}
