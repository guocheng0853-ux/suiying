package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.173：播放页「选集」点一集后，在面板左边直接列出这一集的片源（不再退回剧集详情页重新选）。
 * 这里是不依赖 Android 的部分：排序、条数上限、窗口位置。
 */
final class EpisodeSourceRules {
 private EpisodeSourceRules(){}
 /** 最多列出的条数；Torrentio 一类插件一集能返回上百条，后面的基本都是同一资源的低种子副本。 */
 static final int LIMIT=40;
 /** 可播放的片源按 SourceCardRules.score 从高到低排，分数相同保持插件返回的先后；外部网页链接不在应用内播放，排到最后。 */
 static int[] order(JSONArray all,String audioPref,int limit){
  if(all==null||limit<=0)return new int[0];
  ArrayList<int[]> keep=new ArrayList<>();
  for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null||!StremioStreams.playable(s))continue;int v=SourceCardRules.score(s,audioPref);if(!s.optString("externalUrl").isEmpty())v=Integer.MIN_VALUE+1;keep.add(new int[]{i,v});}
  keep.sort((x,y)->x[1]!=y[1]?Integer.compare(y[1],x[1]):Integer.compare(x[0],y[0]));
  int n=Math.min(limit,keep.size());int[] out=new int[n];for(int i=0;i<n;i++)out[i]=keep.get(i)[0];return out;
 }
 /** 同一条链接（url 或 infoHash+fileIdx）只留第一条：几个插件常常转发同一个种子。 */
 static String key(JSONObject s){if(s==null)return "";String h=s.optString("infoHash");if(!h.isEmpty())return "bt:"+h.toLowerCase(Locale.ROOT)+":"+s.optInt("fileIdx",-1);String u=s.optString("url");if(!u.isEmpty())return "url:"+u;return "ext:"+s.optString("externalUrl");}
 /**
  * 片源窗口放在选集面板左边，留同样的边距；屏幕不够并排放两块时（竖屏小手机）就盖在面板上面。
  * 返回窗口右边缘到屏幕右边的距离（Gravity.END 的 x）。
  */
 static int windowX(int screenW,int panelW,int edge){if(screenW<=0||panelW<=0)return edge;int side=edge+panelW+edge;return screenW-side-panelW>=edge?side:edge;}
 /** 片源行第二行：清晰度、HDR、编码、音频、体积、做种数，加上来自哪个插件。 */
 static String detail(List<String> tags,String addon){StringBuilder b=new StringBuilder();for(String t:tags){if(t==null||t.isEmpty())continue;if(b.length()>0)b.append(" · ");b.append(t);}String n=addon==null?"":addon.replace('\n',' ').trim();if(!n.isEmpty()){if(b.length()>0)b.append(" · ");b.append(n);}return b.toString();}
}
