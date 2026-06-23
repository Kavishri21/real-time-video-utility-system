package com.qualityanalyzer.dto;

import java.util.List;

public class AiDetailedRequest {
    private List<String> newestComments;
    private List<String> oldestComments;

    public AiDetailedRequest() {}

    public AiDetailedRequest(List<String> newestComments, List<String> oldestComments) {
        this.newestComments = newestComments;
        this.oldestComments = oldestComments;
    }

    public List<String> getNewestComments() { return newestComments; }
    public void setNewestComments(List<String> newestComments) { this.newestComments = newestComments; }

    public List<String> getOldestComments() { return oldestComments; }
    public void setOldestComments(List<String> oldestComments) { this.oldestComments = oldestComments; }
}
