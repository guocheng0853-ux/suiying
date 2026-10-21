package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.Toast;import org.json.*;import java.io.*;import java.util.*;
/**
 * 0.1.106 备份与恢复：透明页面，只负责调起系统「保存 / 打开文件」并读写备份。
 * 用系统文件选择器，不需要存储权限；文件可以存到下载目录、网盘 App 或发给自己。
 */
public final class BackupActivity extends Activity {
 static final int EXPORT=7301,IMPORT=7302;
 boolean secrets;

 static void export(Activity a,boolean withSecrets){Intent in=new Intent(a,BackupActivity.class);in.putExtra("mode","export");in.putExtra("secrets",withSecrets);a.startActivity(in);}
 static void restore(Activity a){Intent in=new Intent(a,BackupActivity.class);in.putExtra("mode","import");a.startActivity(in);}
 /** 设置页「备份与恢复」的菜单。 */
 static void menu(Activity a){List<CinemaMenu.Item> rows=new ArrayList<>();
  rows.add(CinemaMenu.item("导出备份（含账号与令牌，换手机直接能用）",()->export(a,true)));
  rows.add(CinemaMenu.item("导出备份（不含账号与令牌，适合分享）",()->export(a,false)));
  rows.add(CinemaMenu.item("从备份恢复…",()->restore(a)));
  CinemaMenu.show(a,"备份与恢复",rows);}

 @Override protected void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null)return;
  String mode=getIntent().getStringExtra("mode");secrets=getIntent().getBooleanExtra("secrets",true);
  try{if("export".equals(mode)){Intent pick=new Intent(Intent.ACTION_CREATE_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("application/json");pick.putExtra(Intent.EXTRA_TITLE,BackupRules.fileName(version(this),System.currentTimeMillis()));startActivityForResult(pick,EXPORT);}
   else{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");startActivityForResult(pick,IMPORT);}
  }catch(Exception e){toast("这台设备没有文件选择器："+e.getClass().getSimpleName());finish();}}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("secrets",secrets);}
 @Override protected void onRestoreInstanceState(Bundle in){super.onRestoreInstanceState(in);secrets=in.getBoolean("secrets",true);}

 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  Uri uri=data==null?null:data.getData();if(result!=RESULT_OK||uri==null){finish();return;}
  if(request==EXPORT){try{writeBackup(uri);toast("备份已保存");}catch(Exception e){toast("备份失败："+reason(e));}finish();return;}
  if(request==IMPORT){try{JSONObject root=BackupRules.open(read(uri));
    new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("从备份恢复？").setMessage(BackupRules.summary(root)+"\n\n备份里的设置会覆盖手机上的同名设置，其余保留。恢复后随影会重新打开。")
     .setPositiveButton("恢复",(d,w)->{try{apply(root);toast("已恢复");restart();}catch(Exception e){toast("恢复失败："+reason(e));finish();}})
     .setNegativeButton("取消",(d,w)->finish()).setOnCancelListener(d->finish()).show();
   }catch(Exception e){toast(reason(e));finish();}}
 }

 void writeBackup(Uri uri)throws Exception{
  Map<String,JSONObject> sections=new TreeMap<>();
  for(String name:prefsFiles(this))sections.put(name,BackupRules.encode(getSharedPreferences(name,0).getAll(),secrets));
  byte[] bytes=BackupRules.wrap(version(this),System.currentTimeMillis(),secrets,sections).toString(1).getBytes("UTF-8");
  try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("无法写入所选位置");out.write(bytes);}
 }
 String read(Uri uri)throws Exception{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("无法读取文件");ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n;long total=0;while((n=in.read(buf))>0){total+=n;if(total>32L*1024*1024)throw new IOException("文件太大，不像随影备份");b.write(buf,0,n);}return b.toString("UTF-8");}}
 void apply(JSONObject root)throws Exception{
  boolean withSecrets=root.optBoolean("secrets",true);JSONObject prefs=root.getJSONObject("prefs");
  for(Iterator<String> files=prefs.keys();files.hasNext();){String name=files.next();if(!name.matches("[A-Za-z0-9._-]{1,80}"))continue;JSONObject section=prefs.optJSONObject(name);if(section==null)continue;
   SharedPreferences sp=getSharedPreferences(name,0);SharedPreferences.Editor e=sp.edit();
   for(Iterator<String> keys=section.keys();keys.hasNext();){String k=keys.next();JSONObject cell=section.optJSONObject(k);if(cell==null||BackupRules.skip(k))continue;String t=cell.optString("t");
    switch(t){
     case "s":{String v=cell.optString("v");if(!withSecrets)v=BackupRules.mergeSecrets(v,sp.getString(k,null));e.putString(k,v);break;}
     case "i":e.putInt(k,cell.optInt("v"));break;
     case "l":e.putLong(k,cell.optLong("v"));break;
     case "f":e.putFloat(k,(float)cell.optDouble("v"));break;
     case "b":e.putBoolean(k,cell.optBoolean("v"));break;
     case "ss":{JSONArray arr=cell.optJSONArray("v");Set<String> set=new HashSet<>();for(int i=0;arr!=null&&i<arr.length();i++)set.add(arr.optString(i));e.putStringSet(k,set);break;}
     default:break;}}
   if(!e.commit())throw new IOException("写入设置失败："+name);}
 }
 void restart(){Intent home=new Intent(this,ScrapedMainActivity.class);home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(home);finish();}
 static List<String> prefsFiles(Context c){List<String> out=new ArrayList<>();File dir=new File(c.getApplicationInfo().dataDir,"shared_prefs");String[] names=dir.list();if(names!=null)for(String n:names){if(!n.endsWith(".xml"))continue;String base=n.substring(0,n.length()-4);if(base.startsWith("WebView")||base.startsWith("com.google")||base.startsWith("androidx"))continue;out.add(base);}if(!out.contains("data"))out.add("data");Collections.sort(out);return out;}
 static String version(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception e){return "?";}}
 static String reason(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
 void toast(String s){Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();}
}
