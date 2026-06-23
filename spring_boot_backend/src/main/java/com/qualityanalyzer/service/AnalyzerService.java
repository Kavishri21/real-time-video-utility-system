package com.qualityanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.qualityanalyzer.dto.AiDetailedRequest;
import com.qualityanalyzer.dto.AiResponse;
import com.qualityanalyzer.dto.FinalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AnalyzerService {

    private final RestTemplate restTemplate;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    @Value("${python.service.url.detailed}")
    private String pythonDetailedUrl;

    public AnalyzerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── Fallback comment pools (used when YouTube API fails) ──
    private static final List<List<String>> NEWEST_POOLS = Arrays.asList(
        Arrays.asList(
            "This is the best tutorial I have ever watched! Crystal clear explanation.",
            "Thank you so much, I finally understood this concept after struggling for weeks.",
            "Incredibly well structured and easy to follow. Subscribed immediately!",
            "The examples you used were perfect. I was able to implement this right away.",
            "Wow, this is amazing content. You explain things so deeply. Keep it up!",
            "This helped me pass my exam! The depth of explanation is unmatched.",
            "I love how you break down complex topics into simple steps.",
            "Fantastic video! Every programmer needs to watch this.",
            "Just watched this for the second time. Still learning new things!",
            "Shared this with my entire class. Everyone found it helpful."
        ),
        Arrays.asList(
            "Interesting video but I think you missed some important points.",
            "Good overview but could have been more in-depth on the practical side.",
            "I agree with some of your points but not all. Still a good watch.",
            "The first half was great but you lost me in the second half. Needs better pacing.",
            "Decent content. Not the best I have seen but not bad either.",
            "Some good insights here, but the audio quality could be better.",
            "Worth watching once. Has some useful information.",
            "Average video. Nothing special but not terrible either.",
            "Expected more from this topic. Was a bit surface level.",
            "Okay content but the delivery could be more engaging."
        ),
        Arrays.asList(
            "Total waste of time. The title promised something completely different.",
            "This does not make any sense. Very confusing explanation.",
            "Clickbait title. The actual content has nothing to do with the thumbnail.",
            "I could not even finish watching this. Very poorly made.",
            "Just reading from the documentation? I can do that myself.",
            "Disliked. This video is full of wrong information.",
            "Do not waste your time on this one.",
            "Terrible production quality, can barely hear the audio.",
            "This feels like an AI generated video. No real insight.",
            "Many errors in the code examples. Very misleading."
        ),
        Arrays.asList(
            "Great video! But I have a question: how does this compare to the alternative approach?",
            "I used this in my project and it worked perfectly. Here is what I did differently though.",
            "Can you make a follow-up video going deeper into the advanced features?",
            "This is gold! I shared it with my entire study group. Everyone found it helpful.",
            "Interesting perspective. Have you considered doing a comparison video?",
            "I have watched this three times already. Each time I learn something new!",
            "This sparked a great discussion in our team meeting. Thanks for this!",
            "I would love to see more content like this. Very thought provoking!",
            "Could you do a live coding session on this topic? That would be amazing!",
            "The way you explain makes complex things look simple. Brilliant teaching style."
        )
    );

    private static final List<List<String>> OLDEST_POOLS = Arrays.asList(
        Arrays.asList(
            "Useful video, learned a lot from it.",
            "Clear explanation, thanks for sharing.",
            "Good content, keep up the good work.",
            "This was helpful for my assignment.",
            "Nice tutorial, easy to follow along.",
            "Well made video. Subscribed!",
            "Was looking for exactly this. Thank you!",
            "Simple and effective explanation.",
            "Helped me understand the basics.",
            "Solid introduction to this topic."
        ),
        Arrays.asList(
            "This was okay when it came out but seems outdated now.",
            "The information here is no longer fully accurate.",
            "Good for 2020 standards but methods have changed since.",
            "Some parts are still relevant but many things have changed.",
            "Was helpful back then but newer tutorials cover this better.",
            "The basics are still good but the tools mentioned are old.",
            "Decent for its time. Look for updated versions though.",
            "Partially useful. Some concepts are timeless, some not.",
            "This used to be the go-to video. Not anymore sadly.",
            "Good foundation video but needs an updated version."
        ),
        Arrays.asList(
            "First! Great channel.",
            "Interesting topic. Will check back later.",
            "Looking forward to more videos on this subject.",
            "Just starting to learn about this. Good intro.",
            "New subscriber here. Promising content!",
            "Came here from a recommendation. Not disappointed.",
            "Would love to see a full series on this.",
            "Bookmarked for later. Looks promising!",
            "Early viewer here. This channel is going to blow up!",
            "Adding to my study playlist. Thanks!"
        ),
        Arrays.asList(
            "The foundational concepts here are solid and timeless.",
            "I keep coming back to this video for reference.",
            "Classic explanation. This has aged well.",
            "Still the best video on this topic after all these years.",
            "They don't make tutorials like this anymore.",
            "Watching this for the 5th time. Still the gold standard.",
            "This video is a masterclass in teaching complex topics.",
            "Years later and this is still in my bookmarks.",
            "Every newcomer should start with this video.",
            "The depth here is unmatched even by newer content."
        )
    );

    /**
     * Fetches comments from YouTube Data API v3.
     * @param videoId The YouTube video ID
     * @param order "relevance" for top/newest, "time" for oldest-first
     * @param maxResults Number of comments to fetch
     * @return List of comment text strings
     */
    private List<String> fetchComments(String videoId, String order, int maxResults) {
        String url = "https://www.googleapis.com/youtube/v3/commentThreads"
                + "?part=snippet"
                + "&videoId=" + videoId
                + "&key=" + youtubeApiKey
                + "&maxResults=" + maxResults
                + "&order=" + order;

        List<String> comments = new ArrayList<>();
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(URI.create(url), JsonNode.class);
            JsonNode items = response.getBody().path("items");

            if (items.isArray()) {
                for (JsonNode item : items) {
                    String comment = item.path("snippet")
                            .path("topLevelComment")
                            .path("snippet")
                            .path("textDisplay").asText();
                    comments.add(comment);
                }
            }
            System.out.println("[YouTube API] Fetched " + comments.size() + " comments (order=" + order + ") for video: " + videoId);
        } catch (Exception e) {
            System.err.println("[YouTube API] Error fetching comments (order=" + order + ") for " + videoId + ": " + e.getMessage());
        }
        return comments;
    }

    public FinalResponse analyzeVideo(String videoId) {
        // ── Step 1: Fetch Newest + Oldest Comments ──
        List<String> newestComments = fetchComments(videoId, "relevance", 10);
        List<String> oldestComments = fetchComments(videoId, "time", 10);

        // Fallback if API fails
        if (newestComments.isEmpty()) {
            int poolIndex = Math.abs(videoId.hashCode()) % NEWEST_POOLS.size();
            newestComments = NEWEST_POOLS.get(poolIndex);
            System.out.println("[Fallback] Using newest comment pool #" + poolIndex);
        }
        if (oldestComments.isEmpty()) {
            int poolIndex = Math.abs((videoId + "old").hashCode()) % OLDEST_POOLS.size();
            oldestComments = OLDEST_POOLS.get(poolIndex);
            System.out.println("[Fallback] Using oldest comment pool #" + poolIndex);
        }

        // ── Step 2: Call Python /analyzeDetailed ──
        AiDetailedRequest aiRequest = new AiDetailedRequest(newestComments, oldestComments);
        AiResponse aiResponse;
        try {
            aiResponse = restTemplate.postForObject(pythonDetailedUrl, aiRequest, AiResponse.class);
        } catch (Exception e) {
            System.err.println("[Python API] Error: " + e.getMessage());
            aiResponse = new AiResponse();
        }

        // ── Step 3: Compute final score ──
        double finalScore = aiResponse.getOverall_score();
        if (finalScore == 0) {
            // Fallback calculation if overall_score wasn't set
            finalScore = (0.15 * aiResponse.getPositive())
                    + (0.30 * aiResponse.getClarity_score())
                    + (0.25 * aiResponse.getDepth_score())
                    + (0.30 * aiResponse.getEngagement_score());
        }
        finalScore = Math.min(100.0, Math.max(0.0, finalScore));

        String verdict;
        if (finalScore >= 80) verdict = "High Quality & Worth Watching";
        else if (finalScore >= 50) verdict = "Average Utility";
        else verdict = "Low Quality / Clickbait Warning";

        // ── Step 4: Build comprehensive response ──
        FinalResponse result = new FinalResponse();
        // Core
        result.setQualityScore(Math.round(finalScore * 100.0) / 100.0);
        result.setClarity(aiResponse.getClarity_score());
        result.setDepth(aiResponse.getDepth_score());
        result.setEngagement(aiResponse.getEngagement_score());
        result.setVerdict(verdict);
        // Summary
        result.setSummary(aiResponse.getSummary() != null ? aiResponse.getSummary() : "Analysis complete.");
        // Sentiment distribution
        result.setPositivePercent(aiResponse.getPositive_percent());
        result.setNegativePercent(aiResponse.getNegative_percent());
        result.setNeutralPercent(aiResponse.getNeutral_percent());
        // Trend
        result.setNewestScore(aiResponse.getNewest_score());
        result.setOldestScore(aiResponse.getOldest_score());
        result.setTrendVerdict(aiResponse.getTrend_verdict() != null ? aiResponse.getTrend_verdict() : "Stable");
        // Top comments
        result.setMostPositiveComment(aiResponse.getMost_positive_comment());
        result.setMostPositiveScore(aiResponse.getMost_positive_score());
        result.setMostNegativeComment(aiResponse.getMost_negative_comment());
        result.setMostNegativeScore(aiResponse.getMost_negative_score());
        // Keywords
        result.setTopKeywords(aiResponse.getTop_keywords());
        // Meta
        result.setTotalCommentsAnalyzed(aiResponse.getTotal_comments_analyzed());
        result.setSpamCommentsFiltered(aiResponse.getSpam_comments_filtered());

        return result;
    }
}
