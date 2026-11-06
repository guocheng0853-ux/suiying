package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class EpisodeSourceRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static JSONObject s(String title,String hash)throws Exception{JSONObject o=new JSONObject();o.put("name","Torrentio\n4k");o.put("title",title);if(hash!=null)o.put("infoHash",hash);return o;}
 public static void main(String[] x)throws Exception{
  JSONArray all=new JSONArray();
  all.put(s("Show.S01E03.720p.WEB 👤 5 1.2 GB","a1"));
  all.put(s("Show.S01E03.2160p.WEB 👤 300 8 GB","b2"));
  all.put(new JSONObject().put("title","nothing playable"));
  all.put(new JSONObject().put("externalUrl","https://example.com/watch").put("title","web"));
  all.put(s("Show.S01E03.1080p.WEB 👤 80 2 GB","c3"));
  int[] o=EpisodeSourceRules.order(all,"auto",40);
  check(o.length==4,"unplayable entries are dropped: "+Arrays.toString(o));
  check(o[0]==1,"4K with many seeds first: "+Arrays.toString(o));
  check(o[o.length-1]==3,"external web links go last");
  check(EpisodeSourceRules.order(all,"auto",2).length==2,"limit is applied");
  check(EpisodeSourceRules.order(null,"auto",5).length==0,"null input");
  check(EpisodeSourceRules.key(s("a","ABC")).equals(EpisodeSourceRules.key(s("b","abc"))),"same torrent from two addons is one source");
  JSONObject f=s("a","abc");f.put("fileIdx",2);check(!EpisodeSourceRules.key(f).equals(EpisodeSourceRules.key(s("a","abc"))),"different file in the same pack is a different source");
  check(EpisodeSourceRules.windowX(2400,700,20)==740,"source window sits left of the panel");
  check(EpisodeSourceRules.windowX(1200,700,20)==20,"too narrow: overlay the panel");
  check(EpisodeSourceRules.detail(Arrays.asList("4K","","HDR10","8.0 GB"),"Torrentio\n4k").equals("4K · HDR10 · 8.0 GB · Torrentio 4k"),"detail line");
  int[] d=GlassRules.dropDown(2400,1080,600,2350,120,20,10);check(d[0]==1750&&d[1]==130&&d[2]==930,"More drops down right-aligned under the button: "+Arrays.toString(d));
  d=GlassRules.dropDown(2400,1080,600,300,120,20,10);check(d[0]==20,"clamped inside the left edge");
  d=GlassRules.dropDown(2400,1080,600,2350,900,20,10);check(d[1]==20&&d[2]==1040,"no room below: use the full height");
  System.out.println("PASS: episode sources ranked, deduplicated, placed left of the panel; More menu drops down under its button.");
 }
}
