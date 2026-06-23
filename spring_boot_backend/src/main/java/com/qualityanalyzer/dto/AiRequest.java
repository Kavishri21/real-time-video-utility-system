package com.qualityanalyzer.dto;

import java.util.List;

public class AiRequest {
    private List<String> comments;

    public AiRequest(List<String> comments) {
        this.comments = comments;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
    }
}
