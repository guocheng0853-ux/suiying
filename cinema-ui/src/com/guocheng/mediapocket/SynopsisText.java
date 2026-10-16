package com.guocheng.mediapocket;
import java.util.*;
/** TVBox vod_content is HTML, often with entities and with the leading '&' already stripped. */
final class SynopsisText {
 static final Map<String,String> NAMED=new HashMap<>();
 static {String[][] rows={
  {"amp","&"},{"lt","<"},{"gt",">"},{"quot","\""},{"apos","'"},{"nbsp","\u00A0"},
  {"mdash","\u2014"},{"ndash","\u2013"},{"hellip","\u2026"},
  {"ldquo","\u201C"},{"rdquo","\u201D"},{"lsquo","\u2018"},{"rsquo","\u2019"},
  {"bull","\u2022"},{"middot","\u00B7"},{"copy","\u00A9"},{"reg","\u00AE"},{"trade","\u2122"},
  {"laquo","\u00AB"},{"raquo","\u00BB"}};
  for(String[] row:rows)NAMED.put(row[0],row[1]);}
 private SynopsisText(){}
 static String plain(String html){
  if(html==null)return "";
  String s=html.replace("\r\n","\n").replace('\r','\n');
  s=s.replaceAll("(?i)<\\s*br\\s*/?\\s*>","\n");
  s=s.replaceAll("(?i)</\\s*p\\s*>","\n");
  s=s.replaceAll("(?i)<\\s*/?\\s*p\\b[^>]*>","");
  s=s.replaceAll("<[^>]+>","");
  s=decode(s);
  s=s.replaceAll("[ \\t\\f\\u00A0]+"," ");
  s=s.replaceAll(" *\\n *","\n").replaceAll("\\n{3,}","\n\n");
  return s.trim();
 }
 static String decode(String s){String cur=s;for(int n=0;n<4;n++){String next=scan(cur,true);if(next.equals(cur))break;cur=next;}return scan(cur,false);}
 static String scan(String s,boolean ampersand){
  StringBuilder out=new StringBuilder(s.length());int i=0,n=s.length();
  while(i<n){
   char c=s.charAt(i);int semi=s.indexOf(';',i+1);
   if(semi>i&&semi-i<=16){
    char prev=i==0?0:s.charAt(i-1);boolean boundary=i==0||!word(prev);
    String value=null;
    if(ampersand&&c=='&')value=entity(s.substring(i+1,semi));
    else if(!ampersand&&c=='#'&&boundary)value=numeric(s.substring(i+1,semi));
    else if(!ampersand&&boundary&&letter(c)){String name=s.substring(i,semi);if(name.chars().allMatch(SynopsisText::letter))value=NAMED.get(name.toLowerCase(Locale.ROOT));}
    if(value!=null){out.append(value);i=semi+1;continue;}
   }
   out.append(c);i++;
  }
  return out.toString();
 }
 static String entity(String body){if(body.isEmpty())return null;if(body.charAt(0)=='#')return numeric(body.substring(1));if(!body.chars().allMatch(SynopsisText::letter))return null;return NAMED.get(body.toLowerCase(Locale.ROOT));}
 static String numeric(String body){
  if(body.isEmpty())return null;int cp;try{if(body.charAt(0)=='x'||body.charAt(0)=='X'){if(body.length()<2)return null;cp=Integer.parseInt(body.substring(1),16);}else cp=Integer.parseInt(body);}catch(NumberFormatException e){return null;}
  if(cp<0||cp>0x10FFFF||(cp>=0xD800&&cp<=0xDFFF))return null;return new String(Character.toChars(cp));
 }
 static boolean word(char c){return c>='0'&&c<='9'||c>='A'&&c<='Z'||c>='a'&&c<='z';}
 static boolean letter(int c){return c>='A'&&c<='Z'||c>='a'&&c<='z';}
}
