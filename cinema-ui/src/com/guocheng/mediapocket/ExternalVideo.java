package com.guocheng.mediapocket;
import android.app.Activity;import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.Parcelable;import org.json.*;import java.util.*;
/**
 * 0.1.114 交给手机上的其他视频播放器（Just Player、MX Player、mpv 等）。不列随影自己。
 * 带上片名和起播位置（MX / Just Player 认 position、title），需要请求头的片源按 MX 的 headers 约定传。
 */
final class ExternalVideo {
 private ExternalVideo(){}
 static Intent view(String url,String title,long pos,JSONObject headers){Intent in=new Intent(Intent.ACTION_VIEW);in.setDataAndType(Uri.parse(url),"video/*");if(title!=null&&!title.isEmpty()){in.putExtra(Intent.EXTRA_TITLE,title);in.putExtra("title",title);}if(pos>0){in.putExtra("position",(int)Math.min(Integer.MAX_VALUE,pos));in.putExtra("from_start",false);}
  if(headers!=null&&headers.length()>0){List<String> kv=new ArrayList<>();for(Iterator<String> it=headers.keys();it.hasNext();){String k=it.next();String v=headers.optString(k);if(!k.isEmpty()&&!v.isEmpty()){kv.add(k);kv.add(v);}}if(!kv.isEmpty())in.putExtra("headers",kv.toArray(new String[0]));}
  return in;}
 static List<ResolveInfo> players(Context c,String url){List<ResolveInfo> out=new ArrayList<>();try{for(ResolveInfo r:c.getPackageManager().queryIntentActivities(view(url,null,0,null),0))if(r.activityInfo!=null&&!c.getPackageName().equals(r.activityInfo.packageName))out.add(r);}catch(Exception ignored){}return out;}
 static boolean available(Context c,String url){return !players(c,url).isEmpty();}
 static String names(Context c,String url){StringBuilder b=new StringBuilder();PackageManager pm=c.getPackageManager();for(ResolveInfo r:players(c,url)){if(b.length()>0)b.append("、");b.append(r.loadLabel(pm));if(b.length()>40)break;}return b.toString();}
 /** 打开成功返回 true；手机上没有其他播放器时返回 false。 */
 static boolean open(Activity a,String url,String title,long pos,JSONObject headers){List<ResolveInfo> list=players(a,url);if(list.isEmpty())return false;Intent in=view(url,title,pos,headers);
  try{if(list.size()==1){in.setClassName(list.get(0).activityInfo.packageName,list.get(0).activityInfo.name);a.startActivity(in);}else{Intent chooser=Intent.createChooser(in,"用其他播放器打开");chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS,new Parcelable[]{new ComponentName(a,ExternalPlayActivity.class)});a.startActivity(chooser);}DebugLog.event("外部播放器","打开 · "+LinkProbe.host(url));return true;}catch(Exception e){return false;}}
 static JSONObject headers(JSONObject stream){JSONObject hints=stream==null?null:stream.optJSONObject("behaviorHints");JSONObject proxy=hints==null?null:hints.optJSONObject("proxyHeaders");JSONObject req=proxy==null?null:proxy.optJSONObject("request");return req==null?new JSONObject():req;}
}
