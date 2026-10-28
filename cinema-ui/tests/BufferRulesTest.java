package com.guocheng.mediapocket;
public class BufferRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x)throws Exception{
  // 文件从种子第 2 个分块中间开始：offset 1.5 块，大小 10 块，分块 100 字节
  long off=150,size=1000;int piece=100;
  boolean[] have=new boolean[13];for(int i=1;i<=6;i++)have[i]=true;
  long end=BufferRules.contiguousEnd(off,size,piece,0,p->p<have.length&&have[p]);
  check(end==550,"contiguous pieces 1..6 cover file bytes 0..550, got "+end);
  check(BufferRules.contiguousEnd(off,size,piece,700,p->p<have.length&&have[p])==700,"piece under playhead missing → nothing ahead");
  for(int i=0;i<have.length;i++)have[i]=true;check(BufferRules.contiguousEnd(off,size,piece,0,p->have[p])==size,"fully downloaded file ends at file size");
  check(BufferRules.byteAt(30_000,60_000,1000)==500&&BufferRules.byteAt(-5,60_000,1000)==0&&BufferRules.byteAt(5,0,1000)==0,"time to byte by average bitrate");
  check(BufferRules.secondary(550,1000,1000,200)==550,"buffered tick");check(BufferRules.secondary(100,1000,1000,200)==200,"never behind played");check(BufferRules.secondary(5,0,1000,30)==30,"unknown size");
  int[] calls={0};BufferRules.contiguousEnd(0,100_000,1,0,p->{calls[0]++;return true;});check(calls[0]==BufferRules.MAX_SCAN,"scan is capped");
  String overlay=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlayerOverlay.java"));
  int exec=overlay.indexOf("IO.execute"),scanAt=overlay.indexOf("h::havePiece"),interval=overlay.indexOf("BufferRules.INTERVAL_MS");
  check(interval>=0&&exec>=0&&scanAt>exec,"havePiece scan stays on the 1s cadence and runs off the main thread");
  String repair=java.nio.file.Files.readString(java.nio.file.Paths.get("src/com/guocheng/mediapocket/PlaybackRepair.java"));
  int vw=repair.indexOf("field(video,\"vw\")"),track=repair.indexOf("getCurrentVideoTrack");
  check(vw>=0&&track>vw,"rendered probe reuses a known video width before another track read");
  System.out.println("PASS: P2P buffer bar from contiguous pieces, capped scan, never behind playback.");
 }
}
