package com.example.soundvibe;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.options.StorageDownloadFileOptions;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

public class SongsListAdapter extends RecyclerView.Adapter<SongsListAdapter.ViewHolder> implements Filterable {
    Context context;
    private ArrayList<SongsModel> songsList;
    private ArrayList<SongsModel> songsListFull; // This will hold the full list for filtering

    public SongsListAdapter(Context context, ArrayList<SongsModel> songsList) {
    
        this.context = context;
        this.songsList = songsList;
        this.songsListFull = new ArrayList<>(songsList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SongsModel song = songsList.get(position);

        holder.SongNameTextView.setText(song.getTitle());
        holder.ArtistNameTextView.setText(song.getArtist());

        // Use Glide to load the image into the ImageView
        Glide.with(holder.itemView.getContext())
                .load(song.getImageUrl())
                .error(R.drawable.sound_vibe)
                .into(holder.SongImageView);

    }

    @Override
    public int getItemCount() {return songsList.size();}

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView SongNameTextView, ArtistNameTextView;
        ImageView SongImageView;
        ImageView downloadBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            SongNameTextView = itemView.findViewById(R.id.SongNameTextView);
            ArtistNameTextView = itemView.findViewById(R.id.ArtistNameTextView);
            SongImageView = itemView.findViewById(R.id.song_img);
            downloadBtn = itemView.findViewById(R.id.downloadBtn);

            // Set onClickListener for the itemView
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //start the player service
                    context.startService(new Intent(context.getApplicationContext(), PlayerNotificationService.class));

                    // Create an Intent to start the PlayerActivity
                    Intent intent = new Intent(itemView.getContext(), PlayerActivity.class);

                    // Pass data to the PlayerActivity
                    intent.putExtra("SONG_POSITION", getBindingAdapterPosition());

                    // Start the PlayerActivity
                    itemView.getContext().startActivity(intent);
                }
            });

            downloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // Get the song position for download
                    int songPosition = getBindingAdapterPosition();

                    // Use the song position to perform the download action
                    downloadSong(songPosition);
                }
            });
        }

        private void downloadSong(int songPosition) {
            // Use the song position to get the corresponding song from the list
            SongsModel song = songsList.get(songPosition);
            String SongDuration = song.getDuration();

            String formattedDuration = SongDuration.replace(":", ".");

            // Get the default Android download directory
            File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            // Specify the desired filename
            String fileName = song.getTitle() + "- " + song.getArtist() + ".mp3";

            // Create a File object with the download path
            File downloadFile = new File(downloadDirectory, fileName);

            // Perform the download action using the song information
            Amplify.Storage.downloadFile(
                    "Songs/" + song.getTitle() + "- " + song.getArtist() + "- " + formattedDuration + ".mp3",  // Adjust the path based on your storage structure
                    downloadFile,
                    StorageDownloadFileOptions.defaultInstance(),
                    progress -> {
                        // Handle download progress
                        Log.i("MyAmplifyApp", "Fraction completed: " + progress.getFractionCompleted());
                    },
                    result -> {
                        // Handle successful download
                        Log.i("MyAmplifyApp", "Successfully downloaded: " + result.getFile().getName());
                        // You may want to notify the user that the download is complete
                        Toast.makeText(context, "Download complete for " + song.getTitle(), Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        // Handle download failure
                        Log.e("MyAmplifyApp", "Download Failure", error);
                        Log.e("MyAmplifyApp", "Songs/" + song.getTitle() + "- " + song.getArtist() + "- " + song.getDuration() + ".mp3", error);
                        // You may want to notify the user that the download failed
                        Toast.makeText(context, "Download failed for " + song.getTitle(), Toast.LENGTH_SHORT).show();
                    }
            );
        }

    }

    // update songsListFull
    public void updateSongsListFull(ArrayList<SongsModel> updatedList) {
        songsListFull.clear();
        songsListFull.addAll(updatedList);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return songsFilter;
    }

    private Filter songsFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<SongsModel> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                // If the search query is empty, show the full list
                filteredList.addAll(songsListFull);
            } else {
                // Convert the constraint to lowercase for case-insensitive filtering
                String filterPattern = constraint.toString().toLowerCase().trim();

                // Iterate through the full list and add items that match the search query
                for (SongsModel song : songsListFull) {
                    if (song.getTitle().toLowerCase().contains(filterPattern) ||
                            song.getArtist().toLowerCase().contains(filterPattern)) {
                        filteredList.add(song);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Clear the current list
            songsList.clear();

            // Add the filtered results to the current list
            songsList.addAll((ArrayList<SongsModel>) results.values);

            // Notify the adapter that the data set has changed
            notifyDataSetChanged();
        }
    };

    // Method to reset the data to the original list
    public void resetData() {
        songsList.clear();
        songsList.addAll(songsListFull);
        notifyDataSetChanged();
    }
}

