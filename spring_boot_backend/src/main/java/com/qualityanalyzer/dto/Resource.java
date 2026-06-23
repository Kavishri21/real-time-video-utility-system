package com.qualityanalyzer.dto;

public class Resource {
    private String title;
    private String url;
    private String type;        // "documentation", "github", "tutorial", "article"
    private String description;

    public Resource() {}

    public Resource(String title, String url, String type, String description) {
        this.title = title;
        this.url = url;
        this.type = type;
        this.description = description;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
