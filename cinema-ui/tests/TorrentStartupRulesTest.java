package com.guocheng.mediapocket;
import org.json.*;
public final class TorrentStartupRulesTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static JSONObject torrent(String name,int seeds)throws Exception{return new JSONObject().put("infoHash",String.format("%040d",seeds)).put("title",name+" 1080p 👤 "+seeds);}
 public static void main(String[]args)throws Exception{
  long need=TorrentStartupRules.requiredRate(2_160_000_000L,120);
  check(need==300000&&TorrentStartupRules.requiredRate(100,0)==0,"required bandwidth uses the actual selected file and runtime");
  check(TorrentStartupRules.switchSource(30000,30,15000,50000,need,1_500_000),"observed slow start may use its backup after 30 seconds");
  check(!TorrentStartupRules.switchSource(29999,30,15000,50000,need,1_500_000),"keep initial connection budget");
  check(!TorrentStartupRules.switchSource(30000,30,14999,50000,need,1_500_000),"a brief speed dip does not cause switching");
  check(!TorrentStartupRules.switchSource(40000,90,20000,50000,need,1_500_000),"respect an explicitly longer user timeout");
  check(!TorrentStartupRules.switchSource(30000,30,15000,250000,need,1_500_000),"sufficient speed stays");
  check(!TorrentStartupRules.switchSource(30000,30,15000,50000,0,1_500_000),"unknown runtime is not guessed");
  check(!TorrentStartupRules.switchSource(30000,30,15000,50000,need,7_000_000),"already buffered startup data stays");
  check(!TorrentStartupRules.switchSource(30000,30,15000,-1,need,0),"missing counters stay");
  check(!TorrentStartupRules.switchSource(30000,30,15000,10000,20000,0),"very low bitrate media is not judged by a fixed speed threshold");
  JSONObject slow=torrent("slow",4),fast=torrent("fast",100),direct=new JSONObject().put("url","https://fixture.invalid/video").put("title","1080p AAC"),risky=new JSONObject().put("url","https://fixture.invalid/p5").put("title","1080p DV Profile 5");
  JSONArray original=new JSONArray().put(slow).put(risky).put(fast).put(direct),ordered=TorrentStartupRules.backups(original,"auto");
  check(ordered.getJSONObject(0)==direct&&ordered.getJSONObject(1)==fast&&ordered.getJSONObject(2)==slow&&ordered.getJSONObject(3)==risky,"safe direct backups first, then healthy torrents, risky Dolby last");
  check(original.getJSONObject(0)==slow,"do not reorder the caller's list in place");
  check(TorrentStartupRules.backups(null,"auto").length()==0,"empty backup list");
  System.out.println("PASS: measured slow-start policy, timeout/buffer guards, safe backup ordering.");
 }
}
