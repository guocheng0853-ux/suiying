package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** Episode IDs remain owned by one detail; film IDs are never sent as episode stream IDs. */
final class EpisodeActions {
 static boolean belongs(JSONObject meta,String parent,String episode){if(parent==null||episode==null||parent.isEmpty()||episode.isEmpty()||parent.equals(episode))return false;JSONArray videos=meta==null?null:meta.optJSONArray("videos");for(int i=0;videos!=null&&i<videos.length();i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&episode.equals(v.optString("id")))return true;}return episode.matches(java.util.regex.Pattern.quote(parent)+":[0-9]+:[0-9]+");}
 static ArrayList<JSONObject> list(JSONObject meta){ArrayList<JSONObject> result=new ArrayList<>();HashSet<String> seen=new HashSet<>();JSONArray videos=meta.optJSONArray("videos");for(int i=0;videos!=null&&i<Math.min(videos.length(),3000);i++){JSONObject v=videos.optJSONObject(i);if(v!=null&&!v.optString("id").isEmpty()&&seen.add(v.optString("id")))result.add(v);}result.sort(Comparator.comparingInt((JSONObject v)->v.optInt("season",0)).thenComparingInt(v->v.optInt("episode",0)));return result;}
 static int indexOf(ArrayList<JSONObject> list,String id){if(list==null||id==null)return -1;for(int i=0;i<list.size();i++)if(id.equals(list.get(i).optString("id")))return i;return -1;}
 static ArrayList<Integer> seasons(ArrayList<JSONObject> list){ArrayList<Integer> out=new ArrayList<>();if(list==null)return out;for(JSONObject v:list){if(v==null||!v.has("season"))continue;int s=v.optInt("season",0);if(!out.contains(s))out.add(s);}return out;}
 static boolean inSeason(JSONObject video,int season){if(season<0)return true;return video!=null&&video.has("season")&&video.optInt("season")==season;}
 /** 当前季（或「全部」）按列表顺序每 30 集一段。不足或刚好 30 集不分组。每段是 [from, to)，to 停在实际条数。 */
 static final int SEGMENT=30;
 static ArrayList<int[]> segments(int count){ArrayList<int[]> out=new ArrayList<>();if(count<=SEGMENT)return out;for(int from=0;from<count;from+=SEGMENT)out.add(new int[]{from,Math.min(count,from+SEGMENT)});return out;}
 static String segmentLabel(int from,int to){return (from+1)+"-"+to;}
 /** 过滤后列表里的下标落在哪一段。没有分段或下标无效时返回 -1。 */
 static int segmentAt(int count,int position){if(count<=SEGMENT||position<0)return -1;int index=position/SEGMENT,last=(count-1)/SEGMENT;return index>last?last:index;}
 static String label(JSONObject video){String id=video.optString("id"),title=video.optString("title");String prefix;if(video.has("season")&&video.has("episode"))prefix="第 "+video.optInt("season")+" 季 · 第 "+video.optInt("episode")+" 集";else if(video.optInt("episode",0)>0)prefix="第 "+video.optInt("episode")+" 集";else prefix=StremioStreams.episodeLabel(id);if(prefix.isEmpty())prefix="剧集";if(title.isEmpty()||repeatsEpisode(title,prefix,video))return prefix;return prefix+" · "+title;}
 /** 标题只是把集号又写了一遍（第100集、第 100 集，或整段前缀）时不再拼上去。独立标题如「离乡」「第三集」保留。 */
 static boolean repeatsEpisode(String title,String prefix,JSONObject video){String compact=title.replaceAll("\\s+","");if(!prefix.isEmpty()&&compact.equals(prefix.replaceAll("\\s+","")))return true;int ep=video.optInt("episode",0);if(ep>0&&compact.equals("第"+ep+"集"))return true;if(video.has("season")&&ep>0){String season="第"+video.optInt("season")+"季";if(compact.equals(season+"第"+ep+"集")||compact.equals(season+"·第"+ep+"集"))return true;}return false;}
 static String key(String type,String parent){return type+"|"+parent;}
}
