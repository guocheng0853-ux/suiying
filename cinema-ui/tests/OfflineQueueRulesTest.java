package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class OfflineQueueRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
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
  System.out.println("PASS: download dedupe, unsupported sources, batch order, summary and current-episode range.");
 }
}
