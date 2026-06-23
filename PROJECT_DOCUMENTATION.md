# YouTube Content Quality Analyzer — Project Documentation

## 1. Project Overview

The **YouTube Content Quality Analyzer** is a multi-tiered system that analyzes YouTube video comments using AI-powered sentiment analysis to produce a **Content Quality Score**. Instead of relying on likes or clickbait titles, this system reads viewer comments and deduces the true quality of any video.

### Architecture Summary

```
┌──────────────────┐       ┌──────────────────────┐       ┌─────────────────────┐
│  Chrome Extension │──────▶│  Spring Boot Backend  │──────▶│  Python AI Service  │
│  (Frontend UI)    │◀──────│  (Orchestrator)       │◀──────│  (Sentiment Engine) │
│  Port: N/A        │       │  Port: 8080           │       │  Port: 8000         │
└──────────────────┘       └──────────────────────┘       └─────────────────────┘
                                     │
                                     ▼
                          ┌──────────────────────┐
                          │  YouTube Data API v3  │
                          │  (Google Cloud)       │
                          └──────────────────────┘
```

### Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Chrome Extension (Manifest V3), Vanilla JS/CSS |
| Backend | Java 17, Spring Boot 3.1.5, Maven |
| AI Service | Python 3, FastAPI, VADER (NLTK) |
| External API | YouTube Data API v3 |

---

## 2. File-by-File Documentation

---

### 2.1 Chrome Extension (`chrome_extension/`)

#### `manifest.json` — Extension Configuration

Defines the Chrome extension's identity, permissions, and behavior using **Manifest V3** format.

| Field | Purpose |
|---|---|
| `manifest_version: 3` | Uses the latest Chrome extension standard |
| `permissions` | `activeTab` — access the current tab; `storage` — reserved for future caching |
| `host_permissions` | Allows HTTP requests to `localhost:8080` (Spring Boot) and `youtube.com` |
| `content_scripts` | Injects `content.js` and `style.css` into every YouTube page at `document_idle` |

#### `content.js` — Core Extension Logic

This is the main script that runs inside every YouTube page. It handles:

**Key Functions:**

| Function | Description |
|---|---|
| `getVideoId(url)` | Extracts the `v=` parameter (video ID) from the YouTube URL using `URLSearchParams`. Returns `null` if not on a video page. |
| `injectUI()` | Creates and injects an HTML panel into the YouTube DOM containing the "Analyze Video Quality" button, metric bars (Clarity, Depth, Engagement), total score display, and verdict text. Skips injection if the panel already exists. |
| `handleAnalyzeClick()` | The main action handler. Sends a `POST` request to `http://localhost:8080/analyzeVideo` with the video ID. Updates the UI with the returned scores and verdict. Shows loading state during the request. |
| `checkAndInit()` | Detects if the current page is a YouTube video page. If yes, injects the UI panel. If no (e.g., YouTube homepage), hides the panel. |

**Navigation Handling:**
- Listens for YouTube's custom `yt-navigate-finish` event (YouTube is a Single Page Application, so normal page load events don't fire on navigation).
- Also uses a `MutationObserver` on `document.body` to detect URL changes as a fallback.

**Data Flow:**
1. Extracts `videoId` from URL → sends `{ videoId: "xxx" }` to Spring Boot
2. Receives `{ qualityScore, clarity, depth, engagement, verdict }` → updates the UI bars and labels

#### `style.css` — Panel Styling

Styles the injected UI panel with:
- **Fixed positioning** (top-right corner, `z-index: 2147483647` to stay on top of everything)
- **YouTube-native look**: Uses YouTube's color scheme (`#cc0000` red for the button, `#065fd4` blue for metric bars)
- **Dark mode support**: Detects YouTube's `html[dark]` attribute and switches to dark backgrounds/colors automatically
- **Smooth animations**: Progress bars use `transition: width 0.5s ease` for animated fill effects

---

### 2.2 Spring Boot Backend (`spring_boot_backend/`)

#### `pom.xml` — Maven Project Configuration

Defines the project as a Spring Boot 3.1.5 application with Java 17. Dependencies:
- `spring-boot-starter-web` — Provides REST API support, embedded Tomcat server, and Jackson JSON serialization
- Jackson (transitive) — Handles JSON parsing for YouTube API responses and Python service communication

#### `application.properties` — Runtime Configuration

| Property | Value | Purpose |
|---|---|---|
| `server.port` | `8080` | HTTP port for the Spring Boot server |
| `youtube.api.key` | `AIzaSy...` | Google Cloud YouTube Data API v3 key for fetching comments |
| `python.service.url` | `http://localhost:8000/predictSentiment` | URL of the Python AI service endpoint |

#### `QualityAnalyzerApplication.java` — Application Entry Point

The main Spring Boot application class. Contains:
- `main()` — Bootstraps the Spring application
- `restTemplate()` — A `@Bean` that creates a `RestTemplate` instance (Spring's HTTP client) for making API calls to YouTube and the Python service

#### `controller/VideoController.java` — REST API Controller

Exposes the single REST endpoint that the Chrome extension calls.

| Annotation/Method | Purpose |
|---|---|
| `@RestController` | Marks this class as a REST API controller |
| `@CrossOrigin(origins = "*")` | Allows requests from any origin (required for the Chrome extension to call `localhost`) |
| `@PostMapping("/analyzeVideo")` | Maps POST requests to `/analyzeVideo` |
| `analyzeVideo(@RequestBody VideoRequest)` | Receives `{ "videoId": "xxx" }`, validates it, calls `AnalyzerService`, and returns the `FinalResponse` as JSON |

#### `service/AnalyzerService.java` — Business Logic (Core Orchestrator)

This is the heart of the backend. It orchestrates the entire analysis pipeline.

**Method: `analyzeVideo(String videoId)`**

| Step | Action |
|---|---|
| **Step 1** | Constructs the YouTube Data API v3 URL and fetches the top 10 comments for the given video using `RestTemplate` and `URI.create()`. |
| **Step 2 (Fallback)** | If the YouTube API fails (quota/network), selects one of 4 pre-defined comment pools based on the video ID's hash code. This ensures different videos still produce different scores. |
| **Step 3** | Sends the list of comments to the Python AI service (`POST /predictSentiment`) and receives sentiment + aspect scores. |
| **Step 4** | Computes the final **Content Quality Score** using a weighted formula: `15% × Positive Sentiment + 30% × Clarity + 25% × Depth + 30% × Engagement`. |
| **Step 5** | Determines the verdict: ≥80 = "High Quality & Worth Watching", ≥50 = "Average Utility", <50 = "Low Quality / Clickbait Warning". |
| **Step 6** | Returns the complete `FinalResponse` JSON to the Chrome extension. |

**Fallback Comment Pools:**

| Pool | Theme | Triggered When |
|---|---|---|
| Pool 0 | Very Positive (tutorial praise) | `videoId.hashCode() % 4 == 0` |
| Pool 1 | Mixed (reviews/opinions) | `videoId.hashCode() % 4 == 1` |
| Pool 2 | Negative (clickbait complaints) | `videoId.hashCode() % 4 == 2` |
| Pool 3 | Engaged Discussion (questions) | `videoId.hashCode() % 4 == 3` |

#### `dto/VideoRequest.java` — Incoming Request DTO

Simple POJO with a single field `videoId` (String). Represents the JSON body sent by the Chrome extension: `{ "videoId": "dQw4w9WgXcQ" }`.

#### `dto/AiRequest.java` — Python Service Request DTO

Contains a `List<String> comments` field. This is serialized to JSON and sent to the Python service as `{ "comments": ["comment1", "comment2", ...] }`.

#### `dto/AiResponse.java` — Python Service Response DTO

Maps the JSON response from the Python service. Fields:

| Field | Type | Description |
|---|---|---|
| `positive` | double | Average positive sentiment (0–100) |
| `negative` | double | Average negative sentiment (0–100) |
| `neutral` | double | Average neutral sentiment (0–100) |
| `clarity_score` | double | How clearly the video explains its topic (0–100) |
| `depth_score` | double | How in-depth the content is (0–100) |
| `engagement_score` | double | How engaged the audience is (0–100) |

#### `dto/FinalResponse.java` — Final Response DTO

The JSON response sent back to the Chrome extension. Fields:

| Field | Type | Description |
|---|---|---|
| `qualityScore` | double | The final Content Quality Score (0–100) |
| `clarity` | double | Clarity aspect score |
| `depth` | double | Depth aspect score |
| `engagement` | double | Engagement aspect score |
| `verdict` | String | Human-readable verdict text |

---

### 2.3 Python AI Service (`python_service/`)

#### `requirements.txt` — Python Dependencies

| Package | Version | Purpose |
|---|---|---|
| `fastapi` | 0.103.2 | Modern async web framework for the REST API |
| `uvicorn` | 0.23.2 | ASGI server to run FastAPI |
| `vaderSentiment` | 3.3.2 | Pre-trained lexicon-based sentiment analyzer (no training required) |
| `pydantic` | 2.4.2 | Data validation and serialization for request/response models |

#### `main.py` — AI Sentiment Analysis Engine

**Pydantic Models:**

| Model | Purpose |
|---|---|
| `CommentsPayload` | Validates incoming request: `{ "comments": ["...", "..."] }` |
| `AnalysisResponse` | Defines the response shape with all 6 score fields |

**Endpoint: `POST /predictSentiment`**

Receives a list of YouTube comments and returns sentiment + aspect scores.

**Analysis Pipeline:**

| Step | Process | Details |
|---|---|---|
| **1. VADER Sentiment** | Each comment is analyzed using VADER's `polarity_scores()` | Returns `pos`, `neg`, `neu` (0–1 each) and `compound` (-1 to +1) per comment |
| **2. Aspect Heuristics** | Counts word length and question marks per comment | Used to estimate depth and engagement |
| **3. Averaging** | All per-comment scores are averaged | `pos`, `neg`, `neu` are scaled to 0–100 |
| **4. Compound Score** | VADER's compound score is averaged and normalized to 0–100 | Better overall sentiment indicator than raw `pos` alone |
| **5. Clarity** | `40 + (compound × 0.6) + (pos × 0.3) − (neg × 0.3)` | Clear content generates positive, non-negative reactions |
| **6. Depth** | `35 + length_bonus + question_bonus + (compound × 0.4)` | Longer comments + questions = deeper content discussion |
| **7. Engagement** | `40 + (compound × 0.5) + (pos × 0.3) + volume_bonus` | Active, positive commenting = high engagement |

**Why VADER?**
VADER (Valence Aware Dictionary and sEntiment Reasoner) is a pre-trained, lexicon-based analyzer that works out-of-the-box without any training data. It's specifically tuned for social media text, handling slang, emojis, punctuation emphasis (e.g., "AMAZING!!!"), and capitalization — making it ideal for YouTube comments.

---

## 3. End-to-End Workflow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    COMPLETE REQUEST FLOW                            │
└─────────────────────────────────────────────────────────────────────┘

  USER clicks "Analyze Video Quality" on YouTube
       │
       ▼
  ┌─────────────────────────────────────────────────┐
  │ 1. CHROME EXTENSION (content.js)                │
  │    • Extracts videoId from URL (e.g., "abc123") │
  │    • Shows "Analyzing..." loading state         │
  │    • Sends POST to localhost:8080/analyzeVideo   │
  │      Body: { "videoId": "abc123" }              │
  └────────────────────┬────────────────────────────┘
                       │
                       ▼
  ┌─────────────────────────────────────────────────┐
  │ 2. SPRING BOOT (VideoController.java)           │
  │    • Receives the request                       │
  │    • Validates videoId is not null/empty         │
  │    • Delegates to AnalyzerService                │
  └────────────────────┬────────────────────────────┘
                       │
                       ▼
  ┌─────────────────────────────────────────────────┐
  │ 3. SPRING BOOT (AnalyzerService.java)           │
  │    • Calls YouTube Data API v3:                 │
  │      GET commentThreads?videoId=abc123          │
  │    • Extracts top 10 comment texts              │
  │    • (Fallback: uses hashed mock comments       │
  │      if API fails)                              │
  └────────────────────┬────────────────────────────┘
                       │
                       ▼
  ┌─────────────────────────────────────────────────┐
  │ 4. PYTHON AI SERVICE (main.py)                  │
  │    • Receives: { "comments": ["...", "..."] }   │
  │    • Runs VADER sentiment on each comment       │
  │    • Computes compound sentiment score          │
  │    • Calculates Clarity, Depth, Engagement      │
  │      using heuristic formulas                   │
  │    • Returns all 6 scores as JSON               │
  └────────────────────┬────────────────────────────┘
                       │
                       ▼
  ┌─────────────────────────────────────────────────┐
  │ 5. SPRING BOOT (AnalyzerService.java)           │
  │    • Receives Python response                   │
  │    • Computes Final Quality Score:              │
  │      15% Positive + 30% Clarity                 │
  │      + 25% Depth + 30% Engagement               │
  │    • Determines verdict string                  │
  │    • Returns FinalResponse JSON                 │
  └────────────────────┬────────────────────────────┘
                       │
                       ▼
  ┌─────────────────────────────────────────────────┐
  │ 6. CHROME EXTENSION (content.js)                │
  │    • Receives the JSON response                 │
  │    • Updates Clarity/Depth/Engagement bars       │
  │    • Displays total score (e.g., "72.5/100")    │
  │    • Shows verdict ("High Quality & Worth       │
  │      Watching")                                 │
  │    • Button changes to "Re-Analyze Video"       │
  └─────────────────────────────────────────────────┘
```

---

## 4. Scoring Formula Summary

### Python AI Service — Aspect Score Computation

| Aspect | Formula | Range |
|---|---|---|
| **Clarity** | `40 + (compound_norm × 0.6) + (avg_pos × 0.3) − (avg_neg × 0.3)` | 0–100 |
| **Depth** | `35 + length_bonus(max 25) + question_bonus(max 15) + (compound_norm × 0.4)` | 0–100 |
| **Engagement** | `40 + (compound_norm × 0.5) + (avg_pos × 0.3) + volume_bonus(max 15)` | 0–100 |

### Spring Boot — Final Quality Score

```
Final Score = (0.15 × Positive) + (0.30 × Clarity) + (0.25 × Depth) + (0.30 × Engagement)
```

### Verdict Thresholds

| Score Range | Verdict |
|---|---|
| ≥ 80 | High Quality & Worth Watching |
| 50 – 79 | Average Utility |
| < 50 | Low Quality / Clickbait Warning |

---

## 5. How to Run

### Prerequisites
- Java 17+, Maven, Python 3.x, Google Chrome

### Step 1 — Python AI Service
```bash
cd python_service
venv\Scripts\activate
pip install -r requirements.txt    # first time only
python main.py                     # starts on port 8000
```

### Step 2 — Spring Boot Backend
```bash
cd spring_boot_backend
mvn spring-boot:run                # starts on port 8080
```

### Step 3 — Chrome Extension
1. Open `chrome://extensions/`
2. Enable **Developer mode**
3. Click **Load unpacked** → select `chrome_extension/` folder

### Step 4 — Test
Open any YouTube video → click **"Analyze Video Quality"** → view results!
