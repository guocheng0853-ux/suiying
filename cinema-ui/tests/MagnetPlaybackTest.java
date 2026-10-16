package com.guocheng.mediapocket;
import org.json.*;import java.net.URLEncoder;import java.util.Base64;
/** HTTP-only address guard stays on config/jar URLs; magnet episodes become the existing infoHash/P2P stream. Synopsis entities decode. */
public class MagnetPlaybackTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  String hash="0123456789abcdef0123456789abcdef01234567";
  String magnet="magnet:?xt=urn:btih:"+hash.toUpperCase()+"&dn=MobLand&tr="+URLEncoder.encode("udp://tracker.example:1337/announce","UTF-8")+"&tr="+URLEncoder.encode("udp://tracker.example:1337/announce","UTF-8");
  boolean rejected=false;try{TvBoxRules.resolve("https://config.example/tv.json",magnet);}catch(IllegalArgumentException e){rejected="仅支持 HTTP / HTTPS 地址".equals(e.getMessage());}
  check(rejected,"config/jar resolver still rejects magnet");
  check(TvBoxRules.resolve("https://config.example/dir/a.json","ep.m3u8").equals("https://config.example/dir/ep.m3u8"),"relative HTTP still resolved");
  boolean fileRejected=false;try{TvBoxRules.resolve("https://config.example/tv.json","file:///tmp/a.mp4");}catch(IllegalArgumentException e){fileRejected="仅支持 HTTP / HTTPS 地址".equals(e.getMessage());}
  check(fileRejected,"non-magnet non-HTTP still rejected by the guard");
  FusionCatalog.Session session=new FusionCatalog.Session("https://config.example/tv.json",new JSONObject(),new JSONObject().put("name","磁力源"));
  TvBoxPlay.Resolved resolved=TvBoxPlay.resolve(null,session,"此磁力源是边下载边播",new JSONObject().put("parse",0).put("url",magnet),4000);
  check(resolved.stream.getString("infoHash").equals(hash),"v1 hash handed to TorrentEngine");
  check(resolved.stream.optString("url").isEmpty(),"magnet is not passed to the HTTP player");
  check(!resolved.check.definitive&&"磁力 / P2P".equals(resolved.via),"probe does not block P2P");
  check(resolved.stream.getJSONArray("sources").length()==1&&resolved.stream.getJSONArray("sources").getString(0).equals("tracker:udp://tracker.example:1337/announce"),"original tracker kept once");
  check(!resolved.stream.has("fileIdx"),"largest video file stays the engine default");
  String zeros="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",ones="77777777777777777777777777777777",one="AEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  check(MagnetLinks.parse("magnet:?xt=urn:btih:"+zeros).infoHash.equals(new String(new char[40]).replace('\0','0')),"base32 zeros");
  check(MagnetLinks.parse("magnet:?xt=urn:btih:"+ones).infoHash.equals(new String(new char[40]).replace('\0','f')),"base32 ones");
  check(MagnetLinks.parse("magnet:?xt=urn:btih:"+one).infoHash.equals("01"+new String(new char[38]).replace('\0','0')),"base32 bit order");
  String thunder="thunder://"+Base64.getEncoder().encodeToString(("AA"+magnet+"ZZ").getBytes("UTF-8"));
  check(MagnetLinks.parse(thunder).infoHash.equals(hash),"thunder wrapper unwraps to the same magnet");
  JSONObject direct=TvBoxPlay.orMagnet(new JSONObject(),"  "+magnet+"  ");
  check(TvBoxPlay.resolve(null,session,"源",direct,1000).stream.getString("infoHash").equals(hash),"episode value itself is enough when player returns no url");
  boolean v2=false;try{TvBoxPlay.resolve(null,session,"源",new JSONObject().put("url","magnet:?xt=urn:btmh:1220abcd"),1000);}catch(Exception e){v2=e.getMessage().contains("BT v1")&&!e.getMessage().contains("仅支持 HTTP");}
  check(v2,"v2-only magnet explains the engine limit instead of the HTTP guard");
  check(MagnetLinks.parse("https://cdn.example/a.mp4")==null&&!MagnetLinks.isMagnet("https://cdn.example/a.mp4"),"HTTP play URLs stay on the HTTP path");
  String dash="\u2014\u2014";
  check(FusionCatalog.plain("伦敦两大犯罪家族mdash;mdash;哈里根家族").equals("伦敦两大犯罪家族"+dash+"哈里根家族"),"broken mdash without &");
  check(FusionCatalog.plain("伦敦两大犯罪家族&mdash;&mdash;哈里根家族").equals("伦敦两大犯罪家族"+dash+"哈里根家族"),"&mdash;");
  check(FusionCatalog.plain("&amp;mdash;&amp;mdash;").equals(dash),"double-encoded mdash");
  check(FusionCatalog.plain("A&amp;B").equals("A&B"),"&amp;");
  check(FusionCatalog.plain("a&nbsp;b").equals("a b"),"&nbsp;");
  check(FusionCatalog.plain("nbsp;").equals(""),"broken nbsp trims");
  check(FusionCatalog.plain("&#8212;&#x2014;").equals(dash),"decimal and hex numeric entities");
  check(FusionCatalog.plain("#8212;").equals("\u2014"),"broken numeric entity");
  check(FusionCatalog.plain("<p>hello<br/>world</p>").equals("hello\nworld"),"tags become text");
  check(FusionCatalog.plain("xmdash;").equals("xmdash;"),"entity name inside a word is left alone");
  check(FusionCatalog.plain(null).isEmpty(),"null synopsis");
  System.out.println("PASS: HTTP guard still rejects magnet/file for config URLs; magnet episodes become infoHash P2P streams; synopsis entities decode.");
 }
}
