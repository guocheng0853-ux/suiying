package com.guocheng.mediapocket;
import java.util.*;import java.util.regex.*;
final class SourceSearchRules {
 static List<String> keywords(String title){LinkedHashSet<String> out=new LinkedHashSet<>();String clean=MediaMatchRules.clean(title).replaceAll("[·丨|]+$","").trim();if(!clean.isEmpty())out.add(clean);String base=clean.replaceAll("第[0-9一二三四五六七八九十]+季","").replaceAll("[：:丨|·].*$","").trim();if(!base.isEmpty())out.add(base);if(base.matches(".*[\\u4e00-\\u9fff].*")&&base.contains(" ")){String first=base.split("\\s+",2)[0];if(first.length()>=2)out.add(first);}ArrayList<String> list=new ArrayList<>(out);return list.size()>3?list.subList(0,3):list;}
 static String year(String value){Matcher m=Pattern.compile("(?:19|20)\\d{2}").matcher(value);return m.find()?m.group():"";}
 static boolean sameFilm(String name,String release,String wanted,String wantedYear){String a=year(release),b=year(wantedYear);return MediaMatchRules.key(name).equals(MediaMatchRules.key(wanted))&&(a.isEmpty()||b.isEmpty()||a.equals(b));}
}
