package com.qualityanalyzer.dto;

public class LearnMoreRequest {
    private String title;
    private String videoId;

    public LearnMoreRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
}
