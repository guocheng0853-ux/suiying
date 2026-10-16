package com.guocheng.mediapocket;
import org.json.*;import java.util.*;import java.util.regex.*;
/** 详情页片源卡片的纯规则：自动预选最佳片源、片源/音轨/字幕三行文字（可单测）。 */
final class SourceCardRules {
 static final String LOADING="正在查找片源…",NONE="暂无片源",NEED_EPISODE="先选择剧集";
 /** 文本中可识别的音轨语言（按偏好代码）。 */
 static boolean speaks(String text,String pref){String n=" "+text.toLowerCase(Locale.ROOT).replaceAll("[._\\-\\[\\]()]"," ")+" ";switch(pref){
  case "zh":return n.contains("国语")||n.contains("普通话")||n.contains("中配")||n.contains("mandarin")||n.contains(" chs ")||n.contains("🇨🇳");
  case "yue":return n.contains("粤")||n.contains("cantonese")||n.contains("🇭🇰");
  case "en":return n.contains("english")||n.contains(" eng ")||n.contains("🇬🇧")||n.contains("🇺🇸");default:return false;}}
 /** 推荐分：在 StreamInfo.score 基础上，命中音轨语言偏好加分（低于分辨率档差，高于 HDR）。 */
 static int score(JSONObject s,String audioPref){StreamInfo i=StreamInfo.parse(s);int v=i.score(StremioStreams.playable(s));if(!"auto".equals(audioPref)&&speaks(StreamInfo.text(s),audioPref))v+=80;return v;}
 /** 返回最佳片源下标；没有可播放条目时返回 -1。 */
 static int best(JSONArray all,String audioPref){int at=-1,top=Integer.MIN_VALUE;if(all==null)return -1;for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null||!StremioStreams.playable(s))continue;int v=score(s,audioPref);if(v>top){top=v;at=i;}}return at;}
 static String name(JSONObject s){StreamInfo i=StreamInfo.parse(s);if(!i.file.isEmpty())return i.file;String t=s.optString("title");if(t.isEmpty())t=s.optString("description");if(t.isEmpty())t=s.optString("name","片源");return t.split("\n")[0].trim();}
 /** 第一行：两行文本（名称 + 标签），state=loading|none|ready。 */
 static String sourceLine(JSONObject picked,String state,int minutes,boolean needEpisode){if(needEpisode)return NEED_EPISODE;if(picked==null)return "loading".equals(state)?LOADING:"none".equals(state)?NONE:"选择播放源";List<String> t=StreamInfo.parse(picked).tags(minutes);return name(picked)+(t.isEmpty()?"":"\n"+String.join(" / ",t));}
 static final String[][] LANG={{"zh","国语"},{"yue","粤语"},{"en","English"}};
 static String langName(String code){for(String[] l:LANG)if(l[0].equals(code))return l[1];return "";}
 /** 第二行：音轨。已选真实音轨优先；否则片源中识别出的语言 + 编码/声道标签；再退回偏好。 */
 static String audioLine(JSONObject picked,String pref,String prefLabel){if(picked!=null&&!picked.optString("mp.audioLabel").isEmpty())return picked.optString("mp.audioLabel");if(picked==null)return prefLabel;String text=StreamInfo.text(picked),lang="";if(!"auto".equals(pref)&&speaks(text,pref))lang=langName(pref);else for(String[] l:LANG)if(speaks(text,l[0])){lang=l[1];break;}String codec=StreamInfo.parse(picked).audio;if(text.toUpperCase(Locale.ROOT).matches(".*\\bMULTI\\b.*")&&lang.isEmpty())lang="多音轨";String out=(lang+" "+codec).trim();return out.isEmpty()?prefLabel+"（片源未标注）":out+("auto".equals(pref)?" (默认)":"");}
 /** 第三行：字幕。已选真实字幕优先；片源自带外挂字幕显示条数；再退回偏好。 */
 static String subtitleLine(JSONObject picked,String prefLabel){if(picked!=null&&!picked.optString("mp.subtitleLabel").isEmpty())return picked.optString("mp.subtitleLabel");JSONArray subs=picked==null?null:picked.optJSONArray("subtitles");if(subs!=null&&subs.length()>0)return prefLabel+" · 片源附带 "+subs.length()+" 条字幕";String t=picked==null?"":StreamInfo.text(picked).toLowerCase(Locale.ROOT);if(t.contains("中字")||t.contains("中文字幕")||t.contains("简中")||t.contains("繁中"))return prefLabel+" · 片源含中文字幕";return prefLabel;}
}
