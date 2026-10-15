package com.guocheng.mediapocket;
import java.net.URI;
final class MarketInstallRules {static String manifest(String value){if(value==null)return null;try{String url=value.replaceFirst("(?i)^stremio://","https://");URI u=URI.create(url);String scheme=u.getScheme();return ("https".equalsIgnoreCase(scheme)||"http".equalsIgnoreCase(scheme))&&u.getHost()!=null&&u.getRawUserInfo()==null&&u.getRawQuery()==null&&u.getPath()!=null&&u.getPath().endsWith("/manifest.json")?url:null;}catch(Exception e){return null;}}}
