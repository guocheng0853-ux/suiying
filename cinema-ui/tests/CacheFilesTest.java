package com.guocheng.mediapocket;
import java.io.File;
public final class CacheFilesTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a)throws Exception{
  File root=new File(System.getProperty("java.io.tmpdir"),"mp-cache-test-"+System.nanoTime());
  root.mkdirs();
  CacheFiles.refreshEpoch=0;
  CacheFiles files=new CacheFiles(root);
  check(files.write("cinema.catalog.movie.","[1]"),"first write");
  check(!files.write("cinema.catalog.movie.","[1]"),"identical content is not rewritten");
  check("[1]".equals(files.read("cinema.catalog.movie.")),"read back");
  long now=System.currentTimeMillis();
  check(files.fresh("cinema.catalog.movie.",now,AggregateRules.FRESH_MS),"new write is fresh");
  files.importPref("cinema.catalog.series.","[2]",1L);
  check(!files.fresh("cinema.catalog.series.",now,AggregateRules.FRESH_MS),"imported unknown time stays stale");
  files.importPref("cinema.meta.movie.old","{\"name\":\"旧\"}",now-AggregateRules.FRESH_MS-1000);
  check(!files.fresh("cinema.meta.movie.old",now,AggregateRules.FRESH_MS),"old meta is stale");
  CacheFiles.invalidateFreshness();
  check(!files.fresh("cinema.catalog.movie.",System.currentTimeMillis(),AggregateRules.FRESH_MS),"refresh epoch invalidates");
  check(files.write("cinema.catalog.movie.","[3]"),"rewrite after refresh");
  check(files.fresh("cinema.catalog.movie.",System.currentTimeMillis(),AggregateRules.FRESH_MS),"rewritten value is fresh again");
  for(int i=0;i<90;i++)files.write("cinema.meta.movie."+i,"{\"name\":"+i+"}");
  check(files.countMeta()<=CacheFiles.META_CAP,"meta cap "+files.countMeta());
  check(files.removePrefix("cinema.catalog.")>=1,"prefix removal");
  check(files.read("cinema.catalog.movie.")==null,"removed");
  long previousCap=CacheFiles.FILE_CAP_BYTES;int previousMem=CacheFiles.MEMORY_CAP_CHARS;
  CacheFiles.FILE_CAP_BYTES=Long.MAX_VALUE;
  String chunk=new String(new char[1000]).replace('\0','x');
  for(int i=0;i<6;i++){files.write("fusion.home:bulk"+i,chunk+i);files.touch("fusion.home:bulk"+i,1000L+i);}
  CacheFiles.FILE_CAP_BYTES=2500;
  check(files.trimFiles()>0,"trims non-meta files");
  check(files.countOther()<=3,"non-meta files stay under the byte cap, have "+files.countOther());
  check(files.read("fusion.home:bulk5")!=null,"newest non-meta file is kept");
  CacheFiles.FILE_CAP_BYTES=previousCap;
  CacheFiles.MEMORY_CAP_CHARS=5000;
  String big=new String(new char[3000]).replace('\0','m');
  files.write("fusion.home:mem-a",big+"a");
  files.write("fusion.home:mem-b",big+"b");
  check(CacheFiles.memory("fusion.home:mem-a")==null,"older memory entry is evicted by the byte cap");
  check((big+"b").equals(CacheFiles.memory("fusion.home:mem-b")),"newest memory entry stays");
  CacheFiles.MEMORY_CAP_CHARS=previousMem;
  check(files.removeIf(k->k.startsWith("fusion.home:"))>=1,"predicate removal");
  System.out.println("PASS: cache files write/read, skip identical rewrite, stale import, refresh epoch, meta cap, memory and file byte caps.");
 }
}
