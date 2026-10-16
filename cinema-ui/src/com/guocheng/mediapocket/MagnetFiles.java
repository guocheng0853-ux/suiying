package com.guocheng.mediapocket;
import org.json.*;import java.io.File;import java.util.*;import java.util.regex.*;
/**
 * 磁力合集选对集数。原 TorrentEngine 在 stream 没有 fileIdx 时播整个种子里最大的视频文件；
 * 影视仓站点若每集都给同一个整季磁力，选哪集都会播同一个文件。
 * 只在「像合集」时（详情里多集共用同一哈希，或磁力名像整季包）先取一次元数据，按集号匹配文件写入 fileIdx；
 * 取不到或匹配不唯一就不改，保持原行为。必须在后台线程调用。
 */
final class MagnetFiles {
 static final Pattern PACK=Pattern.compile("(?i)(\\bS\\d{1,2}\\b(?!\\s*E\\d)|\\bS\\d{1,2}(?:[-~]S?\\d{1,2})\\b|\\bcomplete\\b|\\bseason\\s*\\d+\\b(?!.*\\bE\\d)|全集|合集|全\\s*\\d+\\s*集|\\d+\\s*[-~～]\\s*\\d+\\s*集|第\\s*\\d+\\s*[-~～]\\s*\\d+\\s*集)");
 static final Pattern VIDEO=Pattern.compile("(?i)\\.(mkv|mp4|avi|webm|ts|m2ts|mov|wmv|flv)$");
 private MagnetFiles(){}
 /** 详情选集里有至少两集指向同一个哈希，或磁力名像整季包。 */
 static boolean packLikely(String infoHash,String magnetName,JSONObject detail){
  if(magnetName!=null&&PACK.matcher(magnetName).find())return true;
  if(detail==null||infoHash==null||infoHash.isEmpty())return false;
  int same=0;
  for(String group:detail.optString("vod_play_url").split("\\$\\$\\$",-1))for(String item:group.split("#")){String[] ep=item.split("\\$",2);if(ep.length<2)continue;MagnetLinks.Info m=MagnetLinks.parse(ep[1]);if(m!=null&&infoHash.equalsIgnoreCase(m.infoHash)&&++same>=2)return true;}
  return false;
 }
 /**
  * 按集号挑文件：只看视频文件；季号两边都明确时必须相同。多个命中（如样片）取最大的；没有命中返回 -1。
  * 只有一个视频文件时返回 -1（原行为已经正确）。
  */
 static int choose(String[] paths,long[] sizes,String episode,String context){
  if(paths==null||paths.length==0)return -1;
  FusionPlaybackRules.Episode want=FusionPlaybackRules.episode(episode==null?"":episode,context==null?"":context);if(want.number<=0)return -1;
  int videos=0,best=-1;long bestSize=-1;
  for(int i=0;i<paths.length;i++){
   String path=paths[i]==null?"":paths[i];if(!VIDEO.matcher(path).find())continue;videos++;
   String name=path.substring(Math.max(path.lastIndexOf('/'),path.lastIndexOf('\\'))+1);
   String folder=path.length()>name.length()?path.substring(0,path.length()-name.length()):"";
   FusionPlaybackRules.Episode got=FusionPlaybackRules.episode(name,folder);
   if(got.number!=want.number)continue;
   if(want.season>=0&&got.season>=0&&want.season!=got.season)continue;
   long size=sizes!=null&&i<sizes.length?sizes[i]:0;if(size>bestSize){bestSize=size;best=i;}
  }
  return videos<=1?-1:best;
 }
 /** 在后台线程调用：像合集且没有 fileIdx 时取元数据并写入 fileIdx。返回说明文字（用于状态/记录），不改则返回空串。 */
 static String select(JSONObject stream,String episode,String context,JSONObject detail,int timeoutSec,File cacheDir){
  try{
   if(stream==null||!stream.has("infoHash")||stream.has("fileIdx"))return "";
   String hash=stream.optString("infoHash");if(!packLikely(hash,stream.optString("mp.magnetName"),detail))return "";
   StringBuilder uri=new StringBuilder("magnet:?xt=urn:btih:").append(hash);
   JSONArray src=stream.optJSONArray("sources");for(int i=0;src!=null&&i<src.length();i++){String t=src.optString(i);if(t.startsWith("tracker:"))uri.append("&tr=").append(java.net.URLEncoder.encode(t.substring(8),"UTF-8"));}
   com.frostwire.jlibtorrent.SessionManager session=TorrentEngine.acquireSession();if(session==null)return "";
   File temp=new File(cacheDir,"torrent-meta");temp.mkdirs();
   byte[] data=session.fetchMagnet(TorrentBoost.magnet(uri.toString()),timeoutSec,temp);if(data==null||data.length==0)return "合集元数据未取到，按最大文件播放";
   com.frostwire.jlibtorrent.FileStorage files=com.frostwire.jlibtorrent.TorrentInfo.bdecode(data).files();int n=files.numFiles();
   String[] paths=new String[n];long[] sizes=new long[n];for(int i=0;i<n;i++){paths[i]=files.filePath(i);sizes[i]=files.fileSize(i);}
   int idx=choose(paths,sizes,episode,context);if(idx<0)return "合集里没找到对应集数的文件，按最大文件播放";
   stream.put("fileIdx",idx);return "合集已选中："+paths[idx].substring(Math.max(paths[idx].lastIndexOf('/'),0)+(paths[idx].contains("/")?1:0));
  }catch(Throwable t){return "";}
 }
}
