package com.guocheng.mediapocket;
public class Media3ErrorRulesTest {
 public static void main(String[] args){
  check(Media3ErrorRules.message(2004,403).contains("HTTP 403"),"actual HTTP response exposed");
  check(Media3ErrorRules.message(2004,0).contains("服务器"),"unknown status is still IO");
  check(Media3ErrorRules.message(2004,404).contains("文件不存在"),"missing file");
  check(Media3ErrorRules.message(2004,416).contains("续播位置"),"range rejected");
  check(Media3ErrorRules.message(HttpRange.SKIP_ERROR,0).equals(HttpRange.SKIP_MESSAGE),"ignored-range skip is explained and not retried as a generic IO error");
  check(Media3ErrorRules.message(2002,0).contains("无法读取"),"network failure");
  check(Media3ErrorRules.message(3003,0).contains("解析"),"container error");
  check(Media3ErrorRules.message(4001,0).contains("无法播放"),"decoder error");
  System.out.println("PASS: HTTP status, network, container and decoder errors remain distinct.");
 }
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
