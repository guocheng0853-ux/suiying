package com.guocheng.mediapocket;
import android.app.Activity;import java.util.*;import org.json.JSONObject;
/** UI-thread owner of a single refresh; cancelled results can never launch a player. */
final class EmbyAutoRecovery {
 static final class Attempt {volatile boolean cancelled;boolean pending=true;final Runnable fallback;Attempt(Runnable fallback){this.fallback=fallback;}}
 static final Map<Activity,Attempt> ALL=new WeakHashMap<>();
 static int count(Activity a){JSONObject s=TrackSelection.stream(a);return s.optString("emby.item").equals(a.getIntent().getStringExtra(EmbyRecoveryRules.ITEM))?a.getIntent().getIntExtra(EmbyRecoveryRules.COUNT,0):0;}
 static boolean pending(Activity a){Attempt t=ALL.get(a);return t!=null&&t.pending&&!t.cancelled;}
 static void cancel(Activity a){cancel(a,false);}
 static void suspend(Activity a){cancel(a,true);}
 private static void cancel(Activity a,boolean interrupted){Attempt t=ALL.get(a);if(t!=null){int before=count(a);if(EmbyRecoveryRules.afterCancel(t.pending&&!t.cancelled,interrupted,before)<before){a.getIntent().removeExtra(EmbyRecoveryRules.COUNT);a.getIntent().removeExtra(EmbyRecoveryRules.ITEM);}t.cancelled=true;t.pending=false;}}
 static void dispose(Activity a){cancel(a);ALL.remove(a);}
 static void manual(Activity a){cancel(a);ALL.remove(a);a.getIntent().removeExtra(EmbyRecoveryRules.COUNT);a.getIntent().removeExtra(EmbyRecoveryRules.ITEM);}
 static boolean valid(Activity a,Attempt t){return t!=null&&!t.cancelled&&!a.isFinishing()&&!a.isDestroyed();}
 static boolean start(Activity a,JSONObject stream,boolean failure,boolean active,boolean blocked,String raw,Runnable fallback){
  if(pending(a))return true;
  boolean emby=!stream.optString("emby.server").isEmpty()&&!stream.optString("emby.item").isEmpty();
  if(!EmbyRecoveryRules.eligible(a.getSharedPreferences("data",0).getBoolean("autoSwitch",true),emby,failure,active,blocked,count(a),raw))return false;
  JSONObject server=EmbyStore.get(a,stream.optString("emby.server"));if(server==null)return false;
  a.getIntent().putExtra(EmbyRecoveryRules.COUNT,1).putExtra(EmbyRecoveryRules.ITEM,stream.optString("emby.item"));
  Attempt t=new Attempt(fallback);ALL.put(a,t);DebugLog.event("Emby","自动恢复：重新获取播放信息（1/1）");
  EmbyPlayback.playItem(a,server,stream.optString("emby.item"),stream.optString("emby.mediaSource"),true,t);return true;
 }
 static void failed(Activity a,Attempt t){if(!valid(a,t))return;if(!a.hasWindowFocus()){suspend(a);return;}t.pending=false;DebugLog.event("Emby","自动恢复未成功，保留手动恢复入口");if(t.fallback!=null)t.fallback.run();}
 static void complete(Activity a,Attempt t){if(t!=null)t.pending=false;}
 static void nativeTick(Activity a,boolean failure,boolean blocked,String raw){
  boolean active=Boolean.TRUE.equals(OnlineSubtitleSearch.field(a,"active"));
  if(blocked||!active){suspend(a);return;}
  start(a,TrackSelection.stream(a),failure,active,blocked,raw,null);
 }
 private EmbyAutoRecovery(){}
}
