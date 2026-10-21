package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import org.json.*;import java.util.UUID;
/** 服务器、令牌和用户编号放在应用私有偏好里，和影视仓接口同一处，不写入日志。 */
final class EmbyStore {
 static final String KEY="emby.servers";
 private EmbyStore(){}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 /** 上次成功写入的原文。list/get 每次都解析出新对象，调用方改字段不会碰到别人手里的那份。 */
 static String listRaw;
 /** 把保存的原文拆成各自独立的服务器对象。改返回值不会改缓存，也不会改别一次 list() 的结果。 */
 static JSONArray copyList(String raw){JSONArray parsed;try{parsed=new JSONArray(raw==null||raw.isEmpty()?"[]":raw);}catch(Exception e){parsed=new JSONArray();}JSONArray out=new JSONArray();for(int i=0;i<parsed.length();i++){JSONObject s=parsed.optJSONObject(i);if(s==null)continue;try{out.put(new JSONObject(s.toString()));}catch(JSONException ignored){}}return out;}
 /** 用 server 替换同 id 的那条，返回新的原文。不改传入的 raw。调用方没调用这里，登录失败的半截修改不会进缓存。 */
 static String publish(String raw,JSONObject server)throws JSONException{if(server==null)return raw==null?"[]":raw;if(server.optString("id").isEmpty())server.put("id",UUID.randomUUID().toString());JSONArray all=copyList(raw),out=new JSONArray();boolean found=false;JSONObject stored=new JSONObject(server.toString());for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null)continue;if(stored.optString("id").equals(s.optString("id"))){out.put(stored);found=true;}else out.put(s);}if(!found)out.put(stored);return out.toString();}
 static String raw(Context c){synchronized(EmbyStore.class){if(listRaw!=null)return listRaw;}String disk=prefs(c).getString(KEY,"[]");synchronized(EmbyStore.class){if(listRaw==null)listRaw=disk;return listRaw;}}
 static JSONArray list(Context c){return copyList(raw(c));}
 static JSONObject get(Context c,String id){JSONArray all=list(c);for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&id!=null&&id.equals(s.optString("id")))return s;}return null;}
 static void save(Context c,JSONObject server)throws JSONException{synchronized(EmbyStore.class){String body=publish(listRaw!=null?listRaw:prefs(c).getString(KEY,"[]"),server);prefs(c).edit().putString(KEY,body).apply();listRaw=body;}}
 static void delete(Context c,String id){synchronized(EmbyStore.class){JSONArray all=copyList(listRaw!=null?listRaw:prefs(c).getString(KEY,"[]")),out=new JSONArray();for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&(id==null||!id.equals(s.optString("id"))))out.put(s);}String body=out.toString();prefs(c).edit().putString(KEY,body).apply();listRaw=body;}}
 static String deviceId(Context c){SharedPreferences p=prefs(c);String id=p.getString("emby.deviceId","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("emby.deviceId",id).apply();}return id;}
 /** 界面和日志只用名称与主机，不带令牌和密码。 */
 static String label(JSONObject server){String name=server==null?"":server.optString("name");if(name.isEmpty())name="Emby";String host="";try{host=new java.net.URI(server.optString("url")).getHost();}catch(Exception ignored){}return host==null||host.isEmpty()?name:name+" · "+host;}
}
