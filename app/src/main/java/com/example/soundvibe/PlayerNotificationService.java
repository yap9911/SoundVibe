package com.example.soundvibe;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_LOW;
import static com.example.soundvibe.MainActivity.songsList;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PlayerNotificationService extends Service {

    private final IBinder serviceBinder = new ServiceBinder();
    ExoPlayer player;
    PlayerNotificationManager notificationManager;
    FloatingActionButton playPauseBtn;

    public class ServiceBinder extends Binder {
        public PlayerNotificationService getPlayerService(){
            return PlayerNotificationService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("PlayerNotificationService", "onCreate");
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player.setAudioAttributes(audioAttributes, true);

        //notification manager
        final String channelId = getResources().getString(R.string.app_name) + " Music Channel ";
        final int notificationId = 1;

        notificationManager = new PlayerNotificationManager.Builder(this, notificationId, channelId)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setChannelImportance(IMPORTANCE_LOW)
                .setSmallIconResourceId(R.drawable.baseline_notifications_24)
                .setPauseActionIconResourceId(R.drawable.baseline_pause_notification)
                .setPlayActionIconResourceId(R.drawable.baseline_play_arrow_notification)
                .setNextActionIconResourceId(R.drawable.baseline_skip_next_24)
                .setPreviousActionIconResourceId(R.drawable.baseline_skip_previous_24)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setChannelNameResourceId(R.string.app_name)
                .build();

        //set notification
        notificationManager.setPlayer(player);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);

    }

    //notification listener
    PlayerNotificationManager.NotificationListener notificationListener = new PlayerNotificationManager.NotificationListener() {
        @Override
        public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
            Log.d("PlayerNotificationService", "onNotificationCancelled");
            PlayerNotificationManager.NotificationListener.super.onNotificationCancelled(notificationId, dismissedByUser);
            if(player.isPlaying())
                player.pause();

            stopForeground(true);

            playPauseBtn = PlayerActivity.playPauseBtn;
            playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_24);

            // Stop the music if the notification is removed when the app is close
            if (PlayerActivity.instance == null) {
                notificationManager.setPlayer(null);
                player.release();
                player = null;
                stopForeground(true);
                stopSelf();
            }

        }

        @Override
        public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {

            Log.d("PlayerNotificationService", "onNotificationPosted");
            PlayerNotificationManager.NotificationListener.super.onNotificationPosted(notificationId, notification, ongoing);

            if (ongoing) {
                // Set the notification as ongoing, making it irremovable
                startForeground(notificationId, notification);
            } else {
                // Allow the notification to be removed
                stopForeground(false);
            }

            if (PlayerActivity.instance != null) {
                playPauseBtn = PlayerActivity.playPauseBtn;
                playPauseBtn.setImageResource(player.isPlaying() ? R.drawable.baseline_pause_24: R.drawable.baseline_play_arrow_24);
            }

        }
    };

    //notification description adapter
    PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter = new PlayerNotificationManager.MediaDescriptionAdapter() {

        @Nullable
        @Override
        public PendingIntent createCurrentContentIntent(Player player) {
            //intent to open the app when clicked
            Intent openAppIntent = new Intent(getApplicationContext(), MainActivity.class);

            return PendingIntent.getActivity(getApplicationContext(), 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        public CharSequence getCurrentContentTitle(Player player) {
            int currentSongPosition = player.getCurrentMediaItemIndex();

            SongsModel currentSong = songsList.get(currentSongPosition);
            return currentSong.getTitle();
        }

        @Nullable
        @Override
        public CharSequence getCurrentContentText(Player player) {
            int currentSongPosition = player.getCurrentMediaItemIndex();

            SongsModel currentSong = songsList.get(currentSongPosition);
            return currentSong.getArtist();
        }

        @SuppressLint("StaticFieldLeak")
        @Nullable
        @Override
        public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
            int currentSongPosition = player.getCurrentMediaItemIndex();

            SongsModel currentSong = songsList.get(currentSongPosition);

            // Load and set the song cover image using Glide (or any other image loading library) on a background thread
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    try {
                        return Glide.with(getApplicationContext())
                                .asBitmap()
                                .load(currentSong.getImageUrl())
                                .error(R.drawable.sound_vibe)
                                .submit()
                                .get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    // If loading is successful, set the large icon
                    if (bitmap != null) {
                        callback.onBitmap(bitmap);
                    } else {
                        // If loading fails, return a default icon
                        callback.onBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.sound_vibe));
                    }
                }
            }.execute();

            return null; // Return null synchronously, as the result will be delivered asynchronously
        }
    };

}

