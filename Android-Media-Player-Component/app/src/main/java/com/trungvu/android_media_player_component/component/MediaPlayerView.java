package com.trungvu.android_media_player_component.component;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.util.Log;

import com.trungvu.android_media_player_component.R;


/**
 * Created by TrungVT on 2/29/16.
 */
public class MediaPlayerView extends RelativeLayout {

    private static final int SECOND_BY_MILISECONDS = 1000;
    private static final int MINUTE_BY_SECONDS = 60;
    private static final String TIME_SPAN_BY_MINUTES_SECONDS_FORMATTER = "%02d:%02d";

    private static final int FULL_PERCENTAGE = 100;

    private View mLoadingView;
    private ImageButton mControlButton;
    private TextView mCurrentTimeTextView;
    private TextView mTotalTimeTextView;
    private SeekBar mSeekBar;

    private MediaPlayer mPlayer;

    private Handler mHandler;
    private TimerTask mPlayingTimerTask;
    private Timer mTimer;
    private boolean isMediaPlayerReady;

    private Runnable mMediaPlayingRunnable = new Runnable() {
        @Override
        public void run() {
            int percent = (int) Math.floor(mPlayer.getCurrentPosition() * FULL_PERCENTAGE / mPlayer.getDuration());
            mCurrentTimeTextView.setText(getMMSSFromMiliseconds(mPlayer.getCurrentPosition()));
            mTotalTimeTextView.setText(getMMSSFromMiliseconds(mPlayer.getDuration()));
            mSeekBar.setProgress(percent);
        }
    };

    public MediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public MediaPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaPlayerView(Context context) {
        super(context, null);
    }

    protected void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.media_player_view, this, true);
        mHandler = new Handler();
        mPlayer = new MediaPlayer();
        mLoadingView = findViewById(R.id.audio_loading_prg);
        mLoadingView.setVisibility(GONE);

        mControlButton = (ImageButton) findViewById(R.id.audio_player_button_controller);
        mControlButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayAction();
            }
        });
        mCurrentTimeTextView = (TextView) findViewById(R.id.audio_player_current_time);
        mTotalTimeTextView = (TextView) findViewById(R.id.audio_player_total_time);
        mCurrentTimeTextView.setText("00:00");
        mTotalTimeTextView.setText("00:00");
        mSeekBar = (SeekBar) findViewById(R.id.audio_player_seek_bar);

        setUpMediaPlayer();
        setUpSeekBar();
    }

    /**
     * Load the media url then play it
     * @param resourceUrl
     */
    public void loadResourceUrl(String resourceUrl) {
        try {
            isMediaPlayerReady = false;
            mPlayer.setDataSource(resourceUrl);
            mPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reset the media player
     */
    public void release() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        cancelTimerTasks();
        mHandler.removeCallbacks(mMediaPlayingRunnable);
    }

    private void setUpMediaPlayer() {
        mPlayer.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isMediaPlayerReady = true;
                if (mLoadingView.getVisibility() == View.VISIBLE) {
                    onPlayAction();
                }
                mLoadingView.setVisibility(View.GONE);
                mCurrentTimeTextView.setText(getMMSSFromMiliseconds(0));
                mTotalTimeTextView.setText(getMMSSFromMiliseconds(mp.getDuration()));
            }
        });
        mPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mCurrentTimeTextView.setText(getMMSSFromMiliseconds(mp.getDuration()));
                mControlButton.setImageResource(R.drawable.icon_play);
                cancelTimerTasks();
            }
        });
        mPlayer.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    private void setUpSeekBar() {
        mSeekBar.setProgress(0);
        mSeekBar.setMax(FULL_PERCENTAGE);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayAction();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int currentPosition = mPlayer.getDuration() * seekBar.getProgress() / FULL_PERCENTAGE;
                mPlayer.seekTo(currentPosition);
                onPlayAction();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                int currentPosition = mPlayer.getDuration() * progress / FULL_PERCENTAGE;
                mCurrentTimeTextView.setText(getMMSSFromMiliseconds(currentPosition));
            }
        });
    }

    private void onPlayAction() {
        if (!isMediaPlayerReady) {
            mLoadingView.setVisibility(View.VISIBLE);
            return;
        }
        if (mPlayer == null || mPlayer.getDuration() == 0) {
            Log.d("ERROR - ", "Error with Media Player");
            return;
        }
        if (mPlayer.isPlaying()) { // should be paused
            mControlButton.setImageResource(R.drawable.icon_play);
            mPlayer.pause();
            cancelTimerTasks();
        } else { // should be played
            mControlButton.setImageResource(R.drawable.icon_pause);
            mPlayer.start();
            scheduleTimerTask();
        }
    }

    private void scheduleTimerTask() {
        if (mPlayingTimerTask == null) {
            mPlayingTimerTask = new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(mMediaPlayingRunnable);
                }
            };
        }
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(mPlayingTimerTask, 0, SECOND_BY_MILISECONDS);
        }
    }

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

    /**
     * mm:ss Date time format string
     * @param miliseconds
     * @return mm:ss format string
     */
    private static String getMMSSFromMiliseconds(int miliseconds) {
        int totalSecs = miliseconds / SECOND_BY_MILISECONDS;
        return String.format(TIME_SPAN_BY_MINUTES_SECONDS_FORMATTER,
                totalSecs / MINUTE_BY_SECONDS, totalSecs % MINUTE_BY_SECONDS);
    }
}
