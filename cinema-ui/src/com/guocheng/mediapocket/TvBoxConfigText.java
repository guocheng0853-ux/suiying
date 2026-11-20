package com.guocheng.mediapocket;
import org.json.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Base64;
/**
 * 影视仓配置正文。设备上的 org.json.JSONTokener 已经接受单引号、双引号，以及字符串外的 //、块注释和 # 行注释
 * （AOSP libcore JSONTokener.nextCleanInternal / nextString）。这里只在同样的字符串边界内去掉注释，并解一层 Base64。
 * 括号配平之后再用运行时的 JSONObject 验一次。平台 JSONException 的 message 会带上整段原文，所以换成固定 IOException，cause 保持为空。
 * 完整对象后面的多余文字与 new JSONObject 一样不因此失败。这不是 RFC 严格 JSON。
 */
final class TvBoxConfigText {
 static final int MAX_CHARS=8*1024*1024;
 static final int MAX_DEPTH=32;
 static String fetchConfig(String address,JSONObject body,String ua)throws Exception{return normalize(TvBoxNet.fetch(address,body,ua));}
 static String normalize(String raw)throws IOException{
  if(raw==null||raw.isEmpty())throw new IOException("配置内容是空的");
  if(raw.length()>MAX_CHARS)throw new IOException("配置过大");
  String text=bom(raw.trim());
  if(text.isEmpty())throw new IOException("配置内容是空的");
  if(base64(text)){
   String decoded=decode(text);
   if(decoded.length()>MAX_CHARS)throw new IOException("配置过大");
   return json(decoded);
  }
  return json(text);
 }
 private static String bom(String text){int i=0;while(i<text.length()&&text.charAt(i)=='\uFEFF')i++;return i==0?text:text.substring(i).trim();}
 private static String json(String text)throws IOException{
  text=strip(bom(text)).trim();
  if(text.isEmpty()||text.charAt(0)!='{')throw new IOException("配置不是合法 JSON");
  structure(text);
  try{new JSONObject(text);}catch(JSONException e){throw new IOException("配置不是合法 JSON");}
  return text;
 }
 private static boolean base64(String text){
  int body=0,pad=0;boolean padding=false,url=false,std=false;
  for(int i=0;i<text.length();i++){
   char c=text.charAt(i);
   if(c==' '||c=='\n'||c=='\r'||c=='\t')continue;
   if(c=='='){if(pad==2)return false;padding=true;pad++;continue;}
   if(padding)return false;
   if(c=='+'||c=='/')std=true;
   else if(c=='-'||c=='_')url=true;
   else if(!alnum(c))return false;
   body++;
  }
  return body>0&&body%4!=1&&!(std&&url);
 }
 private static String decode(String text)throws IOException{
  StringBuilder body=new StringBuilder(text.length());
  int pad=0;boolean padding=false,url=false;
  for(int i=0;i<text.length();i++){
   char c=text.charAt(i);
   if(c==' '||c=='\n'||c=='\r'||c=='\t')continue;
   if(c=='='){padding=true;pad++;continue;}
   if(c=='-'||c=='_')url=true;
   body.append(c);
  }
  int need=(4-body.length()%4)%4;
  if(pad!=0&&pad!=need)throw new IOException("配置不是合法 JSON");
  if(url)for(int i=0;i<body.length();i++){char c=body.charAt(i);if(c=='-')body.setCharAt(i,'+');else if(c=='_')body.setCharAt(i,'/');}
  for(int i=0;i<need;i++)body.append('=');
  byte[] bytes;
  try{bytes=Base64.getDecoder().decode(body.toString());}
  catch(IllegalArgumentException e){throw new IOException("配置不是合法 JSON");}
  try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
  catch(CharacterCodingException e){throw new IOException("配置编码无法识别");}
 }
 private static String strip(String text)throws IOException{
  StringBuilder out=new StringBuilder(text.length());
  char quote=0;boolean escape=false;int n=text.length(),i=0;
  while(i<n){
   char c=text.charAt(i);
   if(quote!=0){
    out.append(c);i++;
    if(escape){escape=false;continue;}
    if(c=='\\'){escape=true;continue;}
    if(c==quote)quote=0;
    continue;
   }
   if(c=='"'||c=='\''){quote=c;out.append(c);i++;continue;}
   if(c=='#'){i=lineComment(text,i+1,out);continue;}
   if(c=='/'&&i+1<n){
    char next=text.charAt(i+1);
    if(next=='/'){
     i+=2;
     while(i<n){
      char d=text.charAt(i);
      if(d=='\n'||d=='\u2028'||d=='\u2029'){out.append(d);i++;break;}
      if(d=='\r'){out.append('\n');i++;if(i<n&&text.charAt(i)=='\n')i++;break;}
      i++;
     }
     continue;
    }
    if(next=='*'){
     int j=i+2;
     while(j+1<n&&!(text.charAt(j)=='*'&&text.charAt(j+1)=='/'))j++;
     if(j+1>=n)throw new IOException("配置注释不完整");
     out.append(' ');i=j+2;continue;
    }
   }
   out.append(c);i++;
  }
   if(quote!=0||escape)throw new IOException("配置不是合法 JSON");
  return out.toString();
 }
 private static int lineComment(String text,int i,StringBuilder out){
  int n=text.length();
  while(i<n){
   char d=text.charAt(i);
   if(d=='\n'||d=='\u2028'||d=='\u2029'){out.append(d);return i+1;}
   if(d=='\r'){out.append('\n');if(i+1<n&&text.charAt(i+1)=='\n')return i+2;return i+1;}
   i++;
  }
  return i;
 }
 private static void structure(String text)throws IOException{
  int n=text.length(),i=0;
  while(i<n&&ws(text.charAt(i)))i++;
  if(i>=n||text.charAt(i)!='{')throw new IOException("配置不是合法 JSON");
  int[] stack=new int[MAX_DEPTH];int sp=0;char quote=0;boolean escape=false;
  for(;i<n;i++){
   char c=text.charAt(i);
   if(quote!=0){
    if(escape){escape=false;continue;}
    if(c=='\\'){escape=true;continue;}
    if(c==quote)quote=0;
    continue;
   }
   if(c=='"'||c=='\''){quote=c;continue;}
   if(c=='{'||c=='['){
    if(sp==MAX_DEPTH)throw new IOException("配置结构过深");
    stack[sp++]=c=='{'?'}':']';
    continue;
   }
   if(c=='}'||c==']'){
    if(sp==0||stack[sp-1]!=c)throw new IOException("配置不是合法 JSON");
    if(--sp==0)return;
   }
  }
  throw new IOException("配置不是合法 JSON");
 }
 private static boolean alnum(char c){return c>='A'&&c<='Z'||c>='a'&&c<='z'||c>='0'&&c<='9';}
 private static boolean ws(char c){return c==' '||c=='\n'||c=='\r'||c=='\t';}
}
