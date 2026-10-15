package com.guocheng.mediapocket;
import java.security.MessageDigest;import java.util.Locale;
final class TvSpiderRules {
 static String className(String api){if(!api.matches("csp_[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("不是有效 CSP 入口");return "com.github.catvod.spider."+api.substring(4);}
 static String md5(String declaration){String[] p=declaration.split(";md5;",2);if(p.length<2)return "";String value=p[1].trim().toLowerCase(Locale.ROOT);if(!value.matches("[0-9a-f]{32}"))throw new IllegalArgumentException("组件 MD5 格式无效");return value;}
 static String digest(byte[] bytes,String algorithm)throws Exception{byte[] hash=MessageDigest.getInstance(algorithm).digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static void verify(byte[] bytes,String expected)throws Exception{if(bytes.length==0||bytes.length>16*1024*1024)throw new IllegalArgumentException("组件大小无效");if(!expected.isEmpty()&&!digest(bytes,"MD5").equals(expected))throw new IllegalArgumentException("组件校验不一致，已停止加载");}
}
