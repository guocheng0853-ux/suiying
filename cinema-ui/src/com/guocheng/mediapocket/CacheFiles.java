package com.guocheng.mediapocket;
import android.content.Context;import android.content.SharedPreferences;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
/**
 * 0.1.130：目录、首页、影片资料和大图缓存改存单独文件，不再塞进 SharedPreferences「data」。
 * 写入先落 .tmp 再改名。cinema.meta.* 最多留 {@link #META_CAP} 条。内容没变就不重写。
 */
final class CacheFiles {
 static final int META_CAP=80;
 static final String META_PREFIX="cinema.meta.";
 static volatile long refreshEpoch;
 static volatile boolean migrated;
 static CacheFiles shared;
 final File root,indexFile;
 final JSONObject index=new JSONObject();
 /** 内存里的 JSON 文本大约最多 512 KB。单条更大的只留在文件里。 */
 static int MEMORY_CAP_CHARS=512*1024;
 /** 影片资料以外的缓存文件一共大约最多 8 MB，按写入时间丢掉最旧的。 */
 static long FILE_CAP_BYTES=8L*1024*1024;
 static long memoryChars;
 static final Map<String,String> MEMORY=Collections.synchronizedMap(new LinkedHashMap<String,String>(64,.75f,true));
 static final Map<String,JSONObject> PARSED=Collections.synchronizedMap(new LinkedHashMap<String,JSONObject>(64,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,JSONObject> e){return size()>META_CAP;}});
 CacheFiles(File root){this.root=root;this.indexFile=new File(root,"index.json");root.mkdirs();load();}
 static void invalidateFreshness(){refreshEpoch=System.currentTimeMillis();}
 static String memory(String key){return key==null?null:MEMORY.get(key);}
 static JSONObject parsed(String key){return key==null?null:PARSED.get(key);}
 static boolean isBlob(String key){return key!=null&&(key.startsWith("cinema.catalog.")||key.startsWith("fusion.catalog")||key.startsWith("fusion.home:")||key.startsWith(META_PREFIX)||key.startsWith("cinema.hero"));}
 static CacheFiles of(Context c){File dir=new File(c.getApplicationContext().getCacheDir(),"mp-json-v1");synchronized(CacheFiles.class){if(shared==null||!shared.root.equals(dir))shared=new CacheFiles(dir);return shared;}}
 synchronized String read(String key){JSONObject e=index.optJSONObject(key);if(e==null)return null;File f=new File(root,e.optString("h")+".json");if(!f.isFile())return null;try{return new String(java.nio.file.Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);}catch(Exception ex){return null;}}
 synchronized long time(String key){JSONObject e=index.optJSONObject(key);return e==null?0:e.optLong("t");}
 synchronized boolean fresh(String key,long now,long maxAge){long t=time(key);return t>1&&t>=refreshEpoch&&now-t<maxAge;}
 synchronized boolean write(String key,String value){if(key==null||value==null)return false;if(value.equals(MEMORY.get(key))){remember(key,value);return false;}String current=read(key);if(value.equals(current)){remember(key,value);return false;}
  String h=hash(key);File dest=new File(root,h+".json"),tmp=new File(root,h+".tmp");try{java.nio.file.Files.write(tmp.toPath(),value.getBytes(StandardCharsets.UTF_8));if(dest.exists()&&!dest.delete()){tmp.delete();return false;}if(!tmp.renameTo(dest)){tmp.delete();return false;}
   index.put(key,new JSONObject().put("h",h).put("t",System.currentTimeMillis()));if(key.startsWith(META_PREFIX))trimMeta();else trimFiles();save();remember(key,value);return true;}catch(Exception ex){tmp.delete();return false;}}
 synchronized void importPref(String key,String value,long oldTime){write(key,value);touch(key,oldTime>0?oldTime:1L);}
 synchronized void touch(String key,long time){JSONObject e=index.optJSONObject(key);if(e==null)return;try{e.put("t",time);save();}catch(JSONException ignored){}}
 synchronized void remove(String key){JSONObject e=index.optJSONObject(key);if(e==null)return;new File(root,e.optString("h")+".json").delete();index.remove(key);forget(key);save();}
 synchronized int removePrefix(String prefix){if(prefix==null)return 0;List<String> drop=new ArrayList<>();for(Iterator<String> it=index.keys();it.hasNext();){String k=it.next();if(k.startsWith(prefix))drop.add(k);}for(String k:drop)remove(k);return drop.size();}
 synchronized int trimMeta(){List<String> keys=new ArrayList<>();for(Iterator<String> it=index.keys();it.hasNext();){String k=it.next();if(k.startsWith(META_PREFIX))keys.add(k);}if(keys.size()<=META_CAP)return 0;
  keys.sort(Comparator.comparingLong(this::time));int removed=0;for(int i=0;i<keys.size()-META_CAP;i++){remove(keys.get(i));removed++;}return removed;}
 synchronized int countMeta(){int n=0;for(Iterator<String> it=index.keys();it.hasNext();)if(it.next().startsWith(META_PREFIX))n++;return n;}
 synchronized int countOther(){int n=0;for(Iterator<String> it=index.keys();it.hasNext();)if(!it.next().startsWith(META_PREFIX))n++;return n;}
 synchronized int removeIf(java.util.function.Predicate<String> pred){if(pred==null)return 0;List<String> drop=new ArrayList<>();for(Iterator<String> it=index.keys();it.hasNext();){String k=it.next();if(pred.test(k))drop.add(k);}for(String k:drop)remove(k);return drop.size();}
 synchronized int trimFiles(){List<String> keys=new ArrayList<>();long total=0;for(Iterator<String> it=index.keys();it.hasNext();){String k=it.next();if(k.startsWith(META_PREFIX))continue;keys.add(k);total+=bytes(k);}if(total<=FILE_CAP_BYTES)return 0;keys.sort(Comparator.comparingLong(this::time));int removed=0;for(String k:keys){if(total<=FILE_CAP_BYTES)break;long n=bytes(k);remove(k);total-=n;removed++;}return removed;}
 long bytes(String key){JSONObject e=index.optJSONObject(key);if(e==null)return 0;File f=new File(root,e.optString("h")+".json");return f.isFile()?f.length():0;}
 String readMigrating(Context c,String key){String mem=MEMORY.get(key);if(mem!=null)return mem;String hit;synchronized(this){hit=read(key);}if(hit!=null){remember(key,hit);return hit;}SharedPreferences p=c.getSharedPreferences("data",0);String old=p.getString(key,"");if(old==null||old.isEmpty())return null;long t=0;try{t=p.getLong(key+".time",0);}catch(Exception ignored){}importPref(key,old,t);p.edit().remove(key).remove(key+".time").apply();return old;}
 void migrateAll(SharedPreferences p){if(p==null||migrated)return;Map<String,?> all;try{all=p.getAll();}catch(Exception e){return;}SharedPreferences.Editor ed=p.edit();boolean dirty=false;
  for(Map.Entry<String,?> e:all.entrySet()){String k=e.getKey();if(!isBlob(k)||!(e.getValue() instanceof String))continue;long t=0;Object stamp=all.get(k+".time");if(stamp instanceof Long)t=(Long)stamp;else if(stamp instanceof Integer)t=((Integer)stamp).longValue();
   synchronized(this){if(read(k)==null)importPref(k,(String)e.getValue(),t);}ed.remove(k);if(stamp!=null)ed.remove(k+".time");dirty=true;}
  if(dirty)ed.apply();migrated=true;}
 void load(){if(!indexFile.isFile())return;try{JSONObject saved=new JSONObject(new String(java.nio.file.Files.readAllBytes(indexFile.toPath()),StandardCharsets.UTF_8));for(Iterator<String> it=saved.keys();it.hasNext();){String k=it.next();index.put(k,saved.opt(k));}}catch(Exception ignored){}}
 void save(){File tmp=new File(indexFile.getPath()+".tmp");try{java.nio.file.Files.write(tmp.toPath(),index.toString().getBytes(StandardCharsets.UTF_8));if(indexFile.exists()&&!indexFile.delete()){tmp.delete();return;}if(!tmp.renameTo(indexFile))tmp.delete();}catch(Exception e){tmp.delete();}}
 static void forget(String key){if(key==null)return;synchronized(MEMORY){String prev=MEMORY.remove(key);if(prev!=null)memoryChars-=prev.length();PARSED.remove(key);}}
 static void remember(String key,String value){if(key==null||value==null)return;JSONObject obj=null;if(value.length()<=65536)try{obj=new JSONObject(value);}catch(Exception ignored){}synchronized(MEMORY){if(value.length()>MEMORY_CAP_CHARS){String prev=MEMORY.remove(key);if(prev!=null)memoryChars-=prev.length();PARSED.remove(key);return;}String prev=MEMORY.remove(key);if(prev!=null)memoryChars-=prev.length();MEMORY.put(key,value);memoryChars+=value.length();if(obj!=null)PARSED.put(key,obj);else PARSED.remove(key);while(memoryChars>MEMORY_CAP_CHARS&&!MEMORY.isEmpty()){Iterator<String> it=MEMORY.keySet().iterator();if(!it.hasNext())break;String eldest=it.next();String dropped=MEMORY.remove(eldest);if(dropped!=null)memoryChars-=dropped.length();PARSED.remove(eldest);}}}
 static String hash(String s){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
}
