package com.guocheng.mediapocket;
import java.net.*;
/**
 * 播放和离线下载跟随重定向时，凭证头能带到哪一跳。
 * 与字幕模块同一套「协议 + 主机 + 端口」来源判断。字幕在降级到 http 时直接拒绝；
 * 这里改为继续跟随，但 https→http 不再带任何凭证，跨来源则去掉凭证头。
 */
final class RequestHeaders {
 private RequestHeaders(){}
 static int port(URL u){return u.getPort()<0?u.getDefaultPort():u.getPort();}
 static boolean sensitive(String name){if(name==null)return false;return name.equalsIgnoreCase("Cookie")||name.equalsIgnoreCase("Authorization")||name.equalsIgnoreCase("X-Emby-Token")||name.equalsIgnoreCase("Referer");}
 static boolean sameOrigin(URL a,URL b){if(a==null||b==null||a.getHost()==null||b.getHost()==null)return false;return a.getProtocol().equalsIgnoreCase(b.getProtocol())&&a.getHost().equalsIgnoreCase(b.getHost())&&port(a)==port(b);}
 static boolean downgrade(URL from,URL to){return from!=null&&to!=null&&"https".equalsIgnoreCase(from.getProtocol())&&"http".equalsIgnoreCase(to.getProtocol());}
 /** 这一跳是否可以发送该请求头。非凭证头始终保留。 */
 static boolean allow(String name,URL origin,URL current){if(name==null||name.isEmpty())return false;if(!sensitive(name))return true;if(downgrade(origin,current))return false;return sameOrigin(origin,current);}
}
