package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class DanDanPlayRulesTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  check(DanDanPlayRules.signature("testapp",1735660800L,"/api/v2/comment/123450001","testsecret").equals("mPSSHgHVGnwrxg63KjpjjdG/NkXaXtqVt9tGlObj4NA="),"public sha256 signature");
  check(DanDanPlayRules.signature("my-app",1700000000L,"/api/v2/match?x=1","my-secret").equals(DanDanPlayRules.signature("my-app",1700000000L,"/api/v2/match","my-secret")),"query is not part of the path");
  check(DanDanPlayRules.pathOnly("https://ignored/Api/V2/Match").endsWith("/api/v2/match")||DanDanPlayRules.pathOnly("/Api/V2/Match").equals("/api/v2/match"),"path is lowercased");
  check(DanDanPlayRules.pathOnly("/Api/V2/Match").equals("/api/v2/match"),"lower path");
  check(!DanDanPlayRules.ready("","secret")&&!DanDanPlayRules.ready("id","")&&DanDanPlayRules.ready("mine","secret"),"empty credentials stay empty");
  check(DanDanPlayRules.reason(DanDanPlayRules.BLOCKED_APP,"secret")!=null&&!DanDanPlayRules.ready(DanDanPlayRules.BLOCKED_APP,"secret"),"blocked third-party app id");
  check(DanDanPlayRules.reason("mine","").contains("AppId"),"missing key explains where to fill it");
  DanDanPlayRules.Cue cue=DanDanPlayRules.comment("12.5,1,16777215,0","你好",1000);check(cue!=null&&cue.timeMs==13500&&cue.mode==1&&cue.text.equals("你好"),"scroll comment");
  check(DanDanPlayRules.comment("1,5,255,0","顶",0).mode==5&&DanDanPlayRules.comment("1,4,255,0","底",0).mode==4,"top and bottom modes");
  check(DanDanPlayRules.comment("1,1,0,0","",0)==null&&DanDanPlayRules.comment("bad","字",0)==null,"blank or broken comments drop");
  JSONObject search=new JSONObject().put("animes",new JSONArray().put(new JSONObject().put("animeId",9).put("animeTitle","海贼王").put("episodes",new JSONArray().put(new JSONObject().put("episodeId",3).put("episodeTitle","第2话")))));
  List<DanDanPlayRules.Hit> hits=DanDanPlayRules.searchHits(search);check(hits.size()==1&&hits.get(0).episodeId==3&&DanDanPlayRules.autoPick(hits,2).episodeId==3,"search then auto pick");
  List<DanDanPlayRules.Hit> many=new ArrayList<>();many.add(new DanDanPlayRules.Hit(1,9,"海贼王","第1话",0));many.add(new DanDanPlayRules.Hit(2,9,"海贼王","第2话",0));check(DanDanPlayRules.autoPick(many,2).episodeId==2&&DanDanPlayRules.autoPick(many,0)==null,"ambiguous list waits for a manual pick");
  check(DanDanPlayRules.sameEpisode("第12话",12)&&!DanDanPlayRules.sameEpisode("第12话",2)&&DanDanPlayRules.sameEpisode("第01话",1),"episode number boundaries");
  JSONObject detail=new JSONObject().put("bangumi",new JSONObject().put("animeId",9).put("animeTitle","海贼王").put("episodes",new JSONArray().put(new JSONObject().put("episodeId",8).put("episodeTitle","第3话"))));
  check(DanDanPlayRules.animeHits(detail).get(0).episodeId==8&&DanDanPlayRules.animeQuery(9).equals("/api/v2/bangumi/9"),"bangumi details");
  JSONObject match=new JSONObject().put("isMatched",true).put("matches",new JSONArray().put(new JSONObject().put("episodeId",5).put("animeTitle","片名").put("episodeTitle","文件").put("shift",2)));
  check(DanDanPlayRules.matchHits(match).get(0).shift==2&&DanDanPlayRules.matchBody("a.mp4",90).contains("fileNameOnly"),"filename match fallback");
  JSONObject comments=new JSONObject().put("comments",new JSONArray().put(new JSONObject().put("p","1,1,16777215,0").put("m","甲")).put(new JSONObject().put("p","3,1,16777215,0").put("m","乙")));
  check(DanDanPlayRules.comments(comments,1).get(0).timeMs==2000,"shift applies to comment time");
  check(DanDanPlayRules.fileName("https://cdn.example/a/file%20name.mkv?token=1","片名").equals("file name.mkv"),"match uses the file name");
  check(DanDanPlayRules.searchQuery("海贼王",2).startsWith("/api/v2/search/episodes?"),"search path");
  check(!DanDanPlayRules.signature("testapp",1,"/api/v2/match","secret").isEmpty(),"signature is not empty");
  System.out.println("PASS: DanDanPlay signature, empty credentials, search, bangumi, filename match.");
 }
}
