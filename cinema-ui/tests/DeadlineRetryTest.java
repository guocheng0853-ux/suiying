package com.guocheng.mediapocket;
import com.frostwire.jlibtorrent.TorrentHandle;
public class DeadlineRetryTest{
 public static void main(String[] args){final int[] calls={0};TorrentHandle handle=new TorrentHandle(){public void setPieceDeadline(int p,int d){if(++calls[0]==1)throw new IllegalStateException("temporary invalid handle");}};
  try{TorrentBoost.deadline(handle,1,60000);throw new AssertionError("expected native failure");}catch(IllegalStateException expected){}TorrentBoost.deadline(handle,1,60000);if(calls[0]!=2)throw new AssertionError("failed native deadline suppressed retry");System.out.println("PASS: failed native deadline is cleared so the next read can retry.");
 }
}
