package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.106 收藏与追剧更新的纯逻辑（不依赖 Android）。
 * 收藏沿用原来的 favorites 列表（[{id,type,name,poster,…}]），Emby / 影视仓详情页也能收藏。
 * 追更状态 followup.state：{收藏id:{count 最新集数, seen 看过详情时的集数, latest 最新一集名, checked 时间}}。
 */
final class FollowRules {
 private FollowRules(){}
 /** 要存进收藏的条目：去掉一次性字段（续播位置、直接开播），剧集统一按 series。 */
 static JSONObject favoriteEntry(JSONObject meta)throws JSONException{
  JSONObject m=new JSONObject(meta.toString());m.remove("emby.resumeMs");m.remove("emby.directPlay");m.remove("emby.play");
  if("Series".equals(m.optString("emby.type")))m.put("type","series");
  if(m.optString("id").isEmpty())throw new JSONException("缺少编号");return m;
 }
 static boolean contains(JSONArray favorites,String id){if(favorites==null||id==null)return false;for(int i=0;i<favorites.length();i++){JSONObject f=favorites.optJSONObject(i);if(f!=null&&id.equals(f.optString("id")))return true;}return false;}
 /** 收藏 / 取消收藏；新收藏放最前。 */
 static JSONArray toggle(JSONArray favorites,JSONObject entry){JSONArray out=new JSONArray();String id=entry.optString("id");boolean had=contains(favorites,id);if(!had)out.put(entry);for(int i=0;favorites!=null&&i<favorites.length();i++){JSONObject f=favorites.optJSONObject(i);if(f!=null&&!id.equals(f.optString("id")))out.put(f);}return out;}
 /** 能追更的收藏：Emby 剧集，或影视仓条目（影视仓电影 / 剧集分不清，集数变多就算更新）。 */
 static boolean trackable(JSONObject f){if(f==null)return false;if(f.has("emby.server"))return "Series".equals(f.optString("emby.type"));return f.has("fusion.site");}
 /** 影视仓详情里第一条线路的集数。 */
 static int fusionCount(String playUrl){if(playUrl==null||playUrl.isEmpty())return 0;String first=playUrl.split("\\$\\$\\$",-1)[0];int n=0;for(String e:first.split("#")){String[] p=e.split("\\$",2);if(p.length<2||p[1].isEmpty())continue;n++;}return n;}
 static String fusionLatest(String playUrl){if(playUrl==null)return "";String first=playUrl.split("\\$\\$\\$",-1)[0];String last="";for(String e:first.split("#")){String[] p=e.split("\\$",2);if(p.length<2||p[1].isEmpty())continue;last=p[0];}return last;}
 /**
  * 合并一次检查结果。第一次见到的条目只记基线，不算更新。
  * 返回 true 表示这次检查发现了新集（需要提醒）。
  */
 static boolean record(JSONObject state,String id,int count,String latest,long now)throws JSONException{
  if(count<=0)return false;JSONObject s=state.optJSONObject(id);
  if(s==null){state.put(id,new JSONObject().put("count",count).put("seen",count).put("latest",latest).put("checked",now));return false;}
  int before=s.optInt("count",0);s.put("count",count).put("latest",latest).put("checked",now);
  if(count>before&&count>s.optInt("seen",0)){s.put("notified",false);return true;}
  return false;
 }
 static boolean hasNew(JSONObject state,String id){JSONObject s=state==null?null:state.optJSONObject(id);return s!=null&&s.optInt("count",0)>s.optInt("seen",0);}
 static int newCount(JSONObject state,String id){JSONObject s=state==null?null:state.optJSONObject(id);return s==null?0:Math.max(0,s.optInt("count",0)-s.optInt("seen",0));}
 /** 打开过详情页：当前集数算看过。 */
 static void seen(JSONObject state,String id,int count)throws JSONException{JSONObject s=state.optJSONObject(id);if(s==null){if(count>0)state.put(id,new JSONObject().put("count",count).put("seen",count).put("latest","").put("checked",0));return;}s.put("seen",Math.max(count,s.optInt("count",0)));if(count>s.optInt("count",0))s.put("count",count);}
 /** 通知文案：一部 → 「X 更新到 第12集」；多部 → 「3 部收藏有更新：A、B、C」。 */
 static String[] notice(List<String[]> updates){
  if(updates==null||updates.isEmpty())return null;
  if(updates.size()==1){String[] u=updates.get(0);return new String[]{u[0]+" 有更新",u[1].isEmpty()?"有新的剧集":"更新到 "+u[1]};}
  StringBuilder b=new StringBuilder();for(int i=0;i<updates.size()&&i<4;i++){if(i>0)b.append("、");b.append(updates.get(i)[0]);}if(updates.size()>4)b.append(" 等");
  return new String[]{updates.size()+" 部收藏有更新",b.toString()};
 }
 /** 距离上次检查是否已过 hours 小时。 */
 static boolean due(long last,long now,int hours){return last<=0||now-last>=hours*3600_000L||now<last;}
}
