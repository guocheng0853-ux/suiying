package com.guocheng.mediapocket;
import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.util.*;import java.util.concurrent.*;
/** Full-screen soft artwork backdrop, with tiny cached thumbnails and no per-frame bitmap allocations. */
final class HomeAtmosphere extends FrameLayout {
 final ScrapedMainActivity a;final ImageView before,after;final WeakHashMap<Bitmap,Visual> cache=new WeakHashMap<>();final Set<Bitmap> pending=Collections.newSetFromMap(new WeakHashMap<>());
 final ExecutorService colorWorker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"home-atmosphere-color");t.setDaemon(true);return t;});
 Visual shownBefore,shownAfter;int generation;int currentTone;
 static final class Blur31 {static void apply(View view,int radius){view.setRenderEffect(RenderEffect.createBlurEffect(radius,radius,Shader.TileMode.CLAMP));}}
 static final class Visual {final Bitmap thumb;final int tone;Visual(Bitmap thumb,int tone){this.thumb=thumb;this.tone=tone;}}
 HomeAtmosphere(ScrapedMainActivity a){super(a);this.a=a;currentTone=a.surface();setBackgroundColor(currentTone);before=new ImageView(a);after=new ImageView(a);for(ImageView view:new ImageView[]{before,after}){view.setScaleType(ImageView.ScaleType.CENTER_CROP);view.setAlpha(0f);addView(view,new LayoutParams(-1,-1));if(android.os.Build.VERSION.SDK_INT>=31)Blur31.apply(view,a.dp(24));}}
 Bitmap bitmap(ImageView view){if(view==null||!(view.getDrawable() instanceof BitmapDrawable))return null;Bitmap source=((BitmapDrawable)view.getDrawable()).getBitmap();if(source==null||source.isRecycled())return null;return source;}
 /** Cache-only lookup used on swipe animation frames; never samples pixels on the UI thread. */
 Visual peek(ImageView view){Bitmap source=bitmap(view);return source==null?null:cache.get(source);}
 static Visual compute(Bitmap source,boolean light){long r=0,g=0,b=0;int count=0,w=source.getWidth(),h=source.getHeight();for(int y=1;y<12;y++)for(int x=1;x<12;x++){int pixel=source.getPixel(x*w/12,y*h/12);r+=(pixel>>16)&255;g+=(pixel>>8)&255;b+=pixel&255;count++;}int average=0xff000000|((int)(r/count)<<16)|((int)(g/count)<<8)|(int)(b/count);float scale=32f/Math.max(w,h);Bitmap thumb=Bitmap.createScaledBitmap(source,Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),true);return new Visual(thumb,SceneRules.tone(average,light));}
 /** Schedule background sampling once per bitmap; results are published on the UI thread into cache. */
 void ensure(ImageView view){Bitmap source=bitmap(view);if(source==null||cache.get(source)!=null)return;synchronized(pending){if(!pending.add(source))return;}final Bitmap target=source;final boolean light=a.light;final int ticket=generation;colorWorker.execute(()->{Visual result=null;try{result=compute(target,light);}catch(RuntimeException ignored){}final Visual ready=result;post(()->{synchronized(pending){pending.remove(target);}if(ready!=null)cache.put(target,ready);if(ticket!=generation||!a.homeCanvas||a.isFinishing())return;if(ready!=null&&(shownBefore==null||bitmap(a.heroImage)==target))blend(a.heroImage,null,0);});});}
 void blend(ImageView outgoing,ImageView incoming,float progress){if(!a.homeCanvas)return;ensure(outgoing);ensure(incoming);Visual old=peek(outgoing),next=peek(incoming);if(old==null)old=shownBefore;if(next==null)next=old;if(old==null){paint(a.surface());return;}if(shownBefore!=old){shownBefore=old;before.setImageBitmap(old.thumb);}if(next!=null&&shownAfter!=next){shownAfter=next;after.setImageBitmap(next.thumb);}float t=SceneRules.fade(progress);before.setAlpha(.16f*(1-t));after.setAlpha(.16f*t);paint(SceneRules.blend(old.tone,next==null?old.tone:next.tone,t));}
 void paint(int color){if(color!=currentTone){currentTone=color;setBackgroundColor(color);}if(a.heroShadeDrawable!=null)a.heroShadeDrawable.setColors(shade(color));}
 /** 海报压暗渐变：中段加深托住标题文字，到底边回到全透明，与下方氛围背景无缝衔接（原来底边是实色，形成硬边）。 */
 static int[] shade(int color){int rgb=color&0x00ffffff;return new int[]{rgb,rgb,rgb|0x8c000000,rgb|0x73000000,rgb};}
 void sync(ImageView image){final int ticket=++generation;poll(image,ticket,0);}
 void poll(ImageView image,int ticket,int attempts){if(ticket!=generation||!a.homeCanvas||a.isFinishing())return;if(a.heroSwipe!=null&&a.heroSwipe.busy()){if(attempts<100)postDelayed(()->poll(image,ticket,attempts+1),200);return;}if(peek(image)!=null){blend(image,null,0);return;}ensure(image);if(attempts<80)postDelayed(()->poll(image,ticket,attempts+1),200);}
 void stop(){generation++;}
 @Override protected void onDetachedFromWindow(){stop();colorWorker.shutdownNow();super.onDetachedFromWindow();}
}
