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
  String stale="[{\"id\":\"gone\",\"url\":\"http://old\"}]";
  String restored="[{\"id\":\"kept\",\"url\":\"http://main\",\"backups\":[\"http://bak\"],\"token\":\"old\"},{\"id\":\"other\",\"url\":\"http://other\",\"token\":\"o\"}]";
  check(restored.equals(EmbyStore.fresh(null,restored)),"invalidate then read sees the restored list");
  check(stale.equals(EmbyStore.fresh(stale,stale)),"unchanged prefs keep the cached text");
  check(restored.equals(EmbyStore.fresh(stale,restored)),"a cached list is dropped when prefs hold the restored text");
  String restoredSaved=EmbyStore.publish(EmbyStore.fresh(stale,restored),new JSONObject().put("id","other").put("url","http://other").put("token","n"));
  JSONObject kept=null,other=null;JSONArray restoredList=EmbyStore.copyList(restoredSaved);
  for(int i=0;i<restoredList.length();i++){JSONObject s=restoredList.getJSONObject(i);if("kept".equals(s.getString("id")))kept=s;if("other".equals(s.getString("id")))other=s;check(!"gone".equals(s.getString("id")),"the stale server does not come back");}
  check(kept!=null&&"http://main".equals(kept.getString("url"))&&"http://bak".equals(kept.getJSONArray("backups").getString(0)),"backup restore then save keeps the restored server");
  check(other!=null&&"n".equals(other.getString("token"))&&"http://other".equals(other.getString("url")),"the server being saved still updates");
  String stored="[{\"id\":\"a\",\"url\":\"http://main\",\"backups\":[\"http://bak\"],\"token\":\"old\",\"userId\":\"u\",\"password\":\"p\",\"client\":\"Emby\"},{\"id\":\"b\",\"url\":\"http://b\"}]";
  JSONObject trial=EmbyStore.copyList(stored).getJSONObject(0);
  trial.put("url","http://tried");trial.remove("backups");trial.put("token","new").put("userId","u2").put("client","Jellyfin Android");
  String merged=EmbyStore.mergeAuth(stored,trial);
  JSONObject aServer=EmbyStore.copyList(merged).getJSONObject(0),bServer=EmbyStore.copyList(merged).getJSONObject(1);
  check("http://main".equals(aServer.getString("url"))&&"http://bak".equals(aServer.getJSONArray("backups").getString(0)),"trial copy merge keeps url and backups");
  check("new".equals(aServer.getString("token"))&&"u2".equals(aServer.getString("userId"))&&"Jellyfin Android".equals(aServer.getString("client"))&&"p".equals(aServer.getString("password")),"merge writes client, token and userId only");
  check("http://b".equals(bServer.getString("url")),"other servers stay intact during a trial merge");
  JSONObject cleared=new JSONObject(aServer.toString());cleared.remove("client");cleared.remove("backups");cleared.put("url","http://tried-again").put("token","t3");
  JSONObject afterClear=EmbyStore.copyList(EmbyStore.mergeAuth(merged,cleared)).getJSONObject(0);
  check(!afterClear.has("client")&&"t3".equals(afterClear.getString("token"))&&"http://main".equals(afterClear.getString("url"))&&"http://bak".equals(afterClear.getJSONArray("backups").getString(0)),"clearing client keeps url and backups");
  check(stored.equals(EmbyStore.mergeAuth(stored,new JSONObject().put("id","missing").put("url","http://tried").put("token","x"))),"an unknown trial is not inserted");
  System.out.println("PASS: Emby server list returns isolated copies; backup restore then save keeps the restored list; trial merge keeps url and backups.");
 }
}
