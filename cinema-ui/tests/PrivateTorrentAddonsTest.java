package com.guocheng.mediapocket;
import org.json.*;import java.net.*;import java.io.*;import java.util.*;
public class PrivateTorrentAddonsTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 static String hash(char c){char[] chars=new char[40];Arrays.fill(chars,c);return new String(chars);}
 static JSONObject ez(char hash,int season,int episode,int seeds)throws Exception{return new JSONObject().put("hash",hash(hash)).put("imdb_id",944947).put("season",season).put("episode",episode).put("title","Fixture.S01E02.1080p").put("seeds",seeds);}
 static JSONObject tpb(char hash,String name,int seeds)throws Exception{return new JSONObject().put("info_hash",hash(hash)).put("imdb","tt0944947").put("name",name).put("seeders",seeds);}
 public static void main(String[] args)throws Exception{
  TorrentProviders.Identity id=new TorrentProviders.Identity("series","tt0944947:1:2");
  JSONArray input=new JSONArray().put(ez('a',1,2,10)).put(ez('b',1,3,99)).put(ez('c',2,2,99)).put(ez('a',1,2,20)).put(ez('d',1,2,30));
  input.put(ez('e',1,2,900).put("imdb_id",12345));input.put(ez('z',1,2,999));
  JSONArray found=TorrentProviders.normalize("eztv",input,id);check(found.length()==2,"hash/film/season/episode matching and dedup");check(found.getJSONObject(0).getString("infoHash").equals(hash('d')),"highest seed count first");
  JSONArray tp=new JSONArray().put(tpb('a',"Fixture.S01E02.1080p",10)).put(tpb('b',"Fixture.S01.Complete",99)).put(tpb('c',"Fixture.S01E03",88)).put(tpb('d',"Fixture.S01E02-E03",77)).put(tpb('e',"Fixture.1x02.720p",9));
  check(TorrentProviders.normalize("piratebay",tp,id).length()==2,"packs/multi-episode and other episodes excluded");
  JSONObject manifest=TorrentProviders.manifest("eztv");check(manifest.getJSONArray("types").length()==1&&manifest.getJSONArray("catalogs").length()==0,"EZTV only series streams");
  TorrentProviders.Fetch original=TorrentProviders.fetch;PrivateTorrentAddons.Server server=new PrivateTorrentAddons.Server();
  try{
   final int[] pages={0};TorrentProviders.fetch=(url,budget)->{pages[0]++;check(budget>0&&budget<=18000,"budget propagated");check(url.contains("imdb_id=0944947"),"IMDb query");JSONArray page=new JSONArray();if(url.endsWith("page=1")){for(int i=0;i<100;i++)page.put(ez('b',1,3,1));}else page.put(ez('a',1,2,10));return new JSONObject().put("torrents",page).put("torrents_count",101).toString();};
   JSONArray streams=StremioStreams.fetch(server.prefix()+"eztv/stream/series/tt0944947%3A1%3A2.json",3000);check(streams.length()==1&&pages[0]==2,"real loopback Stremio endpoint paginates and returns infoHash");check(StremioStreams.playable(streams.getJSONObject(0)),"existing Stremio player recognizes torrent result");
   TorrentProviders.fetch=(url,budget)->tp.toString();check(StremioStreams.fetch(server.prefix()+"piratebay/stream/series/tt0944947:1:2.json",3000).length()==2,"PirateBay endpoint routes and filters");
   TorrentProviders.fetch=(url,budget)->{throw new IOException("站点返回 HTTP 403");};boolean failed=false;try{StremioStreams.fetch(server.prefix()+"eztv/stream/series/tt0944947:1:2.json",3000);}catch(IOException e){failed=e.getMessage().contains("403");}check(failed,"upstream failure displayed, not disguised as empty/success");
   HttpURLConnection c=(HttpURLConnection)new URL(server.prefix()+"eztv/manifest.json").openConnection();JSONObject m;try(InputStream in=c.getInputStream()){m=new JSONObject(new String(in.readAllBytes(),"UTF-8"));}check(m.getString("id").equals("com.suiying.private.eztv"),"standard manifest endpoint");
   System.out.println("PASS: private Stremio manifests/endpoints, EZTV pagination, IMDb and exact episode filters, pack rejection, dedup/seed ordering, torrent player handoff, visible upstream errors.");
  }finally{TorrentProviders.fetch=original;server.socket.close();server.io.shutdownNow();}
 }
}
