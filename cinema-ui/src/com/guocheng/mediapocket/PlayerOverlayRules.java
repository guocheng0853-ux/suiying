package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 横屏播放控件的纯规则：解码标签、倍速药丸步进、集标题、底栏三个图标各管什么。
 * 底栏从左到右固定为音轨、字幕、选集，不随片源对调。
 */
final class PlayerOverlayRules {
 static final String AUDIO="audio",SUBTITLE="subtitle",EPISODES="episodes";
 private PlayerOverlayRules(){}
 /** 底栏右侧三个图标，0 起：音符、CC、最右选集。 */
 static String bottomAction(int index){return index==0?AUDIO:index==1?SUBTITLE:index==2?EPISODES:"";}
 /** 显式档位优先。hardware 是 HW+，hw 是普通硬解，software 是软解。自动模式才看当前实际走的是软件还是硬件。 */
 static String decoderLabel(String mode,boolean softwareKnown,boolean software){
  if("software".equals(mode))return "SW";
  if("hw".equals(mode))return "HW";
  if("hardware".equals(mode))return "HW+";
  if(softwareKnown)return software?"SW":"HW+";
  return "AUTO";
 }
 /** 点一下按 HW+ → HW → SW → HW+ 循环。自动模式从当前实际档的下一档开始；还不知道时先到 HW+。 */
 static String toggleDecode(String mode,boolean softwareKnown,boolean software){
  String cur=mode;
  if(cur==null||cur.isEmpty()||"auto".equals(cur)){if(softwareKnown&&software)cur="software";else if(softwareKnown)cur="hardware";else return "hardware";}
  if("hardware".equals(cur))return "hw";
  if("hw".equals(cur))return "software";
  if("software".equals(cur))return "hardware";
  return "hardware";
 }
 /** 长按解码面板的三个选项，和单击循环同一套值。 */
 static String[] decodeChoices(){return new String[]{"hardware","hw","software"};}
 static String decodeChoiceLabel(String mode){return decoderLabel(mode,false,false);}
 static boolean canToggleDecode(boolean hasVideo,boolean hasUrl){return hasVideo&&hasUrl;}
 /**
  * 底座 GesturePlayerActivity.buttonAt 只把 Button 和 SeekBar 交给控件。
  * TextView 上的点击会被当成画面手势直接吃掉，所以解码芯片必须是有尺寸、可点、可聚焦的 Button。
  */
 static boolean chipReceivesTap(boolean button,int width,int height,boolean clickable,boolean focusable){return button&&width>0&&height>0&&clickable&&focusable;}
 /** 点一下之后中间那行字。 */
 static String decodeHud(String tier){if("software".equals(tier))return "SW 软解";if("hw".equals(tier))return "HW 硬解（兼容）";return "HW+ 硬解";}
 /** setHWDecoderEnabled 的两个参数：HW+ 是 (true, true)，HW 是 (true, false)，SW 是 (false, false)。 */
 static boolean hwEnabled(String tier){return !"software".equals(tier);}
 static boolean directRendering(String tier){return hwEnabled(tier)&&!"hw".equals(tier);}
 /** setHWDecoderEnabled 之外还要写上的选项。HW 关掉直接渲染；SW 跟底座一样强制 avcodec。 */
 static java.util.List<String> decodeOptions(String tier,boolean file,java.util.Map<String,String> headers){
  java.util.ArrayList<String> o=new java.util.ArrayList<>();
  if("hw".equals(tier))o.add(":no-mediacodec-dr");
  if("software".equals(tier)){o.add(":codec=avcodec");o.add(":avcodec-hw=none");}
  o.add(file?":file-caching=1500":":network-caching=1500");
  o.add(":audio-language=eng,en");
  if(!file){String ua=header(headers,"User-Agent");o.add(":http-user-agent="+(ua.isEmpty()?"MediaPocket/0.1.3":ua));if(headers!=null)for(java.util.Map.Entry<String,String> e:headers.entrySet()){String k=e.getKey();if(k==null||k.equalsIgnoreCase("User-Agent"))continue;String v=e.getValue()==null?"":e.getValue();o.add(k.equalsIgnoreCase("Referer")?":http-referrer="+v:":http-header="+k+": "+v);}}
  return o;
 }
 static String header(java.util.Map<String,String> headers,String name){if(headers==null)return "";for(java.util.Map.Entry<String,String> e:headers.entrySet())if(e.getKey()!=null&&e.getKey().equalsIgnoreCase(name)&&e.getValue()!=null)return e.getValue();return "";}
 /**
  * 药丸 + / −。低于 1 时每格 0.1，1 及以上每格 0.25，停在 0.1 和 8.0。
  * 从 0.9 往上先到 1.0，再往上才是 1.25；从 1.25 往下先回到 1.0。
  */
 static float stepSpeed(float current,int direction){
  float v=PlaybackPrefs.clampRate(current);int dir=direction<0?-1:1;int milli=Math.round(v*1000f);
  int step=dir>0?(milli<1000?100:250):(milli<=1000?100:250);
  int next=milli+dir*step;
  if(dir>0&&milli<1000&&next>1000)next=1000;
  if(dir<0&&milli>1000&&next<1000)next=1000;
  if(next<100)next=100;if(next>8000)next=8000;
  return next/1000f;
 }
 /** 1 显示成 1.0x，0.5 显示成 0.5x，0.75 / 1.25 保留两位。按实际倍速格式化，不再吸回旧六档。 */
 static String speedPill(float v){float n=PlaybackPrefs.clampRate(v);String s=String.format(Locale.ROOT,"%.2f",n);if(s.endsWith("00"))s=s.substring(0,s.length()-1);else if(s.endsWith("0"))s=s.substring(0,s.length()-1);return s+"x";}
 /** 右侧面板：屏幕放得下 480dp 就用 480dp，否则用宽度的 45%。 */
 static int panelWidthPx(int screenPx,float density){if(screenPx<=0)return 0;float d=density<=0f?1f:density;int full=Math.round(480f*d);if(full<=screenPx)return full;return Math.max(1,Math.round(screenPx*0.45f));}
 static String clock(int posMs,int durMs){return mmss(posMs)+" / "+mmss(durMs);}
 static String mmss(long ms){long s=Math.max(0,ms)/1000,h=s/3600,m=(s/60)%60,sec=s%60;return h>0?String.format(Locale.ROOT,"%d:%02d:%02d",h,m,sec):String.format(Locale.ROOT,"%02d:%02d",m,sec);}
 /** 连接中或已经结束时留一行状态；正常播放时底栏不再铺诊断文字。 */
 static boolean showStatus(boolean rendered,boolean terminal,String text){return text!=null&&!text.trim().isEmpty()&&(terminal||!rendered);}
 static String seriesTitle(String name,JSONObject meta){
  if(meta!=null){String fusion=meta.optString("fusion.title").trim();if(!fusion.isEmpty())return fusion;String line=meta.optString("emby.line").trim();if(!line.isEmpty()&&name!=null&&name.endsWith(line)){String cut=name.substring(0,name.length()-line.length()).replaceAll("[\\s·]+$","");if(!cut.isEmpty())return cut;}}
  return name==null?"":name;
 }
 /** 底栏第一行。有集名时写成「第55集 · 神秘的魂殿」，只有集号时写成「第55集」。电影返回空串。 */
 static String episodeLine(JSONObject meta,String id,String name){return presentEpisode(episodePlain(meta,id,name));}
 static String episodePlain(JSONObject meta,String id,String name){
  String series=seriesTitle(name,meta);JSONObject video=current(meta,id);
  if(video!=null){int ep=video.has("episode")?video.optInt("episode",0):0;String title=video.optString("title").trim();if(ep>0&&!title.isEmpty()&&!title.equals(series)){if(title.matches("^"+ep+"\\s*[:：].*")||title.matches("^第\\s*"+ep+"\\s*集.*"))return tidyEpisode(title);return ep+": "+title;}if(ep>0)return String.valueOf(ep);if(!title.isEmpty()&&!title.equals(series))return title;}
  if(meta!=null){String emby=embyLine(meta.optString("emby.line"));if(!emby.isEmpty())return emby;String sub=meta.optString("subtitleEpisode").trim();if(!sub.isEmpty())return tidyEpisode(sub);}
  return fromId(id);
 }
 /** 「55: 标题」变成「第55集 · 标题」，单独的集号变成「第55集」。已经是片名的原样返回。 */
 static String presentEpisode(String raw){if(raw==null)return "";String t=raw.trim();if(t.isEmpty())return "";java.util.regex.Matcher m=java.util.regex.Pattern.compile("^(\\d{1,4})\\s*:\\s*(.+)$").matcher(t);if(m.find())return "第"+Integer.parseInt(m.group(1))+"集 · "+m.group(2).trim();if(t.matches("\\d{1,4}"))return "第"+Integer.parseInt(t)+"集";return t;}
 static JSONObject current(JSONObject meta,String id){if(meta==null||id==null||id.isEmpty())return null;for(JSONObject video:EpisodeActions.list(meta))if(id.equals(video.optString("id")))return video;return null;}
 static String neighbor(JSONObject meta,String id,int direction){if(meta==null||id==null||direction==0)return null;ArrayList<JSONObject> list=EpisodeActions.list(meta);for(int i=0;i<list.size();i++){if(!id.equals(list.get(i).optString("id")))continue;int j=i+direction;if(j<0||j>=list.size())return null;String next=list.get(j).optString("id");return next.isEmpty()?null:next;}return null;}
 static boolean showNeighbor(String id){return id!=null&&!id.isEmpty();}
 /** 有剧集列表，或影视仓剧集（选集要现拉）时才显示最右图标。 */
 static boolean showEpisodes(int videos,boolean series,boolean tvbox){return videos>0||(tvbox&&series);}
 static String embyLine(String line){if(line==null)return "";java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)S\\d+E(\\d+)\\s*(?:·\\s*)?(.*)").matcher(line.trim());if(!m.find())return "";String title=m.group(2)==null?"":m.group(2).trim();int ep=Integer.parseInt(m.group(1));return title.isEmpty()?String.valueOf(ep):ep+": "+title;}
 static String tidyEpisode(String raw){
  if(raw==null)return "";String t=raw.trim();if(t.isEmpty())return "";
  java.util.regex.Matcher colon=java.util.regex.Pattern.compile("^(\\d{1,4})\\s*[:：]\\s*(.+)$").matcher(t);if(colon.find())return Integer.parseInt(colon.group(1))+": "+colon.group(2).trim();
  java.util.regex.Matcher se=java.util.regex.Pattern.compile("(?i)^S\\d+E(\\d+)\\s*[·.\\-]*\\s*(.*)$").matcher(t);if(se.find()){String title=se.group(2).trim();int ep=Integer.parseInt(se.group(1));return title.isEmpty()?String.valueOf(ep):ep+": "+title;}
  java.util.regex.Matcher ep=java.util.regex.Pattern.compile("^第\\s*(\\d{1,4})\\s*集\\s*(.*)$").matcher(t);if(ep.find()){String title=ep.group(2).trim();int n=Integer.parseInt(ep.group(1));return title.isEmpty()?String.valueOf(n):n+": "+title;}
  java.util.regex.Matcher num=java.util.regex.Pattern.compile("^(\\d{1,4})\\s+(.+)$").matcher(t);if(num.find())return Integer.parseInt(num.group(1))+": "+num.group(2).trim();
  java.util.regex.Matcher only=java.util.regex.Pattern.compile("^(\\d{1,4})$").matcher(t);if(only.find())return String.valueOf(Integer.parseInt(only.group(1)));
  return t;
 }
 static String fromId(String id){if(id==null)return "";String[] p=id.split(":");if(p.length<3||"emby".equals(p[0])||"tvbox".equals(p[0]))return "";try{int ep=Integer.parseInt(p[p.length-1]);return ep>0?String.valueOf(ep):"";}catch(Exception e){return "";}}
 /** 旧底栏胶囊里这些按钮改由新图标或「更多」接管，滚动行里不再留。 */
 static boolean retiredBottom(String text,String description){
  if(PlayerChromeRules.legacySpeed(text,description))return true;
  return one(text,description,"字幕")||one(text,description,"画面比例")||one(text,description,"比例")||one(text,description,"播放器")||one(text,description,"更多")||one(text,description,"选集")||starts(text,"影视仓源")||starts(description,"影视仓源");
 }
 static boolean one(String text,String description,String key){return key.equals(text)||key.equals(description);}
 static boolean starts(String value,String prefix){return value!=null&&value.startsWith(prefix);}
}
