from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer
from bs4 import BeautifulSoup
from collections import Counter
import re
import math
import os
import nltk
import google.generativeai as genai

# Download NLTK stopwords (runs once, cached after)
try:
    nltk.data.find('corpora/stopwords')
except LookupError:
    nltk.download('stopwords', quiet=True)

from nltk.corpus import stopwords

app = FastAPI(title="YouTube Quality Analyzer AI Service")
analyzer = SentimentIntensityAnalyzer()
STOP_WORDS = set(stopwords.words('english'))

# ─── Additional low-value words to exclude from keyword extraction ───
EXTRA_STOP_WORDS = {
    'video', 'like', 'really', 'just', 'one', 'also', 'get', 'got',
    'would', 'could', 'make', 'much', 'even', 'still', 'way', 'thing',
    'go', 'going', 'know', 'see', 'want', 'think', 'watch', 'watched',
    'good', 'great', 'best', 'nice', 'well', 'thanks', 'thank',
    'please', 'im', "i'm", 'dont', "don't", 'cant', "can't", 'amp',
    'channel', 'subscribe', 'comment', 'youtube', 'u', 'ur', 'lol'
}

# ─── Spam / bot patterns ───
SPAM_PATTERNS = [
    r'check\s+out\s+my',
    r'sub\s*4\s*sub',
    r'subscribe\s+to\s+my',
    r'visit\s+my\s+channel',
    r'follow\s+me\s+on',
    r'free\s+(v-?bucks|robux|gift\s*card)',
    r'click\s+(here|the\s+link)',
    r'earn\s+money\s+online',
    r'(www\.|https?://)\S+',  # URLs / self-promotion links
]
COMPILED_SPAM_PATTERNS = [re.compile(p, re.IGNORECASE) for p in SPAM_PATTERNS]


# ═══════════════════════════════════════════════════════════════════
# Pydantic Models
# ═══════════════════════════════════════════════════════════════════

class CommentsPayload(BaseModel):
    comments: List[str]

class DetailedPayload(BaseModel):
    newestComments: List[str]
    oldestComments: List[str]

class AnalysisResponse(BaseModel):
    positive: float
    negative: float
    neutral: float
    clarity_score: float
    depth_score: float
    engagement_score: float

class DetailedAnalysisResponse(BaseModel):
    # Core scores
    positive: float
    negative: float
    neutral: float
    clarity_score: float
    depth_score: float
    engagement_score: float
    overall_score: float
    # Sentiment distribution (percentages)
    positive_percent: float
    negative_percent: float
    neutral_percent: float
    # Trend analysis
    newest_score: float
    oldest_score: float
    trend_verdict: str        # "Improving", "Stable", or "Declining"
    # Top comments
    most_positive_comment: str
    most_positive_score: float
    most_negative_comment: str
    most_negative_score: float
    # Keywords and summary
    top_keywords: List[str]
    summary: str
    # Meta
    total_comments_analyzed: int
    spam_comments_filtered: int


# ═══════════════════════════════════════════════════════════════════
# Text Preprocessing
# ═══════════════════════════════════════════════════════════════════

def preprocess_comment(text: str) -> str:
    """Clean a raw YouTube comment for analysis."""
    # 1. Strip HTML tags (YouTube comments can contain <br>, <a>, <b>, etc.)
    text = BeautifulSoup(text, "html.parser").get_text(separator=" ")
    
    # 2. Remove URLs
    text = re.sub(r'https?://\S+|www\.\S+', '', text)
    
    # 3. Decode HTML entities
    text = text.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>')
    text = text.replace('&#39;', "'").replace('&quot;', '"')
    
    # 4. Reduce excessive repeated characters (e.g., "sooooo goooood" → "soo good")
    text = re.sub(r'(.)\1{3,}', r'\1\1', text)
    
    # 5. Reduce excessive punctuation (e.g., "!!!!!!" → "!!")
    text = re.sub(r'([!?.]){3,}', r'\1\1', text)
    
    # 6. Remove excessive whitespace
    text = re.sub(r'\s+', ' ', text).strip()
    
    return text


def is_spam(text: str) -> bool:
    """Detect spam/bot comments using pattern matching."""
    # Check against known spam patterns
    for pattern in COMPILED_SPAM_PATTERNS:
        if pattern.search(text):
            return True
    
    # Very short comments that add no value (less than 3 words)
    words = text.split()
    if len(words) < 3:
        return False  # Don't filter very short — they might be valid ("Great video!")
    
    # Detect excessive repetitive characters (e.g., "aaaaaaa bbbbbbb")
    if re.match(r'^(.)\1{10,}$', text.replace(' ', '')):
        return True
    
    return False


def extract_keywords(comments: List[str], top_n: int = 10) -> List[str]:
    """Extract the most frequent meaningful words from comments."""
    all_words = []
    for text in comments:
        # Lowercase, keep only alphabetic words
        words = re.findall(r'[a-zA-Z]{3,}', text.lower())
        # Filter out stopwords and low-value words
        words = [w for w in words if w not in STOP_WORDS and w not in EXTRA_STOP_WORDS]
        all_words.extend(words)
    
    # Count and return top keywords
    counter = Counter(all_words)
    return [word for word, count in counter.most_common(top_n) if count >= 1]


def analyze_batch(comments: List[str]) -> dict:
    """Analyze a batch of comments and return aggregated scores."""
    if not comments:
        return {
            'avg_pos': 0, 'avg_neg': 0, 'avg_neu': 0,
            'compound_normalized': 50,
            'clarity': 50, 'depth': 50, 'engagement': 50,
            'overall': 50,
            'per_comment_scores': []
        }
    
    total = len(comments)
    pos_sum = neg_sum = neu_sum = 0.0
    total_length = 0
    question_count = 0
    per_comment_scores = []
    
    for text in comments:
        scores = analyzer.polarity_scores(text)
        pos_sum += scores['pos']
        neg_sum += scores['neg']
        neu_sum += scores['neu']
        total_length += len(text.split())
        question_count += text.count("?")
        per_comment_scores.append({
            'text': text,
            'compound': scores['compound'],
            'pos': scores['pos'],
            'neg': scores['neg']
        })
    
    avg_pos = (pos_sum / total) * 100
    avg_neg = (neg_sum / total) * 100
    avg_neu = (neu_sum / total) * 100
    
    compound_scores = [s['compound'] for s in per_comment_scores]
    avg_compound = sum(compound_scores) / total
    compound_normalized = (avg_compound + 1) * 50  # Scale -1..1 to 0..100
    
    # Clarity
    clarity = max(0, min(100, 40 + (compound_normalized * 0.6) + (avg_pos * 0.3) - (avg_neg * 0.3)))
    
    # Depth
    avg_length = total_length / total
    depth_bonus = min(25, (avg_length / 8) * 5)
    question_bonus = min(15, question_count * 3)
    depth = max(0, min(100, 35 + depth_bonus + question_bonus + (compound_normalized * 0.4)))
    
    # Engagement
    volume_bonus = min(15, total * 1.5)
    engagement = max(0, min(100, 40 + (compound_normalized * 0.5) + (avg_pos * 0.3) + volume_bonus))
    
    # Overall (same formula as Spring Boot uses)
    overall = (0.15 * avg_pos) + (0.30 * clarity) + (0.25 * depth) + (0.30 * engagement)
    overall = max(0, min(100, overall))
    
    return {
        'avg_pos': round(avg_pos, 2),
        'avg_neg': round(avg_neg, 2),
        'avg_neu': round(avg_neu, 2),
        'compound_normalized': round(compound_normalized, 2),
        'clarity': round(clarity, 2),
        'depth': round(depth, 2),
        'engagement': round(engagement, 2),
        'overall': round(overall, 2),
        'per_comment_scores': per_comment_scores
    }


def generate_summary(results: dict, newest_score: float, oldest_score: float, 
                     trend: str, spam_count: int, total_analyzed: int) -> str:
    """Generate a 3-line template-based summary of the analysis."""
    
    # Line 1: Overall sentiment
    pos_pct = results['positive_pct']
    neg_pct = results['negative_pct']
    
    if pos_pct >= 70:
        line1 = f"This video has overwhelmingly positive viewer reception with {pos_pct:.0f}% positive comments."
    elif pos_pct >= 50:
        line1 = f"This video has a generally positive reception with {pos_pct:.0f}% positive and {neg_pct:.0f}% negative comments."
    elif neg_pct >= 50:
        line1 = f"This video has a predominantly negative reception with {neg_pct:.0f}% negative comments."
    else:
        line1 = f"Viewer opinions on this video are mixed — {pos_pct:.0f}% positive, {neg_pct:.0f}% negative, with the rest neutral."
    
    # Line 2: Strongest aspect
    aspects = {
        'clarity of explanation': results['clarity'],
        'content depth': results['depth'],
        'audience engagement': results['engagement']
    }
    best_aspect = max(aspects, key=aspects.get)
    best_val = aspects[best_aspect]
    line2 = f"Viewers particularly rate the {best_aspect} highly (score: {best_val:.0f}/100)."
    
    # Line 3: Trend
    if trend == "Improving":
        line3 = "The content trend is improving — newer comments are more positive than older ones."
    elif trend == "Declining":
        line3 = "Warning: The sentiment trend is declining — recent comments are less positive than older ones."
    else:
        line3 = "The sentiment trend is stable — viewer reception has remained consistent over time."
    
    # Optional spam note
    if spam_count > 0:
        line3 += f" ({spam_count} spam/bot comments were filtered out.)"
    
    return f"{line1} {line2} {line3}"


# ═══════════════════════════════════════════════════════════════════
# Endpoints
# ═══════════════════════════════════════════════════════════════════

@app.post("/predictSentiment", response_model=AnalysisResponse)
async def predict_sentiment(payload: CommentsPayload):
    """Legacy V1 endpoint — kept for backward compatibility."""
    if not payload.comments:
        raise HTTPException(status_code=400, detail="No comments provided")
    
    cleaned = [preprocess_comment(c) for c in payload.comments]
    cleaned = [c for c in cleaned if c]  # Remove empty strings
    if not cleaned:
        cleaned = payload.comments  # Fallback to originals if all cleaned out
    
    results = analyze_batch(cleaned)
    
    return AnalysisResponse(
        positive=results['avg_pos'],
        negative=results['avg_neg'],
        neutral=results['avg_neu'],
        clarity_score=results['clarity'],
        depth_score=results['depth'],
        engagement_score=results['engagement']
    )


@app.post("/analyzeDetailed", response_model=DetailedAnalysisResponse)
async def analyze_detailed(payload: DetailedPayload):
    """
    V2 detailed endpoint.
    Accepts newest and oldest comments separately for trend analysis.
    Returns full analysis with sentiment distribution, trend, keywords, and summary.
    """
    if not payload.newestComments and not payload.oldestComments:
        raise HTTPException(status_code=400, detail="No comments provided")
    
    # ── Step 1: Preprocess all comments ──
    raw_newest = payload.newestComments or []
    raw_oldest = payload.oldestComments or []
    
    cleaned_newest = [preprocess_comment(c) for c in raw_newest]
    cleaned_oldest = [preprocess_comment(c) for c in raw_oldest]
    
    # ── Step 2: Filter spam ──
    spam_count = 0
    
    filtered_newest = []
    for c in cleaned_newest:
        if c and not is_spam(c):
            filtered_newest.append(c)
        elif c:
            spam_count += 1
    
    filtered_oldest = []
    for c in cleaned_oldest:
        if c and not is_spam(c):
            filtered_oldest.append(c)
        elif c:
            spam_count += 1
    
    # Fallback: if all filtered out, use originals
    if not filtered_newest and raw_newest:
        filtered_newest = [preprocess_comment(c) for c in raw_newest if preprocess_comment(c)]
    if not filtered_oldest and raw_oldest:
        filtered_oldest = [preprocess_comment(c) for c in raw_oldest if preprocess_comment(c)]
    
    # ── Step 3: Analyze both batches ──
    newest_results = analyze_batch(filtered_newest)
    oldest_results = analyze_batch(filtered_oldest)
    
    # Combined analysis for overall scores
    all_comments = filtered_newest + filtered_oldest
    combined_results = analyze_batch(all_comments)
    total_analyzed = len(all_comments)
    
    # ── Step 4: Sentiment distribution ──
    if total_analyzed > 0:
        all_scores = combined_results['per_comment_scores']
        pos_count = sum(1 for s in all_scores if s['compound'] >= 0.05)
        neg_count = sum(1 for s in all_scores if s['compound'] <= -0.05)
        neu_count = total_analyzed - pos_count - neg_count
        pos_pct = round((pos_count / total_analyzed) * 100, 1)
        neg_pct = round((neg_count / total_analyzed) * 100, 1)
        neu_pct = round(100 - pos_pct - neg_pct, 1)
    else:
        pos_pct = neg_pct = neu_pct = 33.3
    
    # ── Step 5: Find most positive & most negative comments ──
    all_per_comment = combined_results['per_comment_scores']
    
    if all_per_comment:
        sorted_by_compound = sorted(all_per_comment, key=lambda x: x['compound'])
        most_negative = sorted_by_compound[0]
        most_positive = sorted_by_compound[-1]
    else:
        most_positive = {'text': 'N/A', 'compound': 0}
        most_negative = {'text': 'N/A', 'compound': 0}
    
    # ── Step 6: Trend analysis ──
    newest_score = newest_results['overall']
    oldest_score = oldest_results['overall']
    score_diff = newest_score - oldest_score
    
    if score_diff > 5:
        trend_verdict = "Improving"
    elif score_diff < -5:
        trend_verdict = "Declining"
    else:
        trend_verdict = "Stable"
    
    # ── Step 7: Extract keywords ──
    keywords = extract_keywords(all_comments, top_n=10)
    
    # ── Step 8: Generate summary ──
    summary_data = {
        'positive_pct': pos_pct,
        'negative_pct': neg_pct,
        'clarity': combined_results['clarity'],
        'depth': combined_results['depth'],
        'engagement': combined_results['engagement'],
    }
    summary = generate_summary(
        summary_data, newest_score, oldest_score, 
        trend_verdict, spam_count, total_analyzed
    )
    
    return DetailedAnalysisResponse(
        # Core scores
        positive=combined_results['avg_pos'],
        negative=combined_results['avg_neg'],
        neutral=combined_results['avg_neu'],
        clarity_score=combined_results['clarity'],
        depth_score=combined_results['depth'],
        engagement_score=combined_results['engagement'],
        overall_score=combined_results['overall'],
        # Distribution
        positive_percent=pos_pct,
        negative_percent=neg_pct,
        neutral_percent=neu_pct,
        # Trend
        newest_score=newest_score,
        oldest_score=oldest_score,
        trend_verdict=trend_verdict,
        # Top comments
        most_positive_comment=most_positive['text'],
        most_positive_score=round((most_positive['compound'] + 1) * 50, 1),
        most_negative_comment=most_negative['text'],
        most_negative_score=round((most_negative['compound'] + 1) * 50, 1),
        # Keywords & summary
        top_keywords=keywords,
        summary=summary,
        # Meta
        total_comments_analyzed=total_analyzed,
        spam_comments_filtered=spam_count
    )


# ═══════════════════════════════════════════════════════════════════
# LEARN MORE — Hardcoded Knowledge Base (No API Needed)
# ═══════════════════════════════════════════════════════════════════

# Pre-defined learning paths for 10 popular topics
COURSE_DATA = {
    "react": {
        "roadmap": [
            "HTML, CSS & Modern JavaScript (ES6+)",
            "React Fundamentals (Components, JSX, Props)",
            "State Management (useState, Context API, Redux)",
            "Routing with React Router",
            "Hooks in depth (useEffect, useMemo, custom hooks)",
            "API Integration & Async Operations",
            "Performance Optimization",
            "Next.js & Server-Side Rendering (Optional)"
        ],
        "resources": [
            {"title": "React Official Docs", "url": "https://react.dev/", "type": "documentation", "description": "The best place to start learning React directly from the creators."},
            {"title": "MDN Web Docs", "url": "https://developer.mozilla.org/en-US/docs/Learn/Tools_and_testing/Client-side_JavaScript_frameworks/React_getting_started", "type": "article", "description": "Mozilla's guide to React fundamentals."},
            {"title": "React GitHub Repository", "url": "https://github.com/facebook/react", "type": "github", "description": "Explore the React source code and contribute."},
            {"title": "FreeCodeCamp React Course", "url": "https://www.freecodecamp.org/learn/front-end-development-libraries/", "type": "tutorial", "description": "Comprehensive free course to practice React concepts."}
        ]
    },
    "javascript": {
        "roadmap": [
            "Basic Syntax, Variables, & Data Types",
            "Control Structures (Loops, If/Else)",
            "Functions, Scope, and Closures",
            "DOM Manipulation & Events",
            "Asynchronous JavaScript (Promises, async/await)",
            "ES6+ Features (Arrow functions, Destructuring)",
            "Object-Oriented JavaScript",
            "Module Bundlers & Build Tools"
        ],
        "resources": [
            {"title": "MDN JavaScript Guide", "url": "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide", "type": "documentation", "description": "The authoritative resource for everything JavaScript."},
            {"title": "JavaScript.info", "url": "https://javascript.info/", "type": "tutorial", "description": "Modern JavaScript Tutorial from basics to advanced."},
            {"title": "Eloquemnt JavaScript", "url": "https://eloquentjavascript.net/", "type": "article", "description": "A modern, free book on JavaScript programming."}
        ]
    },
    "python": {
        "roadmap": [
            "Basic Python Syntax & Variables",
            "Data Structures (Lists, Dictionaries, Sets)",
            "Control Flow & Functions",
            "Object-Oriented Programming (OOP)",
            "Modules & Packages",
            "File Handling & Exceptions",
            "Web Scraping / APIs",
            "Frameworks (Django/Flask or Data Science)"
        ],
        "resources": [
            {"title": "Python Official Docs", "url": "https://docs.python.org/3/", "type": "documentation", "description": "Official Python documentation and language reference."},
            {"title": "Automate the Boring Stuff", "url": "https://automatetheboringstuff.com/", "type": "article", "description": "Practical Python programming for beginners."},
            {"title": "Real Python", "url": "https://realpython.com/", "type": "tutorial", "description": "In-depth articles and tutorials on Python."}
        ]
    },
    "java": {
        "roadmap": [
            "Java Basics & Setup",
            "Object-Oriented Programming Concepts",
            "Data Structures & Collections Framework",
            "Exception Handling",
            "Multithreading & Concurrency",
            "Java Streams & Lamdas",
            "Database Connectivity (JDBC)",
            "Spring Framework / Spring Boot Basics"
        ],
        "resources": [
            {"title": "Oracle Java Tutorials", "url": "https://docs.oracle.com/javase/tutorial/", "type": "documentation", "description": "Official Java tutorials by Oracle."},
            {"title": "Baeldung Java Guides", "url": "https://www.baeldung.com/java-tutorial", "type": "tutorial", "description": "Exceptional, detailed guides on modern Java."},
            {"title": "Java GitHub (OpenJDK)", "url": "https://github.com/openjdk/jdk", "type": "github", "description": "The open-source reference implementation of Java SE."}
        ]
    },
    "spring": {
        "roadmap": [
            "Core Java & OOP principles",
            "Spring Core (Inversion of Control, Dependency Injection)",
            "Spring Boot Basics & Auto-configuration",
            "Spring MVC & Creating REST APIS",
            "Spring Data JPA & Hibernate",
            "Spring Security (Authentication & Authorization)",
            "Testing Spring Applications",
            "Deploying Spring Boot microservices"
        ],
        "resources": [
            {"title": "Spring Boot Reference", "url": "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/", "type": "documentation", "description": "The complete Spring Boot reference guide."},
            {"title": "Spring Guides", "url": "https://spring.io/guides", "type": "tutorial", "description": "Step-by-step guides to build specific Spring applications."},
            {"title": "Baeldung Spring Boot", "url": "https://www.baeldung.com/spring-boot", "type": "article", "description": "Practical tutorials and code examples for Spring Boot."}
        ]
    },
    "node": {
        "roadmap": [
            "Advanced JavaScript Concepts",
            "Node.js Architecture & Event Loop",
            "Core Modules (fs, path, http)",
            "Express.js Framework Basics",
            "Building REST APIs & Middleware",
            "Working with Databases (MongoDB/SQL)",
            "Authentication (JWT, OAuth)",
            "Performance & Deployment"
        ],
        "resources": [
            {"title": "Node.js Official Documentation", "url": "https://nodejs.org/en/docs/", "type": "documentation", "description": "Official API reference and guides."},
            {"title": "Express.js Guide", "url": "https://expressjs.com/", "type": "tutorial", "description": "Fast, unopinionated, minimalist web framework for Node.js."},
            {"title": "Node.js Best Practices", "url": "https://github.com/goldbergyoni/nodebestpractices", "type": "github", "description": "Comprehensive list of Node.js best practices."}
        ]
    },
    "machine": {
        "roadmap": [
            "Python & Math Fundamentals (Linear Algebra, Calculus)",
            "Data Manipulation (Pandas, NumPy)",
            "Data Visualization (Matplotlib, Seaborn)",
            "Supervised Learning Algorithms",
            "Unsupervised Learning Algorithms",
            "Model Evaluation & Tuning",
            "Deep Learning Basics (Neural Networks)",
            "Frameworks (TensorFlow or PyTorch)"
        ],
        "resources": [
            {"title": "Scikit-Learn Docs", "url": "https://scikit-learn.org/stable/", "type": "documentation", "description": "Machine Learning in Python."},
            {"title": "Kaggle Learn", "url": "https://www.kaggle.com/learn", "type": "tutorial", "description": "Interactive micro-courses for ML and data science."},
            {"title": "Google Machine Learning Crash Course", "url": "https://developers.google.com/machine-learning/crash-course", "type": "course", "description": "Fast-paced, practical introduction to machine learning."}
        ]
    },
    "aws": {
        "roadmap": [
            "Cloud Computing Fundamentals",
            "Identity and Access Management (IAM)",
            "Compute Services (EC2, Lambda)",
            "Storage Services (S3, EBS)",
            "Networking (VPC, Route53)",
            "Databases (RDS, DynamoDB)",
            "Security & Compliance",
            "Architecture Best Practices"
        ],
        "resources": [
            {"title": "AWS Documentation", "url": "https://docs.aws.amazon.com/", "type": "documentation", "description": "Official guides and API references for all AWS services."},
            {"title": "AWS Skill Builder", "url": "https://explore.skillbuilder.aws/", "type": "course", "description": "Free digital training provided by AWS."},
            {"title": "AWS Architecture Center", "url": "https://aws.amazon.com/architecture/", "type": "article", "description": "Best practices and reference architectures."}
        ]
    },
    "sql": {
        "roadmap": [
            "Relational Database Basics",
            "Basic Queries (SELECT, WHERE, ORDER BY)",
            "Filtering and Functions",
            "Joins (INNER, LEFT, RIGHT)",
            "Grouping & Aggregation (GROUP BY, HAVING)",
            "Subqueries & CTEs",
            "Database Design & Normalization",
            "Indexes & Performance Tuning"
        ],
        "resources": [
            {"title": "PostgreSQL Documentation", "url": "https://www.postgresql.org/docs/", "type": "documentation", "description": "World's most advanced open source relational database."},
            {"title": "SQL Tutorial (W3Schools)", "url": "https://www.w3schools.com/sql/", "type": "tutorial", "description": "Easy to follow SQL reference and exercises."},
            {"title": "Mode SQL Tutorial", "url": "https://mode.com/sql-tutorial/", "type": "article", "description": "Learn SQL for data analysis."}
        ]
    },
    "c++": {
        "roadmap": [
            "Basic Syntax & Data Types",
            "Control Structures & Functions",
            "Pointers & Memory Management",
            "Object-Oriented Programming (Classes, Inheritance)",
            "Templates & Generic Programming",
            "Standard Template Library (STL)",
            "Advanced C++ (Smart Pointers, Move Semantics)",
            "Concurrency & Multithreading"
        ],
        "resources": [
            {"title": "C++ Reference", "url": "https://en.cppreference.com/w/", "type": "documentation", "description": "Complete online reference for the C and C++ languages."},
            {"title": "LearnCpp.com", "url": "https://www.learncpp.com/", "type": "tutorial", "description": "Free website devoted to teaching you how to program in C++."},
            {"title": "C++ Core Guidelines", "url": "https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines", "type": "github", "description": "Best practices from the creators of C++."}
        ]
    }
}

# General fallback if topic doesn't match the 10 specific ones
GENERAL_ROADMAP = [
    "Understand the core concepts and vocabulary",
    "Set up your development environment",
    "Complete a basic 'Hello World' tutorial",
    "Build a simple, guided project",
    "Read official documentation and best practices",
    "Build an independent project without a guide",
    "Learn advanced topics and edge cases",
    "Deploy or share your project"
]

GENERAL_RESOURCES = [
    {"title": "Official Developer Documentation", "url": "#", "type": "documentation", "description": "Search for the official docs for the most accurate information."},
    {"title": "GitHub Repositories", "url": "https://github.com", "type": "github", "description": "Look for heavily starred open source projects related to the topic."},
    {"title": "Stack Overflow", "url": "https://stackoverflow.com", "type": "article", "description": "Search Q&A for specific technical hurdles."}
]

# Filler words to strip from video titles when extracting topics
TITLE_FILLER_WORDS = {
    'tutorial', 'course', 'full', 'complete', 'crash', 'beginner', 'beginners',
    'advanced', 'intermediate', 'introduction', 'intro', 'guide', 'masterclass',
    'learn', 'learning', 'training', 'lecture', 'class', 'lesson', 'workshop',
    'explained', 'basics', 'fundamentals', 'deep', 'dive', 'overview',
    'tamil', 'hindi', 'english', 'telugu', 'malayalam', 'kannada',
    'free', 'paid', 'premium', 'pro', 'for', 'in', 'with', 'and', 'the',
    'a', 'an', 'to', 'of', 'on', 'at', 'by', 'from', 'up', 'out',
    'hours', 'hour', 'minutes', 'min', 'hrs', 'part', 'ep', 'episode',
    'video', 'videos', 'series', 'playlist', 'chapter', 'chapters',
    '2020', '2021', '2022', '2023', '2024', '2025', '2026',
    'new', 'latest', 'updated', 'edition', 'version',
    'code', 'io', 'dev', 'tech', 'programming', 'vs', 'versus'
}


def extract_topics_from_title(title: str) -> list:
    """Extract core topic keywords from a YouTube video title."""
    # Remove special characters except dots (for names like Node.js)
    cleaned = re.sub(r'[^a-zA-Z0-9.\s]', ' ', title)
    words = cleaned.split()
    
    topics = []
    i = 0
    while i < len(words):
        word = words[i]
        # Skip pure numbers and filler words
        if word.lower() in TITLE_FILLER_WORDS or word.isdigit():
            i += 1
            continue
        # Keep words with dots (Node.js, Express.js, etc.)
        if '.' in word and len(word) > 2:
            topics.append(word)
        elif len(word) >= 2:
            topics.append(word)
        i += 1
    
    # Deduplicate while preserving order
    seen = set()
    unique_topics = []
    for t in topics:
        key = t.lower()
        if key not in seen:
            seen.add(key)
            unique_topics.append(t)
    
    return unique_topics[:5]  # Max 5 topics


class LearnMorePayload(BaseModel):
    title: str
    geminiApiKey: str  # Note: Kept for API compatibility, but we no longer use it here!

class ResourceItem(BaseModel):
    title: str
    url: str
    type: str
    description: str

class LearnMoreAiResponse(BaseModel):
    extractedTopics: List[str]
    roadmapSteps: List[str]
    resources: List[ResourceItem]


@app.post("/learnMore", response_model=LearnMoreAiResponse)
async def learn_more(payload: LearnMorePayload):
    """
    Extract topics from video title, then look up hardcoded roadmap and resources.
    Guaranteed to work instantly without API rate limits!
    """
    if not payload.title:
        raise HTTPException(status_code=400, detail="No title provided")
    
    # Step 1: Extract topics
    topics = extract_topics_from_title(payload.title)
    print(f"[LearnMore] Extracted topics: {topics} from title: '{payload.title}'")
    
    # Step 2: Find best matching course data
    matched_roadmap = GENERAL_ROADMAP
    matched_resources = GENERAL_RESOURCES
    
    # Convert topics to lower case for matching
    lower_topics = [t.lower() for t in topics]
    
    # Check if any identified topic matches our keys
    for t in lower_topics:
        # Check direct match or substring (e.g., 'reactjs' -> 'react')
        for key in COURSE_DATA.keys():
            if key in t:
                print(f"[LearnMore] Direct dictionary match on '{key}'")
                matched_roadmap = COURSE_DATA[key]["roadmap"]
                matched_resources = [ResourceItem(**r) for r in COURSE_DATA[key]["resources"]]
                break
        if matched_roadmap != GENERAL_ROADMAP:
            break
            
    # Fallback checking full title string if token extraction missed it
    if matched_roadmap == GENERAL_ROADMAP:
        title_lower = payload.title.lower()
        for key in COURSE_DATA.keys():
            if key in title_lower:
                print(f"[LearnMore] Loose title match on '{key}'")
                matched_roadmap = COURSE_DATA[key]["roadmap"]
                matched_resources = [ResourceItem(**r) for r in COURSE_DATA[key]["resources"]]
                break

    return LearnMoreAiResponse(
        extractedTopics=topics,
        roadmapSteps=matched_roadmap,
        resources=matched_resources
    )


if __name__ == "__main__":
    import uvicorn
    import os
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)

