package com.guocheng.mediapocket;
public final class HttpRangeSeekTest {
 static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
 public static void main(String[] a){
  long huge=HttpRange.MAX_SKIP+1;
  HttpRange.SeekCap idle=new HttpRange.SeekCap();
  check(!idle.rejects(80L*1024*1024,huge),"open, resume and moov are not capped");
  HttpRange.SeekCap cap=new HttpRange.SeekCap();
  int before=cap.generation;
  cap.arm(600000);
  check(cap.generation==before+1&&cap.targetMs==600000,"arm records generation and target");
  check(!cap.rejects(0,huge)&&cap.byteAt==Long.MIN_VALUE,"a header read does not claim the seek");
  check(!cap.rejects(2048,2048)&&cap.byteAt==2048,"a small skip is allowed and noted");
  check(cap.rejects(90L*1024*1024,huge),"the oversized ignored-range read is the seek");
  check(cap.rejects(90L*1024*1024,huge),"a later retry of that position is still rejected");
  check(!cap.rejects(2048,2048),"a different position is not capped");
  cap.landed(cap.targetMs+20000);
  check(cap.armed,"ready far from the target keeps the seek armed");
  cap.landed(cap.targetMs+500);
  check(!cap.armed&&!cap.rejects(90L*1024*1024,huge),"landing disarms, so a later rebuffer is not capped");
  cap.arm(1000);
  check(cap.generation==before+2&&cap.byteAt==Long.MIN_VALUE,"a new seek drops the previous byte position");
  check(HttpRange.SKIP_ERROR==2008,"skip failure uses the non-retriable position error");
  System.out.println("PASS: 64MB skip cap follows seek generation and target position, and retries of that position stay rejected.");
 }
}
