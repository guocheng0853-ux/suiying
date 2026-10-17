package com.guocheng.mediapocket;
import org.json.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
/**
 * 弹弹play 开放平台公开文档的签名与匹配。
 * https://doc.dandanplay.com/open/ ：X-Signature = Base64(SHA-256(AppId + 秒级时间戳 + 路径 + AppSecret))。
 * 路径不含域名和查询串。请求同时带 X-AppId、X-Timestamp、X-Auth: 1。
 * AppId 与 AppSecret 只来自用户自己的配置，缺省为空。
 */
final class DanDanPlayRules {
 static final String HOST="https://api.dandanplay.net";
 /** 公开搜索接口。清单里的 /api/v2/search/episode 对应这一条。 */
 static final String SEARCH="/api/v2/search/episodes";
 /** 公开番剧详情。清单里的 /api/v2/anime/{id} 对应 /api/v2/bangumi/{animeId}。 */
 static final String ANIME="/api/v2/bangumi/";
 static final String MATCH="/api/v2/match";
 static final String COMMENT="/api/v2/comment/";
 /** 已知不能借用的第三方 AppId。填了也拒绝发送。 */
 static final String BLOCKED_APP="rnraju78qs";
 private DanDanPlayRules(){}
 static boolean ready(String appId,String secret){return reason(appId,secret)==null;}
 static String reason(String appId,String secret){
  String id=appId==null?"":appId.trim(),sec=secret==null?"":secret.trim();
  if(id.isEmpty()||sec.isEmpty())return "弹幕还没有配置。请到「我的」里填写自己申请的 AppId 和 AppSecret。";
  if(id.equalsIgnoreCase(BLOCKED_APP))return "这个 AppId 不能使用。请到弹弹play开放平台申请自己的应用。";
  return null;
 }
 /** 公开算法：Base64(SHA-256(AppId + Timestamp + Path + AppSecret))，哈希取原始字节再编码。 */
 static String signature(String appId,long timestamp,String path,String secret){
  try{String data=appId.trim()+timestamp+pathOnly(path)+secret.trim();byte[] hash=MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(hash);}catch(Exception e){return "";}
 }
 static String pathOnly(String path){if(path==null||path.isEmpty())return "/";int q=path.indexOf('?');String bare=q<0?path:path.substring(0,q);if(!bare.startsWith("/"))bare="/"+bare;return bare.toLowerCase(Locale.ROOT);}
 static String searchQuery(String anime,int episode){StringBuilder s=new StringBuilder(SEARCH).append("?anime=").append(url(anime==null?"":anime));if(episode>0)s.append("&episode=").append(episode);return s.toString();}
 static String animeQuery(long animeId){return ANIME+animeId;}
 static String commentQuery(long episodeId){return COMMENT+episodeId+"?withRelated=true&chConvert=1";}
 static String matchBody(String fileName,int durationSec){JSONObject o=new JSONObject();try{o.put("fileName",fileName==null?"":fileName).put("fileHash","").put("fileSize",0).put("videoDuration",Math.max(0,durationSec)).put("matchMode","fileNameOnly");}catch(Exception ignored){}return o.toString();}
 static String url(String raw){try{return java.net.URLEncoder.encode(raw,"UTF-8");}catch(Exception e){return "";}}
 static final class Hit {final long episodeId,animeId;final String animeTitle,episodeTitle;final int shift;Hit(long episodeId,long animeId,String animeTitle,String episodeTitle,int shift){this.episodeId=episodeId;this.animeId=animeId;this.animeTitle=animeTitle==null?"":animeTitle;this.episodeTitle=episodeTitle==null?"":episodeTitle;this.shift=shift;}
  String label(){String ep=episodeTitle.isEmpty()?("节目 "+episodeId):episodeTitle;return animeTitle.isEmpty()?ep:animeTitle+" · "+ep;}}
 static List<Hit> searchHits(JSONObject json){ArrayList<Hit> out=new ArrayList<>();if(json==null)return out;JSONArray animes=json.optJSONArray("animes");if(animes==null)animes=json.optJSONArray("animes");collectAnimes(animes,out);return out;}
 static void collectAnimes(JSONArray animes,List<Hit> out){if(animes==null)return;for(int i=0;i<animes.length()&&out.size()<80;i++){JSONObject a=animes.optJSONObject(i);if(a==null)continue;long animeId=a.optLong("animeId");String title=a.optString("animeTitle",a.optString("title"));JSONArray episodes=a.optJSONArray("episodes");if(episodes==null||episodes.length()==0){if(animeId>0)out.add(new Hit(0,animeId,title,"",0));continue;}for(int j=0;j<episodes.length()&&out.size()<80;j++){JSONObject e=episodes.optJSONObject(j);if(e==null)continue;long id=e.optLong("episodeId");if(id<=0)continue;out.add(new Hit(id,animeId,title,e.optString("episodeTitle",e.optString("title")),0));}}}
 static List<Hit> animeHits(JSONObject json){ArrayList<Hit> out=new ArrayList<>();if(json==null)return out;JSONObject bangumi=json.optJSONObject("bangumi");if(bangumi==null)bangumi=json.optJSONObject("anime");if(bangumi==null)bangumi=json;JSONArray episodes=bangumi.optJSONArray("episodes");long animeId=bangumi.optLong("animeId");String title=bangumi.optString("animeTitle");if(episodes==null)return out;for(int i=0;i<episodes.length()&&out.size()<200;i++){JSONObject e=episodes.optJSONObject(i);if(e==null)continue;long id=e.optLong("episodeId");if(id<=0)continue;out.add(new Hit(id,animeId,title,e.optString("episodeTitle"),0));}return out;}
 static List<Hit> matchHits(JSONObject json){ArrayList<Hit> out=new ArrayList<>();if(json==null||!json.optBoolean("isMatched",json.optJSONArray("matches")!=null))return out;JSONArray matches=json.optJSONArray("matches");if(matches==null)return out;for(int i=0;i<matches.length()&&out.size()<20;i++){JSONObject m=matches.optJSONObject(i);if(m==null)continue;long id=m.optLong("episodeId");if(id<=0)continue;out.add(new Hit(id,m.optLong("animeId"),m.optString("animeTitle"),m.optString("episodeTitle"),m.optInt("shift")));}return out;}
 /** 自动匹配：只有一条，或集数能唯一对上时直接用；否则交给手动选集。 */
 static Hit autoPick(List<Hit> hits,int episode){ArrayList<Hit> ready=new ArrayList<>();for(Hit h:hits)if(h.episodeId>0)ready.add(h);if(ready.isEmpty())return null;if(ready.size()==1)return ready.get(0);if(episode<=0)return null;Hit found=null;for(Hit h:ready){if(!sameEpisode(h.episodeTitle,episode))continue;if(found!=null)return null;found=h;}return found;}
 static boolean sameEpisode(String title,int episode){if(episode<=0||title==null)return false;FusionPlaybackRules.Episode parsed=FusionPlaybackRules.episode(title,"");if(parsed.number==episode)return true;return title.matches("(?s).*\\b0*"+episode+"\\b.*");}
 static long loneAnime(List<Hit> hits){long id=0;for(Hit h:hits){if(h.animeId<=0)continue;if(id==0)id=h.animeId;else if(id!=h.animeId)return 0;}return id;}
 static final class Cue {final long timeMs;final int mode;final int color;final String text;Cue(long timeMs,int mode,int color,String text){this.timeMs=timeMs;this.mode=mode;this.color=color;this.text=text==null?"":text;}}
 static List<Cue> comments(JSONObject json,int shiftSec){ArrayList<Cue> out=new ArrayList<>();if(json==null)return out;JSONArray list=json.optJSONArray("comments");if(list==null)return out;long shift=shiftSec*1000L;for(int i=0;i<list.length()&&out.size()<4000;i++){JSONObject c=list.optJSONObject(i);if(c==null)continue;Cue cue=comment(c.optString("p"),c.optString("m"),shift);if(cue!=null)out.add(cue);}out.sort((a,b)->Long.compare(a.timeMs,b.timeMs));return out;}
 /** p 为「秒,模式,颜色,…」。1/2/3 滚动，4 底部，5 顶部。 */
 static Cue comment(String p,String text,long shiftMs){if(text==null||text.trim().isEmpty()||p==null)return null;String[] parts=p.split(",");if(parts.length<3)return null;try{double sec=Double.parseDouble(parts[0].trim());int mode=Integer.parseInt(parts[1].trim());long color=Long.parseLong(parts[2].trim());if(sec<0||sec>24*3600)return null;int kind=mode==4?4:mode==5?5:1;int rgb=(int)(color&0xffffff);if(rgb==0)rgb=0xffffff;return new Cue(Math.round(sec*1000.0)+shiftMs,kind,0xff000000|rgb,text.trim());}catch(Exception e){return null;}}
 static String fileName(String url,String title){String path=url==null?"":url;int q=path.indexOf('?');if(q>=0)path=path.substring(0,q);int slash=path.lastIndexOf('/');String name=slash>=0?path.substring(slash+1):path;try{name=java.net.URLDecoder.decode(name,"UTF-8");}catch(Exception ignored){}if(name.isEmpty()||name.length()>180||!name.contains(".")){String t=title==null||title.trim().isEmpty()?"video":title.trim();return t+".mp4";}return name;}
 static String failure(JSONObject json,int status){if(json!=null){String m=json.optString("errorMessage",json.optString("error"));if(!m.isEmpty()&&!json.optBoolean("success",true))return m;}if(status==401||status==403)return "弹幕接口拒绝了这次请求，请核对 AppId、AppSecret 和手机时间。";if(status>=400)return "弹幕接口返回错误（HTTP "+status+"）";return "没有匹配到弹幕。";}
}
