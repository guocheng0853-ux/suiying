package com.guocheng.mediapocket;
public class EmbyRecoveryRulesTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  check(EmbyRecoveryRules.eligible(true,true,true,true,false,0,"系统解码错误 1 / -1"),"unknown terminal connection failure gets one fresh PlaybackInfo");
  check(!EmbyRecoveryRules.eligible(true,true,true,true,false,1,"超时"),"refreshed activity cannot start a retry loop");
  check(!EmbyRecoveryRules.eligible(false,true,true,true,false,0,"超时"),"auto-switch off is respected");
  check(!EmbyRecoveryRules.eligible(true,false,true,true,false,0,"超时"),"non-Emby sources do not refresh credentials");
  check(!EmbyRecoveryRules.eligible(true,true,false,true,false,0,"播放结束"),"normal completion never refreshes");
  check(!EmbyRecoveryRules.eligible(true,true,true,false,false,0,"超时"),"background player never launches a recovery");
  check(!EmbyRecoveryRules.eligible(true,true,true,true,true,0,"超时"),"menu and source selection take priority");
  for(String raw:new String[]{"HTTP 401","HTTP 403","HTTP 409","密码错误","没有权限","解码器不支持","unsupported format"})check(!EmbyRecoveryRules.eligible(true,true,true,true,false,0,raw),"permanent/user-action failure: "+raw);
  check(EmbyRecoveryRules.carry(true,"same","same",1)==1,"same-film replacement preserves the one-shot budget");
  check(EmbyRecoveryRules.carry(true,"old","next",1)==0,"new episode gets a new budget");
  check(EmbyRecoveryRules.carry(false,"same","same",1)==0,"new user playback starts a fresh chain");
  check(EmbyRecoveryRules.afterCancel(true,true,1)==0,"notification/window interruption refunds an unfinished refresh");
  check(EmbyRecoveryRules.afterCancel(false,true,1)==1,"completed or failed refresh never regains a budget on focus changes");
  check(EmbyRecoveryRules.afterCancel(true,false,1)==1,"explicit cancel must not cause the native poll to restart automatically");
  check(EmbyRecoveryRules.afterCancel(false,false,0)==0,"no attempt keeps its original budget");
  int budget=1;budget=EmbyRecoveryRules.afterCancel(true,true,budget);
  check(EmbyRecoveryRules.eligible(true,true,true,true,false,budget,"超时"),"returning to the player resumes one interrupted attempt");
  budget=1;budget=EmbyRecoveryRules.afterCancel(false,true,budget);
  check(!EmbyRecoveryRules.eligible(true,true,true,true,false,budget,"超时"),"another focus cycle after a result cannot retry");
  java.nio.file.Path src=java.nio.file.Paths.get("src/com/guocheng/mediapocket");
  String playback=java.nio.file.Files.readString(src.resolve("EmbyPlayback.java"));
  int capture=playback.indexOf("final boolean wasTranscode=(recovery!=null||media3Refresh)&&TrackSelection.stream(a).optBoolean(\"emby.transcodeFallback\",false);");
  check(capture>=0&&capture<playback.indexOf("OPEN.execute(new Runnable()"),"capture the active transcode route on the UI thread before refreshing");
  check(playback.contains("if(recovery!=null||media3Refresh)vlc.accept(wasTranscode);")&&!playback.contains("if(recovery!=null)vlc.accept(false);"),"automatic and Media3 manual refresh preserve the chosen transcode-first route instead of forcing direct P5");
  // 0.1.147 C3：Media3 里手动「重新获取地址」同一集，留在 Media3
  check(EmbyRecoveryRules.keepMedia3(true,true,"item-1","item-1"),"Media3 manual refresh of the same item stays in Media3");
  check(!EmbyRecoveryRules.keepMedia3(true,true,"item-1","item-2"),"next episode from Media3 keeps the normal player choice");
  check(!EmbyRecoveryRules.keepMedia3(false,true,"item-1","item-1"),"VLC/native refresh is unchanged");
  check(!EmbyRecoveryRules.keepMedia3(true,false,"item-1","item-1"),"a fresh playback from a detail page is not a refresh");
  check(!EmbyRecoveryRules.keepMedia3(true,true,"",""),"unknown current item is not treated as the same film");
  int capture3=playback.indexOf("final boolean media3Refresh=EmbyRecoveryRules.keepMedia3(a instanceof SystemPlayerActivity,replace,TrackSelection.stream(a).optString(\"emby.item\"),itemId);");
  check(capture3>=0&&capture3<playback.indexOf("OPEN.execute(new Runnable()"),"Media3 refresh is decided on the UI thread from the playing stream");
  int consumer=playback.indexOf("if(media3Refresh){SystemPlayerActivity.open(a,first,watch,meta.optString(\"name\"),pos,in);if(replace)a.finish();return;}a.startActivity(in);");
  check(consumer>0,"manual Media3 refresh reopens SystemPlayerActivity instead of PlayerActivity");
  String system=java.nio.file.Files.readString(src.resolve("SystemPlayerActivity.java"));
  check(system.contains("EmbyPlayback.playItem(this,server,item,media,true);"),"retrySource refreshes the same item with replace=true");
  check(playback.contains("if(transcodeFirst&&all.length()>1)"),"a single available route remains usable");
  org.json.JSONObject direct=new org.json.JSONObject().put("emby.direct",true).put("emby.item","fixture-item").put("emby.mediaSource","fixture-source").put("emby.playSession","fixture-session");
  org.json.JSONArray routes=EmbyApi.withTranscode(direct,"https://example.invalid","fixture-token","fixture-device");
  check(routes.length()==2&&!routes.getJSONObject(0).optBoolean("emby.transcodeFallback")&&routes.getJSONObject(1).optBoolean("emby.transcodeFallback"),"actual Emby routes identify the selected server-transcode path");
  check(EmbyApi.withTranscode(routes.getJSONObject(1),"https://example.invalid","fixture-token","fixture-device").length()==1,"an existing transcode route is not duplicated");
  check(EmbyRecoveryRules.resume(0,123456,10000)==123456,"stopped player keeps the last known position");
  check(EmbyRecoveryRules.resume(20000,123456,10000)==20000,"live position wins over retained state");
  check(EmbyRecoveryRules.resume(-1,0,10000)==10000,"startup failure retains original resume");
  System.out.println("PASS: one-shot recovery, user settings, background/menu guards, permanent failures, handoff budget and Media3 manual refresh.");
 }
}
