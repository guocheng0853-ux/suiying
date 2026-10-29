package com.guocheng.mediapocket;
/**
 * 自动档先硬解。底座原来在片源名命中 10-bit / HDR / DV 时就把 VLC 设成软解，
 * HDR 帧被画进 SDR Surface，颜色发灰发紫。黑屏仍由底座 DecodePolicy.retry 再切软解，这里不负责。
 * hints 保留是为了和底座 DecodePolicy.software(String, String) 的调用点同一签名。
 */
final class DecodePolicyRules {
 private DecodePolicyRules(){}
 static boolean software(String mode,String hints){return "software".equals(mode);}
}
