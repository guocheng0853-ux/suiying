package com.guocheng.mediapocket;
import java.util.*;
/** Preserve the earliest deadline across overlapping read-ahead requests. */
public final class PieceDeadlines {
 private final LinkedHashMap<Integer,Long> deadlines=new LinkedHashMap<Integer,Long>();
 public synchronized boolean advance(int piece,int delay,long now){
  long due=now+Math.max(0,delay);Long old=deadlines.get(piece);
  if(old!=null&&old<=due)return false;
  deadlines.put(piece,due);
  if(deadlines.size()>4096)deadlines.remove(deadlines.keySet().iterator().next());
  return true;
 }
}
