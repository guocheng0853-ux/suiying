package com.guocheng.mediapocket;
import java.util.*;
/** 内存里最多留两份 BIF。一份就是整段预览文件（可达十几 MB），看很多集时不能一直堆着。 */
final class BifCache {
 static final int CAP=2;
 private final Map<String,BifIndex> map=new LinkedHashMap<String,BifIndex>(4,.75f,true){
  protected boolean removeEldestEntry(Map.Entry<String,BifIndex> e){return size()>CAP;}
 };
 synchronized BifIndex get(String id){return id==null?null:map.get(id);}
 synchronized void put(String id,BifIndex index){if(id!=null&&index!=null)map.put(id,index);}
 synchronized boolean contains(String id){return id!=null&&map.containsKey(id);}
 synchronized int size(){return map.size();}
 synchronized void clear(){map.clear();}
}
