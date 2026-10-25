package com.guocheng.mediapocket;
import java.util.Locale;
/** One automatic refresh per playback chain. Manual retry starts a new chain. */
final class EmbyRecoveryRules {
 static final String COUNT="mp.emby.recovery.count",ITEM="mp.emby.recovery.item";
 static boolean eligible(boolean enabled,boolean emby,boolean failure,boolean active,boolean blocked,int count,String raw){
  if(!enabled||!emby||!failure||!active||blocked||count>=1)return false;
  String s=raw==null?"":raw.toLowerCase(Locale.ROOT);
  for(String permanent:new String[]{"http 401","http 403","http 409","用户名","密码","权限","未授权","格式不支持","解码器不支持","unsupported"})if(s.contains(permanent))return false;
  return true;
 }
 static int carry(boolean replace,String previous,String next,int count){return replace&&previous!=null&&previous.equals(next)?Math.min(1,Math.max(0,count)):0;}
 static int resume(int current,long retained,long initial){return (int)Math.min(Integer.MAX_VALUE,current>0?current:retained>0?retained:Math.max(0,initial));}
 private EmbyRecoveryRules(){}
}
