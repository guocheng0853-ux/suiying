package com.guocheng.mediapocket;
import java.net.URI;
final class ClawUnpinRules {
 static boolean isBundled(String base,String id){if(!"com.torrentclaw.addon".equals(id))return false;try{URI u=URI.create(base);String p=u.getPath().replaceAll("/+$","");return "torrentclaw.com".equalsIgnoreCase(u.getHost())&&u.getRawQuery()==null&&(p.equals("/api/stremio")||p.equals("/api/stremio/manifest.json"));}catch(Exception e){return false;}}
}
