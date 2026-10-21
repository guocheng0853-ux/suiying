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
  System.out.println("PASS: cache files write/read, skip identical rewrite, stale import, refresh epoch, meta cap.");
 }
}
