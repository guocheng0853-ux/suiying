package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** Retain the original player response's subtitles through direct and parsed playback. */
final class StreamSubtitles {
 static void attach(JSONObject stream,JSONObject response,String base)throws JSONException{
  JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();
  append(out,seen,stream.opt("subtitles"),base);
  append(out,seen,response.opt("subt"),base);
  append(out,seen,response.opt("subtitles"),base);
  stream.put("subtitles",out);
 }
 static void append(JSONArray out,Set<String> seen,Object raw,String base){
  if(raw instanceof String)try{raw=new JSONArray((String)raw);}catch(Exception ignored){return;}
  if(!(raw instanceof JSONArray))return;JSONArray list=(JSONArray)raw;
  for(int i=0;i<list.length()&&out.length()<100;i++)try{
   JSONObject sub=list.optJSONObject(i);if(sub==null||sub.optString("url").isEmpty())continue;
   String url=TvPosterRules.resolve(base,sub.getString("url"));
   if(seen.add(url))out.put(new JSONObject(sub.toString()).put("url",url));
  }catch(Exception ignored){}
 }
}
