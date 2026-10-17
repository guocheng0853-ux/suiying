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
  System.out.println("PASS: Assrt and OpenSubtitles parsing, empty keys, subtitle folder.");
 }
}
