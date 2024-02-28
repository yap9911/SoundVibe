package com.example.soundvibe;

public class SongsModel {
    private String SongUrl;
    private String ImageUrl;
    private String title;
    private String artist;
    private String duration;

    public SongsModel(String url, String imageUrl, String title, String artist, String duration) {
        SongUrl = url;
        ImageUrl = imageUrl;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
    }

    public String getSongUrl() {
        return SongUrl;
    }

    public void setSongUrl(String songUrl) {
        SongUrl = songUrl;
    }

    public String getImageUrl() {
        return ImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
