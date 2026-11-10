package com.guocheng.mediapocket;
import java.lang.reflect.*;import java.util.concurrent.ConcurrentHashMap;
/**
 * 0.1.176：反射查找结果缓存。
 * 扩展层通过反射读写原版播放页的私有字段（video、top、state、vw、vh…），播放时每 250ms 的轮询、字幕渲染、手势和面板
 * 一秒要查上百次。原来每次都沿着类继承链 getDeclaredField，子类上找不到就抛一次 NoSuchFieldException（要填调用栈），
 * 再到父类找。现在同一个（类, 名字）只找一次，找不到也记住。纯 Java，可单测。
 */
final class ReflectCache {
 private ReflectCache(){}
 private static final Object MISSING=new Object();
 private static final ConcurrentHashMap<Class<?>,ConcurrentHashMap<String,Object>> FIELDS=new ConcurrentHashMap<>(),METHODS=new ConcurrentHashMap<>();
 /** 沿继承链找到的第一个同名字段（已 setAccessible）；没有返回 null。 */
 static Field field(Class<?> type,String name){
  if(type==null||name==null)return null;
  ConcurrentHashMap<String,Object> known=FIELDS.get(type);if(known==null){FIELDS.putIfAbsent(type,new ConcurrentHashMap<String,Object>());known=FIELDS.get(type);}
  Object hit=known.get(name);if(hit!=null)return hit==MISSING?null:(Field)hit;
  Field found=null;
  for(Class<?> c=type;c!=null&&found==null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);found=f;}catch(Exception|LinkageError ignored){}
  known.put(name,found==null?MISSING:found);return found;
 }
 /** 沿继承链找到的第一个同名同参数方法（已 setAccessible）；没有返回 null。 */
 static Method method(Class<?> type,String name,Class<?>[] types){
  if(type==null||name==null)return null;
  ConcurrentHashMap<String,Object> known=METHODS.get(type);if(known==null){METHODS.putIfAbsent(type,new ConcurrentHashMap<String,Object>());known=METHODS.get(type);}
  String key=key(name,types);Object hit=known.get(key);if(hit!=null)return hit==MISSING?null:(Method)hit;
  Method found=null;
  for(Class<?> c=type;c!=null&&found==null;c=c.getSuperclass())try{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);found=m;}catch(NoSuchMethodException|LinkageError ignored){}
  known.put(key,found==null?MISSING:found);return found;
 }
 static String key(String name,Class<?>[] types){if(types==null||types.length==0)return name;StringBuilder b=new StringBuilder(name);for(Class<?> t:types)b.append(',').append(t==null?"?":t.getName());return b.toString();}
}
