package com.guocheng.mediapocket;
import java.util.*;import com.github.catvod.crawler.Spider;
public class TvSpiderRulesTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static class Fake extends Spider{String seen="";public String homeContent(boolean filter){seen="home:"+filter;return "home";}public String categoryContent(String id,String page,boolean filter,HashMap<String,String> ext){seen=id+":"+page+":"+filter;return "category";}public String searchContent(String word,boolean quick){return "";}public String searchContent(String word,boolean quick,String page){seen=word+":"+quick+":"+page;return "search";}public String detailContent(List<String> ids){seen=ids.get(0);return "detail";}public String playerContent(String flag,String id,List<String> vip){seen=flag+":"+id+":"+vip.get(0);return "player";}}
 static class DualSearch extends Spider {public String searchContent(String key,boolean quick){return "{\"list\":[{\"vod_id\":\"legacy\"}]}";}public String searchContent(String key,boolean quick,String page){return "{\"list\":[]}";}}
 static class MutatingDetail extends Spider {public String detailContent(List<String> ids){String original=ids.get(0);ids.set(0,"resolved:"+original);ids.add("temporary");ids.remove(1);return ids.get(0);}}
 static String clip(String s){return s==null?"":s.length()>220?s.substring(0,220):s;}
 public static void main(String[] args)throws Exception{byte[] bytes="fixture".getBytes("UTF-8");String md5=TvSpiderRules.digest(bytes,"MD5");TvSpiderRules.verify(bytes,md5);check(TvSpiderRules.md5("https://example.org/jar;md5;"+md5.toUpperCase(Locale.ROOT)).equals(md5),"case-normalized checksum");boolean blocked=false;try{TvSpiderRules.verify(bytes,"00000000000000000000000000000000");}catch(Exception e){blocked=true;}check(blocked,"checksum mismatch blocks load");check(TvSpiderRules.className("csp_LibvioGuard").equals("com.github.catvod.spider.LibvioGuard"),"CSP class mapping");blocked=false;try{TvSpiderRules.className("csp_../../Other");}catch(Exception e){blocked=true;}check(blocked,"invalid class input rejected");Fake f=new Fake();List<String> vip=Arrays.asList("VIP");check(TvSpiderDispatch.invoke(f,"home","","",1,vip).equals("home")&&f.seen.equals("home:true"),"home call");TvSpiderDispatch.invoke(f,"category","movies","",2,vip);check(f.seen.equals("movies:2:true"),"category args");TvSpiderDispatch.invoke(f,"search","test","",3,vip);check(f.seen.equals("test:false:3"),"paged search fallback");TvSpiderDispatch.invoke(f,"detail","video1","",1,vip);check(f.seen.equals("video1"),"detail IDs");TvSpiderDispatch.invoke(f,"player","token","route",1,vip);check(f.seen.equals("route:token:VIP"),"player args and VIP flags");check(TvSpiderDispatch.invoke(new MutatingDetail(),"detail","video1","",1,vip).equals("resolved:video1"),"detail plugin may replace/add/remove IDs without UnsupportedOperationException");check(TvSpiderDispatch.invoke(new DualSearch(),"search","test","",1,vip).contains("legacy"),"empty paged response falls back to implemented legacy search");
 byte[] empty=new byte[0];String sizeLabel="";try{TvSpiderRules.verify(empty,"");}catch(Exception e){sizeLabel=TvSpiderRules.failureLabel(e);check(e instanceof IllegalArgumentException,"empty component still rejected");}check(sizeLabel.equals("size bytes=0"),"empty component is size bytes=0, got "+sizeLabel);
 String declaredEmpty="";try{TvSpiderRules.verify(empty,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");}catch(Exception e){declaredEmpty=TvSpiderRules.failureLabel(e);}check(declaredEmpty.equals("size bytes=0"),"empty bytes stay size even when an md5 is declared, got "+declaredEmpty);
 int limit=16*1024*1024;byte[] exact=new byte[limit];String exactMd5=TvSpiderRules.digest(exact,"MD5");TvSpiderRules.verify(exact,exactMd5);byte[] over=new byte[limit+1];String overLabel="";try{TvSpiderRules.verify(over,"");}catch(Exception e){overLabel=TvSpiderRules.failureLabel(e);}check(overLabel.equals("size bytes="+(limit+1)),"oversize is size with byte count, got "+overLabel);
 String mismatch="";try{TvSpiderRules.verify(bytes,"00000000000000000000000000000000");}catch(Exception e){mismatch=TvSpiderRules.failureLabel(e);}check(mismatch.equals("checksum"),"md5 mismatch is checksum, got "+mismatch);
 Exception noisy=new java.io.IOException("https://cdn.example/jar?token=SECRET");
 String leaked=TvSpiderRules.failureLabel(noisy);check(leaked.equals("IOException")&&!leaked.contains("http")&&!leaked.contains("SECRET")&&!leaked.contains("cdn.example"),"unrelated failure stays the class name and drops the url");
 check(TvSpiderRules.payloadFault(null).equals("empty"),"null payload is empty, got "+TvSpiderRules.payloadFault(null));
 String blanks=" \n\t";String blankFault=TvSpiderRules.payloadFault(blanks);check(blanks.length()==3&&blankFault.equals("empty bytes=3"),"blank payload reports length only, got "+blankFault);
 check(TvSpiderRules.payloadFault("{\"list\":[]}").isEmpty(),"json empty list is not a blank payload");
 String body="https://cdn.example/list?token=SECRET";String bodyFault=TvSpiderRules.payloadFault(body);check(bodyFault.isEmpty()&&!bodyFault.contains("http")&&!bodyFault.contains("SECRET"),"non-blank payload is not classified empty and is not echoed");
 java.io.IOException wrapped=new java.io.IOException("https://cdn.example/jar?token=SECRET");try{TvSpiderRules.verify(bytes,"00000000000000000000000000000000");}catch(Exception e){wrapped.initCause(e);}String wrappedLabel=TvSpiderRules.failureLabel(wrapped);check(wrappedLabel.equals("checksum")&&!wrappedLabel.contains("http")&&!wrappedLabel.contains("SECRET")&&!wrappedLabel.contains("cdn"),"wrapped checksum label drops the url, got "+wrappedLabel);
 check(TvSpiderRules.failureLabel(null).equals("unknown"),"null cause has no message");
 Throwable chain=null;try{TvSpiderRules.verify(bytes,"00000000000000000000000000000000");}catch(Exception e){chain=e;}for(int i=0;i<3;i++){java.io.IOException next=new java.io.IOException("https://cdn.example/wrap?token=SECRET");next.initCause(chain);chain=next;}String deepLabel=TvSpiderRules.failureLabel(chain);check(deepLabel.equals("checksum")&&!deepLabel.contains("http")&&!deepLabel.contains("SECRET"),"checksum stays visible through a short cause chain, got "+deepLabel);
 Throwable buried=null;try{TvSpiderRules.verify(bytes,"00000000000000000000000000000000");}catch(Exception e){buried=e;}for(int i=0;i<9;i++){java.io.IOException next=new java.io.IOException("https://cdn.example/wrap?token=SECRET");next.initCause(buried);buried=next;}String buriedLabel=TvSpiderRules.failureLabel(buried); check(buriedLabel.equals("IOException")&&!buriedLabel.contains("http")&&!buriedLabel.contains("SECRET")&&!buriedLabel.contains("cdn"),"a too-deep cause chain does not leak the wrapper url");
 check(TvSpiderRules.MAX_COMPONENT_BYTES==16*1024*1024,"cap stays 16MiB");
 int cap=TvSpiderRules.MAX_COMPONENT_BYTES;
 Throwable overEx=null;try{TvSpiderRules.readComponent(new java.io.ByteArrayInputStream(new byte[cap+1]));}catch(Throwable e){overEx=e;}
 TvSpiderRules.Report overUi=TvSpiderRules.componentReport("category",overEx);
 check(overUi.error.getMessage().equals("线路组件错误：size bytes="+(cap+1)),"download cap reaches the UI as size, got "+overUi.error.getMessage());
 check(overUi.log!=null&&overUi.log.contains("size bytes="+(cap+1))&&overUi.log.contains("TvSpiderRules.readComponent")&&!overUi.log.contains("http"),"oversize log names the download frame, got "+clip(overUi.log));
 byte[] downloadedEmpty=TvSpiderRules.readComponent(new java.io.ByteArrayInputStream(new byte[0]));
 Throwable emptyEx=null;try{TvSpiderRules.verify(downloadedEmpty,"");}catch(Throwable e){emptyEx=e;}
 TvSpiderRules.Report emptyUi=TvSpiderRules.componentReport("detail",emptyEx);
 check(emptyUi.error.getMessage().equals("线路组件错误：size bytes=0"),"empty download then verify is size bytes=0, got "+emptyUi.error.getMessage());
 Throwable sumEx=null;try{TvSpiderRules.verify(TvSpiderRules.readComponent(new java.io.ByteArrayInputStream("fixture".getBytes("UTF-8"))),"00000000000000000000000000000000");}catch(Throwable e){sumEx=e;}
 TvSpiderRules.Report sumUi=TvSpiderRules.componentReport("detail",sumEx);
 check(sumUi.error.getMessage().equals("线路组件错误：checksum")&&!sumUi.error.getMessage().contains("0000"),"checksum mismatch UI, got "+sumUi.error.getMessage());
 check(sumUi.log!=null&&sumUi.log.contains("checksum")&&sumUi.log.contains("TvSpiderRules.verify")&&!sumUi.log.contains("00000000000000000000000000000000"),"checksum log keeps the host frame and drops the digest, got "+clip(sumUi.log));
 String decl="https://cdn.example/component.jar?token=SECRET;md5;not-hex";
 Throwable md5Ex=null;try{TvSpiderRules.md5(decl);}catch(Throwable e){md5Ex=e;}
 TvSpiderRules.Report md5Ui=TvSpiderRules.componentReport("category",md5Ex);
 check(md5Ui.error.getMessage().equals("线路组件错误：md5-format"),"malformed md5 declaration, got "+md5Ui.error.getMessage());
 check(md5Ui.log!=null&&!md5Ui.error.getMessage().contains("cdn.example")&&!md5Ui.log.contains("cdn.example")&&!md5Ui.log.contains("SECRET")&&!md5Ui.log.contains("not-hex"),"md5-format does not echo the declaration, got "+clip(md5Ui.log));
 Throwable nameEx=null;try{TvSpiderRules.className("csp_../../Other?token=SECRET");}catch(Throwable e){nameEx=e;}
 TvSpiderRules.Report nameUi=TvSpiderRules.componentReport("category",nameEx);
 check(nameUi.error.getMessage().equals("线路组件错误：classname"),"bad CSP name, got "+nameUi.error.getMessage());
 check(nameUi.log!=null&&!nameUi.error.getMessage().contains("SECRET")&&!nameUi.log.contains("SECRET")&&!nameUi.log.contains(".."),"classname does not echo the api text, got "+clip(nameUi.log));
 TvSpiderRules.Report nullUi=TvSpiderRules.payloadReport("category",null);
 check(nullUi.error.getMessage().equals("线路返回空数据"),"null payload stays 线路返回空数据");
 check(nullUi.log!=null&&nullUi.log.equals(DebugText.sanitize("category: empty"))&&nullUi.log.equals("category: empty")&&!nullUi.log.contains("http"),"null payload log, got "+clip(nullUi.log));
 TvSpiderRules.Report blankUi=TvSpiderRules.payloadReport("category"," \n\t");
 check(blankUi.error.getMessage().equals("线路返回空数据"),"blank payload stays 线路返回空数据");
 check(blankUi.log!=null&&blankUi.log.equals("category: empty bytes=3")&&!blankUi.log.contains("http"),"blank payload log has length only, got "+clip(blankUi.log));
 check(TvSpiderRules.payloadReport("category","{\"list\":[]}")==null,"json empty list is not a blank payload");
 java.io.IOException foreign=new java.io.IOException("https://cdn.example/jar?token=SECRET");
 TvSpiderRules.Report foreignUi=TvSpiderRules.componentReport("category",foreign);
 check(foreignUi.error.getMessage().equals("线路组件错误：IOException")&&!foreignUi.error.getMessage().contains("http")&&!foreignUi.error.getMessage().contains("SECRET"),"unrelated URL stays out of the UI, got "+foreignUi.error.getMessage());
 check(foreignUi.log==null,"host-frame gate drops a URL from outside the loader");
 String poisoned="https://cdn.example/a.jar?token=SECRET";
 String sanitized=TvSpiderRules.logLine("category",poisoned,null);
 check(sanitized.equals(DebugText.sanitize("category: "+poisoned))&&sanitized.contains("cdn.example")&&sanitized.contains("/…")&&!sanitized.contains("SECRET")&&!sanitized.contains("a.jar")&&!sanitized.contains("token="),"logLine runs DebugText.sanitize, got "+clip(sanitized));
 byte[] capped=TvSpiderRules.readComponent(new java.io.ByteArrayInputStream(new byte[cap]));
 TvSpiderRules.verify(capped,TvSpiderRules.digest(capped,"MD5"));
 check(capped.length==cap,"a component of exactly 16MiB still passes the download cap");
  System.out.println("PASS: mutable detail ID regression; component checksums, CSP class mapping, all five operation arguments and paged search fallback; size and checksum labels");}
}
