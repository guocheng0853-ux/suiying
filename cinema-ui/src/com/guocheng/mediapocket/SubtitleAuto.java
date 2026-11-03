package com.guocheng.mediapocket;
import java.util.*;
/**
 * 开播时选哪条字幕。不引用 Android。
 * 优先级：本片被用户关掉，然后是已记住的外挂文件，然后是用户点过的内嵌轨，然后是偏好语言的完整内嵌轨，最后才是其它完整内嵌轨里的默认或第一条。
 * 纯强制、歌词、特效、评论轨不自动选。只有这些轨时保持关闭，避免看起来像没字幕。
 */
final class SubtitleAuto {
 static final int PENDING=Integer.MIN_VALUE,KEEP=Integer.MIN_VALUE+1,EXTERNAL=Integer.MIN_VALUE+2,DISABLE=-1;
 static final class Track {
  final int id; final String label; final boolean forced,meaningless,commentary,preferredDefault;
  Track(int id,String label,boolean forced,boolean meaningless,boolean commentary,boolean preferredDefault){this.id=id;this.label=label==null?"":label;this.forced=forced;this.meaningless=meaningless;this.commentary=commentary;this.preferredDefault=preferredDefault;}
 }
 static boolean forced(String n){String l=n==null?"":n.toLowerCase(Locale.ROOT);return l.contains("forced")||l.contains("强制");}
 static boolean sparse(String n){String l=n==null?"":n.toLowerCase(Locale.ROOT);return l.contains("signs")||l.contains("songs")||l.contains("lyrics")||l.contains("歌词")||l.contains("特效");}
 static boolean markedDefault(String n){String l=n==null?"":n.toLowerCase(Locale.ROOT);return l.contains("default")||l.contains("默认");}
 static Track[] fromNames(int[] ids,String[] names,String[] titleLangs){
  if(ids==null||names==null)return null;
  String[] raw=new String[ids.length];
  for(int i=0;i<ids.length;i++)raw[i]=i<names.length&&names[i]!=null?names[i]:"";
  String[] shown=TrackSelection.mapped(raw,titleLangs);Track[] out=new Track[ids.length];
  for(int i=0;i<ids.length;i++){String label=shown!=null&&i<shown.length&&shown[i]!=null?shown[i]:raw[i];String both=raw[i]+" "+label;boolean commentary=TrackSelection.commentary(both);out[i]=new Track(ids[i],label.isEmpty()?raw[i]:label,forced(both),sparse(both),commentary,markedDefault(raw[i]));}
  return out;
 }
 static boolean dialogue(Track t){return t!=null&&t.id>=0&&!t.forced&&!t.meaningless&&!t.commentary&&!"off".equals(TrackSelection.script(t.label));}
 static boolean inPreferred(String label,String pref){String k=TrackSelection.script(label);if("eng".equals(pref))return "en".equals(k);if("hant".equals(pref))return "hant".equals(k)||"zh".equals(k)||"hans".equals(k);if("off".equals(pref))return false;return "hans".equals(k)||"zh".equals(k)||"hant".equals(k);}
 static int languageScore(String label,String pref){String k=TrackSelection.script(label);if("eng".equals(pref))return "en".equals(k)?300:"hans".equals(k)?80:"zh".equals(k)?60:"hant".equals(k)?40:1;if("hant".equals(pref))return "hant".equals(k)?300:"zh".equals(k)?220:"hans".equals(k)?160:"en".equals(k)?40:1;return "hans".equals(k)?300:"zh".equals(k)?220:"hant".equals(k)?160:"en".equals(k)?40:1;}
 /** 用户点过的轨还在就返回它的 id；轨道列表还没来是 PENDING；对不上是 -2。 */
 static int remember(Track[] tracks,int id,String name){
  if(tracks==null)return PENDING;
  if(id>=0)for(Track t:tracks)if(t!=null&&t.id==id)return t.id;
  if(name!=null&&!name.isEmpty()){
   for(Track t:tracks)if(t!=null&&t.id>=0&&name.equals(t.label))return t.id;
   for(Track t:tracks)if(t!=null&&t.id>=0&&!t.label.isEmpty()&&(t.label.contains(name)||name.contains(t.label)))return t.id;
   String want=TrackSelection.script(name);
   if(!"other".equals(want)&&!"off".equals(want))for(Track t:tracks)if(t!=null&&t.id>=0&&dialogue(t)&&want.equals(TrackSelection.script(t.label)))return t.id;
  }
  return -2;
 }
 static boolean matched(Track[] tracks,int id,String name){int hit=remember(tracks,id,name);return hit>=0;}
 static int auto(Track[] tracks,String pref){
  if(tracks==null)return PENDING;
  ArrayList<Track> full=new ArrayList<>();for(Track t:tracks)if(dialogue(t))full.add(t);
  if(full.isEmpty())return KEEP;
  String p=SubtitlePrefs.language(false,pref);ArrayList<Track> preferred=new ArrayList<>();
  for(Track t:full)if(inPreferred(t.label,p))preferred.add(t);
  ArrayList<Track> pool=preferred.isEmpty()?full:preferred;Track best=null;int bestScore=-1;
  for(Track t:pool){int s=preferred.isEmpty()?1:languageScore(t.label,p);if(t.preferredDefault)s+=5;if(s>bestScore){bestScore=s;best=t;}}
  return best==null?KEEP:best.id;
 }
 /** 第二字幕只接受 http/https。本地文件必须是用户自己选的 content/file URI，不能拿片源 JSON 里的路径。 */
 static boolean secondaryUrl(String url){if(url==null)return false;String u=url.trim();return u.startsWith("https://")||u.startsWith("http://");}
 static boolean secondaryLocal(String url){if(url==null)return false;String u=url.trim();return u.startsWith("content:")||u.startsWith("file:")||u.startsWith("/");}
 /** 和播放页音轨菜单同一套写法，记住的名字才能对上。 */
 static String media3Label(String label,String language,String codec){String name=label==null?"":label.trim(),lang=language==null?"":language.trim(),code=codec==null?"":codec.trim();String head=name.isEmpty()?(lang.isEmpty()?"未标语言":lang):(lang.isEmpty()?name:name+" · "+lang);return code.isEmpty()?head:head+" · "+code;}
 static java.util.Map<String,String> sidecarHeaders(org.json.JSONObject item){java.util.LinkedHashMap<String,String> headers=new java.util.LinkedHashMap<>();if(item==null)return headers;org.json.JSONObject declared=item.optJSONObject("header");if(declared==null)return headers;java.util.Iterator<String> keys=declared.keys();while(keys.hasNext()){String key=keys.next();if(key==null)continue;String value=declared.optString(key,"");if(!value.isEmpty())headers.put(key,value);}return headers;}
 static boolean preferredText(String label,String pref){return inPreferred(label==null?"":label,SubtitlePrefs.language(false,pref));}
 /** 重开第二字幕时只带回 Referer 和 User-Agent，不把 Cookie 写进偏好。 */
 static java.util.Map<String,String> safeSecondaryHeaders(java.util.Map<String,String> headers){java.util.LinkedHashMap<String,String> out=new java.util.LinkedHashMap<>();if(headers==null)return out;for(java.util.Map.Entry<String,String> e:headers.entrySet()){if(e.getKey()==null||e.getValue()==null||e.getValue().isEmpty())continue;String k=e.getKey().toLowerCase(java.util.Locale.ROOT);if(k.equals("referer"))out.put("Referer",e.getValue());else if(k.equals("user-agent"))out.put("User-Agent",e.getValue());}return out;}
 /**
  * 一次开播轮询，播放页直接调用。tracks 为 null 表示还没有字幕轨列表。
 * 有记住的内嵌就不下片源外挂。偏好语言的内嵌优先于别的语言的外挂。
 * 外挂是偏好语言，或确认没有内嵌时，才用外挂，并且不再打开内嵌。同一时刻只留一条第一字幕。
 * manual 表示这次播放里用户手动选过字幕（内嵌、没存成文件的外挂，或关闭）。之后不再自动接管，只把内嵌保持关掉。
 */
 static final class Poll {
  Track[] tracks;String pref="zho",rememberedName="";
  boolean userOff,rememberedFile,hasSidecar,sidecarPreferred,sidecarVisible,playing,waitedOut,manual;
  int rememberedId=-1;
 }
 static final class Step {
  final boolean loadSidecar,cancelSidecar,finished;final int spu;
  Step(boolean load,boolean cancel,int spu,boolean finished){loadSidecar=load;cancelSidecar=cancel;this.spu=spu;this.finished=finished;}
 }
 /** 播放器已经停在目标轨上时不要再 setSpuTrack。目标不是一条具体轨时返回 false，调用方自己决定要不要设置。 */
 static boolean alreadySpu(int current,int target){return (target==-1||target>=0)&&current==target;}
 static Step vlcPoll(Poll in){
  if(in==null)return new Step(false,false,PENDING,false);
  String pref=in.pref==null?"zho":in.pref;
  if(in.userOff||"off".equals(SubtitlePrefs.language(false,pref)))return new Step(false,true,-1,true);
  if(in.rememberedFile)return new Step(false,false,-1,false);
  boolean memory=in.rememberedId>=0||(in.rememberedName!=null&&!in.rememberedName.isEmpty());
  boolean timed=in.playing&&in.waitedOut;
  if(memory){if(in.tracks==null&&!timed)return new Step(false,false,PENDING,false);int hit=in.tracks==null?-2:remember(in.tracks,in.rememberedId,in.rememberedName);if(hit>=0)return new Step(false,true,hit,true);}
  if(in.manual)return new Step(false,false,-1,false);
  int embedded=in.tracks==null?(timed?KEEP:PENDING):auto(in.tracks,pref);
  if(embedded==PENDING)return new Step(false,false,PENDING,false);
  boolean embeddedPreferred=false;
  if(embedded>=0&&in.tracks!=null)for(Track t:in.tracks)if(t!=null&&t.id==embedded&&preferredText(t.label,pref))embeddedPreferred=true;
  if(embeddedPreferred&&!in.sidecarPreferred)return new Step(false,true,embedded,false);
  if(in.sidecarVisible)return new Step(false,false,-1,false);
  boolean sidecarWins=in.hasSidecar&&(embedded==KEEP||in.sidecarPreferred);
  if(sidecarWins)return new Step(true,false,-1,false);
  if(embedded>=0)return new Step(false,true,embedded,false);
  return new Step(false,true,KEEP,false);
 }
 /**
  * externalFile 为真表示这部片子已经记住外挂字幕。这时直接返回 EXTERNAL，不等内嵌轨列表，调用方关掉内嵌，避免两行叠在一起。
  * 返回轨道 id、DISABLE、EXTERNAL、KEEP 或 PENDING。
  */
 static int decide(Track[] tracks,String pref,boolean userOff,int rememberedId,String rememberedName,boolean externalFile){
  if(userOff||"off".equals(SubtitlePrefs.language(false,pref)))return DISABLE;
  if(externalFile)return EXTERNAL;
  boolean wants=rememberedId>=0||(rememberedName!=null&&!rememberedName.isEmpty());
  if(wants){if(tracks==null)return PENDING;int hit=remember(tracks,rememberedId,rememberedName);if(hit>=0)return hit;}
  return auto(tracks,pref);
 }
 static int choose(Track[] tracks,String pref){return decide(tracks,pref,false,-1,null,false);}
 /** 第二条完整轨，避开已经用作第一字幕的 id。没有就 KEEP。 */
 static int secondary(Track[] tracks,int primaryId,String pref){
  if(tracks==null)return KEEP;
  ArrayList<Track> rest=new ArrayList<>();for(Track t:tracks)if(t!=null&&t.id!=primaryId)rest.add(t);
  if(rest.isEmpty())return KEEP;
  int hit=auto(rest.toArray(new Track[0]),pref);return hit==PENDING?KEEP:hit;
 }
}
