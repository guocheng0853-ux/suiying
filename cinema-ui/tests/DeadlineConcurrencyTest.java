package com.guocheng.mediapocket;
import com.frostwire.jlibtorrent.TorrentHandle;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public class DeadlineConcurrencyTest{
 public static void main(String[] args)throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),urgentStarted=new CountDownLatch(1);AtomicInteger last=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();
  TorrentHandle handle=new TorrentHandle(){public void setPieceDeadline(int p,int d){if(d==60000){entered.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new AssertionError("release timeout");}catch(InterruptedException e){throw new RuntimeException(e);}}last.set(d);}};
  Thread slow=new Thread(()->{try{TorrentBoost.deadline(handle,7,60000);}catch(Throwable e){error.set(e);}}),urgent=new Thread(()->{urgentStarted.countDown();try{TorrentBoost.deadline(handle,7,10000);}catch(Throwable e){error.set(e);}});
  slow.start();if(!entered.await(3,TimeUnit.SECONDS))throw new AssertionError("first call timeout");urgent.start();urgentStarted.await();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);while(urgent.getState()!=Thread.State.BLOCKED&&urgent.isAlive()&&System.nanoTime()<until)Thread.yield();release.countDown();slow.join(3000);urgent.join(3000);
  if(slow.isAlive()||urgent.isAlive()||error.get()!=null||last.get()!=10000)throw new AssertionError("Concurrent deadline postponed urgent read",error.get());
  System.out.println("PASS: urgent native deadline remains last across overlapping scheduling calls.");
 }
}
