package com.guocheng.mediapocket;
/** Separate scene channels: horizontal image paging, fixed captions, and a blended ambient palette. */
final class SceneRules {
 static float progress(float offset,int width){return width<=0?0:Math.max(0,Math.min(1,Math.abs(offset)/width));}
 static float fade(float progress){float t=Math.max(0,Math.min(1,progress));return t*t*(3-2*t);}
 static int blend(int from,int to,float progress){float t=Math.max(0,Math.min(1,progress));int r=Math.round(((from>>16)&255)*(1-t)+((to>>16)&255)*t),g=Math.round(((from>>8)&255)*(1-t)+((to>>8)&255)*t),b=Math.round((from&255)*(1-t)+(to&255)*t);return 0xff000000|(r<<16)|(g<<8)|b;}
 static int tone(int average,boolean light){int r=(average>>16)&255,g=(average>>8)&255,b=average&255;float scale=light?.06f:.19f;int floor=light?225:25;return 0xff000000|((floor+Math.round(r*scale))<<16)|((floor+Math.round(g*scale))<<8)|(floor+Math.round(b*scale));}
}
