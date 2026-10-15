package com.guocheng.mediapocket;
import android.app.Activity;import android.os.Looper;import android.webkit.*;import org.json.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/**
 * parse=1 线路：用不可见 WebView 打开播放页，拦截页面实际请求的视频地址，连同当时的 Referer / UA / Cookie 一起返回。
 * 必须在后台线程调用（内部切到 UI 线程创建 WebView 并等待结果）。不放行证书错误，不开放 JS 桥，不跟随非 HTTP 跳转。
 * 页面可能先请求广告视频；拿到的是第一个命中嗅探规则的地址。
 */
final class WebSniffer {
 static JSONObject sniff(Activity a,String page,JSONObject headers,long timeoutMs)throws Exception{
  if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("sniff on UI thread");
  CountDownLatch done=new CountDownLatch(1);AtomicReference<JSONObject> hit=new AtomicReference<>();AtomicReference<WebView> view=new AtomicReference<>();AtomicReference<String> failure=new AtomicReference<>();AtomicBoolean cancelled=new AtomicBoolean();
  String ua=headers==null?"":headers.optString("User-Agent",headers.optString("user-agent",""));
  a.runOnUiThread(()->{
   if(cancelled.get()||a.isFinishing()||a.isDestroyed()){failure.set("页面已关闭");done.countDown();return;}
   try{
    WebView w=new WebView(a);view.set(w);WebSettings s=w.getSettings();
    s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);s.setAllowFileAccess(false);s.setAllowContentAccess(false);
    if(!ua.isEmpty())s.setUserAgentString(ua);
    final String agent=s.getUserAgentString();
    CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(w,true);
    w.setWebViewClient(new WebViewClient(){
     @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
      String u=r.getUrl().toString();
      if(hit.get()==null&&SniffRules.isMedia(u)){
       try{JSONObject h=new JSONObject();Map<String,String> sent=r.getRequestHeaders();if(sent!=null)for(Map.Entry<String,String> e:sent.entrySet())if(e.getKey()!=null&&!e.getKey().equalsIgnoreCase("Range"))h.put(e.getKey(),e.getValue());
        if(!h.has("Referer"))h.put("Referer",page);if(!h.has("User-Agent"))h.put("User-Agent",agent);
        String cookie=CookieManager.getInstance().getCookie(u);if(cookie!=null&&!cookie.isEmpty())h.put("Cookie",cookie);
        if(hit.compareAndSet(null,new JSONObject().put("url",u).put("header",h)))done.countDown();
       }catch(Exception ignored){}
      }
      return null;
     }
     @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String scheme=r.getUrl().getScheme();return scheme==null||!(scheme.equals("http")||scheme.equals("https"));}
     @Override public void onReceivedSslError(WebView v,SslErrorHandler h,android.net.http.SslError e){h.cancel();}
    });
    w.setWebChromeClient(new WebChromeClient());
    HashMap<String,String> extra=new HashMap<>();if(headers!=null){Iterator<String> k=headers.keys();while(k.hasNext()){String key=k.next();if(!key.equalsIgnoreCase("User-Agent"))extra.put(key,headers.optString(key));}}
    w.loadUrl(page,extra);
   }catch(Throwable t){failure.set("WebView 不可用："+t.getClass().getSimpleName());done.countDown();}
  });
  boolean finished;
  try{finished=done.await(timeoutMs,TimeUnit.MILLISECONDS);}
  finally{cancelled.set(true);a.runOnUiThread(()->{WebView w=view.getAndSet(null);if(w!=null){try{w.stopLoading();w.loadUrl("about:blank");w.destroy();}catch(Throwable ignored){}}});}
  if(hit.get()!=null)return hit.get();
  if(failure.get()!=null)throw new Exception(failure.get());
  throw new Exception(finished?"网页嗅探失败":"播放页 "+(timeoutMs/1000)+" 秒内没有请求视频地址");
 }
}
