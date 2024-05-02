package com.example.soundvibe;

import static android.content.ContentValues.TAG;
import static com.example.soundvibe.MainActivity.songsList;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.bullhead.equalizer.EqualizerFragment;
import com.bumptech.glide.Glide;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.chibde.visualizer.LineBarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements Player.Listener {

    private ExoPlayer player;
    private ImageView songCover, nextBtn, prevBtn, playOrderBtn, backBtn, eqButton;
    private TextView songName, artistName, durationPlayed, totalDuration;
    private RelativeLayout mContainer;
    public static FloatingActionButton playPauseBtn;
    private SeekBar seekBar;
    private PlayerView playerView;
    static ArrayList<SongsModel> musicList;
    private int currentSongPosition;
    private LineBarVisualizer visualizerLineBar;
    private FrameLayout eqContainer;

    //is the activity bound?
    boolean isBound = false;
    public static PlayerActivity instance;
    private int repeatMode = 1; // repeat all = 1, repeat one = 2, shuffle all = 3

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        }

        // Initialize instance
        instance = this;

        initView();

        // Retrieve data from Intent
        Intent intent = getIntent();
        currentSongPosition = intent.getIntExtra("SONG_POSITION", 0);

        musicList = songsList;

        //bind to the player service
        doBindService();

        // Set click listeners for next and previous buttons
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNextSong();
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPreviousSong();
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlayPause();

            }
        });

        playOrderBtn.setOnClickListener(view -> {
            if (repeatMode == 1) {
                // repeat one
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                playOrderBtn.setImageResource(R.drawable.baseline_repeat_one_24);
            } else if (repeatMode == 2) {
                // shuffle all
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 3;
                playOrderBtn.setImageResource(R.drawable.baseline_shuffle);
            } else if (repeatMode == 3) {
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                playOrderBtn.setImageResource(R.drawable.baseline_repeat);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Seek to the specified position
                    player.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not needed for this example
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed(); // This will trigger the onBackPressed method
            }
        });

        eqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eqContainer.getVisibility() == View.GONE){
                    eqContainer.setVisibility(View.VISIBLE);
                }
                int sessionId = player.getAudioSessionId();

                EqualizerFragment equalizerFragment = EqualizerFragment.newBuilder()
                        .setAccentColor(Color.parseColor("#4caf50"))
                        .setAudioSessionId(sessionId)
                        .build();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.eqFrame, equalizerFragment)
                        .commit();
            }
        });

    }

    private void doBindService() {
        Intent playerServiceIntent = new Intent(this, PlayerNotificationService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //get the service instance
            PlayerNotificationService.ServiceBinder binder = (PlayerNotificationService.ServiceBinder) iBinder;
            player = binder.getPlayerService().player;
            isBound = true;

            // Set up ExoPlayer with the current song
            setupPlayer(currentSongPosition);

            // Update UI elements
            updateSongInfo(currentSongPosition);

            // Set ExoPlayer event listener
            player.addListener(PlayerActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // Handle service disconnection
            player = null; // Set player to null when disconnected
            isBound = false;
        }
    };

    private void initView() {
        songCover = findViewById(R.id.song_cover);
        nextBtn = findViewById(R.id.next_btn);
        prevBtn = findViewById(R.id.prev_btn);
        playOrderBtn = findViewById(R.id.playOrder_btn);
        backBtn = findViewById(R.id.back_btn);
        songName = findViewById(R.id.song_name);
        artistName = findViewById(R.id.artist_name);
        durationPlayed = findViewById(R.id.durationPlayed);
        totalDuration = findViewById(R.id.totalDuration);
        playPauseBtn = findViewById(R.id.play_pause_btn);
        seekBar = findViewById(R.id.seekBar);
        playerView = findViewById(R.id.playerView);
        visualizerLineBar = findViewById(R.id.visualizerLineBar);
        mContainer = findViewById(R.id.mContainer);
        eqButton = findViewById(R.id.equalizer_btn);
        eqContainer = findViewById(R.id.eqFrame);
    }

    private void setupPlayer(int currentSongPosition) {

        // Set up your ExoPlayer view
        playerView.setPlayer(player);

        // Activate the visualizer
        activateAudioVisualizer();

        player.clearMediaItems();

        // Add each media item to the concatenating media source
        for (int i = 0; i < musicList.size(); i++) {
            SongsModel song = musicList.get(i);
            MediaItem mediaItem = MediaItem.fromUri(song.getSongUrl());
            player.addMediaItem(mediaItem);
        }

        player.seekTo(currentSongPosition, 0);

        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);

        // Prepare the player
        player.prepare();

        // Start playback
        player.play();


    }

    private void activateAudioVisualizer() {
        int audioSessionId = player.getAudioSessionId();

        if (audioSessionId != AudioTrack.ERROR_BAD_VALUE && audioSessionId != AudioTrack.ERROR) {

            // set custom color to the line.
            visualizerLineBar.setColor(ContextCompat.getColor(this, R.color.teal_200));

            // define custom number of bars you want in the visualizer between (10 - 256).
            visualizerLineBar.setDensity(70);

            // Initialize and set up the visualizer
            visualizerLineBar.setPlayer(audioSessionId);

        } else {
            // Handle the case where the audio session ID is invalid
            Log.e(TAG, "Invalid audio session ID");
            // You might want to handle this case by showing an error message or taking appropriate action.
        }
    }

    private void updateSongInfo(int position) {

        // Update UI with the current song information
        SongsModel currentSong = musicList.get(position);
        songName.setText(currentSong.getTitle());
        artistName.setText(currentSong.getArtist());

        // Total duration
        String duration = currentSong.getDuration();
        totalDuration.setText(duration);

        // Update other UI elements
        playPauseBtn.setImageResource(R.drawable.baseline_pause_24);

        // Load and set the song cover image using Glide (or any other image loading library)
        Glide.with(this)
                .load(currentSong.getImageUrl())
                .error(R.drawable.sound_vibe)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        // Handle image loading failure
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        // Extract colors from the loaded image using Palette
                        extractColorsFromImage(resource);
                        return false;
                    }
                })
                .into(songCover);

        songCover.setAnimation(loadRotation());

    }

    private void extractColorsFromImage(Drawable drawable) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                if (palette != null) {
                    // Get the dominant color from the palette
                    int dominantColor = palette.getDominantColor(ContextCompat.getColor(getApplicationContext(), R.color.black));

                    // Set background color
                    mContainer.setBackgroundColor(dominantColor);

                    // Set text color
                    songName.setTextColor(getTextColor(dominantColor));
                    artistName.setTextColor(getTextColor(dominantColor));
                    totalDuration.setTextColor(getTextColor(dominantColor));
                    durationPlayed.setTextColor(getTextColor(dominantColor));
                }
            }
        });
    }

    private int getTextColor(int backgroundColor) {
        // Use a simple logic to determine whether to use black or white text color based on the background color
        Log.e("luminance", Double.toString(ColorUtils.calculateLuminance(backgroundColor)));
        if (ColorUtils.calculateLuminance(backgroundColor) >= 0.5) {
            // Use dark text color for light backgrounds
            return ContextCompat.getColor(getApplicationContext(), R.color.black);
        } else {
            // Use light text color for dark backgrounds
            return ContextCompat.getColor(getApplicationContext(), R.color.white);
        }
    }

    public void playNextSong() {
        // Check if there is a next song
        if (player.hasNextMediaItem() && player.getRepeatMode() == Player.REPEAT_MODE_ALL) {
            player.seekToNext();
        } else if (player.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            // If repeat mode is "repeat one," seek to the beginning
            player.seekTo(0);
        }
    }

    public void playPreviousSong() {
        // Check if there is a previous song
        if (player.hasPreviousMediaItem() && player.getRepeatMode() == Player.REPEAT_MODE_ALL) {
            player.seekToPrevious();
        } else if (player.getRepeatMode() == Player.REPEAT_MODE_ONE) {
            // If repeat mode is "repeat one," seek to the beginning
            player.seekTo(0);
        }
    }

    public void togglePlayPause() {
        if (player.isPlaying()) {
            player.pause();
            playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_24);

        } else {
            player.play();
            playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {

        int currentSongIndex = player.getCurrentMediaItemIndex();

        // Check if the activity is not finishing (not destroyed)
        if (!isFinishing()) {
            // Update song info only if the activity is not destroyed
            ImageAnimation();
            updateSongInfo(currentSongIndex);
        }

        if (!player.isPlaying()) {
            player.play();
        }

    }

    @Override
    public void onPlaybackStateChanged(int state) {

        if (state == ExoPlayer.STATE_READY) {

            // Start updating durationPlayed and seekBar
            startUpdatingSeekBar();
        }
    }

    private String formatTime(long millis) {
        // Format the time in MM:SS or HH:MM:SS depending on the duration

        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        } else {
            long hours = minutes / 60;
            minutes %= 60;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
    }

    private void startUpdatingSeekBar() {
        // Update seekBar and durationPlayed every second
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSeekBarAndDuration();
                startUpdatingSeekBar(); // Schedule the next update
            }
        }, 1000);
    }

    private void updateSeekBarAndDuration() {
        long currentPosition = player.getCurrentPosition();
        seekBar.setMax((int) player.getDuration());
        seekBar.setProgress((int) currentPosition);
        durationPlayed.setText(formatTime(currentPosition));
    }

    private void stopUpdatingSeekBar() {
        // Remove any pending callbacks to stop updating the seekBar
        new Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null);
    }

    public void ImageAnimation() {
        // Create a scale-up animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(songCover, "scaleX", 0.8f, 1.0f);
        scaleX.setDuration(500); // Set the duration in milliseconds

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(songCover, "scaleY", 0.8f, 1.0f);
        scaleY.setDuration(500); // Set the duration in milliseconds

        // Create an alpha (opacity) animation
        ObjectAnimator alpha = ObjectAnimator.ofFloat(songCover, "alpha", 0.5f, 1.0f);
        alpha.setDuration(500); // Set the duration in milliseconds

        // Create a set of animations to play together
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);

        // Start the animation
        animatorSet.start();
    }

    private Animation loadRotation(){
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUpdatingSeekBar();
        doUnbindService();
        instance = null;
    }

    public void doUnbindService() {
        if (isBound) {
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }


    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.eqFrame);
        if(eqContainer.getVisibility() == View.GONE){
            super.onBackPressed();
        } else {
            if (fragment.isVisible() && eqContainer.getVisibility() == View.VISIBLE) {
                eqContainer.setVisibility(View.GONE);
            } else {
                if (player != null) {
                    player.release();
                }
                super.onBackPressed();
            }
        }

    }

}