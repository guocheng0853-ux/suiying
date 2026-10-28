package com.guocheng.mediapocket;
import android.app.Application;
/** Starts the title font on a background thread before the first activity draws. */
public final class CinemaApp extends Application {
 @Override public void onCreate(){super.onCreate();CinemaTheme.warm(this);
  // 0.1.155：以前手动切的「软解」会一直沿用到之后每次播放；升级后回到自动（硬解优先），之后手动切换只对当次有效
  try{android.content.SharedPreferences p=getSharedPreferences("data",0);String[] unwind=PlaybackEndRules.unwindDecodeSession(p.getBoolean("decodeSession.open",false),p.getString("decodeSession.prevMode",""),p.getString("decodeSession.prevTier",""));String mode=unwind!=null?(unwind[0].isEmpty()?"auto":unwind[0]):p.getString("decodeMode","auto");android.content.SharedPreferences.Editor e=p.edit();boolean dirty=false;if(unwind!=null){if(unwind[0].isEmpty())e.remove("decodeMode");else e.putString("decodeMode",unwind[0]);if(unwind[1].isEmpty())e.remove("decodeTier");else e.putString("decodeTier",unwind[1]);e.remove("decodeSession.open").remove("decodeSession.prevMode").remove("decodeSession.prevTier");dirty=true;}if(PlaybackEndRules.resetLegacyDecode(mode,p.getBoolean("decodeSession.v1",false))){e.putString("decodeMode","auto").remove("decodeTier");dirty=true;}if(!p.getBoolean("decodeSession.v1",false)){e.putBoolean("decodeSession.v1",true);dirty=true;}if(dirty)e.apply();}catch(RuntimeException ignored){}}
}
