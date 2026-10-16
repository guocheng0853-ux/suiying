package com.guocheng.mediapocket;
/** Gesture decisions independent from Android animation and wall-clock timing. */
final class CarouselRules {
 static int target(float offset,float velocity,int width,int minimumVelocity){if(width<=0)return 0;boolean flick=Math.abs(velocity)>=minimumVelocity&&Math.abs(offset)>=Math.min(24,width*.06f);if(flick)return velocity<0?1:-1;return Math.abs(offset)>=width*.24f?(offset<0?1:-1):0;}
 static int index(int current,int step,int count){if(count<=0)return 0;int next=(current+step)%count;return next<0?next+count:next;}
 static int duration(float from,float to,int width,float velocity){if(width<=0)return 220;float distance=Math.abs(to-from);if(distance<1)return 0;if(Math.abs(velocity)>1)return Math.max(150,Math.min(340,Math.round(distance/Math.abs(velocity)*1000*1.5f)));return Math.max(180,Math.min(340,Math.round(180+160*distance/width)));}
 static boolean horizontal(float x,float y,int slop){return Math.abs(x)>slop&&Math.abs(x)>Math.abs(y)*1.2f;}
}
