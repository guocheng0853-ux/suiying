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
 static String urlSafe(String text){return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));}
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
  String commented="{\n/* head */\n\"url\" /* mid */ : \"https://cdn.example/x//y\",\n// see https://example.com/hidden\n\"sites\":[ // line\n{\"key\":\"k\",\"name\":\"演示\",\"type\":3,\"api\":\"csp_Demo\"}\n]\r\n}\n";
  JSONObject commentedJson=must(commented);
  check(commentedJson.getString("url").equals("https://cdn.example/x//y")&&commentedJson.getJSONArray("sites").getJSONObject(0).getString("api").equals("csp_Demo"),"line and block comments");
  check(new JSONObject(TvBoxConfigText.normalize(commented)).toString().equals(commentedJson.toString()),"fusion load and normalize match");
  String inner="{\n// c\n\"lives\":[{\"name\":\"直播\",\"url\":\"http://live.example/a.m3u\"}]\n}";
  for(String encoded:new String[]{b64(inner),unpadded(inner),folded(inner),urlSafe(inner),b64(commented)}){
   JSONObject o=must(encoded);
   check(o.has("lives")||o.has("sites"),"base64 json");
  }
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
  System.out.println("PASS: tvbox config text comments and one base64 layer.");
 }
}
