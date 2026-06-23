package com.qualityanalyzer.dto;

import java.util.List;

public class FinalResponse {
    // Core scores
    private double qualityScore;
    private double clarity;
    private double depth;
    private double engagement;
    private String verdict;
    // AI Summary
    private String summary;
    // Sentiment distribution
    private double positivePercent;
    private double negativePercent;
    private double neutralPercent;
    // Trend analysis
    private double newestScore;
    private double oldestScore;
    private String trendVerdict;
    // Top comments
    private String mostPositiveComment;
    private double mostPositiveScore;
    private String mostNegativeComment;
    private double mostNegativeScore;
    // Keywords
    private List<String> topKeywords;
    // Meta
    private int totalCommentsAnalyzed;
    private int spamCommentsFiltered;

    public FinalResponse() {}

    // ── Core scores ──
    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public double getClarity() { return clarity; }
    public void setClarity(double clarity) { this.clarity = clarity; }

    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }

    public double getEngagement() { return engagement; }
    public void setEngagement(double engagement) { this.engagement = engagement; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    // ── Summary ──
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // ── Sentiment distribution ──
    public double getPositivePercent() { return positivePercent; }
    public void setPositivePercent(double positivePercent) { this.positivePercent = positivePercent; }

    public double getNegativePercent() { return negativePercent; }
    public void setNegativePercent(double negativePercent) { this.negativePercent = negativePercent; }

    public double getNeutralPercent() { return neutralPercent; }
    public void setNeutralPercent(double neutralPercent) { this.neutralPercent = neutralPercent; }

    // ── Trend analysis ──
    public double getNewestScore() { return newestScore; }
    public void setNewestScore(double newestScore) { this.newestScore = newestScore; }

    public double getOldestScore() { return oldestScore; }
    public void setOldestScore(double oldestScore) { this.oldestScore = oldestScore; }

    public String getTrendVerdict() { return trendVerdict; }
    public void setTrendVerdict(String trendVerdict) { this.trendVerdict = trendVerdict; }

    // ── Top comments ──
    public String getMostPositiveComment() { return mostPositiveComment; }
    public void setMostPositiveComment(String mostPositiveComment) { this.mostPositiveComment = mostPositiveComment; }

    public double getMostPositiveScore() { return mostPositiveScore; }
    public void setMostPositiveScore(double mostPositiveScore) { this.mostPositiveScore = mostPositiveScore; }

    public String getMostNegativeComment() { return mostNegativeComment; }
    public void setMostNegativeComment(String mostNegativeComment) { this.mostNegativeComment = mostNegativeComment; }

    public double getMostNegativeScore() { return mostNegativeScore; }
    public void setMostNegativeScore(double mostNegativeScore) { this.mostNegativeScore = mostNegativeScore; }

    // ── Keywords ──
    public List<String> getTopKeywords() { return topKeywords; }
    public void setTopKeywords(List<String> topKeywords) { this.topKeywords = topKeywords; }

    // ── Meta ──
    public int getTotalCommentsAnalyzed() { return totalCommentsAnalyzed; }
    public void setTotalCommentsAnalyzed(int totalCommentsAnalyzed) { this.totalCommentsAnalyzed = totalCommentsAnalyzed; }

    public int getSpamCommentsFiltered() { return spamCommentsFiltered; }
    public void setSpamCommentsFiltered(int spamCommentsFiltered) { this.spamCommentsFiltered = spamCommentsFiltered; }
}
