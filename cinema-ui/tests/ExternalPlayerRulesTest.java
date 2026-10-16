package com.guocheng.mediapocket;
import org.json.*;import java.util.*;
public class ExternalPlayerRulesTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args)throws Exception{
  check(ExternalPlayerRules.sensitive("Cookie")&&ExternalPlayerRules.sensitive("authorization")&&ExternalPlayerRules.sensitive("Proxy-Authorization")&&ExternalPlayerRules.sensitive("X-Api-Key"),"credential headers flagged");
  check(!ExternalPlayerRules.sensitive("User-Agent")&&!ExternalPlayerRules.sensitive("Referer")&&!ExternalPlayerRules.sensitive("Origin"),"non-credential headers kept eligible");
  JSONObject req=new JSONObject().put("User-Agent","UA").put("Referer","https://example/").put("Cookie","sid=secret").put("Authorization","Bearer x").put("Origin","https://example");
  LinkedHashMap<String,String> safe=ExternalPlayerRules.sanitize(req);
  check(safe.containsKey("User-Agent")&&safe.containsKey("Referer")&&safe.containsKey("Origin"),"safe headers retained");
  check(!safe.containsKey("Cookie")&&!safe.containsKey("Authorization"),"credentials stripped");
  String[] pairs=ExternalPlayerRules.pairs(safe);check(pairs.length==6,"flat pairs for Intent extras");
  check(ExternalPlayerRules.sanitize(null).isEmpty()&&ExternalPlayerRules.pairs(null).length==0,"null-safe");
  System.out.println("PASS: external player header sanitization strips credentials, keeps User-Agent/Referer/Origin.");
 }
}
