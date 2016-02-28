package com.trungvu.android_media_player_component.component;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.trungvu.android_media_player_component.R;
import com.trungvu.android_media_player_component.utility.TimeUtil;

public class VideoPlayerView extends RelativeLayout {
    private final int BAR_HIDING_DELAY_TIME_IN_MILISECONDS = 3000;
    private final int FULL_PERCENTAGE = 100;
    
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_PREPARED = 1;
    public static final int STATE_PLAYBACK_STARTED = 2;
    public static final int STATE_PLAYBACK_PAUSED = 3;
    public static final int STATE_PLAYBACK_COMPLETED = 4;
    public static final int STATE_ERROR = 5;
    
    public static final int ACTION_SEEK_NONE = 100;
    public static final int ACTION_SEEK_BACKWARD = 101;
    public static final int ACTION_SEEK_FORWARD = 102;
    
    private Context mContext;
    private Handler mHandler;
    private View mHeaderBar;
    private LinearLayout mFooterBar;
    private View mVideoBackground;
    private VideoView mVideoView;
    private ImageButton mPlayControlButton;
    private SeekBar mSeekBar;
    private TextView mCurrentPlayingTime;
    private TextView mVideoLengthTime;
    private ProgressBar mLoadingBar;
    private ImageView mPlayingStatusImage;
    
    private boolean mIsBarShowing, mIsChangingTrackSeekBar, mIsPlaybackFinished;
    private Uri mVideoUri;
    private int mVideoPlayerState, mLastSeekbarAction;
    private int mLastPlaybackPosition;
    private int mCurrentVideoWidth, mCurrentVideoHeight;
    private Animation mShowingAnimation;
    private Animation mHidingAnimation;
    private Animation mScaleZoomInAnimation;
    private Animation mScaleZoomOutAnimation;
    private Animation mFadeOutAnimation;
    private Timer mTimer;
    private TimerTask mPlayingTimerTask;
    
    private VideoPlayViewListener mVideoPlayViewListener;
    
    public VideoPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }
    
    public VideoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public VideoPlayerView(Context context) {
        this(context, null, 0);
    }
    /**
     * Video Player view initialization
     * @param context
     */
    protected void initView(Context context) {
        mHandler = new Handler();
        mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.video_player_view, this, true);
        mFooterBar = (LinearLayout) findViewById(R.id.video_player_footer_bar);
        mVideoBackground = findViewById(R.id.video_background);
        mVideoBackground.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                controlVideoByCurrentState();
            }
        });
        mLoadingBar = (ProgressBar) findViewById(R.id.video_player_loading_progress_bar);
        mLoadingBar.setVisibility(GONE);
        
        mVideoView = (VideoView) findViewById(R.id.video_player_view_main);
        mCurrentVideoWidth = mVideoView.getWidth();
        mCurrentVideoHeight = mVideoView.getHeight();
        setVideoViewEventListener();
        mPlayControlButton = (ImageButton) mFooterBar.findViewById(R.id.video_player_btn_play);
        mPlayControlButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                controlVideoByCurrentState();
            }
        });
        mSeekBar = (SeekBar) mFooterBar.findViewById(R.id.video_player_seekbar);
        setUpSeekBar();
        String timingDefaultText = TimeUtil.getMMSSFromMiliseconds(0);
        mCurrentPlayingTime = (TextView) mFooterBar.findViewById(R.id.video_player_current_playing_time);
        mCurrentPlayingTime.setText(timingDefaultText);
        mVideoLengthTime = (TextView) mFooterBar.findViewById(R.id.video_player_length_time);
        mVideoLengthTime.setText(timingDefaultText);
        mPlayingStatusImage = (ImageView) findViewById(R.id.video_playing_status);
        mPlayingStatusImage.setVisibility(GONE);
        // Animation
        initAnimation();
        mIsBarShowing = true;
        setBackgroundColor(Color.BLACK);
    }
    /**
     * Set header bar of Video Play screen
     * @param headerBar View
     */
    public void setHeaderBar(View headerBar) {
        mHeaderBar = headerBar;
    }
    /**
     * VideoPlayView listener setting up
     * @param listener
     */
    public void setVideoPlayViewListener(VideoPlayViewListener listener) {
        mVideoPlayViewListener = listener;
    }
    /**
     * VideoView listener setting up
     */
    private void setVideoViewEventListener() {
        mVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnSeekCompleteListener(new OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mLoadingBar.setVisibility(GONE);
                    }
                });
                mLastSeekbarAction = ACTION_SEEK_NONE;
                measureLayout();
                mVideoPlayerState = STATE_PREPARED;
                mVideoLengthTime.setText(TimeUtil.getMMSSFromMiliseconds(mVideoView.getDuration()));
                playVideo();
            }
        });
        
        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                processAfterPlaybackCompleted();
            }
        });
        
        mVideoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                processAfterErrorOccured();
                return false;
            }
        });
    }
    /**
     * Measuring the layout of video view
     */
    @SuppressWarnings("deprecation")
    public void measureLayout() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = windowManager.getDefaultDisplay().getWidth();
        int screenHeight = windowManager.getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;
        float videoProportion = (float) mVideoView.getWidth() / (float) mVideoView.getHeight();
        android.view.ViewGroup.LayoutParams videoViewLayoutParams = mVideoView.getLayoutParams();
        if (videoProportion >= screenProportion) {
            videoViewLayoutParams.width = screenWidth;
            videoViewLayoutParams.height = (int) ((float) screenWidth / videoProportion);
        } else {
            videoViewLayoutParams.width = (int) (videoProportion * (float) screenHeight);
            videoViewLayoutParams.height = screenHeight;
        }
        mVideoView.setLayoutParams(videoViewLayoutParams);
        mCurrentVideoWidth = mVideoView.getWidth();
        mCurrentVideoHeight = mVideoView.getHeight();
    }
    /**
     * All process after onCompletionListener was invoked
     */
    private void processAfterPlaybackCompleted() {
        mIsPlaybackFinished = true;
        mPlayControlButton.setImageResource(R.drawable.icon_video_play_controller);
        if (mVideoPlayerState != STATE_ERROR) {
            mPlayControlButton.setImageResource(R.drawable.icon_video_reload_controller);
            mVideoPlayerState = STATE_PLAYBACK_COMPLETED;
        }
        mLoadingBar.setVisibility(View.GONE);
        mCurrentPlayingTime.setText(TimeUtil.getMMSSFromMiliseconds(mVideoView.getDuration()));
        mSeekBar.setProgress(FULL_PERCENTAGE);
        mSeekBar.setEnabled(false);
        mHandler.removeCallbacks(mBarHidingRunnable);
        showBarUI();
        cancelTimerTasks();
    }
    /**
     * All process after Error was occurred
     */
    private void processAfterErrorOccured() {
        // UI
        mCurrentPlayingTime.setText(TimeUtil.getMMSSFromMiliseconds(0));
        mSeekBar.setEnabled(false);
        mSeekBar.setProgress(0);
        mLoadingBar.setVisibility(GONE);
        showBarUI();
        // logic
        mVideoPlayerState = STATE_ERROR;
    }

    /**
     * Seekbar setting
     */
    private void setUpSeekBar() {
        mSeekBar.setEnabled(false);
        mSeekBar.setProgress(0);
        mSeekBar.setMax(FULL_PERCENTAGE);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mVideoView.getDuration() <= 0 || mVideoPlayerState == STATE_PLAYBACK_COMPLETED) {
                    return;
                }
                mVideoPlayerState = STATE_PLAYBACK_PAUSED;
                mIsChangingTrackSeekBar = true;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                }
                mLastPlaybackPosition = mSeekBar.getProgress();
                mHandler.removeCallbacks(mBarHidingRunnable);
                mLoadingBar.setVisibility(VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVideoPlayerState == STATE_PLAYBACK_COMPLETED) {
                    mSeekBar.setProgress(FULL_PERCENTAGE);
                    return;
                }
                mVideoPlayerState = STATE_PLAYBACK_PAUSED;
                mIsChangingTrackSeekBar = false;
                int currentPosition = 0;
                if (mVideoView.canSeekForward() || mVideoView.canSeekBackward()) {
                    if (seekBar.getProgress() >= mLastPlaybackPosition) {
                        mLastSeekbarAction = ACTION_SEEK_FORWARD;
                    } else {
                        mLastSeekbarAction = ACTION_SEEK_BACKWARD;
                    }
                    currentPosition = mVideoView.getDuration() * seekBar.getProgress() / FULL_PERCENTAGE;
                } else {
                    mLastSeekbarAction = ACTION_SEEK_NONE;
                    currentPosition = mVideoView.getDuration() * mLastPlaybackPosition / FULL_PERCENTAGE;
                }
                mVideoView.seekTo(currentPosition);
                cancelTimerTasks();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mIsChangingTrackSeekBar || mVideoPlayerState == STATE_PLAYBACK_COMPLETED) {
                    return;
                }
                int currentPosition = mVideoView.getDuration() * progress / FULL_PERCENTAGE;
                mCurrentPlayingTime.setText(TimeUtil.getMMSSFromMiliseconds(currentPosition));
            }
        });
    }
    /**
     * Animation initialization
     */
    private void initAnimation() {
        mShowingAnimation = AnimationUtils.loadAnimation(mContext, R.anim.icon_fade_in);
        mShowingAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (mHeaderBar != null) {
                    mHeaderBar.setVisibility(View.VISIBLE);
                }
                mFooterBar.setVisibility(View.VISIBLE);
                mIsBarShowing = true;
            }
        });
        mHidingAnimation = AnimationUtils.loadAnimation(mContext, R.anim.icon_fade_out);
        mHidingAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                if (mHeaderBar != null) {
                    mHeaderBar.setVisibility(View.GONE);
                }
                mFooterBar.setVisibility(View.GONE);
                mIsBarShowing = false;
            }
        });
        mFadeOutAnimation = AnimationUtils.loadAnimation(mContext, R.anim.icon_fade_out);
        mFadeOutAnimation.setDuration(300);
        mFadeOutAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mPlayingStatusImage.setVisibility(GONE);
            }
        });
        mScaleZoomInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.scale_zoom_in);
        mScaleZoomInAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationRepeat(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mPlayingStatusImage.setVisibility(GONE);
            }
        });
        mScaleZoomOutAnimation = AnimationUtils.loadAnimation(mContext, R.anim.scale_zoom_out);
        mScaleZoomOutAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPlayingStatusImage.startAnimation(mFadeOutAnimation);
            }
        });
    }

    private void showBarUI() {
        if (mIsBarShowing) {
            return;
        }
        if (mHeaderBar != null) {
            mHeaderBar.startAnimation(mShowingAnimation);
        }
        mFooterBar.startAnimation(mShowingAnimation);
    }

    private void hideBarUI() {
        if (!mIsBarShowing) {
            return;
        }
        if (mHeaderBar != null) {
            mHeaderBar.startAnimation(mHidingAnimation);
        }
        mFooterBar.startAnimation(mHidingAnimation);
    }

    private void showPlayingStatusUI() {
        if (mPlayingStatusImage.getVisibility() == VISIBLE) {
            return;
        }
        if (mVideoPlayerState == STATE_PLAYBACK_PAUSED) {
            mPlayingStatusImage.setImageResource(R.drawable.icon_video_stop_center);
        }
        mPlayingStatusImage.setVisibility(VISIBLE);
        mPlayingStatusImage.startAnimation(mScaleZoomOutAnimation);
    }

    private void hidePlayingStatusUI() {
        mPlayingStatusImage.setImageResource(R.drawable.icon_video_play_center);
        mPlayingStatusImage.startAnimation(mScaleZoomInAnimation);
    }
    /**
     * Runnable task that update UI while video is playing
     * NOTE: please cancel all timing task that related to this runnable variable
     */
    private Runnable mVideoPlayingRunnable = new Runnable() {
        @Override
        public void run() {
            if ((!mVideoView.isPlaying() || mVideoView.getCurrentPosition() == 0) && 
                    mVideoPlayerState == STATE_PLAYBACK_STARTED) {
                mLoadingBar.setVisibility(VISIBLE);
                return;
            }
            if (mVideoView.getDuration() == 0 || mIsChangingTrackSeekBar) { 
                // divide by zero error check
                return;
            }
            mLoadingBar.setVisibility(GONE);
            int currentPosition = mVideoView.getCurrentPosition();
            if (currentPosition >= mVideoView.getDuration() && !mVideoView.isPlaying() && !mIsPlaybackFinished) {
                processAfterPlaybackCompleted();
                return;
            }
            if (mLastSeekbarAction == ACTION_SEEK_NONE && mVideoPlayerState == STATE_PLAYBACK_STARTED) {
                int percent = (int) Math.floor(mVideoView.getCurrentPosition() * FULL_PERCENTAGE / mVideoView.getDuration());
                mCurrentPlayingTime.setText(TimeUtil.getMMSSFromMiliseconds(mVideoView.getCurrentPosition()));
                mVideoLengthTime.setText(TimeUtil.getMMSSFromMiliseconds(mVideoView.getDuration()));
                mSeekBar.setProgress(percent);
            } else {
                mLastSeekbarAction = ACTION_SEEK_NONE;
            }
            if (mVideoView.getWidth() != mCurrentVideoWidth || mVideoView.getHeight() != mCurrentVideoHeight) {
                measureLayout();
            }
        }
    };
    
    /**
     * Start VideoView by video url path
     * @param videoUrl string
     */
    public void start(String videoUrl) {
        mVideoUri = Uri.parse(videoUrl);
        startVideo();
    }
    /**
     * Start VideoView by video uri 
     */
    private void startVideo() {
        try {
            // UI process
            mSeekBar.setProgress(0);
            mSeekBar.setEnabled(false);
            mCurrentPlayingTime.setText(TimeUtil.getMMSSFromMiliseconds(0));
            mPlayControlButton.setImageResource(R.drawable.icon_video_play_controller);
            mLoadingBar.setVisibility(VISIBLE);
            // logic & data process
            mIsPlaybackFinished = false;
            mVideoView.setVideoURI(mVideoUri);
            mVideoPlayerState = STATE_INITIALIZED;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mLoadingBar.setVisibility(GONE);
            if (mVideoPlayerState == STATE_ERROR) {
                if (mVideoPlayViewListener != null) {
                    mVideoPlayViewListener.onError();
                }
                return;
            }
            mVideoPlayerState = STATE_ERROR;
            startVideo();
        }
    }
    /**
     * Play the video
     */
    private void playVideo() {
        // control video view for playing
        mVideoView.start();
        mVideoPlayerState = STATE_PLAYBACK_STARTED;
        // update UI
        mVideoLengthTime.setText(TimeUtil.getMMSSFromMiliseconds(mVideoView.getDuration()));
        mVideoView.setBackgroundColor(Color.TRANSPARENT);
        mSeekBar.setEnabled(true);
        hideBarUI();
        mPlayControlButton.setImageResource(R.drawable.icon_video_stop_controller);
        mLoadingBar.setVisibility(GONE);
        scheduleTimerTasks();
    }
    /**
     * Pause the video player
     */
    private void pauseVideo() {
        mVideoPlayerState = STATE_PLAYBACK_PAUSED;
        // update UI
        mPlayControlButton.setImageResource(R.drawable.icon_video_play_controller);
        showBarUI();
        mHandler.removeCallbacks(mBarHidingRunnable);
        mLoadingBar.setVisibility(GONE);
        showPlayingStatusUI();
        // control video view for pause
        mVideoView.pause();
    }
    /**
     * Resume to the last saved point
     */
    public void resumeVideo() {
        mIsPlaybackFinished = false;
        showBarUI();
        barHidingTrigger();
        scheduleTimerTasks();
        mVideoView.resume();
    }
    /**
     * Free resources and recovery memory
     */
    public void release() {
        cancelTimerTasks();
        mHandler.removeCallbacks(mVideoPlayingRunnable);
        mHandler.removeCallbacks(mBarHidingRunnable);
    }
    /**
     * Suspend the video view by current state
     */
    public void suspend() {
        mVideoView.suspend();
    }
    /**
     * Scheduling timer tasks
     */
    private void scheduleTimerTasks() {
        if (mPlayingTimerTask == null) {
            mPlayingTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(mVideoPlayingRunnable);
                }
            };
        }
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(mPlayingTimerTask, 0, TimeUtil.SECOND_BY_MILISECONDS);
        }
    }
    /**
     * Cancel scheduled timer task
     */
    private void cancelTimerTasks() {
        if (mPlayingTimerTask != null) {
            mPlayingTimerTask.cancel();
            mPlayingTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private void controlVideoByCurrentState() {
        switch (mVideoPlayerState) {
        case STATE_INITIALIZED: // Initialize the Video view with uri
            break;
        case STATE_PREPARED:
        case STATE_PLAYBACK_PAUSED: // to Start Playing
            if (!mIsChangingTrackSeekBar) {
                // if user still tries to seek the bar, do not play the video
                playVideo();
                hidePlayingStatusUI();
            }
            break;
        case STATE_PLAYBACK_STARTED: // from Started Playing to Pause
            pauseVideo();
            break;
        case STATE_PLAYBACK_COMPLETED: // from Stopped Playing to Replay
        case STATE_ERROR: // reload the video view
            startVideo();
            break;
        default:
            startVideo();
            break;
        }
    }
    /**
     * Runnable task for bar hiding trigger
     * NOTE: please cancel all timing task that related to this runnable variable
     */
    private Runnable mBarHidingRunnable = new Runnable() {
        @Override
        public void run() {
            if (mVideoView.isPlaying() && mIsBarShowing) {
                 hideBarUI();
            }
        }
    };
    /**
     * Trigger method that do action after period time of delaying
     */
    private void barHidingTrigger() {
        mHandler.removeCallbacks(mBarHidingRunnable);
        mHandler.postDelayed(mBarHidingRunnable, BAR_HIDING_DELAY_TIME_IN_MILISECONDS);
    }
    /**
     * VideoPlayView Listener
     */
    public interface VideoPlayViewListener {
        // invoke when error was occurred
        public void onError();
    }
}