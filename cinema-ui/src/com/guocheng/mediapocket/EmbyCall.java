package com.guocheng.mediapocket;
import java.io.InterruptedIOException;import java.net.HttpURLConnection;import java.util.concurrent.*;
/**
 * 0.1.147 一次可取消的 Emby 请求链（开播 / 重连）。
 * 取消时：之后的请求不再发出；还在等响应头的连接从后台线程断开，等待马上结束。
 * 已经在读正文时，桌面 JDK 断开会先把剩余正文读完（见 EmbyHealthHttp），所以只在 Android 的 OkHttp 实现上断开；
 * 其余情况由读取循环在下一次 read 返回后自行退出。
 * 开播请求放在可伸缩的线程池里，被取消、还没退出的旧请求不会让新的重连排队（C4）。
 */
final class EmbyCall {
 static final ThreadLocal<EmbyCall> CURRENT=new ThreadLocal<>();
 private static final ExecutorService ABORT=pool("emby-abort");
 private volatile boolean cancelled,reading;private volatile HttpURLConnection conn;

 /** 守护线程、空闲 30 秒回收的线程池；任务之间互不排队。 */
 static ExecutorService pool(String name){ThreadPoolExecutor p=new ThreadPoolExecutor(0,Integer.MAX_VALUE,30,TimeUnit.SECONDS,new SynchronousQueue<>(),r->{Thread t=new Thread(r,name);t.setDaemon(true);return t;});return p;}

 boolean cancelled(){return cancelled;}
 void cancel(){cancelled=true;HttpURLConnection c=conn;if(c!=null&&(!reading||abortableWhileReading(c)))ABORT.execute(()->{try{c.disconnect();}catch(RuntimeException ignored){}});}
 /** Android 的 HttpURLConnection 由 OkHttp 实现，断开会直接关掉连接，读正文的线程立即返回。 */
 static boolean abortableWhileReading(HttpURLConnection c){return c!=null&&c.getClass().getName().startsWith("com.android.okhttp.");}

 /** 当前线程正在执行的调用已被取消时抛出（不是超时，不会被当成可以换备用地址重试的网络错误）。 */
 static void check()throws InterruptedIOException{EmbyCall c=CURRENT.get();if(c!=null&&c.cancelled)throw new Cancelled();}
 static void attach(HttpURLConnection conn)throws InterruptedIOException{EmbyCall c=CURRENT.get();if(c==null)return;c.reading=false;c.conn=conn;if(c.cancelled){c.conn=null;conn.disconnect();throw new Cancelled();}}
 static void reading(HttpURLConnection conn){EmbyCall c=CURRENT.get();if(c!=null&&c.conn==conn)c.reading=true;}
 static void detach(HttpURLConnection conn){EmbyCall c=CURRENT.get();if(c!=null&&c.conn==conn){c.conn=null;c.reading=false;}}
 static boolean isCancel(Throwable e){return e instanceof Cancelled;}

 static final class Cancelled extends InterruptedIOException {Cancelled(){super("已取消");}}
}
