package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** Episode IDs remain owned by one detail; film IDs are never sent as episode stream IDs. */
final class EpisodeActions {
 static boolean belongs(JSONObject meta,String parent,String episode){if(parent==null||episode==null||parent.isEmpty()||episode.isEmpty()||parent.equals(episode))return false;JSONArray videos=meta==null?null:meta.optJSONArray("videos");for(int i=0;videos!=null&&i<videos.length();i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&episode.equals(v.optString("id")))return true;}return episode.matches(java.util.regex.Pattern.quote(parent)+":[0-9]+:[0-9]+");}
 static ArrayList<JSONObject> list(JSONObject meta){ArrayList<JSONObject> result=new ArrayList<>();HashSet<String> seen=new HashSet<>();JSONArray videos=meta.optJSONArray("videos");for(int i=0;videos!=null&&i<Math.min(videos.length(),3000);i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&!v.optString("id").isEmpty()&&seen.add(v.optString("id")))result.add(v);}result.sort(Comparator.comparingInt((JSONObject v)->v.optInt("season",0)).thenComparingInt(v->v.optInt("episode",0)));return result;}
 static int indexOf(ArrayList<JSONObject> list,String id){if(list==null||id==null)return -1;for(int i=0;i<list.size();i++)if(id.equals(list.get(i).optString("id")))return i;return -1;}
 static ArrayList<Integer> seasons(ArrayList<JSONObject> list){ArrayList<Integer> out=new ArrayList<>();if(list==null)return out;for(JSONObject v:list){if(v==null||!v.has("season"))continue;int s=v.optInt("season",0);if(!out.contains(s))out.add(s);}return out;}
 static boolean inSeason(JSONObject video,int season){if(season<0)return true;return video!=null&&video.has("season")&&video.optInt("season")==season;}
 static String label(JSONObject video){String id=video.optString("id"),title=video.optString("title");String prefix;if(video.has("season")&&video.has("episode"))prefix="第 "+video.optInt("season")+" 季 · 第 "+video.optInt("episode")+" 集";else if(video.optInt("episode",0)>0)prefix="第 "+video.optInt("episode")+" 集";else prefix=StremioStreams.episodeLabel(id);return (prefix.isEmpty()?"剧集":prefix)+(title.isEmpty()?"":" · "+title);}
 static String key(String type,String parent){return type+"|"+parent;}
}
