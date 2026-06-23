package com.qualityanalyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.qualityanalyzer.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class LearnMoreController {

    private final RestTemplate restTemplate;

    @Value("${youtube.api.key}")
    private String youtubeApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${wikipedia.api.url}")
    private String wikipediaBaseUrl;

    @Value("${python.service.url.learnmore}")
    private String pythonLearnMoreUrl;

    @Value("${python.service.url}")
    private String pythonSentimentUrl;

    public LearnMoreController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/learnMore")
    public LearnMoreResponse learnMore(@RequestBody LearnMoreRequest request) {
        String title = request.getTitle();
        String videoId = request.getVideoId();

        System.out.println("[LearnMore] Request for title: " + title + ", videoId: " + videoId);

        LearnMoreResponse response = new LearnMoreResponse();

        // ── 1. WIKIPEDIA: Topic Overview ──
        fetchWikipediaInfo(title, response);

        // ── 2. PYTHON + GEMINI: Roadmap & Resources ──
        fetchRoadmapAndResources(title, response);

        // ── 3. YOUTUBE SEARCH: Related Videos + Quick Analysis ──
        fetchRelatedVideos(title, videoId, response);

        return response;
    }

    /**
     * Fetch topic summary from Wikipedia API (free, no key needed)
     */
    private void fetchWikipediaInfo(String title, LearnMoreResponse response) {
        // Extract likely topic words from title
        String[] words = title.replaceAll("[^a-zA-Z0-9.\\s]", " ").split("\\s+");
        List<String> topicCandidates = new ArrayList<>();

        Set<String> fillerWords = Set.of(
            "tutorial", "course", "full", "complete", "crash", "beginner", "beginners",
            "advanced", "introduction", "intro", "guide", "masterclass", "learn",
            "tamil", "hindi", "english", "telugu", "free", "for", "in", "with",
            "and", "the", "a", "an", "to", "of", "on", "hours", "hour", "video",
            "programming", "code", "io", "dev", "tech", "explained", "basics"
        );

        for (String word : words) {
            if (word.length() >= 2 && !fillerWords.contains(word.toLowerCase()) && !word.matches("\\d+")) {
                topicCandidates.add(word);
            }
        }

        // Try each topic candidate with Wikipedia
        for (String topic : topicCandidates) {
            try {
                String capitalizedTopic = topic.substring(0, 1).toUpperCase() + topic.substring(1);
                String encoded = URLEncoder.encode(capitalizedTopic, StandardCharsets.UTF_8);
                String wikiUrl = wikipediaBaseUrl + encoded;

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("User-Agent", "YouTubeQualityAnalyzer/1.0 (https://github.com/user/project)");
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

                ResponseEntity<JsonNode> wikiResponse = restTemplate.exchange(
                    URI.create(wikiUrl),
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    JsonNode.class
                );

                JsonNode body = wikiResponse.getBody();
                if (body != null && body.has("extract") && !body.path("extract").asText().isEmpty()) {
                    response.setTopicTitle(body.path("title").asText());
                    response.setTopicSummary(body.path("extract").asText());

                    // Get thumbnail if available
                    if (body.has("thumbnail")) {
                        response.setTopicImageUrl(body.path("thumbnail").path("source").asText());
                    }

                    // Get Wikipedia URL
                    if (body.has("content_urls")) {
                        response.setWikipediaUrl(
                            body.path("content_urls").path("desktop").path("page").asText()
                        );
                    }

                    System.out.println("[Wikipedia] Found article for: " + topic);
                    return; // Found a match, stop searching
                }
            } catch (Exception e) {
                System.out.println("[Wikipedia] No result for: " + topic);
            }
        }

        // Fallback if no Wikipedia article found
        response.setTopicTitle(title);
        response.setTopicSummary("No Wikipedia article found for this topic. The video covers: " + title);
    }

    /**
     * Call Python service to get Gemini-generated roadmap and resources
     */
    private void fetchRoadmapAndResources(String title, LearnMoreResponse response) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("geminiApiKey", geminiApiKey);

            JsonNode aiResult = restTemplate.postForObject(pythonLearnMoreUrl, payload, JsonNode.class);

            if (aiResult != null) {
                // Parse roadmap steps
                List<String> roadmap = new ArrayList<>();
                if (aiResult.has("roadmapSteps")) {
                    for (JsonNode step : aiResult.path("roadmapSteps")) {
                        roadmap.add(step.asText());
                    }
                }
                response.setRoadmapSteps(roadmap);

                // Parse resources
                List<Resource> resources = new ArrayList<>();
                if (aiResult.has("resources")) {
                    for (JsonNode res : aiResult.path("resources")) {
                        Resource r = new Resource();
                        r.setTitle(res.path("title").asText());
                        r.setUrl(res.path("url").asText());
                        r.setType(res.path("type").asText());
                        r.setDescription(res.path("description").asText());
                        resources.add(r);
                    }
                }
                response.setOfficialResources(resources);

                System.out.println("[Gemini] Generated " + roadmap.size() + " roadmap steps and " + resources.size() + " resources");
            }
        } catch (Exception e) {
            System.err.println("[Python/Gemini Error]: " + e.getMessage());
            response.setRoadmapSteps(List.of("Learn the basics", "Practice with projects", "Study advanced topics"));
            response.setOfficialResources(List.of());
        }
    }

    /**
     * Search YouTube for related videos and do quick quality analysis on each
     */
    private void fetchRelatedVideos(String title, String currentVideoId, LearnMoreResponse response) {
        try {
            // Clean title and create a targeted search query to avoid irrelevant videos & shorts
            String[] words = title.replaceAll("[^a-zA-Z0-9.\\s]", " ").split("\\s+");
            List<String> topicCandidates = new ArrayList<>();
            Set<String> fillerWords = Set.of(
                "full", "complete", "crash", "beginner", "beginners",
                "advanced", "introduction", "intro", "guide", "masterclass", "learn",
                "tamil", "hindi", "english", "telugu", "free", "for", "in", "with",
                "and", "the", "a", "an", "to", "of", "on", "hours", "hour", "video",
                "programming", "code", "io", "dev", "tech", "explained", "basics",
                "part", "ep", "episode", "season", "vs", "versus"
            );
            
            for (String word : words) {
                if (word.length() >= 2 && !fillerWords.contains(word.toLowerCase()) && !word.matches("\\d+")) {
                    topicCandidates.add(word);
                }
            }
            
            // Reconstruct a cleaner search query and append "-shorts" to filter short-form content
            String searchQuery = String.join(" ", topicCandidates);
            if (searchQuery.trim().isEmpty()) {
                searchQuery = title;
            } else {
                searchQuery += " tutorial -shorts"; 
            }

            System.out.println("[YouTube Search] Optimized Query: " + searchQuery);

            String query = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String searchUrl = "https://www.googleapis.com/youtube/v3/search"
                + "?part=snippet&type=video&maxResults=5&q=" + query
                + "&key=" + youtubeApiKey;

            ResponseEntity<JsonNode> searchResult = restTemplate.getForEntity(
                URI.create(searchUrl), JsonNode.class
            );

            List<RelatedVideo> relatedVideos = new ArrayList<>();
            JsonNode body = searchResult.getBody();
            if (body == null || !body.has("items")) {
                response.setRelatedVideos(relatedVideos);
                return;
            }
            
            JsonNode items = body.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    String relVideoId = item.path("id").path("videoId").asText();

                    // Skip the current video
                    if (relVideoId.equals(currentVideoId)) continue;

                    String relTitle = item.path("snippet").path("title").asText();
                    String channelName = item.path("snippet").path("channelTitle").asText();
                    String thumbnail = item.path("snippet").path("thumbnails").path("medium").path("url").asText();

                    // Quick analysis: fetch 3 comments and get a quick score
                    double quickScore = quickAnalyze(relVideoId);

                    String verdict;
                    if (quickScore >= 80) verdict = "High Quality";
                    else if (quickScore >= 50) verdict = "Average";
                    else verdict = "Low Quality";

                    relatedVideos.add(new RelatedVideo(
                        relTitle, relVideoId, channelName, thumbnail, 
                        Math.round(quickScore * 10.0) / 10.0, verdict
                    ));
                }
            }

            response.setRelatedVideos(relatedVideos);
            System.out.println("[YouTube Search] Found " + relatedVideos.size() + " related videos");

        } catch (Exception e) {
            System.err.println("[YouTube Search Error]: " + e.getMessage());
            response.setRelatedVideos(List.of());
        }
    }

    /**
     * Quick analysis: fetch 3 comments for a video and get a rough quality score
     */
    private double quickAnalyze(String videoId) {
        try {
            String url = "https://www.googleapis.com/youtube/v3/commentThreads"
                + "?part=snippet&videoId=" + videoId
                + "&key=" + youtubeApiKey
                + "&maxResults=3&order=relevance";

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                URI.create(url), JsonNode.class
            );

            List<String> comments = new ArrayList<>();
            JsonNode body = response.getBody();
            if (body != null && body.has("items")) {
                JsonNode items = body.path("items");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        comments.add(item.path("snippet").path("topLevelComment")
                            .path("snippet").path("textDisplay").asText());
                    }
                }
            }

            if (comments.isEmpty()) return 50.0; // Neutral default

            // Quick sentiment using Python's legacy endpoint
            Map<String, Object> payload = new HashMap<>();
            payload.put("comments", comments);

            JsonNode aiResult = restTemplate.postForObject(
                pythonSentimentUrl, payload, JsonNode.class
            );

            if (aiResult != null) {
                double clarity = aiResult.path("clarity_score").asDouble(50);
                double depth = aiResult.path("depth_score").asDouble(50);
                double engagement = aiResult.path("engagement_score").asDouble(50);
                double positive = aiResult.path("positive").asDouble(20);
                return Math.min(100, (0.15 * positive) + (0.30 * clarity) + (0.25 * depth) + (0.30 * engagement));
            }
        } catch (Exception e) {
            // Silently fail — comments might be disabled
        }
        return 50.0; // Neutral default
    }
}
