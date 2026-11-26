package com.guocheng.mediapocket;
import com.guocheng.mediapocket.rules.CheckFailed;
import com.guocheng.mediapocket.rules.SpiderComponentRules;
/** Facade over {@link SpiderComponentRules}; keeps package-private call sites stable. */
final class TvSpiderRules {
 static String className(String api){return SpiderComponentRules.className(api);}
 static String md5(String declaration){return SpiderComponentRules.md5(declaration);}
 static String digest(byte[] bytes,String algorithm)throws Exception{return SpiderComponentRules.digest(bytes,algorithm);}
 static final int MAX_COMPONENT_BYTES=SpiderComponentRules.MAX_COMPONENT_BYTES;
 static void verify(byte[] bytes,String expected)throws Exception{SpiderComponentRules.verify(bytes,expected);}
 /** 界面和日志用的原因码。只认校验失败；其他异常只留类名，不读 message。 */
 static String failureLabel(Throwable cause){Throwable cursor=cause;for(int i=0;i<8&&cursor!=null;i++){if(cursor instanceof CheckFailed){CheckFailed check=(CheckFailed)cursor;return SpiderComponentRules.checkLabel(check.code,check.bytes);}cursor=cursor.getCause();}if(cause==null)return "unknown";String name=cause.getClass().getSimpleName();return name==null||name.isEmpty()?"unknown":name;}
 static String payloadFault(String text){return SpiderComponentRules.payloadFault(text);}
 static byte[] readComponent(java.io.InputStream in)throws java.io.IOException{return SpiderComponentRules.readComponent(in);}
 static final class Report {final java.io.IOException error;final String log;Report(java.io.IOException error,String log){this.error=error;this.log=log;}}
 static Report componentReport(String operation,Throwable cause){String label=failureLabel(cause);return new Report(new java.io.IOException("线路组件错误："+label,cause),logLine(operation,label,cause));}
 static Report payloadReport(String operation,String text){String fault=payloadFault(text);if(fault.isEmpty())return null;return new Report(new java.io.IOException("线路返回空数据"),logLine(operation,fault,null));}
 static String logLine(String operation,String label,Throwable cause){String where="";if(cause!=null){for(StackTraceElement frame:cause.getStackTrace()){String owner=frame.getClassName();if(owner.equals(TvSpiderRules.class.getName())||owner.equals(SpiderComponentRules.class.getName())||owner.equals("com.guocheng.mediapocket.TvBoxRules")||owner.equals("com.guocheng.mediapocket.TvSpiderDispatch")||owner.equals("com.guocheng.mediapocket.TvSpiderRuntime")){String simple=owner.equals(SpiderComponentRules.class.getName())?"TvSpiderRules":owner.substring(owner.lastIndexOf('.')+1);where=" at "+simple+"."+frame.getMethodName()+":"+frame.getLineNumber();break;}}}if(cause!=null&&where.isEmpty())return null;return DebugText.sanitize((operation==null?"":operation)+": "+label+where);}
}
