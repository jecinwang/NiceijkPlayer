package com.example.pptv.ijkplayer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.pptv.ijkplayer.utils.LogUtil;
import com.example.pptv.ijkplayer.utils.PlayerUtil;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 实例化播放器
 * Created by wzhx on 2017/5/27.
 */

public class VideoPlayer extends FrameLayout implements IVideoPlayerControl,
        TextureView.SurfaceTextureListener {

    public static final int STATE_IDLE = -1; //播放未开始
    public static final int STATE_PREPARING = 0;//播放准备中
    public static final int STATE_PREPARED = 1;//播放准备完成
    public static final int STATE_PLAYING = 2;//正在播放中
    public static final int STATE_BUFFER_PLAYING = 3;//缓冲播放中
    public static final int STATE_BUFFER_PAUSE = 4;//缓冲暂停
    public static final int STATE_PAUSE = 5;//播放暂停中
    public static final int STATE_ERROR = 6;//播放错误
    public static final int STATE_COMPLETE = 7;//播放完成

    public static final int STATE_NORMAL = 10;
    public static final int STATE_FULL_SCREEN = 11;
    public static final int STATE_MINI_WINDOW = 12;

    private int mCurrentState;//播放状态
    private int mPlayerMode;//播放模式：正常，小窗，全屏
    private boolean isLive;//是否直播

    private Context mContext;
    private VideoPlayerController controller;
    private FrameLayout mContainer;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private IjkMediaPlayer mediaPlayer;
    private String mUrl;
    private int mBufferPercent;

    public VideoPlayer(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        mCurrentState = STATE_IDLE;
        mPlayerMode = STATE_NORMAL;
        mContainer = new FrameLayout(mContext);
        mContainer.setBackgroundColor(Color.parseColor("#111111"));
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);

        //初始化ijkplayer需要的so库文件
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

    }

    /**
     * 初始化播放url
     * @param url
     */
    public void setUp(String url) {
        this.mUrl = url;
    }

    /**
     * 为播放器设置controller对象
     * @param controller
     */
    public void setController(VideoPlayerController controller) {
        this.controller = controller;
        controller.setMediaPlayer(this);
        mContainer.removeView(controller);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(controller, params);
        controller.setVideoState(mPlayerMode, mCurrentState);
    }

    @Override
    public void start() {
        if (mCurrentState == STATE_IDLE) {
            initVideoPlayer();
            initTextureView();
            addTextureView();
        }
    }

    private void addTextureView() {
        mContainer.removeView(mTextureView);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mContainer.addView(mTextureView, 0, params);
    }

    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new TextureView(mContext);
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    /**
     * 初始化播放器
     */
    private void initVideoPlayer() {
        mediaPlayer = new IjkMediaPlayer();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);//播放流媒体的类型
        mediaPlayer.setScreenOnWhilePlaying(true);//是否使用surfaceholder来保持屏幕显示

        mediaPlayer.setOnPreparedListener(mPreparedListener);//播放准备阶段监听
        mediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);//播放缓冲监听
        mediaPlayer.setOnErrorListener(mErrorListener);//播放错误监听
        mediaPlayer.setOnCompletionListener(mCompletionListener);//播放完成监听
        mediaPlayer.setOnInfoListener(mInfoListener);//播放特定警告信息监听
    }

    @Override
    public void pause() {
        if (mCurrentState == STATE_PLAYING) {
            mediaPlayer.pause();
            mCurrentState = STATE_PAUSE;
        }

        if (mCurrentState == STATE_BUFFER_PLAYING) {
            mediaPlayer.pause();
            mCurrentState = STATE_BUFFER_PAUSE;
        }
        controller.setVideoState(mPlayerMode, mCurrentState);
    }

    @Override
    public void restart() {
        if (mCurrentState == STATE_PAUSE) {
            mediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }

        if (mCurrentState == STATE_BUFFER_PAUSE) {
            mediaPlayer.start();
            mCurrentState = STATE_BUFFER_PLAYING;
        }
        controller.setVideoState(mPlayerMode, mCurrentState);
    }

    @Override
    public long getDuration() {
        return mediaPlayer == null ? 0 : mediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mediaPlayer == null ? 0 : mediaPlayer.getCurrentPosition();
    }

    @Override
    public int getBufferedPercent() {
        return mBufferPercent;
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mTextureView != null) {
            mContainer.removeView(mTextureView);
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        mCurrentState = STATE_IDLE;
        controller.reset();
    }

    @Override
    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    @Override
    public void enterFullScreen() {
        PlayerUtil.hideActionBar(mContext);
        PlayerUtil.scanForActivity(mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ViewGroup contentView = (ViewGroup) PlayerUtil.scanForActivity(mContext).findViewById(android.R.id.content);
        this.removeView(mContainer);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        contentView.addView(mContainer, params);

        mPlayerMode = STATE_FULL_SCREEN;
        controller.setVideoState(mPlayerMode, mCurrentState);
    }

    @Override
    public void exitFullScreen() {
        PlayerUtil.showActionBar(mContext);
        PlayerUtil.scanForActivity(mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ViewGroup contentView = (ViewGroup) PlayerUtil.scanForActivity(mContext).findViewById(android.R.id.content);

        contentView.removeView(mContainer);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);

        mPlayerMode = STATE_NORMAL;
        controller.setVideoState(mPlayerMode, mCurrentState);
    }

    @Override
    public void enterMiniWindow() {

        ViewGroup contentView = (ViewGroup) PlayerUtil.scanForActivity(mContext).findViewById(android.R.id.content);
        int width = PlayerUtil.getScreenWidth(mContext) * 2 / 3;
        int height = width * 9 / 16;
        LayoutParams params = new LayoutParams(width, height);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.rightMargin = PlayerUtil.dp2px(mContext, 10);
        params.bottomMargin = PlayerUtil.dp2px(mContext, 10);

        this.removeView(mContainer);
        contentView.addView(mContainer, params);

        mPlayerMode = STATE_MINI_WINDOW;
        controller.setVideoState(mPlayerMode, mCurrentState);

    }

    @Override
    public void exitMiniWindow() {
        ViewGroup contentView = (ViewGroup) PlayerUtil.scanForActivity(mContext).findViewById(android.R.id.content);
        contentView.removeView(mContainer);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mContainer, params);

        mPlayerMode = STATE_NORMAL;
        controller.setVideoState(mPlayerMode, mCurrentState);

    }

    @Override
    public boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mCurrentState == STATE_PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mCurrentState == STATE_PREPARED;
    }

    @Override
    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }

    @Override
    public boolean isPause() {
        return mCurrentState == STATE_PAUSE;
    }

    @Override
    public boolean isBufferPlaying() {
        return mCurrentState == STATE_BUFFER_PLAYING;
    }

    @Override
    public boolean isBufferPause() {
        return mCurrentState == STATE_BUFFER_PAUSE;
    }

    @Override
    public boolean isComplete() {
        return mCurrentState == STATE_COMPLETE;
    }

    @Override
    public boolean isError() {
        return mCurrentState == STATE_ERROR;
    }

    @Override
    public boolean isNormal() {
        return mPlayerMode == STATE_NORMAL;
    }

    @Override
    public boolean isMiniWindow() {
        return mPlayerMode == STATE_MINI_WINDOW;
    }

    @Override
    public boolean isFullScreen() {
        return mPlayerMode == STATE_FULL_SCREEN;
    }

    @Override
    public boolean isLive() {
        return false;
    }

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            mediaPlayer.start();
            mCurrentState = STATE_PREPARED;
            controller.setVideoState(mPlayerMode, mCurrentState);
        }
    };

    IMediaPlayer.OnErrorListener mErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
            mCurrentState = STATE_ERROR;
            controller.setVideoState(mPlayerMode, mCurrentState);
            return false;
        }
    };

    IMediaPlayer.OnCompletionListener mCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer iMediaPlayer) {
            mCurrentState = STATE_COMPLETE;
            controller.setVideoState(mPlayerMode, mCurrentState);
        }
    };

    IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
            mBufferPercent = i;
        }
    };

    IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            switch (i) {
                case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START://视频开始渲染第一帧
                    mCurrentState = STATE_PLAYING;
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_START://视频开始缓冲
                    LogUtil.e("jik buffering start.....");
                    if (mCurrentState == STATE_PAUSE || mCurrentState == STATE_BUFFER_PAUSE) {
                        mCurrentState = STATE_BUFFER_PAUSE;
                    } else {
                        mCurrentState = STATE_BUFFER_PLAYING;
                    }
                    break;
                case IMediaPlayer.MEDIA_INFO_BUFFERING_END://视频缓冲结束
                    LogUtil.e("ijk buffering end.....");
                    if (mCurrentState == STATE_BUFFER_PLAYING) {
                        mCurrentState = STATE_PLAYING;
                    } else if (mCurrentState == STATE_BUFFER_PAUSE) {
                        mCurrentState = STATE_PAUSE;
                    }
                    break;
            }
            controller.setVideoState(mPlayerMode, mCurrentState);
            return true;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surface;
            openMediaPlayer();
        } else {
            mTextureView.setSurfaceTexture(mSurfaceTexture);
        }
    }

    private void openMediaPlayer() {
        try {
            mediaPlayer.setDataSource(mContext, Uri.parse(mUrl), null);
            mediaPlayer.setSurface(new Surface(mSurfaceTexture));
            mediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
            controller.setVideoState(mPlayerMode, mCurrentState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return mSurfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
