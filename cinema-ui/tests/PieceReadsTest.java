package com.guocheng.mediapocket;
import java.io.*;import java.util.*;
public class PieceReadsTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static class Pieces implements VerifiedPieceReader.Pieces {
  Set<Integer> ready=new HashSet<>();boolean active=true;
  public boolean have(int n){return ready.contains(n);}public void request(int n){}public boolean running(){return active;}
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
   check(r.read(0,16,b)==5,"deliver verified prefix before missing next piece");
   check((System.nanoTime()-start)/1000000<80,"short read avoids timeout");
   for(int i=0;i<5;i++)check(b[i]==i,"correct relative file offset");
   boolean timed=false;try{r.read(5,8,b);}catch(IOException e){timed=e.getMessage().contains("超时");}check(timed,"missing data never returned as zeros");
   p.ready.add(1);check(r.read(5,8,b)==8,"same reader recovers after timeout");check(b[0]==5&&b[7]==12,"no skipped bytes after recovery");
   for(int i=0;i<6;i++)p.ready.add(i);
   check(r.read(37,16,b)==3,"EOF bounded");check(b[0]==37&&b[2]==39,"tail bytes exact");check(r.read(40,16,b)==0,"EOF");
   p.active=false;boolean closed=false;try{r.read(0,1,b);}catch(IOException e){closed=true;}check(closed,"stop cancels reads");
   p.active=true;Thread.currentThread().interrupt();p.ready.clear();boolean interrupted=false;try{r.read(0,1,b);}catch(IOException e){interrupted=Thread.currentThread().isInterrupted();}finally{Thread.interrupted();}check(interrupted,"interrupt preserved");
   PieceDeadlines d=new PieceDeadlines();check(d.advance(4,500,1000),"first deadline");check(!d.advance(4,500,1100),"overlapping read does not postpone");check(d.advance(4,100,1100),"urgent read advances deadline");check(!d.advance(4,100,10000),"expired urgent deadline stays urgent");check(d.advance(5,500,1100),"independent next piece");
   System.out.println("PASS: verified short reads, unaligned file offsets, timeout recovery, EOF, cancellation, interrupt, earliest deadline scheduling");
  }finally{f.delete();}
 }
}
