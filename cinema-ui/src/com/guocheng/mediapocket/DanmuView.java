package com.guocheng.mediapocket;
import android.content.Context;import android.graphics.*;import android.view.MotionEvent;import android.view.View;import java.util.*;
/** 自绘弹幕，叠在播放画面上。不接收点击，手势仍交给播放器。 */
final class DanmuView extends View {
 List<DanDanPlayRules.Cue> cues=new ArrayList<>();DanmuRules.Config config=DanmuRules.Config.defaults();long positionMs;final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 DanmuView(Context context){super(context);paint.setAntiAlias(true);setWillNotDraw(false);setClickable(false);setFocusable(false);}
 void bind(List<DanDanPlayRules.Cue> next,DanmuRules.Config cfg){cues=next==null?new ArrayList<DanDanPlayRules.Cue>():next;if(cfg!=null)config=cfg;invalidate();}
 void position(long ms){if(ms<0||Math.abs(ms-positionMs)<30)return;positionMs=ms;if(config.shown)invalidate();}
 @Override public boolean onTouchEvent(MotionEvent event){return false;}
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(!config.shown||cues.isEmpty()||getWidth()<8)return;float textPx=config.textSp*getResources().getDisplayMetrics().scaledDensity;paint.setTextSize(textPx);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setShadowLayer(Math.max(1f,textPx/10f),0f,0f,0xcc000000);int alpha=DanmuRules.alpha(config.opacity);List<DanmuRules.Draw> draws=DanmuRules.layout(cues,positionMs,getWidth(),getHeight(),textPx,new DanmuRules.Measure(){public float width(String text){return paint.measureText(text);}},config);for(DanmuRules.Draw d:draws){paint.setColor(d.color);paint.setAlpha(alpha);canvas.drawText(d.text,d.x,d.y,paint);}}
}
