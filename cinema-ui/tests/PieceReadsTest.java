package com.guocheng.mediapocket;
import java.io.*;import java.util.*;
public class PieceReadsTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class Pieces implements VerifiedPieceReader.Pieces {
  Set<Integer> ready=Collections.synchronizedSet(new HashSet<>());volatile boolean active=true;final List<Integer> requests=Collections.synchronizedList(new ArrayList<>());
  public boolean have(int n){return ready.contains(n);}public void request(int n){requests.add(n);}public boolean running(){return active;}
 }
 public static void main(String[] args)throws Exception{
  File f=File.createTempFile("pieces",".bin");
  try{
   byte[] content=new byte[40];for(int i=0;i<content.length;i++)content[i]=(byte)i;
   try(FileOutputStream o=new FileOutputStream(f)){o.write(content);}
   Pieces p=new Pieces();p.ready.add(0);
   // File begins 3 bytes into a torrent piece; local offset is independent of torrent offset.
   VerifiedPieceReader r=new VerifiedPieceReader(f,40,3,8,p,100);
   byte[] b=new byte[16];long start=System.nanoTime();
   // 0.1.59 起：缺片段时不短读（AppFuse 会把短读当作 EOF），而是等片段到齐后整块返回
   new Thread(()->{try{Thread.sleep(60);}catch(InterruptedException e){}p.ready.add(1);p.ready.add(2);}).start();
   int full=r.read(0,16,b);
   check(full==16,"full requested size returned once pieces arrive (no short read): "+full);
   check((System.nanoTime()-start)/1000000>=50,"waited for missing piece instead of returning a prefix");
   for(int i=0;i<16;i++)check(b[i]==i,"correct relative file offset");
   p.ready.clear();p.ready.add(0);
   boolean timed=false;int before=p.requests.size();try{r.read(0,16,b);}catch(IOException e){timed=e.getMessage().contains("超时");}check(timed,"missing data times out with an error, never a partial or zero-filled read");
   p.ready.add(1);check(r.read(5,8,b)==8,"same reader recovers after timeout");check(b[0]==5&&b[7]==12,"no skipped bytes after recovery");
   for(int i=0;i<6;i++)p.ready.add(i);
   check(r.read(37,16,b)==3,"EOF bounded");check(b[0]==37&&b[2]==39,"tail bytes exact");check(r.read(40,16,b)==0,"EOF");
   p.active=false;boolean closed=false;try{r.read(0,1,b);}catch(IOException e){closed=true;}check(closed,"stop cancels reads");
   p.active=true;Thread.currentThread().interrupt();p.ready.clear();boolean interrupted=false;try{r.read(0,1,b);}catch(IOException e){interrupted=Thread.currentThread().isInterrupted();}finally{Thread.interrupted();}check(interrupted,"interrupt preserved");
   PieceDeadlines d=new PieceDeadlines();check(d.advance(4,500,1000),"first deadline");check(!d.advance(4,500,1100),"overlapping read does not postpone");check(d.advance(4,100,1100),"urgent read advances deadline");check(!d.advance(4,500,1150),"pending urgent deadline not postponed");check(d.advance(4,500,10000),"expired deadline can be re-armed");check(!d.advance(4,500,10100),"re-armed deadline suppresses overlap");d.clear(4);check(d.advance(4,800,10100),"cleared piece can be re-armed");d.reset();check(d.advance(4,800,10100),"reset re-arms");check(d.advance(5,500,1100),"independent next piece");
   System.out.println("PASS: full reads only (waits for missing pieces, no AppFuse short read),  unaligned file offsets, timeout recovery, EOF, cancellation, interrupt, earliest deadline scheduling");
  }finally{f.delete();}
 }
}
