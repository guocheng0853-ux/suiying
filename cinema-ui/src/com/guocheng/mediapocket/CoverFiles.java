package com.guocheng.mediapocket;
import java.io.*;
/** Validates disk hits and repairs corrupt entries before returning bytes. */
final class CoverFiles {
 interface Fetch {byte[] get() throws Exception;}
 interface Check {boolean valid(byte[] bytes);}
 private static final Object[] LOCKS=new Object[32];
 static {for(int i=0;i<LOCKS.length;i++)LOCKS[i]=new Object();}
 static byte[] obtain(File file,Fetch fetch,Check check)throws Exception {
  synchronized(LOCKS[(file.getAbsolutePath().hashCode()&0x7fffffff)%LOCKS.length]){
   if(file.isFile())try{byte[] hit=read(new FileInputStream(file),16*1024*1024);if(check.valid(hit)){file.setLastModified(System.currentTimeMillis());return hit;}}catch(Exception ignored){}
   if(file.exists())file.delete();
   byte[] bytes=fetch.get();if(bytes==null||!check.valid(bytes))throw new IOException("返回内容不是可解码图片");
   File dir=file.getParentFile();if(!dir.exists())dir.mkdirs();
   // A cache write failure must not hide an otherwise valid downloaded image.
   File temp=null;try{temp=File.createTempFile("poster-",".tmp",dir);try(FileOutputStream out=new FileOutputStream(temp)){out.write(bytes);}temp.renameTo(file);}catch(IOException ignored){}finally{if(temp!=null)temp.delete();}
   return bytes;
  }
 }
 static byte[] read(InputStream stream,int limit)throws Exception{try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("图片过大");out.write(b,0,n);}return out.toByteArray();}}
}
