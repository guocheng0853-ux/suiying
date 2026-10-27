package com.guocheng.mediapocket;
import android.app.Application;
/** Starts the title font on a background thread before the first activity draws. */
public final class CinemaApp extends Application {
 @Override public void onCreate(){super.onCreate();CinemaTheme.warm(this);
  // 0.1.155：以前手动切的「软解」会一直沿用到之后每次播放；升级后回到自动（硬解优先），之后手动切换只对当次有效
  try{android.content.SharedPreferences p=getSharedPreferences("data",0);if(PlaybackEndRules.resetLegacyDecode(p.getString("decodeMode","auto"),p.getBoolean("decodeSession.v1",false)))p.edit().putString("decodeMode","auto").remove("decodeTier").apply();if(!p.getBoolean("decodeSession.v1",false))p.edit().putBoolean("decodeSession.v1",true).apply();}catch(RuntimeException ignored){}}
}
