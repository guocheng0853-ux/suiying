package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import org.json.*;import java.util.UUID;
/** 服务器、令牌和用户编号放在应用私有偏好里，和影视仓接口同一处，不写入日志。 */
final class EmbyStore {
 static final String KEY="emby.servers";
 private EmbyStore(){}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 static String listRaw;static JSONArray listCache;
 static JSONArray list(Context c){String raw=prefs(c).getString(KEY,"[]");synchronized(EmbyStore.class){if(raw.equals(listRaw)&&listCache!=null)return listCache;try{listCache=new JSONArray(raw);}catch(Exception e){listCache=new JSONArray();}listRaw=raw;return listCache;}}
 static JSONObject get(Context c,String id){JSONArray all=list(c);for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&id!=null&&id.equals(s.optString("id")))return s;}return null;}
 static void save(Context c,JSONObject server)throws JSONException{
  if(server.optString("id").isEmpty())server.put("id",UUID.randomUUID().toString());
  JSONArray all=list(c),out=new JSONArray();boolean found=false;
  for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s==null)continue;if(server.optString("id").equals(s.optString("id"))){out.put(server);found=true;}else out.put(s);}
  if(!found)out.put(server);prefs(c).edit().putString(KEY,out.toString()).apply();synchronized(EmbyStore.class){listRaw=null;}
 }
 static void delete(Context c,String id){JSONArray all=list(c),out=new JSONArray();for(int i=0;i<all.length();i++){JSONObject s=all.optJSONObject(i);if(s!=null&&!id.equals(s.optString("id")))out.put(s);}prefs(c).edit().putString(KEY,out.toString()).apply();synchronized(EmbyStore.class){listRaw=null;}}
 static String deviceId(Context c){SharedPreferences p=prefs(c);String id=p.getString("emby.deviceId","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("emby.deviceId",id).apply();}return id;}
 /** 界面和日志只用名称与主机，不带令牌和密码。 */
 static String label(JSONObject server){String name=server==null?"":server.optString("name");if(name.isEmpty())name="Emby";String host="";try{host=new java.net.URI(server.optString("url")).getHost();}catch(Exception ignored){}return host==null||host.isEmpty()?name:name+" · "+host;}
}
