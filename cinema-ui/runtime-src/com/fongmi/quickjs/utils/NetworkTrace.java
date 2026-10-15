package com.fongmi.quickjs.utils;

import java.net.URI;
import java.util.*;

/** Bounded per-JS-executor diagnostics. Never stores paths, queries, headers or bodies. */
public final class NetworkTrace {
    private static final ThreadLocal<Deque<Map<String,Object>>> EVENTS = ThreadLocal.withInitial(ArrayDeque::new);
    public static void clear(){EVENTS.get().clear();}
    public static void request(String url,int timeoutMs){
        record(url,0,0,"request_started",null);
        EVENTS.get().getLast().put("timeoutMs",timeoutMs);
    }
    public static void record(String url,int code,int bytes,String kind,Throwable error){
        Map<String,Object> row=new LinkedHashMap<>();
        String host="invalid";try{String h=new URI(url).getHost();if(h!=null)host=h;}catch(Exception ignored){}
        row.put("host",host);row.put("httpStatus",code);row.put("responseBytes",bytes);row.put("contentKind",kind);
        if(error!=null)row.put("error",error.getClass().getName());
        Deque<Map<String,Object>> rows=EVENTS.get();if(rows.size()>=8)rows.removeFirst();rows.addLast(row);
    }
    public static List<Map<String,Object>> snapshot(){return new ArrayList<>(EVENTS.get());}
    public static String kind(byte[] bytes){
        for(byte b:bytes){int c=b&255;if(c==32||c==9||c==10||c==13)continue;return c=='{'||c=='['?"json_like":c=='<'?"html_like":"other";}return "empty";
    }
}
