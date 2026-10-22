package com.guocheng.mediapocket;
public final class BifCacheTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  BifCache cache=new BifCache();
  BifIndex one=BifIndex.parse(sample(1)),two=BifIndex.parse(sample(2)),three=BifIndex.parse(sample(3)),four=BifIndex.parse(sample(4));
  cache.put("a",one);cache.put("b",two);cache.put("c",three);
  check(cache.size()==BifCache.CAP,"only two BIF indexes stay in memory");
  check(!cache.contains("a")&&cache.get("a")==null,"oldest entry is evicted");
  check(cache.get("b")==two,"touched entry stays");
  cache.put("d",four);
  check(cache.get("c")==null,"least recently used entry leaves when a fourth arrives");
  check(cache.get("b")==two&&cache.get("d")==four,"the two newest remain");
  cache.clear();
  check(cache.size()==0&&cache.get("d")==null,"player exit drops the cache");
  System.out.println("PASS: BIF memory cache keeps two entries and clears on exit.");
 }
 static byte[] sample(int mark){int count=1,mult=1000;byte[] jpeg=new byte[]{(byte)0xff,(byte)0xd8,(byte)mark,(byte)0xff,(byte)0xd9};int body=64+16;byte[] file=new byte[body+jpeg.length];System.arraycopy(BifIndex.MAGIC,0,file,0,BifIndex.MAGIC.length);put(file,12,count);put(file,16,mult);put(file,64,0);put(file,68,body);put(file,72,0xffffffff);put(file,76,body+jpeg.length);System.arraycopy(jpeg,0,file,body,jpeg.length);return file;}
 static void put(byte[] d,int at,int v){d[at]=(byte)v;d[at+1]=(byte)(v>>>8);d[at+2]=(byte)(v>>>16);d[at+3]=(byte)(v>>>24);}
}
