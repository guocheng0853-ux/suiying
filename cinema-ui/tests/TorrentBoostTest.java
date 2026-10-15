package com.guocheng.mediapocket;
public class TorrentBoostTest {
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  String base="magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&tr="+java.net.URLEncoder.encode("udp://tracker.opentrackr.org:1337/announce","UTF-8")+"&tr=udp%3A%2F%2Fmy.private%3A1%2Fannounce";
  String m=TorrentBoost.magnet(base);
  check(m.startsWith(base),"original trackers and info hash kept in front");
  int count=m.split("&tr=").length-1;check(count==2+TorrentBoost.TRACKERS.length-1,"existing tracker not duplicated ("+count+")");
  check(m.contains("tracker.qu.ax"),"public trackers appended");
  check(TorrentBoost.magnet(m).equals(m),"idempotent");
  TorrentBoost.enabled=false;check(TorrentBoost.magnet(base).equals(base),"disabled leaves magnet");TorrentBoost.enabled=true;
  check(TorrentBoost.magnet("/sdcard/a.torrent").equals("/sdcard/a.torrent"),"non-magnet untouched");
  System.out.println("PASS: magnet keeps hash/original trackers, appends "+(TorrentBoost.TRACKERS.length-1)+" public trackers without duplicates, idempotent, toggle off and non-magnet untouched.");
 }
}
