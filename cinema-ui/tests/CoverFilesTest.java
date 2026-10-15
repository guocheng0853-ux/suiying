package com.guocheng.mediapocket;
import java.io.*;import java.nio.file.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class CoverFilesTest {
 static void check(boolean x,String s){if(!x)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("cover-test-").toFile();File f=new File(root,"a.img");
  CoverFiles.Check valid=b->b.length==3&&b[0]==1;byte[] image={1,2,3};AtomicInteger calls=new AtomicInteger();
  Files.write(f.toPath(),new byte[]{0});
  check(CoverFiles.obtain(f,()->{calls.incrementAndGet();return image;},valid)[0]==1,"corrupt disk hit repaired");
  check(calls.get()==1,"corrupt entry triggers one fetch");
  CoverFiles.obtain(f,()->{throw new AssertionError("valid cache fetched again");},valid);
  File bad=new File(root,"bad.img");boolean failed=false;try{CoverFiles.obtain(bad,()->new byte[]{0},valid);}catch(IOException e){failed=true;}
  check(failed&&!bad.exists(),"invalid download never cached");
  try{CoverFiles.obtain(bad,()->{throw new IOException("network down");},valid);}catch(IOException expected){}
  check(CoverFiles.obtain(bad,()->image,valid)[0]==1,"failed request does not poison later recovery");
  File shared=new File(root,"shared.img");AtomicInteger fetched=new AtomicInteger();ExecutorService io=Executors.newFixedThreadPool(6);
  java.util.List<Future<byte[]>> jobs=new java.util.ArrayList<>();for(int i=0;i<6;i++)jobs.add(io.submit(()->CoverFiles.obtain(shared,()->{fetched.incrementAndGet();return image;},valid)));
  for(Future<byte[]> job:jobs)check(job.get()[0]==1,"concurrent consumers get image");io.shutdown();check(fetched.get()==1,"one fetch for concurrent same file");
  File blocked=new File(root,"directory.img");blocked.mkdir();Files.write(new File(blocked,"child").toPath(),image);
  check(CoverFiles.obtain(blocked,()->image,valid)[0]==1,"unwritable cache cannot hide valid image");
  Files.walk(root.toPath()).sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception ignored){}});
  System.out.println("PASS: corrupt-cache repair, valid cache reuse, invalid body rejection, retry recovery, concurrent fetch coalescing, cache-write failure tolerance");
 }
}
