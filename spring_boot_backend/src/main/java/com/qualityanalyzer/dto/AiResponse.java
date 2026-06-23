package com.qualityanalyzer.dto;

import java.util.List;

public class AiResponse {
    private double positive;
    private double negative;
    private double neutral;
    private double clarity_score;
    private double depth_score;
    private double engagement_score;
    private double overall_score;
    // Sentiment distribution
    private double positive_percent;
    private double negative_percent;
    private double neutral_percent;
    // Trend analysis
    private double newest_score;
    private double oldest_score;
    private String trend_verdict;
    // Top comments
    private String most_positive_comment;
    private double most_positive_score;
    private String most_negative_comment;
    private double most_negative_score;
    // Keywords and summary
    private List<String> top_keywords;
    private String summary;
    // Meta
    private int total_comments_analyzed;
    private int spam_comments_filtered;

    public AiResponse() {}

    // ── Core scores ──
    public double getPositive() { return positive; }
    public void setPositive(double positive) { this.positive = positive; }

    public double getNegative() { return negative; }
    public void setNegative(double negative) { this.negative = negative; }

    public double getNeutral() { return neutral; }
    public void setNeutral(double neutral) { this.neutral = neutral; }

    public double getClarity_score() { return clarity_score; }
    public void setClarity_score(double clarity_score) { this.clarity_score = clarity_score; }

    public double getDepth_score() { return depth_score; }
    public void setDepth_score(double depth_score) { this.depth_score = depth_score; }

    public double getEngagement_score() { return engagement_score; }
    public void setEngagement_score(double engagement_score) { this.engagement_score = engagement_score; }

    public double getOverall_score() { return overall_score; }
    public void setOverall_score(double overall_score) { this.overall_score = overall_score; }

    // ── Sentiment distribution ──
    public double getPositive_percent() { return positive_percent; }
    public void setPositive_percent(double positive_percent) { this.positive_percent = positive_percent; }

    public double getNegative_percent() { return negative_percent; }
    public void setNegative_percent(double negative_percent) { this.negative_percent = negative_percent; }

    public double getNeutral_percent() { return neutral_percent; }
    public void setNeutral_percent(double neutral_percent) { this.neutral_percent = neutral_percent; }

    // ── Trend analysis ──
    public double getNewest_score() { return newest_score; }
    public void setNewest_score(double newest_score) { this.newest_score = newest_score; }

    public double getOldest_score() { return oldest_score; }
    public void setOldest_score(double oldest_score) { this.oldest_score = oldest_score; }

    public String getTrend_verdict() { return trend_verdict; }
    public void setTrend_verdict(String trend_verdict) { this.trend_verdict = trend_verdict; }

    // ── Top comments ──
    public String getMost_positive_comment() { return most_positive_comment; }
    public void setMost_positive_comment(String most_positive_comment) { this.most_positive_comment = most_positive_comment; }

    public double getMost_positive_score() { return most_positive_score; }
    public void setMost_positive_score(double most_positive_score) { this.most_positive_score = most_positive_score; }

    public String getMost_negative_comment() { return most_negative_comment; }
    public void setMost_negative_comment(String most_negative_comment) { this.most_negative_comment = most_negative_comment; }

    public double getMost_negative_score() { return most_negative_score; }
    public void setMost_negative_score(double most_negative_score) { this.most_negative_score = most_negative_score; }

    // ── Keywords and summary ──
    public List<String> getTop_keywords() { return top_keywords; }
    public void setTop_keywords(List<String> top_keywords) { this.top_keywords = top_keywords; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // ── Meta ──
    public int getTotal_comments_analyzed() { return total_comments_analyzed; }
    public void setTotal_comments_analyzed(int total_comments_analyzed) { this.total_comments_analyzed = total_comments_analyzed; }

    public int getSpam_comments_filtered() { return spam_comments_filtered; }
    public void setSpam_comments_filtered(int spam_comments_filtered) { this.spam_comments_filtered = spam_comments_filtered; }
}
