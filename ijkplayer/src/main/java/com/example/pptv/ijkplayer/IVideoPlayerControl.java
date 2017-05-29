package com.example.pptv.ijkplayer;

/**
 * Created by wzhx on 2017/5/27.
 */

public interface IVideoPlayerControl {
    void start();//开始播放

    void pause();//开始暂停

    void restart();//暂停后重新播放

    long getDuration();//获取播放时长

    long getCurrentPosition();//获取当前播放位置

    int getBufferedPercent();//获取缓存

    void release();//释放播放器

    void seekTo(int position);//指定位置播放

    void enterFullScreen();//进入全屏

    void exitFullScreen();//退出全屏

    void enterMiniWindow();//进入小窗

    void exitMiniWindow();//退出小窗

    boolean isIdle();//是否未播放

    boolean isPreparing();//播放是否准备中

    boolean isPrepared();//播放是否准备完成

    boolean isPlaying();//是否正在播放

    boolean isPause();//播放是否暂停

    boolean isBufferPlaying();//是否缓冲播放中

    boolean isBufferPause();//播放是否缓冲暂停

    boolean isComplete();//播放是否完成

    boolean isError();//是否播放错误

    boolean isNormal();//是否正常模式播放

    boolean isMiniWindow();//是否小窗播放

    boolean isFullScreen();//是够全屏播放

    boolean isLive();//是否直播
}
