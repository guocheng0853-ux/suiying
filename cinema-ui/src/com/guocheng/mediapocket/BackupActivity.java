package com.guocheng.mediapocket;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.text.InputType;import android.widget.*;import org.json.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
/**
 * 0.1.106 备份与恢复：透明页面，只负责调起系统「保存 / 打开文件」并读写备份。
 * 用系统文件选择器，不需要存储权限；文件可以存到下载目录、网盘 App 或发给自己。
 * 0.1.120：默认先给不含账号的备份；含账号的导出前说明风险，可以设密码加密。读写放到后台，避免网盘文件把主线程卡住。
 */
public final class BackupActivity extends Activity {
 static final int EXPORT=7301,IMPORT=7302;
 static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"backup");t.setDaemon(true);return t;});
 boolean secrets;String password="";

 static void export(Activity a,boolean withSecrets){export(a,withSecrets,null);}
 static void export(Activity a,boolean withSecrets,String password){Intent in=new Intent(a,BackupActivity.class);in.putExtra("mode","export");in.putExtra("secrets",withSecrets);if(password!=null)in.putExtra("password",password);a.startActivity(in);}
 static void restore(Activity a){Intent in=new Intent(a,BackupActivity.class);in.putExtra("mode","import");a.startActivity(in);}
 /** 设置页「备份与恢复」的菜单。不含账号的项放在第一位。 */
 static void menu(Activity a){List<CinemaMenu.Item> rows=new ArrayList<>();
  rows.add(CinemaMenu.item("导出备份（不含账号与令牌，适合分享）",()->export(a,false,null)));
  rows.add(CinemaMenu.item("导出备份（含账号与令牌）",()->warnSecrets(a)));
  rows.add(CinemaMenu.item("从备份恢复…",()->restore(a)));
  CinemaMenu.show(a,"备份与恢复",rows);}
 static void warnSecrets(Activity a){EditText pass=new EditText(a);pass.setHint("加密密码，至少 4 位");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  new AlertDialog.Builder(a,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("含账号与令牌的备份").setMessage("文件里会有 Emby 密码、字幕密钥，以及网盘和配置地址中的令牌。不加密时是明文 JSON，任何拿到文件的人都能读取。建议设置密码，或改用不含账号的备份。").setView(pass)
   .setPositiveButton("用密码加密导出",(d,w)->{String p=pass.getText().toString().trim();if(p.length()<4){Toast.makeText(a,"密码至少 4 位，已取消导出",Toast.LENGTH_LONG).show();return;}export(a,true,p);})
   .setNeutralButton("仍导出明文",(d,w)->export(a,true,null)).setNegativeButton("取消",null).show();}

 @Override protected void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null)return;
  String mode=getIntent().getStringExtra("mode");secrets=getIntent().getBooleanExtra("secrets",false);password=getIntent().getStringExtra("password");if(password==null)password="";
  try{if("export".equals(mode)){Intent pick=new Intent(Intent.ACTION_CREATE_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("application/json");pick.putExtra(Intent.EXTRA_TITLE,BackupRules.fileName(version(this),System.currentTimeMillis()));startActivityForResult(pick,EXPORT);}
   else{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("*/*");startActivityForResult(pick,IMPORT);}
  }catch(Exception e){toast("这台设备没有文件选择器："+e.getClass().getSimpleName());finish();}}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("secrets",secrets);out.putString("password",password);}
 @Override protected void onRestoreInstanceState(Bundle in){super.onRestoreInstanceState(in);secrets=in.getBoolean("secrets",false);password=in.getString("password","");}

 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  Uri uri=data==null?null:data.getData();if(result!=RESULT_OK||uri==null){finish();return;}
  if(request==EXPORT){CinemaWait wait=CinemaWait.show(this,"正在备份","正在写入备份…",true,false);final Uri dest=uri;IO.execute(()->{try{writeBackup(dest);runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast(password.isEmpty()?"备份已保存":"加密备份已保存");finish();});}catch(Exception e){runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast("备份失败："+reason(e));finish();});}});return;}
  if(request==IMPORT){CinemaWait wait=CinemaWait.show(this,"正在读取备份","正在读取文件…",true,false);final Uri src=uri;IO.execute(()->{try{String text=read(src);runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(!isFinishing())offer(text);});}catch(Exception e){runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast(reason(e));finish();});}});}
 }
 void offer(String text){try{if(BackupRules.sealed(text)){askPassword(text);return;}confirm(BackupRules.open(text));}catch(Exception e){toast(reason(e));finish();}}
 void askPassword(String text){EditText pass=new EditText(this);pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("备份已加密").setMessage("请输入导出时设置的密码。").setView(pass)
   .setPositiveButton("解密",(d,w)->{String p=pass.getText().toString();CinemaWait wait=CinemaWait.show(this,"正在解密","正在核对密码…",true,false);IO.execute(()->{try{JSONObject root=BackupRules.open(text,p);runOnUiThread(()->{if(wait.isShowing())wait.dismiss();if(!isFinishing())confirm(root);});}catch(Exception e){runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast(reason(e));finish();});}});})
   .setNegativeButton("取消",(d,w)->finish()).setOnCancelListener(d->finish()).show();}
 void confirm(JSONObject root){new AlertDialog.Builder(this,android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("从备份恢复？").setMessage(BackupRules.summary(root)+"\n\n备份里的设置会覆盖手机上的同名设置，其余保留。恢复后随影会重新打开。")
  .setPositiveButton("恢复",(d,w)->{CinemaWait wait=CinemaWait.show(this,"正在恢复","正在写回设置…",true,false);IO.execute(()->{try{apply(root);runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast("已恢复");restart();});}catch(Exception e){runOnUiThread(()->{if(wait.isShowing())wait.dismiss();toast("恢复失败："+reason(e));finish();});}});})
  .setNegativeButton("取消",(d,w)->finish()).setOnCancelListener(d->finish()).show();}

 void writeBackup(Uri uri)throws Exception{
  Map<String,JSONObject> sections=new TreeMap<>();
  for(String name:prefsFiles(this))sections.put(name,BackupRules.encode(getSharedPreferences(name,0).getAll(),secrets));
  String body=BackupRules.wrap(version(this),System.currentTimeMillis(),secrets,sections).toString(1);if(!password.isEmpty())body=BackupRules.seal(body,password);
  byte[] bytes=body.getBytes("UTF-8");
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
 static List<String> prefsFiles(Context c){List<String> out=new ArrayList<>();File dir=new File(c.getApplicationInfo().dataDir,"shared_prefs");String[] names=dir.list();if(names!=null)for(String n:names){if(!n.endsWith(".xml"))continue;String base=n.substring(0,n.length()-4);if(base.startsWith("WebView")||base.startsWith("com.google")||base.startsWith("androidx")||base.equals("offline.progress"))continue;out.add(base);}if(!out.contains("data"))out.add("data");Collections.sort(out);return out;}
 static String version(Context c){try{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName;}catch(Exception e){return "?";}}
 static String reason(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
 void toast(String s){Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();}
}
