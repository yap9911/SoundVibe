package com.example.soundvibe;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    final int NOTIFICATION_PERMISSION_REQUEST_CODE = 112;
    final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 113;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 114;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public static ArrayList<SongsModel> songsList = new ArrayList<>();

    public static SongsListAdapter adapter;
    private ImageButton searchButton, sortButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        }

        try {
            // Add these lines to add the AWSCognitoAuthPlugin and AWSS3StoragePlugin plugins
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSS3StoragePlugin());
            Amplify.configure(getApplicationContext());

            Log.i("MyAmplifyApp", "Initialized Amplify");
        } catch (AmplifyException error) {
            Log.e("MyAmplifyApp", "Could not initialize Amplify", error);
        }

        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("112")){
                getNotificationPermission();
            }
        }

        // Request RECORD_AUDIO permission
        checkRecordAudioPermission();

        // Request download permission
        verifyStoragePermissions();

        // Set up RecyclerView and adapter
        RecyclerView recyclerView = findViewById(R.id.SongsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new SongsListAdapter(this, songsList);
        recyclerView.setAdapter(adapter);

        // Amplify Storage list logic
        StoragePagedListOptions options;
        options = StoragePagedListOptions.builder()
                .setPageSize(1000)
                .build();

        Amplify.Storage.list(
                "Songs/", // Specify the folder path
                options,
                result -> {
                    songsList.clear();
                    for (StorageItem item : result.getItems()) {
                        String itemKey = item.getKey();
                        if (itemKey.equals("Songs/") || !itemKey.endsWith(".mp3")) {
                            continue; // Skip logging and adding if the item is just "Songs/" or not ending with ".mp3"
                        }
                        int index = itemKey.indexOf("Songs/") + "Songs/".length(); // Find the index after "Songs/"
                        String fileNameWithExtension = itemKey.substring(index); // Extract the song name with extension

                        /// Extract the songName, artist, and duration from the fileNameWithExtension
                        String fileName = fileNameWithExtension.replaceFirst("\\.mp3$", ""); // Remove the ".mp3" extension
                        String[] parts = fileName.split("- ");

                        // Split the last part into duration and file extension
                        String[] durationParts = parts[parts.length - 1].split("\\.");
                        String durationString = durationParts[0].trim();
                        int minutes = Integer.parseInt(durationString);
                        int seconds = Integer.parseInt(durationParts[1].trim());

                        // Format the duration as "mm:ss"
                        String formattedDuration = String.format("%01d:%02d", minutes, seconds);

                        // Extract the songName and artist
                        String songName = parts[0].trim(); // The song name, trimming any leading or trailing spaces
                        String artist = parts.length > 1 ? parts[1].trim() : "Unknown Artist"; // The artist name, trimming spaces

                        // Use the extracted information
                        String songUrl = "https://song-vibe-en-songs175638-dev.s3.ap-southeast-2.amazonaws.com/public/Songs/"
                                + fileNameWithExtension.replaceAll(" ", "+");

                        String imageName = songName + "- " + artist + ".jpg";
                        String imageUrl = "https://song-vibe-en-songs175638-dev.s3.ap-southeast-2.amazonaws.com/public/Songs/"
                                + imageName.replaceAll(" ", "+");

                        Log.i("MyAmplifyApp", "Song: " + songName);
                        Log.i("MyAmplifyApp", "Artist: " + artist);
                        Log.i("MyAmplifyApp", "Song Url: " + songUrl);
                        Log.i("MyAmplifyApp", "Image: " + imageName);
                        Log.i("MyAmplifyApp", "ImageUrl: " + imageUrl);
                        Log.i("MyAmplifyApp", "Duration: " + formattedDuration); // Use the formatted duration

                        // Create a SongsModel object and add it to the songsList
                        SongsModel songModel = new SongsModel(songUrl, imageUrl, songName, artist, formattedDuration);
                        songsList.add(songModel);
                    }

                    // Notify the adapter that the data set has changed on the UI thread
                    runOnUiThread(() -> {
                        adapter.updateSongsListFull(songsList);
                        adapter.notifyDataSetChanged();
                    });
                    Log.i("MyAmplifyApp", "Next Token: " + result.getNextToken());
                },
                error -> {
                    Log.e("MyAmplifyApp", "List failure", error);
                }
        );

        searchButton = findViewById(R.id.searchButton);

        // Set up click listener for the search button

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, SearchActivity.class);

                //go to search activity
                MainActivity.this.startActivity(intent);
            }
        });

        sortButton = findViewById(R.id.sortButton);

        // Set up click listener for the sort button

        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortOptions();
            }
        });

    }

    private void showSortOptions() {
        PopupMenu popupMenu = new PopupMenu(this, sortButton);
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());

        // Set up click listener for the menu items
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Handle menu item clicks here
                if (item.getItemId() == R.id.sort_by_name) {
                    sortByTitle();
                    return true;
                } else if (item.getItemId() == R.id.sort_by_artist) {
                    sortByArtist();
                    return true;
                }
                return false;
            }
        });

        // Show the popup menu
        popupMenu.show();
    }

    private void sortByTitle() {
        // Sort the songsList by title
        Collections.sort(songsList, new Comparator<SongsModel>() {
            @Override
            public int compare(SongsModel song1, SongsModel song2) {
                return song1.getTitle().compareToIgnoreCase(song2.getTitle());
            }
        });

        // Notify the adapter that the data set has changed
        adapter.notifyDataSetChanged();
    }

    private void sortByArtist() {
        // Sort the songsList by artist
        Collections.sort(songsList, new Comparator<SongsModel>() {
            @Override
            public int compare(SongsModel song1, SongsModel song2) {
                return song1.getArtist().compareToIgnoreCase(song2.getArtist());
            }
        });

        // Notify the adapter that the data set has changed
        adapter.notifyDataSetChanged();
    }

    public void getNotificationPermission(){
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }catch (Exception e){

        }
    }

    public void checkRecordAudioPermission() {
        // Check if the RECORD_AUDIO permission is not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Request the RECORD_AUDIO permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            // Permission is already granted, you can proceed with your logic
            Log.i("MyAmplifyApp", "RECORD_AUDIO permission already granted");
            // Add any additional logic you want to perform when the permission is already granted
        }
    }

    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case NOTIFICATION_PERMISSION_REQUEST_CODE:
                // Handle notification permission result
                handleNotificationPermissionResult(grantResults);
                break;

            case RECORD_AUDIO_PERMISSION_REQUEST_CODE:
                // Handle record audio permission result
                handleRecordAudioPermissionResult(grantResults);
                break;

            case REQUEST_EXTERNAL_STORAGE:
                handleDownloadPermissionResult(grantResults);
                // Handle download permission result

        }
    }
    private void handleNotificationPermissionResult(int[] grantResults) {
        // Handle notification permission result
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied
            Toast.makeText(this, "Notification permission is required for the app to function properly.", Toast.LENGTH_SHORT).show();
            finishAffinity(); // Close the app
        }
    }

    private void handleRecordAudioPermissionResult(int[] grantResults) {
        // Handle RECORD_AUDIO permission result
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied
            Toast.makeText(this, "Audio recording permission is required for the app to function properly.", Toast.LENGTH_SHORT).show();
            finishAffinity(); // Close the app
        }
    }

    private void handleDownloadPermissionResult(int[] grantResults) {
        // Handle RECORD_AUDIO permission result
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            // Permission denied
            Toast.makeText(this, "Download file permission is required for the app to function properly.", Toast.LENGTH_SHORT).show();
            finishAffinity(); // Close the app
        }
    }
}

