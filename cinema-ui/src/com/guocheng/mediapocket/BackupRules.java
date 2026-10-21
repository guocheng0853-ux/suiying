package com.guocheng.mediapocket;
import org.json.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import java.util.regex.*;import javax.crypto.*;import javax.crypto.spec.*;
/**
 * 0.1.106 备份与恢复的纯逻辑（不依赖 Android，便于测试）。
 * 备份文件是一个 JSON：{app, format, version, created, secrets, prefs:{文件名:{键:{t,v}}}}。
 * t：s 字符串 / i 整数 / l 长整数 / f 小数 / b 开关 / ss 字符串集合。
 * 缓存类的键（目录缓存、元数据缓存、首页海报缓存、设备编号）不进备份。
 * 选「不含账号」时：键名像令牌 / 密码的整项丢掉；键名里的网址一旦会被清洗（例如 tvbox.config: 后面的地址含令牌）也整项丢掉，避免恢复成对不上的键。值是 JSON 的，递归删掉 token / password 等字段。
 */
final class BackupRules {
 private BackupRules(){}
 static final String APP="suiying";static final int FORMAT=1;
 static final String[] SKIP_PREFIXES={"fusion.catalog","fusion.home:","cinema.catalog.","cinema.meta.","home.hero","hero.","cinema.hero","emby.deviceId","update.","followup.lastRun"};
 static final String[] SECRET_FIELDS={"token","accesstoken","password","passwd","secret","apikey","api_key","cookie","authorization"};

 static boolean skip(String key){if(key==null)return true;for(String p:SKIP_PREFIXES)if(key.startsWith(p))return true;return false;}
 static boolean secretKey(String key){if(key==null)return false;String k=key.toLowerCase(Locale.ROOT).replace("-","").replace("_","");for(String s:SECRET_FIELDS){String n=s.replace("_","");if(k.endsWith(n)||k.contains(n+"."))return true;}return k.contains("token")||k.contains("password")||k.endsWith("key");}

 /** 把一个 SharedPreferences.getAll() 的结果编码成备份里的一节。 */
 static JSONObject encode(Map<String,?> all,boolean withSecrets)throws JSONException{
  JSONObject out=new JSONObject();if(all==null)return out;
  TreeMap<String,Object> sorted=new TreeMap<>(all);
  for(Map.Entry<String,Object> e:sorted.entrySet()){String k=e.getKey();Object v=e.getValue();if(skip(k)||v==null)continue;if(!withSecrets&&(secretKey(k)||!k.equals(scrubText(k))))continue;
   JSONObject cell=new JSONObject();
   if(v instanceof String){String s=(String)v;if(!withSecrets)s=scrubText(stripJson(s));cell.put("t","s").put("v",s);}
   else if(v instanceof Integer)cell.put("t","i").put("v",v);
   else if(v instanceof Long)cell.put("t","l").put("v",v);
   else if(v instanceof Float)cell.put("t","f").put("v",((Float)v).doubleValue());
   else if(v instanceof Boolean)cell.put("t","b").put("v",v);
   else if(v instanceof Set){JSONArray arr=new JSONArray();for(Object o:new TreeSet<>(((Set<?>)v).stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet())))arr.put(!withSecrets?scrubText(String.valueOf(o)):String.valueOf(o));cell.put("t","ss").put("v",arr);}
   else continue;
   out.put(k,cell);}
  return out;
 }
 /** 值看起来是 JSON 时删掉里面的令牌 / 密码字段；不是 JSON 原样返回。 */
 static String stripJson(String s){if(s==null)return null;String t=s.trim();if(!(t.startsWith("{")||t.startsWith("[")))return s;try{Object parsed=t.startsWith("{")?new JSONObject(t):new JSONArray(t);Object clean=strip(parsed);return clean.toString();}catch(Exception e){return s;}}
 static Object strip(Object o)throws JSONException{
  if(o instanceof JSONObject){JSONObject in=(JSONObject)o,out=new JSONObject();for(Iterator<String> it=in.keys();it.hasNext();){String k=it.next();if(secretField(k))continue;out.put(k,strip(in.get(k)));}return out;}
  if(o instanceof JSONArray){JSONArray in=(JSONArray)o,out=new JSONArray();for(int i=0;i<in.length();i++)out.put(strip(in.get(i)));return out;}
  if(o instanceof String)return scrubText((String)o);
  return o;
 }
 static boolean secretField(String k){String n=k.toLowerCase(Locale.ROOT).replace("_","");for(String s:SECRET_FIELDS)if(n.equals(s.replace("_","")))return true;return false;}

 static JSONObject wrap(String version,long created,boolean withSecrets,Map<String,JSONObject> sections)throws JSONException{
  JSONObject prefs=new JSONObject();for(Map.Entry<String,JSONObject> e:new TreeMap<>(sections).entrySet())prefs.put(e.getKey(),e.getValue());
  return new JSONObject().put("app",APP).put("format",FORMAT).put("version",version).put("created",created).put("secrets",withSecrets).put("prefs",prefs);
 }
 /** 校验并取出备份内容；不是随影备份时抛出带中文说明的异常。 */
 static boolean sealed(String text){try{return new JSONObject(text).optBoolean("sealed");}catch(Exception e){return false;}}
 static JSONObject open(String text)throws Exception{return open(text,null);}
 static JSONObject open(String text,String password)throws Exception{
  JSONObject root;try{root=new JSONObject(text);}catch(Exception e){throw new IllegalArgumentException("这不是随影的备份文件");}
  if(root.optBoolean("sealed"))root=new JSONObject(unseal(root,password));
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
 static final Pattern URL_IN_TEXT=Pattern.compile("https?://[^\\s\"'<>\\\\)]+");
 static final Pattern USERINFO=Pattern.compile("(?i)^(https?://)(?:[^/@\\s]+(?::[^/@\\s]*)?@)");
 static final int KDF_ITER=210000;
 /** 去掉网址里的账号、查询参数令牌，以及小雅 / 插件 / Debrid 这类路径令牌。普通路径保留。 */
 static String scrubText(String s){if(s==null||s.indexOf("://")<0)return s;Matcher m=URL_IN_TEXT.matcher(s);StringBuffer b=new StringBuffer();while(m.find())m.appendReplacement(b,Matcher.quoteReplacement(scrubUrl(m.group())));m.appendTail(b);return b.toString();}
 static String scrubUrl(String url){return scrubPath(scrubQuery(USERINFO.matcher(url).replaceFirst("$1")));}
 static boolean secretParam(String name){String n=name.toLowerCase(Locale.ROOT).replace("_","");return n.equals("token")||n.equals("apikey")||n.equals("accesstoken")||n.equals("authtoken")||n.equals("password")||n.equals("passwd")||n.equals("pwd")||n.equals("secret")||n.equals("cookie")||n.equals("authorization")||n.equals("key");}
 static String scrubQuery(String url){int q=url.indexOf('?');if(q<0)return url;int h=url.indexOf('#',q);String query=h<0?url.substring(q+1):url.substring(q+1,h);String hash=h<0?"":url.substring(h);StringBuilder kept=new StringBuilder();for(String p:query.split("&")){if(p.isEmpty())continue;int eq=p.indexOf('=');String name=eq<0?p:p.substring(0,eq);if(secretParam(name))continue;if(kept.length()>0)kept.append('&');kept.append(p);}String base=url.substring(0,q);return kept.length()==0?base+hash:base+"?"+kept+hash;}
 static boolean mixedToken(String seg){if(seg==null||!seg.matches("[A-Za-z0-9_\\-]{12,128}"))return false;boolean d=false,l=false;for(int i=0;i<seg.length();i++){char c=seg.charAt(i);if(Character.isDigit(c))d=true;else if(Character.isLetter(c))l=true;}return d&&l;}
 /** 只去掉明显是令牌的路径段：/令牌/tvbox、/令牌/manifest.json、/d|download|dav|token/令牌，以及单独一段的令牌。 */
 static boolean dropSegment(String seg,String prev,String next){if(seg==null||!seg.matches("[A-Za-z0-9_\\-]{8,128}"))return false;if(next!=null&&(next.equalsIgnoreCase("tvbox")||next.equalsIgnoreCase("manifest.json")))return true;if(prev!=null&&(prev.equalsIgnoreCase("d")||prev.equalsIgnoreCase("download")||prev.equalsIgnoreCase("dav")||prev.equalsIgnoreCase("token"))&&mixedToken(seg))return true;return (next==null||next.isEmpty())&&(prev==null||prev.isEmpty())&&mixedToken(seg);}
 static String scrubPath(String url){int scheme=url.indexOf("://");if(scheme<0)return url;int pathStart=url.indexOf('/',scheme+3);if(pathStart<0)return url;int query=url.indexOf('?',pathStart),hash=url.indexOf('#',pathStart),end=url.length();if(query>=0)end=Math.min(end,query);if(hash>=0&&hash<end)end=hash;String[] seg=url.substring(pathStart,end).split("/",-1);StringBuilder nb=new StringBuilder();for(int i=0;i<seg.length;i++){if(i>0)nb.append('/');String next=i+1<seg.length?seg[i+1]:null,prev=i>0?seg[i-1]:null;nb.append(dropSegment(seg[i],prev,next)?"REDACTED":seg[i]);}return url.substring(0,pathStart)+nb+url.substring(end);}
 /** 含账号的备份可选密码。外壳只有盐和密文，里面仍是原来的备份 JSON。 */
 static String seal(String json,String password)throws Exception{if(password==null||password.length()<4)throw new IllegalArgumentException("密码至少 4 位");byte[] salt=new byte[16],iv=new byte[12];SecureRandom r=new SecureRandom();r.nextBytes(salt);r.nextBytes(iv);byte[] ct=cipher(Cipher.ENCRYPT_MODE,password,salt,iv,KDF_ITER).doFinal(json.getBytes(StandardCharsets.UTF_8));return new JSONObject().put("app",APP).put("format",FORMAT).put("sealed",true).put("iter",KDF_ITER).put("salt",b64(salt)).put("iv",b64(iv)).put("data",b64(ct)).toString(1);}
 static String unseal(JSONObject root,String password)throws Exception{if(password==null||password.isEmpty())throw new IllegalArgumentException("这份备份已用密码加密，请输入密码");try{byte[] salt=b64d(root.getString("salt")),iv=b64d(root.getString("iv")),ct=b64d(root.getString("data"));int iter=root.optInt("iter",KDF_ITER);if(iter<1000||iter>2_000_000)throw new IllegalArgumentException("备份的加密参数无效");return new String(cipher(Cipher.DECRYPT_MODE,password,salt,iv,iter).doFinal(ct),StandardCharsets.UTF_8);}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalArgumentException("密码不对，或备份已损坏");}}
 static Cipher cipher(int mode,String password,byte[] salt,byte[] iv,int iter)throws Exception{SecretKeyFactory f=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");byte[] key=f.generateSecret(new PBEKeySpec(password.toCharArray(),salt,iter,256)).getEncoded();Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));return c;}
 static String b64(byte[] b){return Base64.getEncoder().encodeToString(b);}
 static byte[] b64d(String s){return Base64.getDecoder().decode(s);}
 static String fileName(String version,long now){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyyMMdd-HHmm",Locale.ROOT);return "随影备份-"+f.format(new Date(now))+"-v"+version+".json";}
}
