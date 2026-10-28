package com.guocheng.mediapocket;
import org.json.*;
/** 片头标记、BIF、下一集去重、音轨对应、备用地址。 */
public final class EmbyFeaturesTest {
 static int failures;
 public static void main(String[] args)throws Exception{
  if(!EmbyCue.showChapterMarks(true,true,2400000,true))throw new AssertionError("visible timeline shows chapters");
  if(EmbyCue.showChapterMarks(false,true,2400000,true)||EmbyCue.showChapterMarks(true,false,2400000,true)||EmbyCue.showChapterMarks(true,true,0,true)||EmbyCue.showChapterMarks(true,true,2400000,false))throw new AssertionError("hidden controls/seek, unknown duration, empty chapters leave no floating dots");

  JSONArray chapters=new JSONArray()
   .put(new JSONObject().put("StartPositionTicks",0).put("MarkerType","IntroStart").put("Name","片头开始"))
   .put(new JSONObject().put("StartPositionTicks",90L*10000000L).put("MarkerType","IntroEnd"))
   .put(new JSONObject().put("StartPositionTicks",600L*10000000L).put("Name","中段").put("ImageTag","t1"))
   .put(new JSONObject().put("StartPositionTicks",1200L*10000000L).put("MarkerType","CreditsStart").put("Name","片尾"));
  EmbyCue.Span span=EmbyCue.spans(chapters);
  check(span.introStart==0&&span.introEnd==90000&&span.creditsStart==1200000,"marker times");
  JSONArray listed=EmbyCue.chapters(chapters);
  check(listed.length()==1&&listed.getJSONObject(0).getString("name").equals("中段")&&listed.getJSONObject(0).getInt("index")==2,"markers stay off the chapter strip");
  check("跳过片头".equals(EmbyCue.skipText(span,1000,1800000))&&EmbyCue.skipTo(span,1000,1800000,EmbyCue.ASK)==90000L,"ask skips intro to its end");
  check(EmbyCue.skipTo(span,1000,1800000,EmbyCue.OFF)==null&&EmbyCue.skipText(span,5000,1800000)!=null,"off never seeks");
  check("跳过片尾".equals(EmbyCue.skipText(span,1300000,1800000))&&EmbyCue.skipTo(span,1300000,1800000,EmbyCue.AUTO)==1799200L,"credits skip lands just before the end");
  check(EmbyCue.showSkipButton(EmbyCue.ASK,"跳过片头",false,1000)&&!EmbyCue.showSkipButton(EmbyCue.ASK,"跳过片头",false,4000)&&EmbyCue.showSkipButton(EmbyCue.ASK,"跳过片头",true,9000)&&!EmbyCue.showSkipButton(EmbyCue.AUTO,"跳过片头",true,0),"button hides unless controls are up");
  check(EmbyCue.nextDue(true,true,false,1200000,1800000,span.creditsStart)&&!EmbyCue.nextDue(true,true,false,1199999,1800000,span.creditsStart),"next card starts at credits");
  check(EmbyCue.nextDue(true,true,false,90000,120000,-1)&&!EmbyCue.nextDue(true,true,false,89999,120000,-1)&&!EmbyCue.nextDue(false,true,false,100000,120000,-1)&&!EmbyCue.nextDue(true,true,true,100000,120000,-1),"fallback is the last 30 seconds");
  check(EmbyCue.countdownSeconds(0)==10&&EmbyCue.countdownSeconds(10000)==0,"ten second countdown");
  JSONArray next=new JSONArray().put(new JSONObject().put("Id","a")).put(new JSONObject().put("Id","b"));
  JSONArray resume=new JSONArray().put(new JSONObject().put("Id","b"));
  JSONArray deduped=EmbyCue.withoutResume(next,resume);
  check(deduped.length()==1&&deduped.getJSONObject(0).getString("Id").equals("a"),"next up drops items already in resume");
  check("播放".equals(EmbyCue.playLabel(0))&&"继续 12:03".equals(EmbyCue.playLabel(723000))&&"继续 1:02:03".equals(EmbyCue.playLabel(3723000)),"hero play label");
  JSONArray heroes=EmbyCue.pickHeroes(new JSONArray().put(item("r1",true)).put(item("r2",false)),new JSONArray().put(item("l1",true)).put(item("r1",true)).put(item("l2",true)).put(item("l3",true)).put(item("l4",true)));
  check(heroes.length()==5&&heroes.getJSONObject(0).getString("Id").equals("r1")&&heroes.getJSONObject(1).getString("Id").equals("l1"),"heroes prefer resume backdrops and stay unique");
  String[] order=EmbyCue.homeOrder(new JSONArray().put(new JSONObject().put("Type","LatestMedia")).put(new JSONObject().put("Type","Resume")));
  check(order.length==2&&order[0].equals("latest")&&order[1].equals("resume"),"server home section order");
  check(EmbyCue.homeOrder(null).length==3&&EmbyCue.homeOrder(new JSONArray().put(new JSONObject().put("Type","Nope")))[0].equals("resume"),"unknown sections fall back");
  JSONArray hidden=EmbyCue.toggleHidden(new JSONArray(), "lib");
  check(hidden.length()==1&&EmbyCue.toggleHidden(hidden,"lib").length()==0,"hide library toggles");
  JSONArray versions=EmbyCue.versions(new JSONArray().put(new JSONObject().put("Id","a").put("Name","原盘").put("Container","mkv")).put(new JSONObject().put("Id","b").put("Path","/m/目录/精简.mp4")));
  check(versions.length()==2&&versions.getJSONObject(1).getString("name").startsWith("精简.mp4"),"version names");
  check(EmbyCue.versions(new JSONArray().put(new JSONObject().put("Id","only"))).length()==0,"one version needs no picker");
  String[] bases=EmbyCue.bases("http://a:8096",new JSONArray().put("http://b:8096/").put("ftp://x").put("http://A:8096").put(""));
  check(bases.length==2&&bases[0].equals("http://a:8096")&&bases[1].equals("http://b:8096"),"backup urls drop invalid and duplicate hosts");
  check(EmbyCue.retryable(new java.net.ConnectException("x"))&&EmbyCue.retryable(new IllegalArgumentException("没有可播放的视频流"))&&!EmbyCue.retryable(new java.io.IOException("HTTP 401")),"only route and stream failures try the next url");
  check(EmbyCue.matchTrack(2,"chi","ac3",1,new int[]{1,2},new String[]{"eng aac","chi ac3"})==2,"exact emby index maps to the vlc id");
  check(EmbyCue.matchTrack(9,"zh","aac",1,new int[]{4,5},new String[]{"eng aac","zho aac"})==5,"language and codec match when indexes differ");
  check(EmbyCue.matchTrack(9,"ger","",0,new int[]{4,5},new String[]{"one","two"})==4,"otherwise follow audio order");
  check(EmbyCue.matchTrack(9,"ger","",3,new int[]{4},new String[]{"one"})==-1,"no match stays unset");
  check(EmbyCue.libraryPath("u","lib","Movie",0,"喜剧",true).contains("Genres=")&&EmbyCue.libraryPath("u","lib","Movie",0,"喜剧",true).contains("Filters=IsFavorite"),"genre and favorite filters");
  byte[] bif=sample();
  BifIndex index=BifIndex.parse(bif);
  check(index!=null&&index.size()==2&&index.nearest(0)==0&&index.nearest(9999)==0&&index.nearest(10000)==1,"bif timestamps");
  byte[] jpeg=index.jpeg(1);check(jpeg!=null&&jpeg[0]==(byte)0xff&&jpeg[1]==(byte)0xd8&&jpeg[jpeg.length-2]==(byte)0xff,"bif frame is the jpeg slice");
  check(BifIndex.parse(new byte[]{1,2,3,4})==null&&BifIndex.parse(null)==null,"bad bif is ignored");
  check(SpeedHold.tuned(0,1000)==2f&&Math.abs(SpeedHold.tuned(-180,1000)-(2f-1.44f))<0.001f&&SpeedHold.tuned(-360,1000)==0.1f&&Math.abs(SpeedHold.tuned(400,1000)-5.2f)<0.001f&&SpeedHold.tuned(10,1000,32f)==2f,"hold speed stays at 2 inside the dead zone and scales across the screen");
  check(SpeedHold.tuned(0,1000,0,3f)==3f&&SpeedHold.capsule(2f).equals("加速 2.0×"),"hold base is configurable and the hud is chinese");
  check(!EmbyCue.episodePageDone(0,100,100,161)&&EmbyCue.episodePageDone(100,61,100,161)&&EmbyCue.episodePageDone(0,7,100,-1)&&!EmbyCue.episodePageDone(0,100,100,-1),"episode pages stop on a short page or the reported total");
  String episodes=EmbyCue.episodesPath("u","series","",0,100);check(episodes.contains("StartIndex=0")&&episodes.contains("Limit=100"),"episode query is paged");
  JSONObject cachedHome=new JSONObject().put("libs",new JSONArray().put(new JSONObject().put("Id","lib"))).put("latest",new JSONArray().put(new JSONArray().put(new JSONObject().put("Name","A")).put(new JSONObject().put("Name","B"))));
  JSONObject kept=EmbyCue.cachedLatest(cachedHome,"lib");
  check(kept.optJSONArray("Items")!=null&&kept.optJSONArray("Items").length()==2,"cooldown returns the last saved library items");
  check(!EmbyCue.cachedLatest(cachedHome,"other").has("Items")&&!EmbyCue.cachedLatest(null,"lib").has("Items")&&!EmbyCue.cachedLatest(new JSONObject(cachedHome.toString()).put("converted",true),"lib").has("Items"),"missing or converted cache stays an empty object");
  JSONObject incoming=new JSONObject().put("latest",new JSONArray().put(kept.optJSONArray("Items")));
  AggregateRules.EmbyKeep keptPlan=AggregateRules.embyKeep(cachedHome,incoming,false);
  check(keptPlan.writeCache&&!keptPlan.keepRows,"last-good latest is not a weaker refresh");
  AggregateRules.EmbyKeep emptyPlan=AggregateRules.embyKeep(cachedHome,new JSONObject().put("latest",new JSONArray().put(new JSONArray())),false);
  check(!emptyPlan.writeCache&&emptyPlan.keepRows,"a real empty latest still keeps the painted rows");
  String homeSrc=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/EmbyHome.java"));
  check(homeSrc.contains("latestOrKept")&&!homeSrc.contains("skipping(server,\"latest:\"+id)?new JSONObject()"),"latest cooldown no longer returns a silent empty object");
  if(failures>0)throw new RuntimeException(failures+" failed");
  System.out.println("PASS: Emby markers, BIF, NextUp, tracks, backup urls, home sections.");
 }
 static JSONObject item(String id,boolean backdrop)throws JSONException{JSONObject o=new JSONObject().put("Id",id);if(backdrop)o.put("BackdropImageTags",new JSONArray().put("bd"));return o;}
 static byte[] sample(){int count=2,mult=1000;byte[] j0=new byte[]{(byte)0xff,(byte)0xd8,1,(byte)0xff,(byte)0xd9};byte[] j1=new byte[]{(byte)0xff,(byte)0xd8,2,3,(byte)0xff,(byte)0xd9};int indexAt=64,body=indexAt+(count+1)*8;byte[] file=new byte[body+j0.length+j1.length];byte[] magic=BifIndex.MAGIC;System.arraycopy(magic,0,file,0,magic.length);put(file,12,count);put(file,16,mult);put(file,64,0);put(file,68,body);put(file,72,10);put(file,76,body+j0.length);put(file,80,0xffffffff);put(file,84,body+j0.length+j1.length);System.arraycopy(j0,0,file,body,j0.length);System.arraycopy(j1,0,file,body+j0.length,j1.length);return file;}
 static void put(byte[] d,int at,int v){d[at]=(byte)v;d[at+1]=(byte)(v>>>8);d[at+2]=(byte)(v>>>16);d[at+3]=(byte)(v>>>24);}
 static void check(boolean ok,String name){if(!ok){failures++;System.out.println("FAIL "+name);}}
}
