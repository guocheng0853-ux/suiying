package com.guocheng.mediapocket;
import java.security.MessageDigest;import java.util.Locale;
final class TvSpiderRules {
 static String className(String api){if(!api.matches("csp_[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("不是有效 CSP 入口");return "com.github.catvod.spider."+api.substring(4);}
 static String md5(String declaration){String[] p=declaration.split(";md5;",2);if(p.length<2)return "";String value=p[1].trim().toLowerCase(Locale.ROOT);if(!value.matches("[0-9a-f]{32}"))throw new IllegalArgumentException("组件 MD5 格式无效");return value;}
 static String digest(byte[] bytes,String algorithm)throws Exception{byte[] hash=MessageDigest.getInstance(algorithm).digest(bytes);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static final int MAX_COMPONENT_BYTES=16*1024*1024;
 /** 校验失败。message 只有原因码，不含地址、摘要或正文。 */
 static final class CheckFailed extends IllegalArgumentException {
  final String code;final int bytes;
  CheckFailed(String code,int bytes){super(code);this.code=code;this.bytes=bytes;}
 }
 static void verify(byte[] bytes,String expected)throws Exception{if(bytes.length==0||bytes.length>MAX_COMPONENT_BYTES)throw new CheckFailed("size",bytes.length);if(!expected.isEmpty()&&!digest(bytes,"MD5").equals(expected))throw new CheckFailed("checksum",bytes.length);}
 /** 界面和日志用的原因码。只认本类抛出的校验失败；其他异常只留类名，不读 message。 */
 static String failureLabel(Throwable cause){Throwable cursor=cause;for(int i=0;i<8&&cursor!=null;i++){if(cursor instanceof CheckFailed){CheckFailed check=(CheckFailed)cursor;if("size".equals(check.code))return "size bytes="+check.bytes;if("checksum".equals(check.code))return "checksum";}cursor=cursor.getCause();}if(cause==null)return "unknown";String name=cause.getClass().getSimpleName();return name==null||name.isEmpty()?"unknown":name;}
 /** null 或按 String.trim 规则为空白时返回不含正文的原因码，否则返回空串。 */
 static String payloadFault(String text){if(text==null)return "empty";int n=text.length(),i=0,j=n;while(i<n&&text.charAt(i)<=' ')i++;while(j>i&&text.charAt(j-1)<=' ')j--;if(i<j)return "";return "empty bytes="+n;}
}
