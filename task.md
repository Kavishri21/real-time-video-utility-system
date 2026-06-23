# YouTube Content Quality Analyzer

## Phase 1: MVP (Version 1) ✅ COMPLETE
Provides a complete end-to-end flow with simplified ML capabilities to ensure a functional demo.

- [x] **1.1 Python AI Service (MVP)**
  - [x] Setup virtual environment and FastAPI/Flask skeleton.
  - [x] Implement `/predictSentiment` endpoint.
  - [x] Integrate VADER (NLTK) or TextBlob for immediate sentiment analysis (no training required).
  - [x] Implement heuristic or mocked scoring for Aspect-based metrics (clarity, depth, engagement) based on sentiment and text length.
- [x] **1.2 Spring Boot Backend (MVP)**
  - [x] Initialize Spring Boot REST API.
  - [x] Create `/analyzeVideo` endpoint.
  - [x] Integrate YouTube Data API v3 to fetch the top 10 comments for a given `videoId`.
  - [x] Wire up HTTP call to the Python AI service.
  - [x] Calculate the final `Content Quality Score` and return the JSON response.
- [x] **1.3 Chrome Extension (MVP)**
  - [x] Create `manifest.json` (Manifest V3).
  - [x] Develop `content.js` to extract `videoId` from the YouTube URL.
  - [x] Inject a basic CSS/HTML UI panel into the YouTube DOM to display the loading state and results.
  - [x] Handle CORS HTTP request from Extension to Spring Boot Backend.
- [x] **1.4 End-to-End Integration & Demo Prep**
  - [x] Run Python Service, Spring Boot, and install Chrome Extension locally.
  - [x] Test on multiple distinct YouTube videos.
  - [x] Fix any immediate bugs/edge cases hindering the demo path.

## Phase 2: Full Project (Version 2)
Implements advanced analysis, rich UI dashboard, and ML pipeline for the final year submission.

- [ ] **2.1 Advanced Data Preprocessing (Python)**
  - [ ] Strip HTML tags from YouTube comments (BeautifulSoup).
  - [ ] Remove URLs, excessive punctuation, and special characters.
  - [ ] Normalize Unicode and emoji → text conversion.
  - [ ] Lowercase normalization.
- [ ] **2.2 Spam & Credibility Filtering (Python)**
  - [ ] Detect and filter repetitive/copy-paste comments.
  - [ ] Filter very short comments (< 3 words) that add no analytical value.
  - [ ] Detect bot-like patterns (e.g., "Check out my channel", "Sub4Sub").
- [ ] **2.3 Enhanced Python Analysis Endpoint**
  - [ ] Create new `POST /analyzeDetailed` endpoint.
  - [ ] Accept newest + oldest comment batches separately for trend analysis.
  - [ ] Return per-comment sentiment scores to identify most positive/negative comments.
  - [ ] Extract top keywords using word frequency analysis.
  - [ ] Generate a template-based 3-line AI summary.
  - [ ] Compute separate scores for newest vs oldest to determine trend.
  - [ ] Return sentiment distribution (positive%/negative%/neutral%).
- [ ] **2.4 Spring Boot Backend Expansion**
  - [ ] Fetch 20 comments (10 newest + 10 oldest) using YouTube API `order` parameter.
  - [ ] Expand `FinalResponse` DTO with: summary, newestScore, oldestScore, trendVerdict, mostPositiveComment, mostNegativeComment, topKeywords, sentiment percentages.
  - [ ] Expand `AiResponse` DTO to match new Python response.
  - [ ] Update `AnalyzerService` to call new `/analyzeDetailed` endpoint and build enriched response.
- [ ] **2.5 Chrome Extension — Full Dashboard UI Overhaul**
  - [ ] Redesign panel from 320px card → 70% width slide-in overlay sidebar.
  - [ ] Build animated circular score gauge with color coding (red→yellow→green).
  - [ ] Add 3-line AI summary card section.
  - [ ] Build Canvas-based trend bar chart (Newest vs Oldest comment scores).
  - [ ] Build Canvas-based sentiment distribution donut chart.
  - [ ] Add "Top Comments" section (most positive + most negative, color-coded).
  - [ ] Add keyword tag pills section.
  - [ ] Add smooth slide-in/close animation + glassmorphism backdrop.
  - [ ] Ensure full YouTube dark mode support for all new sections.
  - [ ] Add scrollable inner content for smaller screens.
  - [ ] Support YouTube SPA navigation (detecting video changes without full reload).
- [ ] **2.6 End-to-End Testing & Polish**
  - [ ] Test on 5+ diverse YouTube videos (tutorials, reviews, music, controversial, etc.).
  - [ ] Verify trend chart shows meaningful differences.
  - [ ] Verify dark mode on all sections.
  - [ ] Performance test — ensure analysis completes in under 5 seconds.
