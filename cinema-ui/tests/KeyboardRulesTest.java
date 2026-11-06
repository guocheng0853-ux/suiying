package com.guocheng.mediapocket;
public class KeyboardRulesTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] x){
  check(KeyboardRules.action(19,true,false,false)==KeyboardRules.VOLUME_UP&&KeyboardRules.action(20,true,false,false)==KeyboardRules.VOLUME_DOWN,"up / down change volume");
  check(KeyboardRules.action(21,true,false,false)==KeyboardRules.BACK&&KeyboardRules.action(22,true,false,false)==KeyboardRules.FORWARD,"left / right seek");
  check(KeyboardRules.action(19,false,false,false)==KeyboardRules.NONE,"TV remote keeps focus navigation");
  check(KeyboardRules.action(19,true,true,false)==KeyboardRules.NONE&&KeyboardRules.action(22,true,false,true)==KeyboardRules.NONE,"side panel or lock: not taken");
  check(KeyboardRules.action(62,true,false,false)==KeyboardRules.NONE,"space stays play / pause");
  check(KeyboardRules.keyboard(true,false)&&KeyboardRules.keyboard(false,true)&&!KeyboardRules.keyboard(false,false),"tablet keyboard vs TV remote");
  check(KeyboardRules.seekStepMs(10000,0)==10000&&KeyboardRules.seekStepMs(10000,12)==30000,"hold to seek faster");
  System.out.println("PASS: tablet keyboard arrows: volume and seek; TV remote unchanged.");
 }
}
