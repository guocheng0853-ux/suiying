package com.guocheng.mediapocket;
import java.util.concurrent.ConcurrentHashMap;import java.util.regex.Pattern;
/**
 * 0.1.182：正则编译结果缓存。
 * 片源解析（StreamInfo.parse）一条片源要试二十多个正则，原来每次都 Pattern.compile；Torrentio 一集返回上百条，
 * 排序、筛选、画卡片又各解析一遍，等于在主线程上编译几千次同样的正则。这里按（表达式, flags）只编译一次。
 * 上限 {@link #LIMIT} 条：带变量拼出来的表达式不会把缓存撑大，超过后照常编译、不再缓存。
 */
final class Rx {
 private Rx(){}
 static final int LIMIT=512;
 private static final ConcurrentHashMap<String,Pattern> CACHE=new ConcurrentHashMap<>();
 /** 测试可以换成计数实现，确认命中缓存时不再编译。 */
 interface Compiler { Pattern compile(String regex,int flags); }
 static Compiler compiler=Pattern::compile;
 static Pattern of(String regex){return of(regex,0);}
 static Pattern of(String regex,int flags){
  String key=flags==0?regex:flags+"\u0000"+regex;
  Pattern hit=CACHE.get(key);if(hit!=null)return hit;
  Pattern compiled=compiler.compile(regex,flags);
  synchronized(CACHE){Pattern existing=CACHE.get(key);if(existing!=null)return existing;if(CACHE.size()<LIMIT)CACHE.put(key,compiled);return compiled;}
 }
 static int size(){return CACHE.size();}
}
