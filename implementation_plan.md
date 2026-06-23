# YouTube Content Quality Analyzer - Implementation Plan

## Goal Description
Build a multi-tiered architecture (Chrome Extension -> Spring Boot -> Python ML) that analyzes YouTube video comments to determine Content Quality Score based on sentiment, clarity, depth, and engagement. 

The project execution is strictly split into **Version 1 (MVP)** for an immediate demo tomorrow, and **Full System** for the final project submission.

## Version 1 (MVP) - Immediate Action Plan

To ensure a working demo by tomorrow, we will take architectural shortcuts to achieve an end-to-end flow without blocking on rigorous data training.

### 1. Python AI Service (V1)
*   **Tech**: Python, FastAPI/Flask, NLTK/TextBlob.
*   **Implementation**: 
    *   Expose `POST /predictSentiment` receiving a list of comments.
    *   Since training an SVM/Random Forest model overnight on a brand-new dataset is risky and time-consuming, the MVP will use a pre-trained lexicon-based analyzer like **VADER** to get positivity/negativity scores instantly.
    *   *Aspects (Clarity, Depth, Engagement)*: Will be calculated using heuristics combining text length, question marks (depth), and sentiment intensity, or a zero-shot classifier if time permits.

### 2. Spring Boot Backend (V1)
*   **Tech**: Java, Spring Boot Web.
*   **Implementation**:
    *   Expose `POST /analyzeVideo` receiving `{ "videoId": "..." }`.
    *   Use the **YouTube Data API v3** to fetch comment threads. **Note:** Hard-capped to the **top 10 comments** to conserve the provided API quota. (API Key: `AIzaSyCdR_hhs3Z4BFZTAS0hs7gMZ856CK_KOCA`).
    *   Forward the comment text payload to the Python service.
    *   Apply the `Final Score Formula` combining the Python results into a 0-100 scale and return to the frontend.

### 3. Chrome Extension (V1)
*   **Tech**: Vanilla HTML/CSS/JS.
*   **Implementation**:
    *   A content script that detects the `v=` parameter in the active YouTube tab.
    *   Injects a UI Box next to the YouTube player (e.g., above the related videos column) with a "Analyze Quality" button.
    *   Fetches from `localhost:8080` and displays the "Clarity", "Depth", "Engagement", and "Total Quality Score".

## Full Project Strategy (Post-Demo)

The following components will be built out iteratively over the remaining project timeline to satisfy all final year academic requirements.

### Data Preprocessing & Custom ML Pipeline
*   **Data Cleaning**: Spring Boot or Python will implement strict Regex for URL/Emoji removal and stopword filtering.
*   **Model Training**: We will acquire a labeled dataset and train the requested algorithms: `Logistic Regression`, `Random Forest`, and `SVM`.
*   **Aspect Extraction**: Moving from heuristics to actual NLP extraction (dependency parsing or BERT-based embeddings).
*   **Spam Filtering**: Identifying bot patterns, repetitive characters, and dropping them from the Quality Score aggregation.

### Architecture Improvements
*   **Database Integration**: Spring Boot will save computed scores to a database. First time a video is analyzed, it takes time; subsequent requests serve cached data instantly.
*   **Extension Persistence**: Handle YouTube's asynchronous single-page navigation so the extension doesn't break when clicking a new video from the sidebar.

## Verification Plan

### Automated/Manual Verification for MVP
1.  **Python API Test**: `curl -X POST` to the Python API with mock comments to ensure structured JSON returns of sentiment and aspects.
2.  **Spring Integration Test**: Send a hardcoded video ID to the Spring Boot endpoint, verify it calls YouTube API, calls Python, and aggregates the correct JSON.
3.  **UI/Chrome Testing**: Load the extension as an "Unpacked Extension" in Chrome. Navigate to `youtube.com/watch?v=xxx`, click the analyze button, and verify the visually injected panel updates successfully without throwing CORS or DOM errors.

## User Review Required

> [!IMPORTANT]  
> 1. **YouTube API Key**: We will need a valid Google Cloud YouTube Data API v3 Key to fetch comments. (I will request this during execution).
> 2. **MVP Scope Agreement**: The MVP uses pre-trained simple models (VADER/TextBlob) to guarantee functionality for tomorrow's demo. Training Custom ML Models (SVM, Random Forest) will be done *after* tomorrow's demo as part of the Full Project. Please confirm if this approach works for you.
