package com.guocheng.mediapocket;
public class DecodePolicyRulesTest {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  check(!DecodePolicyRules.software("auto","Rick.and.Morty.2160p.HDR.10bit.DV.EAC3"),"auto no longer soft-decodes HDR or 10-bit names");
  check(!DecodePolicyRules.software("auto","plain.1080p.AAC"),"auto is hardware for SDR too");
  check(!DecodePolicyRules.software("hardware","hdr10"),"explicit hardware stays hardware");
  check(DecodePolicyRules.software("software","anything"),"explicit software is still the black-screen choice");
  check(!DecodePolicyRules.software(null,"hdr")&&!DecodePolicyRules.software("","dolby vision"),"missing mode is not software");
  System.out.println("PASS: auto decode prefers hardware; software is explicit only");
 }
}
