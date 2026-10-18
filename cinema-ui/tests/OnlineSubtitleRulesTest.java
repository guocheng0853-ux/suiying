package com.guocheng.mediapocket;
import org.json.*;import java.io.*;import java.util.zip.*;
public class OnlineSubtitleRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(OnlineSubtitleRules.missing("","")==null?false:OnlineSubtitleRules.missing("","").contains("密钥"),"empty keys explain themselves");
  check(OnlineSubtitleRules.missing("token","")==null&&OnlineSubtitleRules.openReady("key")&&!OnlineSubtitleRules.assrtReady("  "),"one key is enough");
  check(OnlineSubtitleRules.assrtSearch("tok","海贼 王").contains("token=tok")&&OnlineSubtitleRules.assrtSearch("tok","海贼 王").contains("q="),"assrt query");
  JSONObject assrt=new JSONObject().put("status",0).put("sub",new JSONObject().put("subs",new JSONArray().put(new JSONObject().put("id",7).put("native_name","简体").put("lang",new JSONObject().put("desc","中文")))));
  check(OnlineSubtitleRules.assrt(assrt).get(0).id==7&&OnlineSubtitleRules.assrt(assrt).get(0).label().startsWith("射手"),"assrt hit");
  JSONObject detail=new JSONObject().put("status",0).put("sub",new JSONObject().put("subs",new JSONArray().put(new JSONObject().put("filelist",new JSONArray().put(new JSONObject().put("url","https://cdn.example/a.srt").put("f","a.srt"))))));
  check(OnlineSubtitleRules.assrtFile(detail).equals("https://cdn.example/a.srt"),"assrt file");
  JSONObject open=new JSONObject().put("data",new JSONArray().put(new JSONObject().put("id",3).put("attributes",new JSONObject().put("language","zh").put("release","组").put("files",new JSONArray().put(new JSONObject().put("file_id",44).put("file_name","a.srt"))))));
  OnlineSubtitleRules.Hit hit=OnlineSubtitleRules.opensubtitles(open).get(0);check(hit.fileId==44&&hit.label().startsWith("OpenSubtitles"),"opensubtitles hit");
  check(OnlineSubtitleRules.openLink(new JSONObject().put("link","https://dl.example/a.srt")).startsWith("https://"),"download link");
  check(OnlineSubtitleRules.folder("a/b:片").equals("a_b_片")&&OnlineSubtitleRules.file("..\\x.ass").equals("x.ass"),"folder and file names");
  ByteArrayOutputStream raw=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(raw)){zip.putNextEntry(new ZipEntry("note.txt"));zip.write("no".getBytes("UTF-8"));zip.closeEntry();zip.putNextEntry(new ZipEntry("sub/a.srt"));zip.write("1\n00:00:01,000 --> 00:00:02,000\n你好\n".getBytes("UTF-8"));zip.closeEntry();}
  String text=new String(OnlineSubtitleRules.unwrap(raw.toByteArray()),"UTF-8");check(text.contains("你好"),"zip unwrap");
  check(OnlineSubtitleRules.choice("assrt",7,0).equals("assrt:7"),"remembered choice");
  check(OnlineSubtitleRules.file("某番剧 中文字幕","[Script Info]\nTitle: x\n".getBytes("UTF-8")).endsWith(".ass"),"ass content keeps ass extension");
  check(OnlineSubtitleRules.file("某番剧 中文字幕","WEBVTT\n\n00:01.000 --> 00:02.000\n你\n".getBytes("UTF-8")).endsWith(".vtt"),"vtt content keeps vtt extension");
  check(OnlineSubtitleRules.file("只有名字",null).endsWith(".srt"),"missing content falls back to srt");
  check(OnlineSubtitleRules.file("包.ass","[Script Info]".getBytes("UTF-8")).endsWith(".ass"),"explicit extension is kept");
  check(OnlineSubtitleRules.extOf(new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF,'W','E','B','V','T','T'}).equals(".vtt"),"utf8 bom vtt sniffed");
  boolean osThrew=false;try{OnlineSubtitleRules.opensubtitles(new JSONObject().put("message","You cannot consume this service"));}catch(Exception e){osThrew=e.getMessage().contains("邮箱没验证");}
  check(osThrew,"OpenSubtitles error json surfaces a readable reason");
  check(OnlineSubtitleRules.OPEN_DOWNLOAD.endsWith("/downloads"),"download endpoint is the plural downloads");
  boolean assrtThrew=false;try{OnlineSubtitleRules.assrt(new JSONObject().put("status",1).put("msg","token 无效"));}catch(Exception e){assrtThrew=e.getMessage().contains("token 无效");}
  check(assrtThrew,"assrt error msg surfaces");
  org.json.JSONObject xl=new org.json.JSONObject().put("code",0).put("result","ok").put("data",new org.json.JSONArray().put(new org.json.JSONObject().put("url","https://subtitle.v.example/a.srt").put("simple_name","中文[SRT]蜘蛛侠(2007)")).put(new org.json.JSONObject().put("name","x.srt").put("languages",new org.json.JSONArray().put("简体"))).put(new org.json.JSONObject().put("name","bad").put("url","ftp://nope")));
  java.util.List<OnlineSubtitleRules.Hit> xlHits=OnlineSubtitleRules.xunlei(xl);
  check(xlHits.size()==1&&xlHits.get(0).label().startsWith("迅雷")&&xlHits.get(0).name.contains("蜘蛛侠")&&xlHits.get(0).url.startsWith("https://"),"xunlei keyless hits parse with simple names");
  check(OnlineSubtitleRules.xunleiSearch("海贼王").contains("name="),"xunlei query built");
  System.out.println("PASS: Assrt and OpenSubtitles parsing, empty keys, subtitle folder, extension sniffing.");
 }
}
