package com.example.pptv.niceijkplayer.presenters;

import android.app.Activity;
import android.content.Context;

import com.example.pptv.ijkplayer.VideoPlayer;
import com.example.pptv.ijkplayer.VideoPlayerController;
import com.example.pptv.niceijkplayer.R;
import com.example.pptv.niceijkplayer.views.activities.IMainView;

/**
 * Created by wzhx on 2017/5/29.
 */

public class MainPresenter implements IMainPresenter<IMainView> {
    private IMainView mainView;
    private Context mContext;
    private VideoPlayer videoPlayer;
    private VideoPlayerController controller;

    public MainPresenter(Context context) {
        this.mContext = context;
    }

    public void play(String url) {
        videoPlayer = (VideoPlayer) ((Activity) mContext).findViewById(R.id.video_player);
        controller = new VideoPlayerController(mContext);
        controller.setVideoBackground("http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-30-43.jpg");
        //videoPlayer.setUp("http://tanzi27niu.cdsb.mobi/wps/wp-content/uploads/2017/05/2017-05-17_17-33-30.mp4");
        //videoPlayer.setUp("http://zv.3gv.ifeng.com/live/zhongwen800k.m3u8");
        videoPlayer.setUp(url);
        videoPlayer.setController(controller);
    }

    @Override
    public void release() {
        videoPlayer.release();
    }

    @Override
    public void playInMiniWindow() {
        if (videoPlayer.isPlaying() || videoPlayer.isBufferPlaying()) {
            videoPlayer.enterMiniWindow();
        }
    }

    @Override
    public void attachView(IMainView view) {
        this.mainView = view;
    }

    @Override
    public void detachView() {
        if(mainView != null){
            mainView = null;
        }
    }
}
