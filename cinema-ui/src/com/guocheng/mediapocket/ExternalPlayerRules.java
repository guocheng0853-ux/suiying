package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
/** Headers safe to hand to a third-party player Intent; credentials stay in-app. */
final class ExternalPlayerRules {
 private ExternalPlayerRules(){}
 static boolean sensitive(String name){if(name==null||name.isEmpty())return true;String l=name.trim().toLowerCase(Locale.ROOT);if(l.equals("cookie")||l.equals("authorization")||l.equals("proxy-authorization")||l.equals("set-cookie")||l.equals("cookie2")||l.equals("x-api-key")||l.equals("api-key")||l.equals("x-auth-token")||l.equals("authentication")||l.equals("access-token")||l.equals("x-access-token"))return true;if(l.contains("authorization")||l.contains("cookie"))return true;return false;}
 /** Keep non-sensitive request headers for external players; drop credentials by default. */
 static LinkedHashMap<String,String> sanitize(JSONObject request){LinkedHashMap<String,String> out=new LinkedHashMap<>();if(request==null)return out;Iterator<String> keys=request.keys();while(keys.hasNext()){String k=keys.next();if(sensitive(k))continue;String v=request.optString(k);if(v==null||v.isEmpty())continue;out.put(k,v);}return out;}
 static String[] pairs(Map<String,String> headers){if(headers==null||headers.isEmpty())return new String[0];ArrayList<String> pairs=new ArrayList<>(headers.size()*2);for(Map.Entry<String,String> e:headers.entrySet()){pairs.add(e.getKey());pairs.add(e.getValue());}return pairs.toArray(new String[0]);}
}
