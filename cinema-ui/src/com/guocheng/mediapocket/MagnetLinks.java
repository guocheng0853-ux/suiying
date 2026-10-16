package com.guocheng.mediapocket;
import org.json.*;import java.net.URLDecoder;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.regex.*;
/**
 * 影视仓选集里的磁力地址。原播放器只在 stream 带 40 位 infoHash 时走 TorrentEngine，
 * 不接受把 magnet: 交给 HTTP 解析。这里抽出 v1 哈希和 tracker，交给那条已有的 P2P 路径。
 */
final class MagnetLinks {
 static final Pattern HASH=Pattern.compile("(?i)(?:^|[?&])xt(?:\\.\\d+)?=urn:btih:([0-9a-z]{32,40})");
 static final Pattern TRACKER=Pattern.compile("(?i)(?:^|[?&])tr=([^&#]*)");
 static final String BASE32="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
 static final class Info {final String infoHash,name;final JSONArray trackers;Info(String h,JSONArray t){this(h,t,"");}Info(String h,JSONArray t,String n){infoHash=h;trackers=t;name=n==null?"":n;}}
 static final Pattern NAME=Pattern.compile("(?i)(?:^|[?&])dn=([^&#]*)");
 private MagnetLinks(){}
 static String normalize(String raw){
  if(raw==null)return "";
  String v=raw.trim();int cut=v.indexOf(";md5;");if(cut>=0)v=v.substring(0,cut).trim();
  if(v.regionMatches(true,0,"thunder://",0,10)){String inner=thunder(v);if(inner!=null)v=inner.trim();}
  if(!v.toLowerCase(Locale.ROOT).contains("urn:btih:")&&v.toLowerCase(Locale.ROOT).contains("urn%3abtih")){try{v=URLDecoder.decode(v,"UTF-8");}catch(Exception ignored){}}
  return v;
 }
 static boolean isMagnet(String raw){return normalize(raw).regionMatches(true,0,"magnet:",0,7);}
 /** @return null when this is not a magnet, or it has no BT v1 hash the existing engine can use. */
 static Info parse(String raw){
  String v=normalize(raw);if(!v.regionMatches(true,0,"magnet:",0,7))return null;
  String hash=hash(v);if(hash==null)return null;
  JSONArray trackers=new JSONArray();LinkedHashSet<String> seen=new LinkedHashSet<>();
  Matcher m=TRACKER.matcher(v);
  while(m.find()){String t=m.group(1);try{t=URLDecoder.decode(t,"UTF-8");}catch(Exception ignored){}t=t.trim();if(t.isEmpty()||!seen.add(t))continue;trackers.put("tracker:"+t);}
  String name="";Matcher d=NAME.matcher(v);if(d.find()){name=d.group(1);try{name=URLDecoder.decode(name,"UTF-8");}catch(Exception ignored){}}
  return new Info(hash,trackers,name.trim());
 }
 static String hash(String magnet){
  Matcher m=HASH.matcher(magnet);String base32=null;
  while(m.find()){String h=m.group(1);if(h.matches("(?i)[0-9a-f]{40}"))return h.toLowerCase(Locale.ROOT);if(base32==null&&h.matches("(?i)[a-z2-7]{32}"))base32=base32Hex(h);}
  return base32;
 }
 static String base32Hex(String text){
  String s=text.toUpperCase(Locale.ROOT);if(s.length()!=32)return null;
  byte[] out=new byte[20];int buffer=0,bits=0,n=0;
  for(int i=0;i<s.length();i++){int v=BASE32.indexOf(s.charAt(i));if(v<0)return null;buffer=(buffer<<5)|v;bits+=5;if(bits>=8){bits-=8;if(n>=out.length)return null;out[n++]=(byte)((buffer>>bits)&0xff);}}
  if(n!=20)return null;StringBuilder hex=new StringBuilder(40);for(byte b:out)hex.append(String.format(Locale.ROOT,"%02x",b&0xff));return hex.toString();
 }
 static String thunder(String url){
  try{byte[] raw=Base64.getMimeDecoder().decode(url.substring(10).trim());String inner=new String(raw,StandardCharsets.UTF_8);if(inner.startsWith("AA")&&inner.endsWith("ZZ")&&inner.length()>=4)return inner.substring(2,inner.length()-2);}catch(Exception ignored){}
  return null;
 }
}
