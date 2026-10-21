package com.guocheng.mediapocket;
import java.net.*;
/**
 * 播放和离线下载跟随重定向时，哪些头可以带到下一跳。
 * Cookie、Authorization、X-Emby-Token 只发给最初那一跳（协议 + 主机 + 端口都相同），
 * https 降到 http 时也不带。Referer 跨主机、跨端口仍然保留（小雅 / Alist 常 302 到需要 Referer 的网盘 CDN），
 * 只在 https→http 时去掉。字幕模块更严：降级到 http 直接拒绝，这里不改那条路径。
 */
final class RequestHeaders {
 private RequestHeaders(){}
 static int port(URL u){return u.getPort()<0?u.getDefaultPort():u.getPort();}
 static boolean credential(String name){if(name==null)return false;return name.equalsIgnoreCase("Cookie")||name.equalsIgnoreCase("Authorization")||name.equalsIgnoreCase("X-Emby-Token");}
 static boolean referer(String name){return name!=null&&name.equalsIgnoreCase("Referer");}
 static boolean sameOrigin(URL a,URL b){if(a==null||b==null||a.getHost()==null||b.getHost()==null)return false;return a.getProtocol().equalsIgnoreCase(b.getProtocol())&&a.getHost().equalsIgnoreCase(b.getHost())&&port(a)==port(b);}
 static boolean downgrade(URL from,URL to){return from!=null&&to!=null&&"https".equalsIgnoreCase(from.getProtocol())&&"http".equalsIgnoreCase(to.getProtocol());}
 /** 这一跳是否可以发送该请求头。普通头始终保留。 */
 static boolean allow(String name,URL origin,URL current){if(name==null||name.isEmpty())return false;if(referer(name))return !downgrade(origin,current);if(!credential(name))return true;if(downgrade(origin,current))return false;return sameOrigin(origin,current);}
}
