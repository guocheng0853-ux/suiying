package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** 自动下一集纯规则：找下一集、何时弹出倒计时卡片。 */
final class AutoNextRules {
 static final int COUNTDOWN_MS=5000;
 static final int[] LEADS={0,10000,20000,30000,60000};
 static final String[] LEAD_LABELS={"仅播完时","最后 10 秒","最后 20 秒","最后 30 秒","最后 60 秒"};
 static final int DEFAULT_LEAD=20000;
 static int lead(int saved){for(int v:LEADS)if(v==saved)return v;return DEFAULT_LEAD;}
 /** 按季/集排序后的下一项；当前集不在列表里时尝试 parent:季:集 的下一集。 */
 static String next(JSONObject meta,String current){if(meta==null||current==null||current.isEmpty())return null;ArrayList<JSONObject> list=EpisodeActions.list(meta);for(int i=0;i<list.size();i++)if(current.equals(list.get(i).optString("id")))return i+1<list.size()?list.get(i+1).optString("id"):null;
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("(.+):([0-9]+):([0-9]+)").matcher(current);if(!m.matches())return null;int s=Integer.parseInt(m.group(2)),e=Integer.parseInt(m.group(3));for(JSONObject v:list)if(v.optInt("season",-1)==s&&v.optInt("episode",-1)==e+1)return v.optString("id");return null;}
 static String label(JSONObject meta,String id){for(JSONObject v:EpisodeActions.list(meta))if(v.optString("id").equals(id))return EpisodeActions.label(v);return "下一集";}
 /** 是否该弹出「下一集」卡片。leadMs=0 表示只在播完（EndReached）时弹出。 */
 static boolean offer(boolean enabled,boolean series,boolean hasNext,boolean cancelled,boolean ended,int posMs,int durMs,int leadMs){if(!enabled||!series||!hasNext||cancelled)return false;if(ended)return true;if(leadMs<=0||durMs<=60000||posMs<=0)return false;return durMs-posMs<=leadMs;}
 static int secondsLeft(long elapsedMs){long left=COUNTDOWN_MS-Math.max(0,elapsedMs);return left<=0?0:(int)((left+999)/1000);}
}
