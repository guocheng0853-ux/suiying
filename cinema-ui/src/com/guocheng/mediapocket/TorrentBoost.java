package com.guocheng.mediapocket;
import com.frostwire.jlibtorrent.*;import com.frostwire.jlibtorrent.swig.settings_pack;import java.net.URLEncoder;import java.util.*;
/**
 * P2P 加速。原 TorrentEngine 会话几乎全用默认值，上传限 128KB/s（BT 按“上传换下载”，会拖低下载），磁力只带 3 个 tracker。
 * 由主 DEX 两处前插调用：会话新建后 tune()；磁力下载前 magnet() 追加公共 tracker。任何异常都忽略，不影响原流程。
 */
public final class TorrentBoost {
 public static volatile boolean enabled=true;
 /** ngosang/trackerslist trackers_best（2026-10-16 取自 GitHub）+ 原内置 3 个；去重后追加。 */
 static final String[] TRACKERS={
  "udp://tracker.opentrackr.org:1337/announce","udp://open.stealth.si:80/announce","udp://open.demonii.com:1337/announce","udp://tracker.torrent.eu.org:451/announce",
  "udp://tracker.skynetcloud.site:6969/announce","udp://tracker.qu.ax:6969/announce","udp://exodus.desync.com:6969/announce","udp://tracker.ducks.party:1984/announce",
  "udp://tracker.nyaa.vc:6969/announce","udp://tracker.corpscorp.online:80/announce","udp://tracker.bittor.pw:1337/announce","udp://tracker-udp.gbitt.info:80/announce",
  "udp://retracker01-msk-virt.corbina.net:80/announce","udp://tracker.theoks.net:6969/announce","udp://explodie.org:6969/announce","udp://tracker.dler.org:6969/announce",
  "http://tracker.dler.com:6969/announce","udp://tracker2.dler.org:80/announce","http://tracker.renfei.net:8080/announce","udp://tracker.peerfect.org:6969/announce",
  "udp://tracker.opentrackr.com:6969/announce","http://tracker.opentrackr.org:1337/announce"};
 public static final int UPLOAD_LIMIT=512*1024;
 private TorrentBoost(){}
 /** 按 info-hash 记账（TorrentHandle 对象未必身份稳定）；取不到 info-hash 时退回对象身份。 */
 private static final Map<Object,PieceDeadlines> DEADLINES=new LinkedHashMap<Object,PieceDeadlines>(16,0.75f,true);
 static Object key(TorrentHandle handle){
  try{Object h=handle.getClass().getMethod("infoHash").invoke(handle);if(h!=null){String s=h.toString();if(!s.isEmpty())return "ih:"+s.toLowerCase(Locale.ROOT);}}catch(Throwable ignored){}
  try{Object h=handle.getClass().getMethod("infoHashes").invoke(handle);if(h!=null){Object b=h.getClass().getMethod("getBest").invoke(h);if(b!=null)return "ih:"+b.toString().toLowerCase(Locale.ROOT);}}catch(Throwable ignored){}
  return handle;
 }
 private static PieceDeadlines book(TorrentHandle handle){
  Object k=key(handle);
  synchronized(DEADLINES){
   PieceDeadlines b=DEADLINES.get(k);
   if(b==null){b=new PieceDeadlines();DEADLINES.put(k,b);if(DEADLINES.size()>64)DEADLINES.remove(DEADLINES.keySet().iterator().next());}
   return b;
  }
 }
 public static void deadline(TorrentHandle handle,int piece,int delay){
  if(handle==null)return;
  PieceDeadlines b=book(handle);synchronized(b){if(b.advance(piece,delay,System.nanoTime()/1000000))try{handle.setPieceDeadline(piece,delay);}catch(RuntimeException e){b.clear(piece);throw e;}} // 仅锁当前种子，避免并发晚写覆盖更紧急截止时间
 }
 /** 拖动进度、换片或分片需重新下载时调用，让后续请求重新设置截止时间。 */
 public static void clearDeadline(TorrentHandle handle,int piece){if(handle!=null)book(handle).clear(piece);}
 public static void resetDeadlines(TorrentHandle handle){if(handle!=null)book(handle).reset();}

 public static void tune(SessionManager s){
  if(!enabled||s==null)return;
  try{
   SettingsPack p=new SettingsPack();
   p.setInteger(settings_pack.int_types.upload_rate_limit.swigValue(),UPLOAD_LIMIT);      // 128KB/s → 512KB/s
   p.setInteger(settings_pack.int_types.connection_speed.swigValue(),80);                 // 每秒发起连接数（默认 30）
   p.setInteger(settings_pack.int_types.torrent_connect_boost.swigValue(),60);            // 新任务立即尝试的连接（默认 30）
   p.setInteger(settings_pack.int_types.peer_connect_timeout.swigValue(),7);              // 连接超时秒（默认 15）
   p.setInteger(settings_pack.int_types.max_out_request_queue.swigValue(),1500);          // 高速对端的请求队列（默认 500）
   p.setInteger(settings_pack.int_types.connections_limit.swigValue(),300);
   p.setBoolean(settings_pack.bool_types.enable_lsd.swigValue(),true);                    // 局域网发现
   p.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(),true);                   // 路由器端口映射，提高可连入性
   p.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(),true);
   p.setBoolean(settings_pack.bool_types.announce_to_all_trackers.swigValue(),true);
   p.setBoolean(settings_pack.bool_types.announce_to_all_tiers.swigValue(),true);         // 磁力里每个 tracker 各自一层，默认只问第一层
   s.applySettings(p);
  }catch(Throwable ignored){}
 }
 public static String magnet(String uri){
  if(!enabled||uri==null||!uri.startsWith("magnet:?"))return uri;
  try{return augment(uri);}catch(Throwable t){return uri;}
 }
 static String augment(String uri)throws Exception{
  HashSet<String> have=new HashSet<>();
  for(String part:uri.substring(8).split("&"))if(part.startsWith("tr="))have.add(java.net.URLDecoder.decode(part.substring(3),"UTF-8").trim().toLowerCase(Locale.ROOT));
  StringBuilder b=new StringBuilder(uri);
  for(String t:TRACKERS)if(have.add(t.toLowerCase(Locale.ROOT)))b.append("&tr=").append(URLEncoder.encode(t,"UTF-8"));
  return b.toString();
 }
}
