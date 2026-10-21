package com.guocheng.mediapocket;
import org.json.*;
public final class EmbyStoreCopyTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a)throws Exception{
  String raw="[{\"id\":\"a\",\"token\":\"old\",\"url\":\"http://x\",\"userId\":\"u\"},{\"id\":\"b\",\"token\":\"keep\"}]";
  JSONObject first=EmbyStore.copyList(raw).getJSONObject(0);
  JSONObject second=EmbyStore.copyList(raw).getJSONObject(0);
  first.put("token","mutated");first.put("url","http://broken");first.put("userId","nope");
  check("old".equals(second.getString("token"))&&"http://x".equals(second.getString("url")),"two list results do not share objects");
  check("old".equals(EmbyStore.copyList(raw).getJSONObject(0).getString("token")),"mutating a copy leaves the stored text alone");
  String saved=EmbyStore.publish(raw,new JSONObject(second.toString()).put("token","new").put("client","Jellyfin Android"));
  check("new".equals(EmbyStore.copyList(saved).getJSONObject(0).getString("token")),"explicit publish writes the new token");
  check("keep".equals(EmbyStore.copyList(saved).getJSONObject(1).getString("token")),"other servers stay intact");
  check("old".equals(EmbyStore.copyList(raw).getJSONObject(0).getString("token")),"failed re-login that never publishes does not replace the saved server");
  System.out.println("PASS: Emby server list returns isolated copies and only explicit publish changes the stored text.");
 }
}
