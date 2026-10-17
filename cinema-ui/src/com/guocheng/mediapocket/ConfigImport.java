package com.guocheng.mediapocket;
import android.app.Activity;import android.content.*;import org.json.*;
/** suiying://import 走到影视仓配置：写入 tvbox.profiles 与 tvbox.active。 */
final class ConfigImport {
 private ConfigImport(){}
 static String consume(Activity a){if(a==null||a.getIntent()==null||a.getIntent().getData()==null)return null;String url=ExternalPlayRules.importUrl(a.getIntent().getData().toString());if(url==null)return null;apply(a.getSharedPreferences("data",0),url);a.getIntent().setData(null);return url;}
 static void apply(SharedPreferences prefs,String url){try{JSONArray profiles=ExternalPlayRules.addProfile(new JSONArray(prefs.getString("tvbox.profiles","[]")),url);prefs.edit().putString("tvbox.profiles",profiles.toString()).putString("tvbox.active",url).putString("fusion.homeProvider","tvbox").apply();}catch(Exception e){prefs.edit().putString("tvbox.active",url).putString("fusion.homeProvider","tvbox").apply();}}
}
