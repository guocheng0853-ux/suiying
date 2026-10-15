package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
final class RetiredSourceRules {
 static boolean site(String key){return Arrays.asList("mp_public_ruyi","mp_public_sony","mp_public_360","mp_public_ffzy").contains(key);}
 static boolean profile(String url){return Arrays.asList("https://raw.githubusercontent.com/qist/tvbox/master/fty.json","http://home.jundie.top:81/top98.json","https://tvbox-public.gaoops.top/").contains(url);}
 static JSONObject clean(JSONObject config)throws Exception{JSONArray old=config.optJSONArray("sites");if(old==null)return config;JSONArray kept=new JSONArray();for(int i=0;i<old.length();i++){JSONObject s=old.optJSONObject(i);if(s==null||!site(s.optString("key")))kept.put(old.get(i));}config.put("sites",kept);return config;}
 static JSONArray profiles(JSONArray old)throws Exception{JSONArray kept=new JSONArray();for(int i=0;i<old.length();i++)if(!profile(old.optString(i)))kept.put(old.get(i));return kept;}
}
