package com.guocheng.mediapocket;
/** Media3 IO errors must never be presented as evidence of decoder incompatibility. */
final class Media3ErrorRules {
 static String message(int code,int status){
  if(code==HttpRange.SKIP_ERROR)return HttpRange.SKIP_MESSAGE;
  if(code==2004){String http=status>0?"（HTTP "+status+"）":"";
   if(status==401||status==403)return "服务器拒绝访问"+http+"。请返回影片详情重新获取播放地址；这还不是解码失败。";
   if(status==404||status==410)return "播放地址已失效或文件不存在"+http+"。请重新打开影片或选择其他版本。";
   if(status==416)return "服务器不接受当前续播位置"+http+"。请从头播放后再试。";
   return "服务器返回了异常响应"+http+"。视频尚未正常读取，请重试或换个片源。";
  }
  if(code>=2000&&code<3000)return "无法读取片源（错误 "+code+"）。请检查连接或重新获取播放地址；这不代表设备不支持杜比解码。";
  if(code>=3000&&code<4000)return "无法解析这个视频文件（错误 "+code+"）。可以尝试其他版本或服务器转码。";
  return "内置播放器无法播放这个视频（错误 "+code+"）。可以尝试其他版本、服务器转码或外部播放器。";
 }
}
