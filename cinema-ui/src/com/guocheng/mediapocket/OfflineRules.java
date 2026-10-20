package com.guocheng.mediapocket;
import java.util.*;
/** 0.1.106 离线下载的纯逻辑（不依赖 Android）：能不能下、文件扩展名、进度文案。 */
final class OfflineRules {
 private OfflineRules(){}
 static final String[] VIDEO_EXT={"mp4","mkv","m4v","mov","avi","webm","flv","ts","wmv","rmvb","mpg","mpeg","iso","m2ts","mts"};
 /** 不能下载时返回原因；能下载返回 null。 */
 static String refuse(String url){
  if(url==null||url.trim().isEmpty())return "这个片源是磁力 / 种子，暂不支持下载";
  String low=url.trim().toLowerCase(Locale.ROOT);
  if(low.startsWith("magnet:"))return "这个片源是磁力 / 种子，暂不支持下载";
  if(!(low.startsWith("http://")||low.startsWith("https://")))return "这个片源不是普通网络地址，无法下载";
  String path=path(low);
  if(path.endsWith(".m3u8")||path.endsWith(".m3u")||low.contains(".m3u8?")||low.contains("/master.m3u8")||low.contains("type=m3u8"))return "这个片源是分段流（m3u8），暂不支持下载";
  if(path.endsWith(".mpd"))return "这个片源是 DASH 分段流，暂不支持下载";
  return null;
 }
 static String path(String url){String s=url;int q=s.indexOf('?');if(q>=0)s=s.substring(0,q);int h=s.indexOf('#');if(h>=0)s=s.substring(0,h);return s;}
 /** 扩展名：先看容器（Emby 给的），再看地址，最后默认 mp4。 */
 static String extension(String container,String url){
  String c=container==null?"":container.trim().toLowerCase(Locale.ROOT);if(c.contains(","))c=c.substring(0,c.indexOf(','));for(String e:VIDEO_EXT)if(e.equals(c))return e;
  String p=url==null?"":path(url.toLowerCase(Locale.ROOT));int dot=p.lastIndexOf('.'),slash=p.lastIndexOf('/');if(dot>slash&&dot>=0){String ext=p.substring(dot+1);for(String e:VIDEO_EXT)if(e.equals(ext))return e;}
  return "mp4";
 }
 /** 文件名只用 ASCII，避免中文 / 空格进 file:// 地址出问题；片名另存在记录里。 */
 static String fileName(long now,String ext){return "suiying-"+now+"."+(ext==null||ext.isEmpty()?"mp4":ext);}
 static String size(long bytes){if(bytes<=0)return "";double v=bytes;String[] u={"B","KB","MB","GB","TB"};int i=0;while(v>=1024&&i<u.length-1){v/=1024;i++;}return (i>=3?String.format(Locale.ROOT,"%.2f",v):String.format(Locale.ROOT,"%.0f",v))+" "+u[i];}
 /** 进度行。status 用 DownloadManager 的数值：1 等待 2 下载中 4 暂停 8 完成 16 失败。 */
 static String line(int status,long done,long total,int reason){
  switch(status){
   case 8:return "已下载 · "+size(total>0?total:done);
   case 16:return "下载失败"+(reason>0?"（"+failReason(reason)+"）":"")+" · 长按删除后可重新下载";
   case 4:return "已暂停 · "+(total>0?percent(done,total)+" · ":"")+pauseReason(reason);
   case 1:return "等待下载…";
   case 2:return total>0?"下载中 "+percent(done,total)+" · "+size(done)+" / "+size(total):"下载中 · "+size(done);
   default:return "状态未知";}
 }
 static String percent(long done,long total){if(total<=0)return "";return Math.min(100,Math.round(done*100.0/total))+"%";}
 static String failReason(int r){if(r>=400&&r<600)return "HTTP "+r;switch(r){case 1006:return "存储空间不足";case 1007:return "找不到存储";case 1001:return "文件错误";case 1004:return "网络数据错误";case 1005:return "重定向过多";case 1002:return "服务器返回异常";default:return "错误 "+r;}}
 static String pauseReason(int r){switch(r){case 1:return "等待重试";case 2:return "等待网络";case 3:return "等待 Wi-Fi";default:return "已暂停";}}
}
