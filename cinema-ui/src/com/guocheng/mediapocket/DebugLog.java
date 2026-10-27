package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.os.Build;import java.io.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;
/**
 * 0.1.66 调试信息：崩溃堆栈与运行事件写入应用私有目录（各保留最近 96KB），
 * 设置 → 故障排查 → 调试信息 可查看、复制、分享。所有网址只保留主机名，不带路径和参数（避免带出个人令牌）。
 */
final class DebugLog {
 static final long LIMIT=96*1024;
 static volatile File dir;static volatile boolean installed;
 private DebugLog(){}
 static synchronized void install(Context c){
  try{if(dir==null){dir=new File(c.getApplicationContext().getFilesDir(),"debug");dir.mkdirs();}
   if(installed)return;installed=true;
   final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
   Thread.setDefaultUncaughtExceptionHandler((t,e)->{try{StringWriter w=new StringWriter();e.printStackTrace(new PrintWriter(w));write("crash.log","=== 崩溃 "+stamp()+" · 线程 "+t.getName()+" · "+version(c)+"\n"+w+"\n");}catch(Throwable ignored){}if(previous!=null)previous.uncaughtException(t,e);});
  }catch(Throwable ignored){}
 }
 static String stamp(){return new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date());}
 /** 运行事件：一行一条，相同内容连续出现只记一次。 */
 static String last="";
 static synchronized void event(String tag,String text){if(dir==null||text==null||text.trim().isEmpty())return;String line=tag+" · "+sanitize(text).replace('\n',' ').trim();if(line.equals(last))return;last=line;write("events.log",stamp()+"  "+line+"\n");}
 /** 网址只留主机名；去掉常见令牌参数。 */
 static String sanitize(String s){return DebugText.sanitize(s);}
 static synchronized void write(String name,String text){try{File f=new File(dir,name);if(f.length()>LIMIT){byte[] all=read(f);int keep=(int)(LIMIT/2);byte[] tail=Arrays.copyOfRange(all,Math.max(0,all.length-keep),all.length);try(FileOutputStream o=new FileOutputStream(f,false)){o.write(tail);}}try(FileOutputStream o=new FileOutputStream(f,true)){o.write(text.getBytes(StandardCharsets.UTF_8));}}catch(Throwable ignored){}}
 static byte[] read(File f)throws IOException{try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 static String tail(String name,int chars){try{File f=new File(dir,name);if(!f.exists())return "（无）";String s=new String(read(f),StandardCharsets.UTF_8);return s.length()>chars?"…"+s.substring(s.length()-chars):s;}catch(Throwable e){return "（读取失败）";}}
 static String version(Context c){try{android.content.pm.PackageInfo p=c.getPackageManager().getPackageInfo(c.getPackageName(),0);return p.versionName+" ("+p.versionCode+")";}catch(Throwable e){return "?";}}
 static String report(Context c){
  install(c);SharedPreferences p=c.getSharedPreferences("data",0);StringBuilder s=new StringBuilder();
  s.append("随影调试信息 · ").append(stamp()).append("\n版本：").append(version(c)).append("\n设备：").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
  s.append("开关：多线程加速=").append(p.getBoolean("accelerate.v1",true)).append(" · P2P 加速=").append(p.getBoolean("torrentBoost.v1",true)).append(" · 比例=").append(p.getString("aspectMode","smart")).append(" · 倍速=").append(p.getFloat("playbackSpeed",1f)).append(" · 字幕偏好=").append(p.getString("subtitleLanguage","zho")).append(" · 自动换源=").append(p.getBoolean("autoSwitch",true)).append("\n");
  s.append("首页来源：").append(p.getString("fusion.homeProvider","?")).append(" · 影视仓接口已配置=").append(!p.getString("tvbox.active","").isEmpty()).append("\n\n");
  s.append("【崩溃记录】\n").append(tail("crash.log",12000)).append("\n\n【运行日志（最近）】\n").append(tail("events.log",16000)).append("\n\n【播放地址检查】\n").append(PlaybackCheck.report(c)).append("\n\n【字幕诊断】\n").append(sanitize(p.getString("subtitleReport","（无）")));
  return s.toString();
 }
 static void show(Activity a){
  String text=report(a);android.widget.TextView v=new android.widget.TextView(a);v.setText(text);v.setTextSize(11);v.setTextIsSelectable(true);int pad=Math.round(16*a.getResources().getDisplayMetrics().density);v.setPadding(pad,pad/2,pad,pad/2);android.widget.ScrollView sv=new android.widget.ScrollView(a);sv.addView(v);
  new AlertDialog.Builder(a).setTitle("调试信息").setView(sv)
   .setPositiveButton("分享",(d,n)->{try{Intent i=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"随影调试信息").putExtra(Intent.EXTRA_TEXT,text);a.startActivity(Intent.createChooser(i,"发送调试信息"));}catch(Exception ignored){}})
   .setNeutralButton("复制",(d,n)->{try{((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("随影调试信息",text));android.widget.Toast.makeText(a,"已复制",android.widget.Toast.LENGTH_SHORT).show();}catch(Exception ignored){}})
   .setNegativeButton("清空",(d,n)->{try{new File(dir,"crash.log").delete();new File(dir,"events.log").delete();last="";}catch(Exception ignored){}}).show();
 }
}
