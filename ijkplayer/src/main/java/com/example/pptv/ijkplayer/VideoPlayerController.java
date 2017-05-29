package com.example.pptv.ijkplayer;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.pptv.ijkplayer.utils.LogUtil;
import com.example.pptv.ijkplayer.utils.PlayerUtil;

import java.util.Formatter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 播放器控制界面（UI控制）
 * Created by wzhx on 2017/5/27.
 */

public class VideoPlayerController extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {
    private Context mContext;
    private IVideoPlayerControl mediaPlayer;
    private ImageView mControllerBackground;
    private ImageView mCenterStart;
    private LinearLayout mTopBar;
    private ImageView mBack;
    private TextView mTitle;
    private LinearLayout mLoading;
    private LinearLayout mComplete;
    private TextView mReplay;
    private TextView mShare;
    private LinearLayout mError;
    private TextView mRetry;
    private LinearLayout mBottomBar;
    private ImageView mStart;
    private TextView mPosition;
    private TextView mDuration;
    private ImageView mFullScreen;
    private SeekBar mPlayProgress;
    private LinearLayout mVolumeContainer;
    private TextView mVolumePercent;
    private LinearLayout mBrightnessContainer;
    private TextView mBrightPercent;

    private CountDownTimer mHideTopAndBottomTimer;
    private Timer mUpdateProgressTimer;
    private TimerTask mUpdateProgressTimerTask;

    private boolean isTopAndBottomVisible;


    private AudioManager mAudioManager;
    private int mMaxVolume = -1;
    private float mBrightness = -1;
    private int mVolume = -1;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateProgress();
            sendEmptyMessageDelayed(0, 300);
        }
    };

    public VideoPlayerController(@NonNull Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    private void init() {
        LayoutInflater.from(mContext).inflate(R.layout.video_controller, this, true);
        mControllerBackground = (ImageView) findViewById(R.id.iv_background);
        mCenterStart = (ImageView) findViewById(R.id.iv_center_start);
        mCenterStart.setOnClickListener(this);

        mTopBar = (LinearLayout) findViewById(R.id.ll_top_container);
        mBack = (ImageView) findViewById(R.id.iv_top_back);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mBack.setOnClickListener(this);

        mLoading = (LinearLayout) findViewById(R.id.ll_loading_container);

        mComplete = (LinearLayout) findViewById(R.id.ll_complete_container);
        mReplay = (TextView) findViewById(R.id.tv_replay);
        mShare = (TextView) findViewById(R.id.tv_share);
        mReplay.setOnClickListener(this);
        mShare.setOnClickListener(this);

        mError = (LinearLayout) findViewById(R.id.ll_error_container);
        mRetry = (TextView) findViewById(R.id.tv_retry);
        mRetry.setOnClickListener(this);

        mVolumeContainer = (LinearLayout) findViewById(R.id.ll_volume_container);
        mVolumePercent = (TextView) findViewById(R.id.tv_volume_percent);
        mBrightnessContainer = (LinearLayout) findViewById(R.id.ll_brightness_container);
        mBrightPercent = (TextView) findViewById(R.id.tv_brightness_percent);

        mBottomBar = (LinearLayout) findViewById(R.id.ll_bottom_container);
        mStart = (ImageView) findViewById(R.id.iv_start);
        mPosition = (TextView) findViewById(R.id.tv_position);
        mDuration = (TextView) findViewById(R.id.tv_duration);
        mPlayProgress = (SeekBar) findViewById(R.id.play_progress);
        mFullScreen = (ImageView) findViewById(R.id.iv_full_screen);
        mStart.setOnClickListener(this);
        mFullScreen.setOnClickListener(this);
        mPlayProgress.setOnSeekBarChangeListener(this);

        this.setOnTouchListener(this);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

    }

    public void setMediaPlayer(IVideoPlayerControl smartVideoPlayer) {
        mediaPlayer = smartVideoPlayer;
    }

    public void setVideoBackground(String url) {
        Glide.with(mContext).load(url).into(mControllerBackground);
    }

    /**
     * 根据播放器状态不同，对UI界面进行调整
     * @param playMode
     * @param videoStatus
     */
    public void setVideoState(int playMode, int videoStatus) {
        switch (playMode) {
            case VideoPlayer.STATE_NORMAL:
                mBack.setVisibility(GONE);
                mFullScreen.setImageResource(R.drawable.ic_player_enlarge);
                break;
            case VideoPlayer.STATE_FULL_SCREEN:
                mBack.setVisibility(VISIBLE);
                mFullScreen.setImageResource(R.drawable.ic_player_shrink);
                break;
            case VideoPlayer.STATE_MINI_WINDOW:
                mBack.setVisibility(VISIBLE);
                break;
        }

        switch (videoStatus) {
            case VideoPlayer.STATE_IDLE:
                mBottomBar.setVisibility(GONE);
                break;
            case VideoPlayer.STATE_PREPARING:
                mComplete.setVisibility(GONE);
                mError.setVisibility(GONE);
                mControllerBackground.setVisibility(GONE);
                mCenterStart.setVisibility(GONE);
                mTopBar.setVisibility(GONE);
                mBottomBar.setVisibility(GONE);
                mLoading.setVisibility(VISIBLE);
                break;
            case VideoPlayer.STATE_PREPARED:
                startUpdateProgress();
                //startUpdateProgressTimer();
                break;
            case VideoPlayer.STATE_PLAYING:
                mLoading.setVisibility(GONE);
                mStart.setImageResource(R.drawable.ic_player_pause);
                startHideTopAndBottomTimer();
                break;
            case VideoPlayer.STATE_PAUSE:
                mLoading.setVisibility(GONE);
                mStart.setImageResource(R.drawable.ic_player_start);
                cancelHideTopAndBottomTimer();
                break;
            case VideoPlayer.STATE_BUFFER_PLAYING:
                mLoading.setVisibility(VISIBLE);
                startHideTopAndBottomTimer();
                break;
            case VideoPlayer.STATE_BUFFER_PAUSE:
                mLoading.setVisibility(VISIBLE);
                cancelHideTopAndBottomTimer();
                break;
            case VideoPlayer.STATE_COMPLETE:
                cancelUpdateProgress();
                //cancelUpdateProgressTimer();
                setTopAndBottomVisible(false);
                mControllerBackground.setVisibility(VISIBLE);
                mComplete.setVisibility(VISIBLE);
                break;
            case VideoPlayer.STATE_ERROR:
                cancelUpdateProgress();
                //cancelUpdateProgressTimer();
                setTopAndBottomVisible(false);
                mError.setVisibility(VISIBLE);
                break;
        }
    }

    private void cancelHideTopAndBottomTimer() {
        if (mHideTopAndBottomTimer != null) {
            mHideTopAndBottomTimer.cancel();
            mHideTopAndBottomTimer = null;
        }
    }

    /**
     * 定义一个倒计时任务隐藏topBar和bottomBar
     */
    private void startHideTopAndBottomTimer() {
        cancelHideTopAndBottomTimer();
        if (mHideTopAndBottomTimer == null) {
            mHideTopAndBottomTimer = new CountDownTimer(6000, 6000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    setTopAndBottomVisible(false);
                }
            };
        }
        mHideTopAndBottomTimer.start();
    }

    /**
     * 设置topBar和bottomBar可见性
     * @param visible
     */
    private void setTopAndBottomVisible(boolean visible) {
        LogUtil.e("setTopAndBottomVisible: " + visible);
        mTopBar.setVisibility(visible ? VISIBLE : GONE);
        mBottomBar.setVisibility(visible ? VISIBLE : GONE);
        isTopAndBottomVisible = visible;
        if (visible) {
            if (!mediaPlayer.isPause()) {//当点击出现topBar和bottomBar的时候，播放器不是onPause的状态都在6秒后隐藏。
                startHideTopAndBottomTimer();
            }
        } else {
            cancelHideTopAndBottomTimer();
        }
    }

    private void startUpdateProgressTimer() {
        cancelUpdateProgressTimer();
        if (mUpdateProgressTimer == null) {
            mUpdateProgressTimer = new Timer();
        }
        if (mUpdateProgressTimerTask == null) {
            mUpdateProgressTimerTask = new TimerTask() {
                @Override
                public void run() {
                    VideoPlayerController.this.post(new Runnable() {
                        @Override
                        public void run() {
                            updateProgress();
                        }
                    });
                }
            };
        }
        mUpdateProgressTimer.schedule(mUpdateProgressTimerTask, 0, 300);
    }

    private void cancelUpdateProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
            mUpdateProgressTimer = null;
        }
        if (mUpdateProgressTimerTask != null) {
            mUpdateProgressTimerTask.cancel();
            mUpdateProgressTimerTask = null;
        }
    }

    /**
     * handler没300毫秒更新播放进度以及时间
     */
    private void startUpdateProgress() {
        cancelUpdateProgress();
        mHandler.sendEmptyMessageDelayed(0, 300);
    }

    /**
     * 取消更新进度
     */
    private void cancelUpdateProgress() {
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 更新播放时间以及seekBar
     */
    private void updateProgress() {
        int position = (int) mediaPlayer.getCurrentPosition();
        int duration = (int) mediaPlayer.getDuration();
        int bufferPercent = mediaPlayer.getBufferedPercent();

        if (duration != 0) {
            int progress = (int) (100f * position / duration);//进度条的值
            mPlayProgress.setProgress(progress);
        }
        if (bufferPercent > 95) {
            bufferPercent = 100;
        }
        mPlayProgress.setSecondaryProgress(bufferPercent);

        mPosition.setText(formatTime(position));
        mDuration.setText(formatTime(duration));
    }

    private String formatTime(int duration) {
        if (duration < 0) {
            return "00:00";
        }

        int totalSecond = duration / 1000;

        int minute = totalSecond / 60;
        int second = totalSecond % 60;

        StringBuilder builder = new StringBuilder();
        Formatter formatter = new Formatter(builder, Locale.getDefault());
        return formatter.format("%02d:%02d", minute, second).toString();
    }

    @Override
    public void onClick(View v) {

        if (v == mCenterStart) {
            if (mediaPlayer.isIdle()) {
                mediaPlayer.start();
            }
        } else if (v == mStart) {
            if (mediaPlayer.isPlaying() || mediaPlayer.isBufferPlaying()) {
                mediaPlayer.pause();
            } else if (mediaPlayer.isPause() || mediaPlayer.isBufferPause()) {
                mediaPlayer.restart();
            }
        } else if (v == mFullScreen) {
            if (mediaPlayer.isNormal()) {
                mediaPlayer.enterFullScreen();
            } else if (mediaPlayer.isFullScreen()) {
                mediaPlayer.exitFullScreen();
            }
        } else if (v == mBack) {
            if (mediaPlayer.isFullScreen()) {
                mediaPlayer.exitFullScreen();
            } else if (mediaPlayer.isMiniWindow()) {
                mediaPlayer.exitMiniWindow();
            }
        } else if (v == mReplay) {
            mediaPlayer.release();
            mediaPlayer.start();
        } else if (v == mRetry) {
            mReplay.performClick();
        } /*else if (v == this) {
            LogUtil.e("controller onclick");
            if (mediaPlayer.isPlaying()) {
                setTopAndBottomVisible(!isTopAndBottomVisible);
            }
        }*/
    }

    boolean isFirst;
    boolean isSeekBarSlide;
    boolean isVolumeSlide;
    float startX;
    float startY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isFirst = true;
                startX = event.getX();
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float currentX = event.getX();//滑动过程中x的值，不断变化
                float currentY = event.getY();

                float deltaX = currentX - startX;
                float deltaY = currentY - startY;

                if (isFirst) {//只判断一次，防止以后每次都判断
                    isSeekBarSlide = Math.abs(deltaX) > Math.abs(deltaY);//根据手势的方向判断是不是横向移动
                    isVolumeSlide = startX > PlayerUtil.getScreenWidth(mContext) / 2;//看看是不是声音控制
                    isFirst = false;
                }

                if (isSeekBarSlide) {
                    float percent = deltaX / getWidth();
                    onSeekBarSlide(percent);
                } else {
                    float percent = -deltaY / getHeight();
                    LogUtil.e("percent:" + percent);
                    if (isVolumeSlide) {
                        onVolumeSlide(percent);
                    } else {
                        onBrightnessSlide(percent);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mVolumeContainer.setVisibility(GONE);
                mBrightnessContainer.setVisibility(GONE);
                if (Math.abs(event.getX() - startX) < 3 && Math.abs(event.getY() - startY) < 3) {
                    if (mediaPlayer.isPlaying()) {
                        setTopAndBottomVisible(!isTopAndBottomVisible);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        cancelHideTopAndBottomTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

        if (mediaPlayer.isBufferPause() || mediaPlayer.isPause()) {
            mediaPlayer.restart();
        }

        int position = (int) (mediaPlayer.getDuration() * seekBar.getProgress() / 100f);
        mediaPlayer.seekTo(position);
        startHideTopAndBottomTimer();
    }

    /**
     * controller重置
     */
    public void reset() {
        cancelUpdateProgress();
        cancelUpdateProgressTimer();
        cancelHideTopAndBottomTimer();
        mPlayProgress.setProgress(0);
        mPlayProgress.setSecondaryProgress(0);

        mControllerBackground.setVisibility(VISIBLE);
        mBottomBar.setVisibility(GONE);
        mTopBar.setVisibility(VISIBLE);
        mCenterStart.setVisibility(VISIBLE);
        mFullScreen.setImageResource(R.drawable.ic_player_enlarge);
        mComplete.setVisibility(GONE);
        mError.setVisibility(GONE);
    }

    /**
     * 滑动调节声音
     * @param percent
     */
    public void onVolumeSlide(float percent) {
        mVolumeContainer.setVisibility(VISIBLE);
        mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mVolume < 0) {
            mVolume = 0;
        }
        int slidedVolume = (int) (percent * mMaxVolume) + mVolume;
        if (slidedVolume > mMaxVolume) {
            slidedVolume = mMaxVolume;
        } else if (slidedVolume <= 0) {
            slidedVolume = 0;
        }

        int volumePercent = 100 * slidedVolume / mMaxVolume;
        mVolumePercent.setText(volumePercent + "%");
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, slidedVolume, 0);
    }

    /**
     * 滑动调节亮度
     * @param percent
     */
    public void onBrightnessSlide(float percent) {
        mBrightnessContainer.setVisibility(VISIBLE);
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            mBrightness = activity.getWindow().getAttributes().screenBrightness;
            if (mBrightness <= 0.00f) {
                mBrightness = 0.50f;//如果亮度小于0，初始化W为50%的亮度
            } else if (mBrightness < 0.01f) {
                mBrightness = 0.01f;
            }

            WindowManager.LayoutParams lps = activity.getWindow().getAttributes();

            float slidedBrightness = mBrightness + percent;
            if (slidedBrightness > 1.0f) {
                slidedBrightness = 1.0f;
            } else if (slidedBrightness < 0.01f) {
                slidedBrightness = 0.01f;
            }

            lps.screenBrightness = slidedBrightness;
            activity.getWindow().setAttributes(lps);

            int brightnessPercent = (int) (slidedBrightness * 100);
            LogUtil.d("brightnessPercent:" + brightnessPercent);
            mBrightPercent.setText(brightnessPercent + "%");
        }
    }

    /**
     * 滑动seekBar
     * @param percent
     */
    public void onSeekBarSlide(float percent) {
        long position = mediaPlayer.getCurrentPosition();
        long duration = mediaPlayer.getDuration();

        long deltaDuration = Math.min(100 * 1000, duration - position);//滑动向后观看影片限制在100秒，否则比例太大
        long slidedPosition = (long) (deltaDuration * percent) + position;
        if (slidedPosition > duration) {
            slidedPosition = duration;
        } else if (slidedPosition <= 0) {
            slidedPosition = 0;
        }
        mediaPlayer.seekTo((int) slidedPosition);
    }
}
