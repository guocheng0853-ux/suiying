package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import java.io.*;
/** 外挂字幕落在 filesDir/subtitles/{片名}/ ，并记住每片选择。 */
final class ExternalSubtitleStore {
 private ExternalSubtitleStore(){}
 static File directory(Context c,String title){File dir=new File(new File(c.getFilesDir(),"subtitles"),OnlineSubtitleRules.folder(title));if(!dir.isDirectory()&&!dir.mkdirs())return c.getFilesDir();return dir;}
 static File save(Context c,String title,String filename,byte[] bytes)throws IOException{File dir=directory(c,title);File file=new File(dir,OnlineSubtitleRules.file(filename));File tmp=new File(dir,file.getName()+".part");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);out.getFD().sync();}if(file.exists()&&!file.delete())throw new IOException("无法替换已有字幕");if(!tmp.renameTo(file))throw new IOException("无法保存字幕");return file;}
 static boolean isLocal(String path){return path!=null&&(path.startsWith("/")||path.startsWith("file:"));}
 static byte[] read(String path)throws IOException{File file=new File(path.startsWith("file:")?path.substring(5):path);if(!file.isFile())throw new IOException("找不到已保存的字幕");if(file.length()>2*1024*1024)throw new IOException("字幕文件过大");try(FileInputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 static String choiceKey(String id){return "subtitle.choice:"+(id==null?"":id);}
 static void remember(SharedPreferences p,String id,String path,String choice){if(p==null||id==null)return;p.edit().putString("subtitleUrl:"+id,path==null?"":path).putString(choiceKey(id),choice==null?"":choice).putBoolean("subtitleDisabled:"+id,false).apply();}
 static String remembered(SharedPreferences p,String id){return p==null?"":p.getString("subtitleUrl:"+(id==null?"":id),"");}
}
