package com.guocheng.mediapocket;
import java.util.*;import org.json.*;
/** Startup policy only: it never interrupts a stream that has rendered. */
final class TorrentStartupRules {
 static final long OBSERVE_MS=15000,MIN_WAIT_MS=30000,MAX_BUFFER=8L*1024*1024;
 private TorrentStartupRules(){}
 static long requiredRate(long fileBytes,int minutes){return fileBytes>0&&minutes>0?fileBytes/(minutes*60L):0;}
 static boolean slow(long rate,long required){return required>=32*1024&&rate>=0&&rate<required*0.65;}
 static boolean switchSource(long elapsed,int timeoutSeconds,long slowFor,long rate,long required,long verified){
  long wait=Math.max(MIN_WAIT_MS,Math.max(0L,(long)timeoutSeconds)*1000L);
  return elapsed>=wait&&slowFor>=OBSERVE_MS&&slow(rate,required)&&verified>=0&&verified<Math.min(MAX_BUFFER,required*20L);
 }
 static final class Ranked {
  final JSONObject stream;final boolean risky,direct;final int score;
  Ranked(JSONObject s,String audio){stream=s;risky=SourceCardRules.vlcRisky(StreamInfo.parse(s));direct=TorrentStartupRules.direct(s);score=SourceCardRules.score(s,audio);}
 }
 /** SourceQueue keeps the explicitly selected item first; order only its backup list. */
 static JSONArray backups(JSONArray all,String audio){
  // Parse each candidate once for each existing rule, rather than on every comparison on the UI thread.
  List<Ranked> items=new ArrayList<>();for(int n=0;all!=null&&n<all.length();n++){JSONObject s=all.optJSONObject(n);if(s!=null)items.add(new Ranked(s,audio));}
  items.sort((a,b)->{int risk=Boolean.compare(a.risky,b.risky);if(risk!=0)return risk;
   int direct=Boolean.compare(!a.direct,!b.direct);if(direct!=0)return direct;
   return Integer.compare(b.score,a.score);});
  JSONArray out=new JSONArray();for(Ranked s:items)out.put(s.stream);return out;
 }
 static boolean direct(JSONObject s){return !s.has("infoHash")&&s.optString("url").matches("(?i)^https?://.+");}
}
