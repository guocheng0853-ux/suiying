package com.guocheng.mediapocket;
import java.io.*;
/** Only expose verified bytes. Return a short read rather than block already available data. */
public final class VerifiedPieceReader {
 public interface Pieces { boolean have(int piece); void request(int piece); boolean running(); }
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
   // POSIX short reads let VLC consume this prefix and request the missing piece separately.
   if(copied>0&&!pieces.have(piece))return copied;
   long started=System.nanoTime();
   while(!pieces.have(piece)){
    check(started,"等待片段超时："+piece);pause();
   }
   int n=(int)Math.min(limit-copied,pieceSize-absolute%pieceSize);
   while(true){
    check(started,"等待已验证片段写入文件超时");
    if(file.exists())try(RandomAccessFile input=new RandomAccessFile(file,"r")){
     input.seek(at);int read=input.read(data,copied,n);
     if(read>0){copied+=read;break;}
    }
    if(copied>0)return copied;
    pause();
   }
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
