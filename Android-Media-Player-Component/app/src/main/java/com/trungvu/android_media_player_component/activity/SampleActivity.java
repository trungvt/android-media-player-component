package com.trungvu.android_media_player_component.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.trungvu.android_media_player_component.R;
import com.trungvu.android_media_player_component.component.AudioPlayerView;
import com.trungvu.android_media_player_component.component.VideoPlayerView;

/**
 * Created by TrungVT on 2/29/16.
 */
public class SampleActivity extends Activity {

    @Override
    public void onCreate(Bundle savedBundleInstance) {
        super.onCreate(savedBundleInstance);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sample_activity_layout);

        // Test Audio Player
        AudioPlayerView audioPlayerView = (AudioPlayerView) findViewById(R.id.audio_player_view_sample);
        audioPlayerView.loadResourceUrl("http://www.sample-videos.com/audio/mp3/crowd-cheering.mp3");

        // Test Video Player
        VideoPlayerView videoPlayView = (VideoPlayerView) findViewById(R.id.video_player_view_sample);
        videoPlayView.start("http://www.html5videoplayer.net/videos/toystory.mp4");
        // You can also try the below sample. So much fun!
        // http://qthttp.apple.com.edgesuite.net/1010qwoeiuryfg/sl.m3u8
    }
}
