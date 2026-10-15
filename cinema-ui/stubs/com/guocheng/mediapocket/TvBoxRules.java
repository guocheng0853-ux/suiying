package com.guocheng.mediapocket;
import java.net.URI;import java.util.*;import java.util.regex.*;
final class TvBoxRules {
 static String resolve(String base,String value){String v=value.trim().split(";md5;",2)[0];String out=URI.create(base).resolve(v).toString();String scheme=URI.create(out).getScheme();if(!"https".equalsIgnoreCase(scheme)&&!"http".equalsIgnoreCase(scheme))throw new IllegalArgumentException("仅支持 HTTP / HTTPS 地址");return out;}
 static String path(String parent,String name){return (parent.endsWith("/")?parent:parent+"/")+name;}
 static boolean media(String name){return name.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|mkv|webm|mov|avi|ts|m4v|mp3|flac|m4a|aac|wav|m3u8)$");}
 static String attr(String line,String key){Matcher m=Pattern.compile(Pattern.quote(key)+"=\"([^\"]*)\"").matcher(line);return m.find()?m.group(1):"";}
 static int separator(String line){boolean quoted=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='"')quoted=!quoted;if(c==','&&!quoted)return i;}return -1;}
 static final class Channel{String name,group,url,ua,referer;Channel(String n,String g,String u,String a,String r){name=n;group=g;url=u;ua=a;referer=r;}}
 static List<Channel> playlist(String text,String base,String defaultUa){List<Channel> out=new ArrayList<>();String name="",group="未分组",ua=defaultUa,ref="";for(String raw:text.replace("\uFEFF","").split("\\r?\\n")){String s=raw.trim();if(s.startsWith("#EXTINF:")){int comma=separator(s);name=comma>=0?s.substring(comma+1).trim():"频道";String g=attr(s,"group-title");group=g.isEmpty()?"未分组":g;ua=defaultUa;ref="";}else if(s.startsWith("#EXTGRP:"))group=s.substring(8).trim();else if(s.startsWith("#EXTVLCOPT:http-user-agent="))ua=s.substring(27);else if(s.startsWith("#EXTVLCOPT:http-referrer="))ref=s.substring(25);else if(!s.isEmpty()&&!s.startsWith("#")){if(s.contains(",#genre#")){group=s.substring(0,s.indexOf(','));continue;}String n=name,u=s;if(n.isEmpty()&&s.contains(",")){int at=s.indexOf(',');n=s.substring(0,at);u=s.substring(at+1);}try{out.add(new Channel(n.isEmpty()?"频道 "+(out.size()+1):n,group,resolve(base,u),ua,ref));}catch(Exception ignored){}name="";if(out.size()>=5000)break;}}return out;}
}
