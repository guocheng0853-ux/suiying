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
 /** 0.1.112 自己下载时的进度行。status：queued / running / paused / done / failed。 */
 static String state(String status,long done,long total,long speed,String error){
  switch(status==null?"":status){
   case "done":return "已下载 · "+size(total>0?total:done);
   case "running":return (total>0?"下载中 "+percent(done,total)+" · "+size(done)+" / "+size(total):"下载中 · "+size(done))+(speed>0?" · "+size(speed)+"/s":"")+" · 点一下暂停";
   case "paused":return "已暂停"+(total>0?" · "+percent(done,total):"")+" · 点一下继续";
   case "failed":return "下载失败"+(error==null||error.isEmpty()?"":"（"+error+"）")+" · 点一下重试，长按删除";
   case "queued":return "排队中…"+(done>0?" 已下 "+size(done):"");
   default:return "状态未知";}
 }
 /** HTTP 状态码怎么处理：ok 正常 / refresh 链接可能过期，重新取地址 / retry 稍后重试 / fail 放弃。 */
 static String classify(int code){if(code==200||code==206)return "ok";if(code==401||code==403||code==404||code==410)return "refresh";if(code==408||code==425||code==429||code>=500)return "retry";return "fail";}
 /** 「bytes 100-199/1000」→ 1000；没有或是 * 返回 -1。 */
 static long totalFromContentRange(String header){if(header==null)return -1;int slash=header.lastIndexOf('/');if(slash<0)return -1;try{return Long.parseLong(header.substring(slash+1).trim());}catch(Exception e){return -1;}}
 /** 第 n 次重试前等多久：2、4、8、16、30、30 秒。 */
 static long backoff(int attempt){return Math.min(30000L,2000L<<Math.max(0,Math.min(attempt,10)));}
 /** 开头就拿到网页（登录页、验证页、错误页）说明不是视频。 */
 static boolean looksLikePage(String contentType,long done){if(done>0||contentType==null)return false;String t=contentType.toLowerCase(Locale.ROOT);return t.startsWith("text/html")||t.startsWith("application/json")||t.startsWith("text/plain");}
 static String percent(long done,long total){if(total<=0)return "";return Math.min(100,Math.round(done*100.0/total))+"%";}
 static String failReason(int r){if(r>=400&&r<600)return "HTTP "+r;switch(r){case 1006:return "存储空间不足";case 1007:return "找不到存储";case 1001:return "文件错误";case 1004:return "网络数据错误";case 1005:return "重定向过多";case 1002:return "服务器返回异常";default:return "错误 "+r;}}
 static String pauseReason(int r){switch(r){case 1:return "等待重试";case 2:return "等待网络";case 3:return "等待 Wi-Fi";default:return "已暂停";}}
}
