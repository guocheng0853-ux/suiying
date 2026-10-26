package com.guocheng.mediapocket;

import android.app.Activity;
import android.os.Build;
import android.view.KeyEvent;
import android.view.Window;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** The open side panel owns Back; restore the window's original dispatch on close. */
final class PlayerPanelBack {
 final Activity activity;
 final Window.Callback original, wrapper;
 OnBackInvokedCallback gesture;

 PlayerPanelBack(Activity a) {
  activity=a;original=a.getWindow().getCallback();
  wrapper=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(proxy,method,args)->{
   if("dispatchKeyEvent".equals(method.getName())&&args!=null&&args.length==1&&args[0] instanceof KeyEvent){
    KeyEvent key=(KeyEvent)args[0];
    if(key.getKeyCode()==KeyEvent.KEYCODE_BACK&&PlayerSidePanel.open(a)){
     if(key.getAction()==KeyEvent.ACTION_UP&&!key.isCanceled())PlayerSidePanel.close(a);
     return true;
    }
   }
   try{return method.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
  });
  a.getWindow().setCallback(wrapper);
  if(Build.VERSION.SDK_INT>=33){gesture=()->PlayerSidePanel.close(a);a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,gesture);}
 }

 void dispose(){
  if(Build.VERSION.SDK_INT>=33&&gesture!=null){activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(gesture);gesture=null;}
  if(activity.getWindow().getCallback()==wrapper)activity.getWindow().setCallback(original);
 }
}
