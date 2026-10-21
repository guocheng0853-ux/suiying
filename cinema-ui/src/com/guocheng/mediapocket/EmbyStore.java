package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import org.json.*;import java.util.UUID;
/** 服务器、令牌和用户编号放在应用私有偏好里，和影视仓接口同一处，不写入日志。 */
final class EmbyStore {
 static final String KEY="emby.servers";
 private EmbyStore(){}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 /** 上次读到的原文。和偏好不一致就丢掉：恢复备份直接写了 emby.servers，进程还在，不能留着旧列表。 */
 static String listRaw;
 /** 把保存的原文拆成各自独立的服务器对象。改返回值不会改缓存，也不会改别一次 list() 的结果。 */
 static JSONArray copyList(String raw){JSONArray parsed;try{parsed=new JSONArray(raw==null||raw.isEmpty()?"[]":raw);}catch(Exception e){parsed=new JSONArray();}JSONArray out=new JSONArray();for(int i=0;i<parsed.length();i++){JSONObject s=parsed.optJSONObject(i);if(s==null)continue;try{out.put(new JSONObject(s.toString()));}catch(JSONException ignored){}}return out;}
 /** 用 server 替换同 id 的那条，返回新的原文。不改传入的 raw。调用方没调用这里，登录失败的半截修改不会进缓存。 */
 static String publish(String raw,JSONObject server)throws JSONException{if(server==null)return raw==null?"[]":raw;if(server.optString("id").isEmpty())server.put("id",UUID.randomUUID().toString());JSONArray all=copyList(raw),out=new JSONArray();boolean found=false;JSONObject stored=new JSONObject(server.toString());for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null)continue;if(stored.optString("id").equals(s.optString("id"))){out.put(stored);found=true;}else out.put(s);}if(!found)out.put(stored);return out.toString();}
 /** 偏好里的原文和缓存不一致时用偏好。invalidate 之后缓存是 null，下一次读也走这里。 */
 static String fresh(String cached,String disk){String onDisk=disk==null?"[]":disk;return cached!=null&&cached.equals(onDisk)?cached:onDisk;}
 /** 试播副本的地址是试过的那条，备用线路也被拿掉了。只按 id 把客户端、令牌和用户编号写回已保存的那条，没有这条就不新增。 */
 static String mergeAuth(String raw,JSONObject trial)throws JSONException{if(trial==null)return raw==null?"[]":raw;String id=trial.optString("id");if(id.isEmpty())return raw==null?"[]":raw;JSONArray all=copyList(raw),out=new JSONArray();boolean found=false;for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null)continue;if(id.equals(s.optString("id"))){if(trial.has("token"))s.put("token",trial.optString("token"));if(trial.has("userId"))s.put("userId",trial.optString("userId"));if(trial.has("client"))s.put("client",trial.optString("client"));else s.remove("client");out.put(s);found=true;}else out.put(s);}return found?out.toString():(raw==null?"[]":raw);}
 /** 直接写了 emby.servers 的地方调用。目前只有备份恢复；raw/save/delete 仍会再对一次偏好，漏调也不会把旧列表写回去。 */
 static void invalidate(){synchronized(EmbyStore.class){listRaw=null;}}
 static String raw(Context c){synchronized(EmbyStore.class){listRaw=fresh(listRaw,prefs(c).getString(KEY,"[]"));return listRaw;}}
 static JSONArray list(Context c){return copyList(raw(c));}
 static JSONObject get(Context c,String id){JSONArray all=list(c);for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&id!=null&&id.equals(s.optString("id")))return s;}return null;}
 static void save(Context c,JSONObject server)throws JSONException{synchronized(EmbyStore.class){listRaw=fresh(listRaw,prefs(c).getString(KEY,"[]"));String body=publish(listRaw,server);prefs(c).edit().putString(KEY,body).apply();listRaw=body;}}
 static void mergeAuth(Context c,JSONObject trial)throws JSONException{synchronized(EmbyStore.class){listRaw=fresh(listRaw,prefs(c).getString(KEY,"[]"));String body=mergeAuth(listRaw,trial);prefs(c).edit().putString(KEY,body).apply();listRaw=body;}}
 static void delete(Context c,String id){synchronized(EmbyStore.class){JSONArray all=copyList(fresh(listRaw,prefs(c).getString(KEY,"[]"))),out=new JSONArray();for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&(id==null||!id.equals(s.optString("id"))))out.put(s);}String body=out.toString();prefs(c).edit().putString(KEY,body).apply();listRaw=body;}}
 static String deviceId(Context c){SharedPreferences p=prefs(c);String id=p.getString("emby.deviceId","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("emby.deviceId",id).apply();}return id;}
 /** 界面和日志只用名称与主机，不带令牌和密码。 */
 static String label(JSONObject server){String name=server==null?"":server.optString("name");if(name.isEmpty())name="Emby";String host="";try{host=new java.net.URI(server.optString("url")).getHost();}catch(Exception ignored){}return host==null||host.isEmpty()?name:name+" · "+host;}
}
