package com.guocheng.mediapocket;
public class CarouselRulesTest {
 static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 public static void main(String[] args){
  check(CarouselRules.target(-140,0,400,400)==1,"left drag commits next");check(CarouselRules.target(140,0,400,400)==-1,"right drag commits previous");check(CarouselRules.target(60,0,400,400)==0,"short drag springs back");
  check(CarouselRules.target(-40,-1200,400,400)==1,"fast flick commits");check(CarouselRules.target(-40,1200,400,400)==-1,"reversal follows velocity");check(CarouselRules.target(1,3000,400,400)==0,"tap never flings");check(CarouselRules.target(100,500,0,400)==0,"unmeasured view safe");
  check(CarouselRules.index(0,-1,5)==4&&CarouselRules.index(4,1,5)==0,"loop wraps both directions");check(CarouselRules.index(0,-12,5)==3,"large jumps normalize");check(CarouselRules.index(0,1,0)==0,"empty carousel");
  check(CarouselRules.horizontal(40,8,10)&&!CarouselRules.horizontal(8,40,10)&&!CarouselRules.horizontal(20,20,10),"vertical/diagonal scroll stays with page");
  check(CarouselRules.duration(0,400,400,0)>CarouselRules.duration(300,400,400,0),"short remaining distance settles faster");check(CarouselRules.duration(0,400,400,9000)>=150,"fast flings retain a visible transition");
  System.out.println("PASS: swipe threshold, flick/reversal, rebound, tap rejection, circular paging, vertical scroll arbitration, settling duration.");
 }
}
