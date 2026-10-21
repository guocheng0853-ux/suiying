package com.guocheng.mediapocket;
/** 下载进度写盘节流。界面读内存；落盘大约 5 秒一次，避免每秒重写主设置文件。 */
final class OfflineProgress {
 private OfflineProgress(){}
 static final long FLUSH_MS=5000;
 static boolean due(long lastFlushMs,long nowMs){return lastFlushMs<0||nowMs-lastFlushMs>=FLUSH_MS;}
}
