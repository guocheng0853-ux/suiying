package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
 public class OfflineQueueRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 /** 播放页下载必须记下正在播的线路。PlaybackDownload.source 转发到 downloadSource。 */
 public static void playingLine()throws Exception{
  String flags="routeA$$$routeB";
  String groups="E01$valueA#E02$valueA2$$$E01$valueB#E02$valueB2";
  String watch="tvbox:watch-e01";
  org.json.JSONArray videos=FusionPlaybackRules.episodeChoices(flags,groups,watch,"E01");
  check(videos.length()==2&&"routeA".equals(videos.getJSONObject(0).optString("fusion.flag"))&&"valueA".equals(videos.getJSONObject(0).optString("fusion.value")),"episode list still keeps only the first line");
  org.json.JSONObject meta=new org.json.JSONObject().put("fusion.config","cfg").put("fusion.site","site").put("videos",videos);
  org.json.JSONObject stream=new org.json.JSONObject().put("url","http://cdn.example/routeB.mp4");
  OfflineQueueRules.rememberLine(stream,meta,"cfg","site","routeB","valueB");
  org.json.JSONObject src=OfflineQueueRules.downloadSource(stream,meta,watch);
  check(src!=null&&"fusion".equals(src.optString("kind"))&&"routeB".equals(src.optString("flag"))&&"valueB".equals(src.optString("value"))&&"cfg".equals(src.optString("config"))&&"site".equals(src.optString("site")),"playing line 2 is the download source, not the deduped first line: "+src);
  org.json.JSONObject urlOnly=new org.json.JSONObject().put("url","http://cdn.example/routeB.mp4");
  org.json.JSONObject metaOnly=new org.json.JSONObject().put("fusion.config","cfg").put("fusion.site","site").put("videos",videos);
  OfflineQueueRules.rememberLine(null,metaOnly,"cfg","site","routeB","valueB");
  org.json.JSONObject fromMeta=OfflineQueueRules.downloadSource(urlOnly,metaOnly,watch);
  check(fromMeta!=null&&"routeB".equals(fromMeta.optString("flag"))&&"valueB".equals(fromMeta.optString("value")),"explicit meta line wins over deduped videos: "+fromMeta);
  org.json.JSONObject untouched=new org.json.JSONObject().put("url","http://cdn.example/routeB.mp4");
  org.json.JSONObject videosOnly=new org.json.JSONObject().put("fusion.config","cfg").put("fusion.site","site").put("videos",videos);
  org.json.JSONObject fallback=OfflineQueueRules.downloadSource(untouched,videosOnly,watch);
  check(fallback!=null&&"routeA".equals(fallback.optString("flag"))&&"valueA".equals(fallback.optString("value")),"without a playing line, the episode list is unchanged");
 }
 public static void main(String[] a)throws Exception{
  check(OfflineQueueRules.duplicate(null)==null&&OfflineQueueRules.duplicate("failed")==null&&OfflineQueueRules.duplicate("")==null,"failed can be queued again");
  check(OfflineQueueRules.DONE.equals(OfflineQueueRules.duplicate("done")),"done blocks");
  check(OfflineQueueRules.QUEUED.equals(OfflineQueueRules.duplicate("queued"))&&OfflineQueueRules.QUEUED.equals(OfflineQueueRules.duplicate("running")),"queue blocks");
  check(OfflineQueueRules.PAUSED.equals(OfflineQueueRules.duplicate("paused")),"paused blocks");

  String magnet="magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567";
  check(OfflineQueueRules.earlyRefuse(magnet).equals(OfflineRules.refuse(null)),"magnet skipped with the existing reason");
  check(OfflineQueueRules.earlyRefuse("http://cdn.example/a.m3u8").equals(OfflineRules.refuse("http://cdn.example/a.m3u8")),"m3u8 skipped");
  check(OfflineQueueRules.earlyRefuse("http://cdn.example/a.mpd").contains("DASH"),"dash skipped");
  check(OfflineQueueRules.earlyRefuse("http://cdn.example/a.mp4")==null,"plain file can be queued");
  check(OfflineQueueRules.earlyRefuse("ep-12")==null&&OfflineQueueRules.earlyRefuse("")==null&&OfflineQueueRules.earlyRefuse(null)==null,"unresolved site ids are not refused yet");

  check(OfflineQueueRules.TRANSCODE.equals(OfflineQueueRules.playerBlock("http://cdn.example/a.m3u8",false,true,true,false,false)),"transcode reason wins over m3u8");
  check(OfflineRules.refuse(null).equals(OfflineQueueRules.playerBlock("",true,true,true,true,true)),"infoHash is a magnet even if emby could refetch");
  check(OfflineQueueRules.playerBlock("http://cdn.example/a.mp4",false,true,true,true,true)==null,"direct emby file can download");
  check(OfflineQueueRules.playerBlock("http://127.0.0.1:9/v",false,true,true,true,true)==null,"loopback emby can still refetch");
  check(OfflineQueueRules.LOOPBACK.equals(OfflineQueueRules.playerBlock("http://127.0.0.1:9/v",false,false,false,true,false)),"loopback without a refetch is blocked");
  check(OfflineRules.refuse(null).equals(OfflineQueueRules.playerBlock("",false,false,false,true,false)),"empty url without a refetch uses the magnet reason");
  check(OfflineQueueRules.playerBlock("",false,true,true,true,true)==null,"empty url can refetch");
  check(OfflineQueueRules.playerBlock("http://cdn.example/a.mpd",false,false,false,true,false).contains("DASH"),"dash on the player");

  JSONObject done=new JSONObject().put("status","done").put("src",new JSONObject().put("kind","emby").put("server","s").put("item","6"));
  JSONObject queued=new JSONObject().put("status","queued").put("src",new JSONObject().put("kind","fusion").put("config","c").put("site","site").put("flag","线路").put("value","v2"));
  JSONObject failed=new JSONObject().put("status","failed").put("url","http://cdn.example/old.mp4").put("src",new JSONObject().put("kind","url").put("url","http://cdn.example/old.mp4"));
  Map<String,String> existing=OfflineQueueRules.index(new JSONArray().put(done).put(queued).put(failed).put(new JSONObject().put("status","paused").put("src",done.getJSONObject("src"))));
  check("done".equals(existing.get(OfflineQueueRules.embyKey("s","6"))),"done outranks a later pause of the same item");
  check(OfflineQueueRules.duplicate("failed")==null&&"failed".equals(existing.get(OfflineQueueRules.urlKey("http://cdn.example/old.mp4"))),"failed stays retryable");

  String m3u8=OfflineRules.refuse("http://cdn.example/3.m3u8"),dash=OfflineRules.refuse("http://cdn.example/4.mpd");
  List<OfflineQueueRules.Item> items=new ArrayList<>();
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","1"),"第一集","http://cdn.example/1.mp4",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","2"),"第二集",magnet,false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","3"),"第三集","http://cdn.example/3.m3u8",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","4"),"第四集","http://cdn.example/4.mpd",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","5"),"第五集","",true));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","1"),"第一集重复","http://cdn.example/1b.mp4",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","6"),"第六集","",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.fusionKey("c","site","线路","v2"),"影视仓已在队列","ep-2",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.embyKey("s","7"),"第七集","not-a-url",false));
  items.add(new OfflineQueueRules.Item(OfflineQueueRules.urlKey("http://cdn.example/old.mp4"),"失败可再下","http://cdn.example/old.mp4",false));
  List<OfflineQueueRules.Row> rows=OfflineQueueRules.plan(items,existing);
  ArrayList<String> accepted=new ArrayList<>();ArrayList<String> skipped=new ArrayList<>();
  for(OfflineQueueRules.Row row:rows)if(row.enqueue)accepted.add(row.name);else skipped.add(row.name+"="+row.reason);
  check(accepted.equals(Arrays.asList("第一集","第七集","失败可再下")),"accepted order "+accepted);
  check(skipped.contains("第二集="+OfflineRules.refuse(null))&&skipped.contains("第三集="+m3u8)&&skipped.contains("第四集="+dash),"unsupported sources keep OfflineRules reasons: "+skipped);
  check(skipped.contains("第五集="+OfflineQueueRules.TRANSCODE),"transcode skipped");
  check(skipped.contains("第一集重复="+OfflineQueueRules.IN_BATCH),"same source once per batch");
  check(skipped.contains("第六集="+OfflineQueueRules.DONE)&&skipped.contains("影视仓已在队列="+OfflineQueueRules.QUEUED),"already downloaded or queued are skipped");

  JSONArray prior=new JSONArray().put(new JSONObject().put("name","B").put("status","queued")).put(new JSONObject().put("name","A").put("status","queued"));
  List<JSONObject> batch=Arrays.asList(new JSONObject().put("name","ep1").put("status","queued"),new JSONObject().put("name","ep2").put("status","queued"));
  check(OfflineQueueRules.downloadOrder(OfflineQueueRules.prependFifo(prior,batch)).equals(Arrays.asList("A","B","ep1","ep2")),"existing queue stays ahead, batch keeps its order");
  check(OfflineQueueRules.downloadOrder(OfflineQueueRules.prependFifo(new JSONArray(),batch)).equals(Arrays.asList("ep1","ep2")),"empty queue downloads the batch in order");

  String text=OfflineQueueRules.summary(2,Arrays.asList(OfflineQueueRules.QUEUED,m3u8,OfflineQueueRules.QUEUED));
  check(text.startsWith("已加入 2 集，跳过 3 集")&&text.contains(OfflineQueueRules.QUEUED+" · 2 集")&&text.contains(m3u8+" · 1 集"),"summary counts "+text);
  check("已加入 1 集".equals(OfflineQueueRules.summary(1,null))&&"已加入 0 集".equals(OfflineQueueRules.summary(0,Collections.<String>emptyList())),"summary without skips");

  check(Arrays.equals(OfflineQueueRules.tail(2,5),new int[]{2,3,4}),"from current through the end");
  check(Arrays.equals(OfflineQueueRules.tail(-1,3),new int[]{0,1,2})&&Arrays.equals(OfflineQueueRules.tail(9,3),new int[]{2})&&OfflineQueueRules.tail(0,0).length==0,"tail bounds");
  check(OfflineQueueRules.resumeIndex(new String[]{"a","b","c"},new boolean[]{true,false,false},new boolean[]{false,true,false},"c")==2,"preferred id wins");
  check(OfflineQueueRules.resumeIndex(new String[]{"a","b","c"},new boolean[]{true,false,false},new boolean[]{false,true,false},null)==1,"in-progress episode");
  check(OfflineQueueRules.resumeIndex(new String[]{"a","b"},new boolean[]{true,false},new boolean[]{false,false},"missing")==1,"first unplayed when preferred is absent");
  check(OfflineQueueRules.lastTrue(new boolean[]{false,true,false,true})==3&&OfflineQueueRules.lastTrue(new boolean[]{false,false})==0,"last started episode, else the first");

  String magnetReason=OfflineRules.refuse(null);
  check(OfflineQueueRules.dialogBlock("done","http://cdn.example/a.mp4",false).equals(OfflineQueueRules.DONE),"downloaded row is locked");
  check(OfflineQueueRules.dialogBlock("queued","",false).equals(OfflineQueueRules.QUEUED)&&OfflineQueueRules.dialogBlock("running","",false).equals(OfflineQueueRules.QUEUED)&&OfflineQueueRules.dialogBlock("paused","",false).equals(OfflineQueueRules.PAUSED),"queued, running and paused rows are locked");
  check(magnetReason.equals(OfflineQueueRules.dialogBlock(null,magnet,false))&&OfflineQueueRules.dialogBlock("failed","http://cdn.example/a.mp4",false)==null,"unsupported source locks, a failed file can be selected again");
  check(OfflineQueueRules.TRANSCODE.equals(OfflineQueueRules.dialogBlock(null,"http://cdn.example/a.mp4",true))&&OfflineQueueRules.dialogBlock(null,"ep-3",false)==null,"transcode locks, unresolved id stays selectable");
  String[] locks=new String[]{null,OfflineQueueRules.QUEUED,magnetReason,null};
  check(Arrays.equals(OfflineQueueRules.allowed(locks,new int[]{0,1,2,3}),new int[]{0,3}),"select all skips locked rows");
  check(Arrays.equals(OfflineQueueRules.allowed(locks,OfflineQueueRules.tail(1,4)),new int[]{3}),"from current onward still skips locked rows");

  check(!OfflineQueueRules.requestNotifyNow(true)&&OfflineQueueRules.requestNotifyNow(false),"player download does not ask for notification permission");
  check(OfflineQueueRules.deferNotify(true,false,false)&&!OfflineQueueRules.deferNotify(true,true,false)&&!OfflineQueueRules.deferNotify(true,false,true)&&!OfflineQueueRules.deferNotify(false,false,false),"only an unanswered player download is deferred");
  check(OfflineQueueRules.requestNotifyOnResume(false,true,false,false)&&!OfflineQueueRules.requestNotifyOnResume(true,true,false,false)&&!OfflineQueueRules.requestNotifyOnResume(false,false,false,false)&&!OfflineQueueRules.requestNotifyOnResume(false,true,true,false),"resume asks once, and never on the player");

  String secret="http://user:secret@cdn.example:8443/a.mp4?token=abc";
  String redacted=OfflineQueueRules.summary(0,Arrays.asList("连接失败 "+secret,"连接失败 http://cdn.example/b.mkv?sign=zzz","磁力 "+magnet));
  check(redacted.contains("cdn.example")&&!redacted.contains("token")&&!redacted.contains("secret")&&!redacted.contains("a.mp4")&&!redacted.contains("btih")&&redacted.contains("磁力"),"summary keeps only the host: "+redacted);
  check(redacted.contains("连接失败 cdn.example · 2 集"),"same host collapses after redaction");

  String flush="连接失败https://fixture.invalid/video.mp4?token=synthetic";
  String flushRed=OfflineQueueRules.redact(flush);
  check(flushRed.contains("fixture.invalid")&&!flushRed.contains("token")&&!flushRed.contains("synthetic")&&!flushRed.contains("video.mp4")&&!flushRed.contains("https://"),"chinese text flush against a url keeps only the host: "+flushRed);
  String paren=OfflineQueueRules.redact("（https://fixture.invalid/a.mp4?token=z）");
  check(paren.equals("（fixture.invalid）"),"brackets stay and the query is masked: "+paren);
  String wrapped=OfflineQueueRules.redact("(https://user:secret@cdn.example:8443/a.mp4?token=abc)");
  check(wrapped.equals("(cdn.example)")&&!wrapped.contains("secret")&&!wrapped.contains("token"),"ascii parens and userinfo: "+wrapped);
  String newline=OfflineQueueRules.redact("失败\nhttps://fixture.invalid/a.mp4?x=1");
  check(newline.contains("fixture.invalid")&&!newline.contains("x=1")&&newline.contains("\n"),"url after a newline: "+newline.replace("\n","\\n"));
  String magnetFlush=OfflineQueueRules.redact("见magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=name");
  check(magnetFlush.equals("见磁力")&&!magnetFlush.contains("btih"),"magnet flush against chinese: "+magnetFlush);
  check(OfflineQueueRules.redact("nothttps://fixture.invalid/a.mp4?token=keep").contains("token=keep"),"a letter glued to the scheme is not a url");

  playingLine();

  check(OfflineQueueRules.accept(false,null)==1&&OfflineQueueRules.accept(false,OfflineQueueRules.QUEUED)==0&&OfflineQueueRules.accept(true,null)<0&&OfflineQueueRules.accept(true,"解析失败")<0,"cancel after resolve refuses the episode");
  java.util.ArrayList<String> kept=new java.util.ArrayList<>();
  String[] names=new String[]{"第一集","第二集","第三集"};
  boolean[] cancelOnReturn=new boolean[]{false,true,true};
  for(int i=0;i<names.length;i++){int decision=OfflineQueueRules.accept(cancelOnReturn[i],null);if(decision<0)break;if(decision>0)kept.add(names[i]);}
  check(kept.equals(java.util.Arrays.asList("第一集")),"an episode that finishes resolving after cancel is not queued, and later episodes are not started: "+kept);

  String m3u8Reason=OfflineRules.refuse("http://cdn.example/3.m3u8");
  String[] seasonBlock=new String[]{null,OfflineQueueRules.DONE,m3u8Reason};
  OfflineQueueRules.Scope season=OfflineQueueRules.scope(seasonBlock,new int[]{0,1,2});
  check(java.util.Arrays.equals(season.enqueue,new int[]{0})&&season.skips.equals(java.util.Arrays.asList(OfflineQueueRules.DONE,m3u8Reason)),"season scope keeps the downloadable index and the skip reasons: "+season.skips);
  String seasonText=OfflineQueueRules.summary(1,season.skips);
  check(seasonText.startsWith("已加入 1 集，跳过 2 集")&&seasonText.contains(OfflineQueueRules.DONE+" · 1 集")&&seasonText.contains(m3u8Reason+" · 1 集"),"season summary counts prefiltered skips: "+seasonText);
  OfflineQueueRules.Scope tailScope=OfflineQueueRules.scope(seasonBlock,OfflineQueueRules.tail(1,3));
  check(java.util.Arrays.equals(tailScope.enqueue,new int[0])&&tailScope.skips.equals(java.util.Arrays.asList(OfflineQueueRules.DONE,m3u8Reason)),"from current onward still reports locked skips");
  check(OfflineQueueRules.summary(0,tailScope.skips).startsWith("已加入 0 集，跳过 2 集"),"an all-skipped range explains why nothing was added");
  OfflineQueueRules.Scope picked=OfflineQueueRules.scope(seasonBlock,new int[]{0});
  check(java.util.Arrays.equals(picked.enqueue,new int[]{0})&&picked.skips.isEmpty(),"a manual selection does not invent skips outside that selection");

  System.out.println("PASS: download dedupe, unsupported sources, batch order, summary, locked rows, deferred notification permission, host-only errors, playing line, cancel and skip counts.");
 }
}
