package com.guocheng.mediapocket;
public final class PosterSampleTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  check(PosterSample.sampleSize(0,100)==1&&PosterSample.sampleSize(100,0)==1,"invalid");
  check(PosterSample.sampleSize(100,200)==1,"source narrower than target");
  check(PosterSample.sampleSize(1000,100)==8,"1000/8=125 still covers 100, /16 does not");
  check(PosterSample.sampleSize(400,100)==4,"400/4=100 still covers the target");
  check(PosterSample.cacheBytes(0)==8*1024*1024,"empty heap floor");
  check(PosterSample.cacheBytes(16L*1024*1024)==8*1024*1024,"small heap floor");
  check(PosterSample.cacheBytes(80L*1024*1024)==10*1024*1024,"eighth of heap");
  check(PosterSample.backdropWidth(1080)==1080&&PosterSample.backdropWidth(0)==1,"backdrop is one screen wide");
  System.out.println("PASS: poster sample size, cache budget, backdrop width.");
 }
}
