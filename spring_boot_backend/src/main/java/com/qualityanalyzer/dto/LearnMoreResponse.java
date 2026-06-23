package com.qualityanalyzer.dto;

import java.util.List;

public class LearnMoreResponse {
    // Topic overview (from Wikipedia)
    private String topicTitle;
    private String topicSummary;
    private String topicImageUrl;
    private String wikipediaUrl;

    // Related videos (from YouTube Search + quick analysis)
    private List<RelatedVideo> relatedVideos;

    // Learning roadmap (from Gemini AI)
    private List<String> roadmapSteps;

    // Official resources (from Gemini AI)
    private List<Resource> officialResources;

    public LearnMoreResponse() {}

    // ── Topic Overview ──
    public String getTopicTitle() { return topicTitle; }
    public void setTopicTitle(String topicTitle) { this.topicTitle = topicTitle; }

    public String getTopicSummary() { return topicSummary; }
    public void setTopicSummary(String topicSummary) { this.topicSummary = topicSummary; }

    public String getTopicImageUrl() { return topicImageUrl; }
    public void setTopicImageUrl(String topicImageUrl) { this.topicImageUrl = topicImageUrl; }

    public String getWikipediaUrl() { return wikipediaUrl; }
    public void setWikipediaUrl(String wikipediaUrl) { this.wikipediaUrl = wikipediaUrl; }

    // ── Related Videos ──
    public List<RelatedVideo> getRelatedVideos() { return relatedVideos; }
    public void setRelatedVideos(List<RelatedVideo> relatedVideos) { this.relatedVideos = relatedVideos; }

    // ── Roadmap ──
    public List<String> getRoadmapSteps() { return roadmapSteps; }
    public void setRoadmapSteps(List<String> roadmapSteps) { this.roadmapSteps = roadmapSteps; }

    // ── Resources ──
    public List<Resource> getOfficialResources() { return officialResources; }
    public void setOfficialResources(List<Resource> officialResources) { this.officialResources = officialResources; }
}
