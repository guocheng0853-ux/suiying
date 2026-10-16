package com.guocheng.mediapocket;
public class HomeMotionRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] a){
  check(HomeMotionRules.scrimAlpha(0,600,100)==0,"rest transparent");check(HomeMotionRules.scrimAlpha(500,600,100)==1,"solid once hero passes bar");check(HomeMotionRules.scrimAlpha(2000,600,100)==1,"stays solid");
  float mid=HomeMotionRules.scrimAlpha(400,600,100);check(mid>0&&mid<1,"ramps");check(HomeMotionRules.scrimAlpha(350,600,100)<HomeMotionRules.scrimAlpha(450,600,100),"monotonic");
  check(HomeMotionRules.scrimAlpha(10,0,100)==1&&HomeMotionRules.scrimAlpha(0,0,100)==0,"no hero");check(HomeMotionRules.gradientAlpha(1)==0&&HomeMotionRules.gradientAlpha(0)==1,"gradient inverse");
  check(HomeMotionRules.imageCounterShift(-200)+(-200)==-100,"image net half speed");check(HomeMotionRules.outgoingText(100)==50,"text half speed");
  check(HomeMotionRules.incomingText(-40,400)==90&&HomeMotionRules.incomingText(-400,400)==0,"incoming slides in from right");check(HomeMotionRules.incomingText(10,400)<0,"incoming from left");
  float r=HomeMotionRules.rubberBand(400,400);check(r>0&&r<400*.18f,"rubber band bounded");check(HomeMotionRules.rubberBand(-50,400)<0&&Math.abs(HomeMotionRules.rubberBand(-50,400))<50,"resists");check(HomeMotionRules.rubberBand(10,0)==0,"unmeasured");
  check(HomeMotionRules.overshootTension(2000,400)>HomeMotionRules.overshootTension(0,400),"flick overshoots more");
  check(HomeMotionRules.dimAlpha(1)==0x80&&HomeMotionRules.dimAlpha(0)==0,"dim 50%");check(HomeMotionRules.menuScale(0)==.9f&&HomeMotionRules.menuScale(1)==1,"scale enter");
  System.out.println("PASS: scrim mapping, parallax, caption motion, rubber-band, overshoot, menu dim/scale.");
 }
}
