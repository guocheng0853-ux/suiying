package com.guocheng.mediapocket;
public class FullscreenRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){check(FullscreenRules.mode("crop",false).equals("stretch"),"upgrade stops previous crop mode");check(FullscreenRules.mode("fit",false).equals("stretch"),"requested full-frame default");check(FullscreenRules.zoom("stretch",1.35f)==1f,"old pinch zoom must not crop full frame");check(FullscreenRules.mode("fit",true).equals("fit"),"later explicit original-ratio selection retained");check(FullscreenRules.zoom("fit",1.25f)==1.25f,"manual zoom in other modes retained");check(FullscreenRules.mode("invalid",true).equals("stretch"),"invalid mode defaults safely");System.out.println("PASS: full-frame default migration, old crop/pinch reset, subsequent explicit aspect choice retained.");}
}
