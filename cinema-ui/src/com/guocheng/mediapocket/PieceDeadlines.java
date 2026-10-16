package com.guocheng.mediapocket;
import java.util.*;
/**
 * Preserve the earliest pending deadline across overlapping read-ahead requests.
 * A recorded deadline only suppresses later calls while it is still pending: once it has
 * expired (libtorrent may have cleared it after hash failure / seek / piece completion),
 * the next request re-arms it. Callers can also clear() a piece or reset() everything.
 */
public final class PieceDeadlines {
 private final LinkedHashMap<Integer,Long> deadlines=new LinkedHashMap<Integer,Long>();
 public synchronized boolean advance(int piece,int delay,long now){
  long due=now+Math.max(0,delay);Long old=deadlines.get(piece);
  if(old!=null&&old>now&&old<=due)return false;
  deadlines.remove(piece);deadlines.put(piece,due);
  if(deadlines.size()>4096)deadlines.remove(deadlines.keySet().iterator().next());
  return true;
 }
 public synchronized void clear(int piece){deadlines.remove(piece);}
 public synchronized void reset(){deadlines.clear();}
}
