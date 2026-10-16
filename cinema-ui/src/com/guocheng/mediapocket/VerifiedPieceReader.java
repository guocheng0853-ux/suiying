package com.guocheng.mediapocket;
import java.io.*;
/**
 * Only expose verified bytes, and always return the full requested size (or stop at EOF / throw).
 * 0.1.50 returned short reads when the next piece was missing. ProxyFileDescriptorCallback.onRead must
 * "return exact requested size of bytes unless it reaches file end": AppFuse is cached FUSE without
 * FOPEN_DIRECT_IO, so a short read is taken as end-of-file and VLC stalls right after the first pieces.
 * While waiting, the missing piece is re-requested periodically so its deadline stays armed.
 */
public final class VerifiedPieceReader {
 public interface Pieces { boolean have(int piece); void request(int piece); boolean running(); }
 static final long REARM_MS=2000;
 final File file; final long length,offset,timeout; final int pieceSize; final Pieces pieces;
 public VerifiedPieceReader(File f,long length,long offset,int size,Pieces p,long timeout){
  if(length<0||offset<0||size<=0||timeout<=0||offset>Long.MAX_VALUE-length)throw new IllegalArgumentException("Invalid torrent file bounds");
  this.file=f;this.length=length;this.offset=offset;pieceSize=size;pieces=p;this.timeout=timeout;
 }
 public int read(long position,int count,byte[] data)throws IOException {
  if(position<0||count<0||data==null||count>data.length)throw new IOException("Invalid read");
  if(count==0||position>=length)return 0;
  int limit=(int)Math.min(count,length-position),copied=0;
  while(copied<limit){
   if(!pieces.running())throw new IOException("Playback closed");
   long at=position+copied,absolute=offset+at,index=absolute/pieceSize;
   if(index>Integer.MAX_VALUE)throw new IOException("Piece index overflow");
   int piece=(int)index;
   pieces.request(piece);
   long started=System.nanoTime(),rearmed=started;
   while(!pieces.have(piece)){
    check(started,"等待片段超时："+piece);pause();
    if((System.nanoTime()-rearmed)/1000000>=REARM_MS){pieces.request(piece);rearmed=System.nanoTime();}
   }
   int n=(int)Math.min(limit-copied,pieceSize-absolute%pieceSize),got=0;
   while(got<n){
    check(started,"等待已验证片段写入文件超时");
    if(file.exists())try(RandomAccessFile input=new RandomAccessFile(file,"r")){
     input.seek(at+got);int read=input.read(data,copied+got,n-got);
     if(read>0){got+=read;continue;}
    }
    pause();
   }
   copied+=n;
  }
  return copied;
 }
 private void check(long started,String message)throws IOException{
  if(!pieces.running())throw new IOException("Playback closed");
  if((System.nanoTime()-started)/1000000>=timeout)throw new IOException(message);
 }
 private void pause()throws IOException{
  try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Read interrupted",e);}
 }
}
