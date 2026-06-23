package com.qualityanalyzer.dto;

public class RelatedVideo {
    private String title;
    private String videoId;
    private String channelName;
    private String thumbnailUrl;
    private double qualityScore;
    private String verdict;

    public RelatedVideo() {}

    public RelatedVideo(String title, String videoId, String channelName, 
                        String thumbnailUrl, double qualityScore, String verdict) {
        this.title = title;
        this.videoId = videoId;
        this.channelName = channelName;
        this.thumbnailUrl = thumbnailUrl;
        this.qualityScore = qualityScore;
        this.verdict = verdict;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
}
