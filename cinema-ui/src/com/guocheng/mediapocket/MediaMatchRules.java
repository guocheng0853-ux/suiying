package com.guocheng.mediapocket;
import java.util.regex.*;
final class MediaMatchRules {
 static String clean(String title){return title.replaceAll("(?i)\\.(mkv|mp4|avi|ts)$","").replaceAll("(?i)[ ._-]*S\\d{1,2}E\\d{1,3}.*$","").replaceAll("[\\[【][^\\]】]*[\\]】]","").replaceAll("(?i)\\b(2160p|1080p|720p|web-dl|bluray|hevc|x264|x265)\\b.*$","").replaceAll("[（(]\\d{4}[)）]","").replaceAll("[._]"," ").replaceAll("\\s+"," ").trim();}
 static String key(String name){return clean(name).toLowerCase(java.util.Locale.ROOT).replaceAll("[\\p{Punct}\\s]","");}
 static String imdb(String value){Matcher m=Pattern.compile("tt\\d{5,12}").matcher(value);return m.find()?m.group():"";}
 static int[] episode(String value){Matcher m=Pattern.compile("(?i)S(\\d{1,2})E(\\d{1,3})").matcher(value);if(m.find())return new int[]{Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))};return new int[]{1,1};}
}
