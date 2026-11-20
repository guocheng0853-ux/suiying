package com.guocheng.mediapocket;
import org.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
/** 影视仓配置正文：注释与一层 Base64。失败必须停在归一化，不能改写已保存的配置。 */
public class TvBoxConfigTextTest {
 static final String TOKEN="SECRET_TOKEN_9f3a";
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 static String sites(String api){return "{\"sites\":[{\"key\":\"k\",\"name\":\"演示\",\"type\":3,\"api\":\""+api+"\"}],\"spider\":\"https://cdn.example/app.jar\"}";}
 static JSONObject must(String raw){
  try{return FusionCatalog.parseFetched(raw);}
  catch(Exception e){throw new AssertionError("parse "+e.getClass().getSimpleName());}
 }
 static void reject(String raw,String message){
  String saved=sites("csp_Keep");
  try{
   saved=FusionCatalog.parseFetched(raw).toString();
   throw new AssertionError("accepted");
  }catch(IOException e){
   check(message.equals(e.getMessage()),"message");
   check(e.getCause()==null,"no cause");
   check(e.getMessage().indexOf(TOKEN)<0 && e.getMessage().length()<80,"sanitized");
  }catch(Exception e){throw new AssertionError("unexpected "+e.getClass().getSimpleName());}
  check(saved.equals(sites("csp_Keep")),"saved config unchanged");
 }
 static String b64(String text){return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
 static String unpadded(String text){String s=b64(text);while(s.endsWith("="))s=s.substring(0,s.length()-1);return s;}
 static String folded(String text){
  String s=b64(text);StringBuilder out=new StringBuilder(" \n");
  for(int i=0;i<s.length();i++){if(i>0&&i%16==0)out.append("\r\n");out.append(s.charAt(i));}
  return out.append("\n ").toString();
 }
 /** Android JSONTokener.nextString: the opening quote closes the string; a backslash consumes the next character. */
 static String androidField(String text,String key){
  int i=0,n=text.length();
  while(i<n){
   char c=text.charAt(i);
   if(c==' '||c=='\n'||c=='\r'||c=='\t'){i++;continue;}
   if(c=='#'){while(i<n&&text.charAt(i)!='\n'&&text.charAt(i)!='\r')i++;continue;}
   if(c=='/'&&i+1<n&&(text.charAt(i+1)=='/'||text.charAt(i+1)=='*')){
    if(text.charAt(i+1)=='/'){i+=2;while(i<n&&text.charAt(i)!='\n'&&text.charAt(i)!='\r')i++;continue;}
    int end=text.indexOf("*/",i+2);if(end<0)throw new AssertionError("unterminated comment in parity scan");i=end+2;continue;
   }
   if(c=='"'||c=='\''){
    String value=androidString(text,i);
    int after=i+1+rawLength(text,i);
    if(value.equals(key)){
     int j=after;while(j<n&&" \n\r\t".indexOf(text.charAt(j))>=0)j++;
     if(j<n&&(text.charAt(j)==':'||text.charAt(j)=='=')){j++;if(j<n&&text.charAt(j)=='>')j++;while(j<n&&" \n\r\t".indexOf(text.charAt(j))>=0)j++;if(j<n&&(text.charAt(j)=='"'||text.charAt(j)=='\''))return androidString(text,j);}
    }
    i=after;continue;
   }
   i++;
  }
  throw new AssertionError("missing field");
 }
 static int rawLength(String text,int open){
  char q=text.charAt(open);int i=open+1;boolean escape=false;
  while(i<text.length()){char c=text.charAt(i);if(escape){escape=false;i++;continue;}if(c=='\\'){escape=true;i++;continue;}if(c==q)return i-open;i++;}
  throw new AssertionError("unterminated");
 }
 static String androidString(String text,int open){
  char q=text.charAt(open);int i=open+1;StringBuilder out=new StringBuilder();boolean escape=false;
  while(i<text.length()){
   char c=text.charAt(i++);
   if(escape){out.append(c);escape=false;continue;}
   if(c=='\\'){escape=true;continue;}
   if(c==q)return out.toString();
   out.append(c);
  }
  throw new AssertionError("unterminated");
 }
 /** 与底座 lambda$6 相同：sites 或 lives 数组非 null 才替换已存配置。 */
 static String afterImport(String saved,String fetched){
  try{
   JSONObject o=new JSONObject(TvBoxConfigText.normalize(fetched));
   if(o.optJSONArray("sites")==null&&o.optJSONArray("lives")==null)throw new java.io.IOException("这不是影视仓配置，原配置已保留");
   return o.toString();
  }catch(java.io.IOException e){
   check(e.getMessage()!=null&&e.getMessage().indexOf(TOKEN)<0&&e.getMessage().length()<80,"import error sanitized");
   check(e.getCause()==null,"import error has no cause");
   return saved;
  }catch(JSONException e){throw new AssertionError("json exception leaked to import");}
 }
 public static void main(String[] args)throws Exception{
  String plain=sites("csp_Plain");
  check(TvBoxConfigText.normalize(plain).equals(plain),"plain json unchanged");
  check(must("\uFEFF \n"+plain+" \t").getJSONArray("sites").getJSONObject(0).getString("api").equals("csp_Plain"),"bom and outer space");
  check(must(" \uFEFF\uFEFF"+plain).getJSONArray("sites").getJSONObject(0).getString("api").equals("csp_Plain"),"repeated bom");
  String literal="{\"url\":\"https://example.com/a//b?q=1#f\",\"note\":\"/* keep */ // keep\",\"esc\":\"quote\\\" and \\\\ slash\"}";
  JSONObject kept=new JSONObject(TvBoxConfigText.normalize(literal));
  check(kept.getString("url").equals("https://example.com/a//b?q=1#f")&&kept.getString("note").equals("/* keep */ // keep")&&kept.getString("esc").equals("quote\" and \\ slash"),"strings keep comment-like text and escapes");
  String single="{'spider':'https://cdn.example/a//b.jar','note':'/* keep */ // keep #hash','sites':[]}";
  String singleOut=TvBoxConfigText.normalize(single);
  check(singleOut.contains("https://cdn.example/a//b.jar")&&singleOut.contains("/* keep */ // keep #hash")&&singleOut.contains("'sites'"),"single quotes keep url and comment markers");
  check(androidField(single,"spider").equals(androidField(singleOut,"spider")),"single quoted value matches Android string rules");
  String singleEsc="{'spider':'it\\'s https://cdn.example/a//b.jar','sites':[]}";
  check(TvBoxConfigText.normalize(singleEsc).contains("https://cdn.example/a//b.jar"),"escaped single quote does not end the string");
  String doubleHash="{\"spider\":\"https://cdn.example/a//b.jar#frag\", # outside\n\"sites\":[]}";
  JSONObject hashed=new JSONObject(TvBoxConfigText.normalize(doubleHash));
  check(hashed.getString("spider").equals("https://cdn.example/a//b.jar#frag"),"hash comment outside a string, hash inside kept");
  String commented="{\n/* head */\n\"url\" /* mid */ : \"https://cdn.example/x//y\",\n// see https://example.com/hidden\n\"sites\":[ // line\n{\"key\":\"k\",\"name\":\"演示\",\"type\":3,\"api\":\"csp_Demo\"}\n]\r\n}\n";
  JSONObject commentedJson=must(commented);
  check(commentedJson.getString("url").equals("https://cdn.example/x//y")&&commentedJson.getJSONArray("sites").getJSONObject(0).getString("api").equals("csp_Demo"),"line and block comments");
  check(new JSONObject(TvBoxConfigText.normalize(commented)).toString().equals(commentedJson.toString()),"fusion load and normalize match");
  String inner="{\n// c\n\"lives\":[{\"name\":\"直播\",\"url\":\"http://live.example/a.m3u\"}]\n}";
  for(String encoded:new String[]{b64(inner),unpadded(inner),folded(inner),b64(commented)}){
   JSONObject o=must(encoded);
   check(o.has("lives")||o.has("sites"),"base64 json");
  }
  String alphabetJson="{\"spider\":\"https://cdn.example/a//b.jar\",\"sites\":[],\"pad\":\">AA?\"}";
  String urlAlphabet="eyJzcGlkZXIiOiJodHRwczovL2Nkbi5leGFtcGxlL2EvL2IuamFyIiwic2l0ZXMiOltdLCJwYWQiOiI-QUE_In0";
  String stdAlphabet="eyJzcGlkZXIiOiJodHRwczovL2Nkbi5leGFtcGxlL2EvL2IuamFyIiwic2l0ZXMiOltdLCJwYWQiOiI+QUE/In0=";
  check(urlAlphabet.indexOf('-')>=0&&urlAlphabet.indexOf('_')>=0&&urlAlphabet.indexOf('+')<0&&urlAlphabet.indexOf('/')<0,"url-safe fixture uses - and _");
  check(stdAlphabet.indexOf('+')>=0&&stdAlphabet.indexOf('/')>=0,"standard fixture uses + and /");
  check(must(urlAlphabet).getString("spider").equals("https://cdn.example/a//b.jar"),"url-safe alphabet decodes");
  check(must(stdAlphabet).getString("spider").equals("https://cdn.example/a//b.jar")&&must(stdAlphabet).getString("pad").equals(">AA?"),"standard alphabet decodes");
  check(must(alphabetJson).getString("pad").equals(">AA?"),"alphabet json round trip");
  reject(urlAlphabet.replaceFirst("-","+"),"配置不是合法 JSON");
  check(must(b64(commented)).getJSONArray("sites").getJSONObject(0).getString("api").equals("csp_Demo"),"base64 of commented json");
  reject("<html>"+TOKEN+"</html>","配置不是合法 JSON");
  reject("401 Unauthorized: "+TOKEN,"配置不是合法 JSON");
  reject("****"+TOKEN,"配置不是合法 JSON");
  reject("abcde","配置不是合法 JSON");
  reject(Base64.getEncoder().encodeToString(new byte[]{(byte)0xff,(byte)0xfe,(byte)0xfd}),"配置编码无法识别");
  reject(b64(b64(plain)),"配置不是合法 JSON");
  reject("clan://"+TOKEN,"配置不是合法 JSON");
  reject("\u00ff\u00d8\u00ff"+TOKEN,"配置不是合法 JSON");
  reject("{ \"a\": 1 /* "+TOKEN,"配置注释不完整");
  reject("{\"a\":\"unterminated}","配置不是合法 JSON");
  reject("","配置内容是空的");
  reject(null,"配置内容是空的");
  String deep="1";for(int i=0;i<33;i++)deep="{\"a\":"+deep+"}";
  reject(deep,"配置结构过深");
  check(must("{\"a\":{\"a\":{\"a\":1}}}").getJSONObject("a").getJSONObject("a").getInt("a")==1,"shallow object");
  char[] huge=new char[TvBoxConfigText.MAX_CHARS+1];
  java.util.Arrays.fill(huge,' ');
  reject(new String(huge),"配置过大");
  String previous=sites("csp_Keep");
  check(afterImport(previous,"<html>"+TOKEN+"</html>").equals(previous),"html import keeps saved config");
  check(afterImport(previous,"{\"code\":401,\"msg\":\""+TOKEN+"\"}").equals(previous),"error object keeps saved config");
  String replaced=afterImport(previous,commented);
  check(!replaced.equals(previous)&&replaced.contains("csp_Demo")&&replaced.indexOf(TOKEN)<0,"commented import replaces saved config");
  check(afterImport(previous,"{\"sites\":[]}").contains("\"sites\":[]"),"empty sites array still counts as present");
  String joined=TvBoxConfigText.normalize("{\"a\":1,//c\n\"b\":2}");
  check(new JSONObject(joined).getInt("a")==1&&new JSONObject(joined).getInt("b")==2,"line comment keeps the line break");
  check(new JSONObject(singleOut).getString("spider").equals("https://cdn.example/a//b.jar"),"runtime JSONObject accepts the single-quote fixture");
  String trailing="{\"sites\":[]} trailing-junk";
  check(new JSONObject(TvBoxConfigText.normalize(trailing)).optJSONArray("sites")!=null,"trailing text after a finished object is kept, same as JSONObject");
  String leaked="{\"spider\":\"https://cdn.example/a//b.jar?token=SECRET\",\"sites\":}";
  String keptConfig=sites("csp_Keep");
  try{
   String shown="加载失败："+TvBoxConfigText.normalize(leaked);
   throw new AssertionError("balanced invalid value accepted "+shown);
  }catch(IOException e){
   String shown="加载失败："+(e.getMessage()==null?"":e.getMessage());
   check(e.getMessage().equals("配置不是合法 JSON")&&e.getCause()==null,"fixed import error");
   check(shown.indexOf("SECRET")<0&&shown.indexOf("https://")<0&&shown.indexOf("cdn.example")<0&&shown.indexOf(leaked)<0,"ui text hides the config");
   check(e.toString().indexOf("SECRET")<0,"exception text hides the config");
  }
  check(afterImport(keptConfig,leaked).equals(keptConfig),"invalid balanced value keeps the saved config");
  try{FusionCatalog.parseFetched(leaked);throw new AssertionError("fusion accepted");}
  catch(IOException e){check(e.getMessage().equals("配置不是合法 JSON")&&e.getCause()==null&&e.getMessage().indexOf("SECRET")<0,"fusion error stays fixed");}
  System.out.println("PASS: tvbox config text comments and one base64 layer.");
 }
}
