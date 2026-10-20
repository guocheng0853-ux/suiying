package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/**
 * 0.1.106 备份与恢复的纯逻辑（不依赖 Android，便于测试）。
 * 备份文件是一个 JSON：{app, format, version, created, secrets, prefs:{文件名:{键:{t,v}}}}。
 * t：s 字符串 / i 整数 / l 长整数 / f 小数 / b 开关 / ss 字符串集合。
 * 缓存类的键（目录缓存、元数据缓存、首页海报缓存、设备编号）不进备份。
 * 选「不含账号」时：键名像令牌 / 密码的整项丢掉；值是 JSON 的，递归删掉 token / password 等字段。
 */
final class BackupRules {
 private BackupRules(){}
 static final String APP="suiying";static final int FORMAT=1;
 static final String[] SKIP_PREFIXES={"fusion.catalog","cinema.meta.","home.hero","hero.","cinema.hero","emby.deviceId","update.","followup.lastRun"};
 static final String[] SECRET_FIELDS={"token","accesstoken","password","passwd","secret","apikey","api_key","cookie","authorization"};

 static boolean skip(String key){if(key==null)return true;for(String p:SKIP_PREFIXES)if(key.startsWith(p))return true;return false;}
 static boolean secretKey(String key){if(key==null)return false;String k=key.toLowerCase(Locale.ROOT).replace("-","").replace("_","");for(String s:SECRET_FIELDS){String n=s.replace("_","");if(k.endsWith(n)||k.contains(n+"."))return true;}return k.contains("token")||k.contains("password");}

 /** 把一个 SharedPreferences.getAll() 的结果编码成备份里的一节。 */
 static JSONObject encode(Map<String,?> all,boolean withSecrets)throws JSONException{
  JSONObject out=new JSONObject();if(all==null)return out;
  TreeMap<String,Object> sorted=new TreeMap<>(all);
  for(Map.Entry<String,Object> e:sorted.entrySet()){String k=e.getKey();Object v=e.getValue();if(skip(k)||v==null)continue;if(!withSecrets&&secretKey(k))continue;
   JSONObject cell=new JSONObject();
   if(v instanceof String){String s=(String)v;if(!withSecrets)s=stripJson(s);cell.put("t","s").put("v",s);}
   else if(v instanceof Integer)cell.put("t","i").put("v",v);
   else if(v instanceof Long)cell.put("t","l").put("v",v);
   else if(v instanceof Float)cell.put("t","f").put("v",((Float)v).doubleValue());
   else if(v instanceof Boolean)cell.put("t","b").put("v",v);
   else if(v instanceof Set){JSONArray arr=new JSONArray();for(Object o:new TreeSet<>(((Set<?>)v).stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet())))arr.put(o);cell.put("t","ss").put("v",arr);}
   else continue;
   out.put(k,cell);}
  return out;
 }
 /** 值看起来是 JSON 时删掉里面的令牌 / 密码字段；不是 JSON 原样返回。 */
 static String stripJson(String s){if(s==null)return null;String t=s.trim();if(!(t.startsWith("{")||t.startsWith("[")))return s;try{Object parsed=t.startsWith("{")?new JSONObject(t):new JSONArray(t);Object clean=strip(parsed);return clean.toString();}catch(Exception e){return s;}}
 static Object strip(Object o)throws JSONException{
  if(o instanceof JSONObject){JSONObject in=(JSONObject)o,out=new JSONObject();for(Iterator<String> it=in.keys();it.hasNext();){String k=it.next();if(secretField(k))continue;out.put(k,strip(in.get(k)));}return out;}
  if(o instanceof JSONArray){JSONArray in=(JSONArray)o,out=new JSONArray();for(int i=0;i<in.length();i++)out.put(strip(in.get(i)));return out;}
  return o;
 }
 static boolean secretField(String k){String n=k.toLowerCase(Locale.ROOT).replace("_","");for(String s:SECRET_FIELDS)if(n.equals(s.replace("_","")))return true;return false;}

 static JSONObject wrap(String version,long created,boolean withSecrets,Map<String,JSONObject> sections)throws JSONException{
  JSONObject prefs=new JSONObject();for(Map.Entry<String,JSONObject> e:new TreeMap<>(sections).entrySet())prefs.put(e.getKey(),e.getValue());
  return new JSONObject().put("app",APP).put("format",FORMAT).put("version",version).put("created",created).put("secrets",withSecrets).put("prefs",prefs);
 }
 /** 校验并取出备份内容；不是随影备份时抛出带中文说明的异常。 */
 static JSONObject open(String text)throws Exception{
  JSONObject root;try{root=new JSONObject(text);}catch(Exception e){throw new IllegalArgumentException("这不是随影的备份文件");}
  if(!APP.equals(root.optString("app"))||root.optJSONObject("prefs")==null)throw new IllegalArgumentException("这不是随影的备份文件");
  if(root.optInt("format")>FORMAT)throw new IllegalArgumentException("备份来自更新的版本，请先升级随影");
  return root;
 }
 /** 统计一份备份里的常用内容，用于恢复前的确认说明。 */
 static String summary(JSONObject root){
  JSONObject data=root.optJSONObject("prefs")==null?null:root.optJSONObject("prefs").optJSONObject("data");
  int keys=0;for(Iterator<String> it=root.optJSONObject("prefs").keys();it.hasNext();){JSONObject sec=root.optJSONObject("prefs").optJSONObject(it.next());if(sec!=null)keys+=sec.length();}
  StringBuilder b=new StringBuilder();b.append("版本 ").append(root.optString("version","?")).append(" · ").append(keys).append(" 项设置");
  int fav=count(data,"favorites"),hist=count(data,"history"),emby=count(data,"emby.servers");
  if(emby>0)b.append(" · Emby 服务器 ").append(emby).append(" 台");if(fav>0)b.append(" · 收藏 ").append(fav);if(hist>0)b.append(" · 观看记录 ").append(hist);
  b.append(root.optBoolean("secrets")?" · 含账号与令牌":" · 不含账号与令牌（恢复后需重新登录 Emby）");
  return b.toString();
 }
 static int count(JSONObject section,String key){if(section==null)return 0;JSONObject cell=section.optJSONObject(key);if(cell==null)return 0;try{return new JSONArray(cell.optString("v","[]")).length();}catch(Exception e){return 0;}}
 /** 恢复时合并：备份里没有的密钥字段保留手机上原来的值（不含账号的备份不会把已登录的服务器清掉）。 */
 static String mergeSecrets(String incoming,String current){
  if(incoming==null||current==null)return incoming;
  try{String a=incoming.trim(),b=current.trim();
   if(a.startsWith("[")&&b.startsWith("[")){JSONArray in=new JSONArray(a),cur=new JSONArray(b);Map<String,JSONObject> byId=new HashMap<>();for(int i=0;i<cur.length();i++){JSONObject o=cur.optJSONObject(i);if(o!=null&&!o.optString("id").isEmpty())byId.put(o.optString("id"),o);}
    JSONArray out=new JSONArray();for(int i=0;i<in.length();i++){Object v=in.get(i);if(v instanceof JSONObject){JSONObject o=(JSONObject)v;JSONObject old=byId.get(o.optString("id"));if(old!=null)for(Iterator<String> it=old.keys();it.hasNext();){String k=it.next();if(secretField(k)&&!o.has(k))o.put(k,old.get(k));}}out.put(v);}return out.toString();}
   if(a.startsWith("{")&&b.startsWith("{")){JSONObject in=new JSONObject(a),cur=new JSONObject(b);for(Iterator<String> it=cur.keys();it.hasNext();){String k=it.next();if(secretField(k)&&!in.has(k))in.put(k,cur.get(k));}return in.toString();}
  }catch(Exception ignored){}
  return incoming;
 }
 static String fileName(String version,long now){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT);return "随影备份-"+f.format(new Date(now))+"-v"+version+".json";}
}
