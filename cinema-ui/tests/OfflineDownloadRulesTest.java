package com.guocheng.mediapocket;
public class OfflineDownloadRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(OfflineRules.classify(200).equals("ok")&&OfflineRules.classify(206).equals("ok"),"ok");
  check(OfflineRules.classify(403).equals("refresh")&&OfflineRules.classify(401).equals("refresh")&&OfflineRules.classify(404).equals("refresh")&&OfflineRules.classify(410).equals("refresh"),"expired link → refresh");
  check(OfflineRules.classify(503).equals("retry")&&OfflineRules.classify(429).equals("retry")&&OfflineRules.classify(408).equals("retry"),"transient → retry");
  check(OfflineRules.classify(400).equals("fail")&&OfflineRules.classify(405).equals("fail"),"other → fail");
  check(OfflineRules.totalFromContentRange("bytes 100-199/1000")==1000&&OfflineRules.totalFromContentRange("bytes */5000")==5000&&OfflineRules.totalFromContentRange("bytes 0-1/*")==-1&&OfflineRules.totalFromContentRange(null)==-1,"content-range");
  check(OfflineRules.backoff(0)==2000&&OfflineRules.backoff(1)==4000&&OfflineRules.backoff(3)==16000&&OfflineRules.backoff(4)==30000&&OfflineRules.backoff(9)==30000,"backoff");
  check(OfflineRules.looksLikePage("text/html; charset=utf-8",0)&&!OfflineRules.looksLikePage("video/x-matroska",0)&&!OfflineRules.looksLikePage("text/html",100)&&!OfflineRules.looksLikePage(null,0),"error page detection");
  check(OfflineRules.state("running",50,200,1024,"").startsWith("下载中 25%")&&OfflineRules.state("running",50,200,1024,"").contains("1 KB/s"),"running line");
  check(OfflineRules.state("failed",0,0,0,"HTTP 403，重新获取地址后仍被拒绝").contains("HTTP 403")&&OfflineRules.state("failed",0,0,0,"").contains("点一下重试"),"failed line");
  check(OfflineRules.state("paused",10,100,0,"").contains("10%")&&OfflineRules.state("done",0,2048,0,"").startsWith("已下载 · 2 KB")&&OfflineRules.state("queued",0,-1,0,"").startsWith("排队"),"other lines");
  check(!OfflineProgress.due(1000,5999)&&OfflineProgress.due(1000,6000)&&OfflineProgress.due(-1,0)&&OfflineProgress.FLUSH_MS==5000,"progress flush about every 5 seconds");
  check(OfflineRules.sameResource("http://cdn.example/a.mp4?token=old","http://cdn.example/a.mp4?token=new"),"a new query on the same file is still that file");
  check(!OfflineRules.sameResource("http://cdn.example/routeB.mp4?token=1","http://cdn.example/routeA.mp4?token=2"),"two lines are different files");
  check(OfflineRules.resumeOffset(true,false,206,400,-1,-1)==400,"an ordinary range resume keeps the bytes already stored");
  check(OfflineRules.resumeOffset(true,true,206,400,1000,1000)==400,"refresh of the same file with the same length resumes");
  check(OfflineRules.resumeOffset(true,true,206,400,1000,1800)==0,"refresh that reports a different length starts over");
  check(OfflineRules.resumeOffset(true,true,206,400,-1,1000)==0,"refresh without a known length does not append");
  check(OfflineRules.resumeOffset(false,true,206,400,1000,1000)==0,"a different source is not spliced onto the partial file even when the length matches");
  check(OfflineRules.resumeOffset(false,false,206,400,1000,1000)==0,"mismatched source bytes are replaced");
  check(OfflineRules.resumeOffset(true,false,200,400,1000,1000)==0,"a full response replaces the partial file");
  System.out.println("PASS: in-app download status classification, Content-Range total, retry backoff, error-page detection, progress lines, flush throttle and resume that refuses a different source.");
 }
}
