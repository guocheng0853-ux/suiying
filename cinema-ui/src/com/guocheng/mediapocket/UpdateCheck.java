package com.guocheng.mediapocket;
import android.app.Activity;import android.content.*;import android.net.Uri;import android.widget.Toast;import org.json.*;import java.io.*;import java.net.*;
/**
 * 0.1.106 检查更新：查 GitHub 仓库最新的正式发布（Releases），版本比手机上的新就提示，点「下载」用浏览器下载安装包。
 * 打开 App 时最多每 12 小时自动查一次（静默，失败不打扰）；「我的 → 检查更新」可手动查。
 */
final class UpdateCheck {
 private UpdateCheck(){}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("data",0);}
 static void auto(Activity a){try{if(!prefs(a).getBoolean("update.auto",true))return;long last=prefs(a).getLong("update.lastCheck",0),now=System.currentTimeMillis();if(!FollowRules.due(last,now,12))return;prefs(a).edit().putLong("update.lastCheck",now).apply();run(a,false);}catch(Exception ignored){}}
 static void manual(Activity a){Toast.makeText(a,"正在检查更新…",Toast.LENGTH_SHORT).show();run(a,true);}
 static void run(Activity a,boolean loud){new Thread(()->{String[] rel=null;String error=null;try{rel=UpdateRules.release(new JSONObject(get(UpdateRules.LATEST)));}catch(FileNotFoundException e){error="仓库还没有发布正式版本";}catch(Exception e){error="检查失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
  final String[] r=rel;final String err=error;a.runOnUiThread(()->{if(a.isFinishing())return;String local=BackupActivity.version(a);
   if(r==null){if(loud)Toast.makeText(a,err==null?"没有找到可用的发布版本":err,Toast.LENGTH_LONG).show();return;}
   if(!UpdateRules.newer(r[0],local)){if(loud)Toast.makeText(a,"已经是最新版本（"+local+"）",Toast.LENGTH_LONG).show();return;}
   if(!loud&&r[0].equals(prefs(a).getString("update.skip","")))return;
   String msg="当前 "+local+" → 新版 "+r[0]+(r[3].isEmpty()?"":"（"+r[3]+"）")+(r[2].isEmpty()?"":"\n\n"+r[2])+"\n\n下载后点开安装包即可覆盖安装，设置和记录都会保留。";
   new CinemaDialog(a).setTitle("发现新版本").setMessage(msg).setPositiveButton("下载",(d,w)->open(a,r[1])).setNeutralButton("跳过这个版本",(d,w)->prefs(a).edit().putString("update.skip",r[0]).apply()).setNegativeButton("以后再说",null).show();});},"update-check").start();}
 static void open(Activity a,String url){try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(Exception e){Toast.makeText(a,"没有可以打开链接的浏览器",Toast.LENGTH_LONG).show();}}
 static String get(String url)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("Accept","application/vnd.github+json");c.setRequestProperty("User-Agent","suiying-update-check");
  try{int code=c.getResponseCode();if(code==404)throw new FileNotFoundException();if(code==403)throw new IOException("GitHub 暂时限制了查询次数，稍后再试");if(code>=400)throw new IOException("HTTP "+code);
   try(InputStream in=c.getInputStream()){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0&&b.size()<2_000_000)b.write(buf,0,n);return b.toString("UTF-8");}}finally{c.disconnect();}}
}
