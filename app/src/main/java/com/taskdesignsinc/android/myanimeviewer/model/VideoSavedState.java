package com.taskdesignsinc.android.myanimeviewer.model;

import com.taskdesignsinc.android.myanimeviewer.fragment.base.PlaybackState;

public class VideoSavedState {

    // region Fields
    private String videoUrl;
    private long currentPosition;
    private float currentVolume;
    private PlaybackState playbackState;
    private String castInfo;
    // endregion

    public VideoSavedState() {

    }

    // region Getters
    public String getVideoUrl() {
        return videoUrl;
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    public float getCurrentVolume() {
        return currentVolume;
    }

    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    public String getCastInfo() {
        return castInfo;
    }

    // endregion

    // region Setters
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = currentPosition;
    }

    public void setCurrentVolume(float currentVolume) {
        this.currentVolume = currentVolume;
    }

    public void setPlaybackState(PlaybackState playbackState) {
        this.playbackState = playbackState;
    }

    public void setCastInfo(String castInfo) {
        this.castInfo = castInfo;
    }

    // endregion
}