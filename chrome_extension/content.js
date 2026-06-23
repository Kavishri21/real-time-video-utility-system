// ═══════════════════════════════════════════════════════════════════
// YouTube Content Quality Analyzer — V2 Chrome Extension
// Injects: sidebar panel (right) + below-video panel (main area)
// ═══════════════════════════════════════════════════════════════════

// --- Environment Configuration ---
const IS_PRODUCTION = true; // Toggle to true when deploying/publishing
const BACKEND_BASE_URL = IS_PRODUCTION 
    ? 'https://youtube-spring-backend.onrender.com' // Deployed Spring Boot URL on Render
    : 'http://localhost:8080';


// ── Utility: extract video ID from URL ──
function getVideoId(url) {
    try {
        const urlParams = new URLSearchParams(new URL(url).search);
        return urlParams.get("v");
    } catch (e) {
        return null;
    }
}

// ── Global state ──
let currentVideoId = null;
let trendChartInstance = null;
let donutChartInstance = null;

// ═══════════════════════════════════════════════════════════════════
// UI INJECTION — TWO PANELS
// ═══════════════════════════════════════════════════════════════════

function injectUI() {
    if (document.getElementById('quality-analyzer-panel')) return;

    // ────────────────────────────────────────
    // PANEL 1: RIGHT SIDEBAR (score, summary, aspects)
    // ────────────────────────────────────────
    const sidebarPanel = document.createElement('div');
    sidebarPanel.id = 'quality-analyzer-panel';
    sidebarPanel.innerHTML = `
        <!-- Header -->
        <div id="qa-header">
            <div id="qa-header-left">
                <span id="qa-header-icon">🛡️</span>
                <span>Content Quality Analyzer</span>
            </div>
            <span id="qa-close-btn">&times;</span>
        </div>

        <!-- Analyze Button -->
        <button id="qa-analyze-btn">🔍 Analyze Video Quality</button>

        <!-- Learn More Button -->
        <button id="qa-learn-btn" style="display:none;">📘 Learn More About This Topic</button>

        <!-- Loading State -->
        <div id="qa-loading" class="qa-loading">
            <div class="qa-loading-spinner"></div>
            <div>Analyzing comments &amp; sentiment...</div>
            <div style="font-size:12px; margin-top:4px; color:#909090;">Fetching from YouTube API → AI Analysis</div>
        </div>

        <!-- ═══ SIDEBAR RESULTS ═══ -->
        <div id="qa-results">

            <!-- 1. SCORE HERO -->
            <div class="qa-score-hero">
                <div class="qa-score-gauge-container">
                    <canvas id="qa-score-gauge" class="qa-score-gauge-canvas" width="140" height="140"></canvas>
                    <div class="qa-score-number" id="qa-score-number">0</div>
                </div>
                <div id="qa-verdict-badge" class="qa-verdict-badge qa-verdict-medium">Waiting</div>
                <div class="qa-trust-row">
                    <div class="qa-trust-item">
                        <span class="qa-trust-icon">📊</span>
                        <span id="qa-comments-count">0 comments analyzed</span>
                    </div>
                    <div class="qa-trust-item">
                        <span class="qa-trust-icon">🤖</span>
                        <span id="qa-spam-count">0 spam filtered</span>
                    </div>
                </div>
            </div>

            <!-- 2. AI SUMMARY -->
            <div class="qa-section">
                <div class="qa-section-title">AI Summary</div>
                <div class="qa-summary-text" id="qa-summary">
                    Analysis pending...
                </div>
            </div>

            <!-- 3. ASPECT METRICS -->
            <div class="qa-section">
                <div class="qa-section-title">Aspect Analysis</div>
                <div class="qa-metric">
                    <span class="qa-metric-label">Clarity</span>
                    <div class="qa-metric-bar-bg"><div id="qa-bar-clarity" class="qa-metric-bar-fill qa-bar-clarity"></div></div>
                    <span id="qa-val-clarity" class="qa-metric-value">0</span>
                </div>
                <div class="qa-metric">
                    <span class="qa-metric-label">Depth</span>
                    <div class="qa-metric-bar-bg"><div id="qa-bar-depth" class="qa-metric-bar-fill qa-bar-depth"></div></div>
                    <span id="qa-val-depth" class="qa-metric-value">0</span>
                </div>
                <div class="qa-metric">
                    <span class="qa-metric-label">Engagement</span>
                    <div class="qa-metric-bar-bg"><div id="qa-bar-engagement" class="qa-metric-bar-fill qa-bar-engagement"></div></div>
                    <span id="qa-val-engagement" class="qa-metric-value">0</span>
                </div>
            </div>
        </div>
    `;

    // ────────────────────────────────────────
    // PANEL 2: BELOW VIDEO (charts, comments, keywords)
    // ────────────────────────────────────────
    const belowPanel = document.createElement('div');
    belowPanel.id = 'qa-below-panel';
    belowPanel.innerHTML = `
        <!-- ═══ BELOW-VIDEO RESULTS (hidden until analysis) ═══ -->
        <div id="qa-below-results" style="display:none;">

            <!-- 4. CHARTS: Trend + Sentiment Donut -->
            <div class="qa-section qa-section-no-border">
                <div class="qa-section-title">Analytics</div>
                <div class="qa-charts-row">
                    <!-- Trend Chart -->
                    <div class="qa-chart-card">
                        <div class="qa-chart-label">Sentiment Trend</div>
                        <canvas id="qa-trend-chart" class="qa-chart-canvas"></canvas>
                        <div id="qa-trend-indicator" class="qa-trend-indicator qa-trend-stable">● Stable</div>
                    </div>
                    <!-- Donut Chart -->
                    <div class="qa-chart-card">
                        <div class="qa-chart-label">Sentiment Split</div>
                        <canvas id="qa-donut-chart" class="qa-chart-canvas"></canvas>
                    </div>
                </div>
            </div>

            <!-- 5. TOP COMMENTS -->
            <div class="qa-section">
                <div class="qa-section-title">Highlighted Comments</div>
                <div class="qa-comments-grid">
                    <div class="qa-comment-card qa-comment-positive">
                        <div class="qa-comment-label">👍 Most Positive</div>
                        <div class="qa-comment-text" id="qa-top-positive">—</div>
                    </div>
                    <div class="qa-comment-card qa-comment-negative">
                        <div class="qa-comment-label">👎 Most Critical</div>
                        <div class="qa-comment-text" id="qa-top-negative">—</div>
                    </div>
                </div>
            </div>

            <!-- 6. KEYWORDS -->
            <div class="qa-section">
                <div class="qa-section-title">Top Keywords</div>
                <div class="qa-keywords-container" id="qa-keywords"></div>
            </div>

            <!-- 7. META FOOTER -->
            <div class="qa-meta-footer">
                <span id="qa-meta-analyzed">Analyzed: 0 comments</span>
                <span>Powered by VADER AI</span>
            </div>
        </div>
    `;

    // ── Inject sidebar panel into YouTube's right column ──
    const sidebarTarget = document.querySelector('#secondary-inner')
                       || document.querySelector('#secondary')
                       || document.querySelector('#related');
    if (sidebarTarget) {
        sidebarTarget.insertBefore(sidebarPanel, sidebarTarget.firstChild);
    } else {
        document.body.appendChild(sidebarPanel);
    }

    // ── Inject below-video panel into YouTube's primary area ──
    // Target: between video info and comments
    const belowTarget = document.querySelector('#below')               // container for desc + comments
                     || document.querySelector('#primary-inner')
                     || document.querySelector('#primary');
    
    if (belowTarget) {
        // Try to insert before the comments section
        const commentsSection = belowTarget.querySelector('#comments')
                             || belowTarget.querySelector('ytd-comments');
        if (commentsSection && commentsSection.parentNode) {
            commentsSection.parentNode.insertBefore(belowPanel, commentsSection);
        } else {
            belowTarget.appendChild(belowPanel);
        }
    } else {
        document.body.appendChild(belowPanel);
    }

    // ── Event listeners ──
    document.getElementById('qa-close-btn').addEventListener('click', () => {
        sidebarPanel.style.display = 'none';
        belowPanel.style.display = 'none';
    });

    document.getElementById('qa-analyze-btn').addEventListener('click', handleAnalyzeClick);

    document.getElementById('qa-learn-btn').addEventListener('click', () => {
        // Get video title from YouTube's DOM
        const titleEl = document.querySelector('yt-formatted-string.style-scope.ytd-watch-metadata')
                     || document.querySelector('h1.ytd-watch-metadata yt-formatted-string')
                     || document.querySelector('#title h1 yt-formatted-string')
                     || document.querySelector('h1.title');
        const videoTitle = titleEl ? titleEl.textContent.trim() : document.title.replace(' - YouTube', '').trim();
        const encodedTitle = encodeURIComponent(videoTitle);
        const encodedVideoId = encodeURIComponent(currentVideoId || '');
        
        // Open learn.html in a new tab
        const learnUrl = chrome.runtime.getURL(`learn.html?title=${encodedTitle}&videoId=${encodedVideoId}`);
        window.open(learnUrl, '_blank');
    });
}


// ═══════════════════════════════════════════════════════════════════
// GAUGE DRAWING (Canvas-based circular score gauge)
// ═══════════════════════════════════════════════════════════════════

function drawScoreGauge(score) {
    const canvas = document.getElementById('qa-score-gauge');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const size = 140;
    const center = size / 2;
    const radius = 55;
    const lineWidth = 10;

    ctx.clearRect(0, 0, size, size);

    // Background arc
    const isDark = document.documentElement.hasAttribute('dark');
    ctx.beginPath();
    ctx.arc(center, center, radius, 0, 2 * Math.PI);
    ctx.strokeStyle = isDark ? '#3a3a3a' : '#e5e5e5';
    ctx.lineWidth = lineWidth;
    ctx.lineCap = 'round';
    ctx.stroke();

    // Score arc
    const scorePercent = Math.max(0, Math.min(100, score)) / 100;
    const startAngle = -Math.PI / 2;
    const endAngle = startAngle + (2 * Math.PI * scorePercent);

    // Color based on score
    let color;
    if (score >= 75) color = '#137333';
    else if (score >= 50) color = '#f9a825';
    else if (score >= 30) color = '#e65100';
    else color = '#c5221f';

    ctx.beginPath();
    ctx.arc(center, center, radius, startAngle, endAngle);
    ctx.strokeStyle = color;
    ctx.lineWidth = lineWidth;
    ctx.lineCap = 'round';
    ctx.stroke();

    // Update the number
    const numEl = document.getElementById('qa-score-number');
    if (numEl) numEl.textContent = Math.round(score);
}


// ═══════════════════════════════════════════════════════════════════
// CHART.JS RENDERING
// ═══════════════════════════════════════════════════════════════════

function renderTrendChart(newestScore, oldestScore, trendVerdict) {
    const canvas = document.getElementById('qa-trend-chart');
    if (!canvas) return;

    if (trendChartInstance) {
        trendChartInstance.destroy();
        trendChartInstance = null;
    }

    const isDark = document.documentElement.hasAttribute('dark');
    const textColor = isDark ? '#aaa' : '#606060';
    const gridColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';

    trendChartInstance = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: ['Older', 'Newer'],
            datasets: [{
                label: 'Quality Score',
                data: [Math.round(oldestScore), Math.round(newestScore)],
                backgroundColor: [
                    isDark ? 'rgba(138, 180, 248, 0.5)' : 'rgba(6, 95, 212, 0.5)',
                    isDark ? 'rgba(129, 201, 149, 0.5)' : 'rgba(19, 115, 51, 0.5)'
                ],
                borderColor: [
                    isDark ? '#8ab4f8' : '#065fd4',
                    isDark ? '#81c995' : '#137333'
                ],
                borderWidth: 2,
                borderRadius: 6,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: isDark ? '#333' : '#fff',
                    titleColor: isDark ? '#f1f1f1' : '#0f0f0f',
                    bodyColor: isDark ? '#ddd' : '#333',
                    borderColor: isDark ? '#555' : '#e5e5e5',
                    borderWidth: 1,
                    cornerRadius: 8,
                    padding: 10,
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: { color: textColor, font: { size: 10 } },
                    grid: { color: gridColor }
                },
                x: {
                    ticks: { color: textColor, font: { size: 11 } },
                    grid: { display: false }
                }
            }
        }
    });

    // Update trend indicator
    const indicator = document.getElementById('qa-trend-indicator');
    if (indicator) {
        indicator.className = 'qa-trend-indicator';
        if (trendVerdict === 'Improving') {
            indicator.textContent = '📈 Improving';
            indicator.classList.add('qa-trend-improving');
        } else if (trendVerdict === 'Declining') {
            indicator.textContent = '📉 Declining';
            indicator.classList.add('qa-trend-declining');
        } else {
            indicator.textContent = '➡️ Stable';
            indicator.classList.add('qa-trend-stable');
        }
    }
}

function renderDonutChart(posPercent, negPercent, neuPercent) {
    const canvas = document.getElementById('qa-donut-chart');
    if (!canvas) return;

    if (donutChartInstance) {
        donutChartInstance.destroy();
        donutChartInstance = null;
    }

    const isDark = document.documentElement.hasAttribute('dark');

    donutChartInstance = new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Positive', 'Negative', 'Neutral'],
            datasets: [{
                data: [posPercent, negPercent, neuPercent],
                backgroundColor: [
                    isDark ? '#81c995' : '#34a853',
                    isDark ? '#f28b82' : '#ea4335',
                    isDark ? '#8ab4f8' : '#4285f4'
                ],
                borderColor: isDark ? '#212121' : '#ffffff',
                borderWidth: 3,
                hoverOffset: 4,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '55%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: isDark ? '#aaa' : '#606060',
                        font: { size: 10 },
                        boxWidth: 10,
                        padding: 8,
                        usePointStyle: true,
                        pointStyle: 'circle',
                    }
                },
                tooltip: {
                    backgroundColor: isDark ? '#333' : '#fff',
                    titleColor: isDark ? '#f1f1f1' : '#0f0f0f',
                    bodyColor: isDark ? '#ddd' : '#333',
                    borderColor: isDark ? '#555' : '#e5e5e5',
                    borderWidth: 1,
                    cornerRadius: 8,
                    callbacks: {
                        label: function(context) {
                            return ` ${context.label}: ${context.parsed}%`;
                        }
                    }
                }
            }
        }
    });
}


// ═══════════════════════════════════════════════════════════════════
// MAIN ANALYSIS HANDLER
// ═══════════════════════════════════════════════════════════════════

async function handleAnalyzeClick() {
    const btn = document.getElementById('qa-analyze-btn');
    const loading = document.getElementById('qa-loading');
    const sidebarResults = document.getElementById('qa-results');
    const belowResults = document.getElementById('qa-below-results');

    if (!currentVideoId) {
        alert("Could not detect YouTube Video ID.");
        return;
    }

    // UI state: loading
    btn.disabled = true;
    btn.textContent = '⏳ Analyzing...';
    loading.style.display = 'block';
    sidebarResults.style.display = 'none';
    if (belowResults) belowResults.style.display = 'none';

    try {
        console.log("[QA Extension] Analyzing video ID:", currentVideoId);
        const response = await fetch(`${BACKEND_BASE_URL}/analyzeVideo`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ videoId: currentVideoId })
        });

        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }

        const data = await response.json();
        console.log("[QA Extension] Response data:", data);

        // ── SIDEBAR: Score Gauge ──
        drawScoreGauge(data.qualityScore);

        // ── SIDEBAR: Verdict Badge ──
        const badge = document.getElementById('qa-verdict-badge');
        badge.textContent = data.verdict;
        badge.className = 'qa-verdict-badge';
        if (data.qualityScore >= 80) badge.classList.add('qa-verdict-high');
        else if (data.qualityScore >= 50) badge.classList.add('qa-verdict-medium');
        else badge.classList.add('qa-verdict-low');

        // ── SIDEBAR: Trust Row ──
        document.getElementById('qa-comments-count').textContent =
            `${data.totalCommentsAnalyzed || 0} comments analyzed`;
        document.getElementById('qa-spam-count').textContent =
            `${data.spamCommentsFiltered || 0} spam filtered`;

        // ── SIDEBAR: AI Summary ──
        document.getElementById('qa-summary').textContent =
            data.summary || 'Analysis complete.';

        // ── SIDEBAR: Aspect Bars ──
        setTimeout(() => {
            document.getElementById('qa-val-clarity').textContent = Math.round(data.clarity);
            document.getElementById('qa-bar-clarity').style.width = `${data.clarity}%`;
            document.getElementById('qa-val-depth').textContent = Math.round(data.depth);
            document.getElementById('qa-bar-depth').style.width = `${data.depth}%`;
            document.getElementById('qa-val-engagement').textContent = Math.round(data.engagement);
            document.getElementById('qa-bar-engagement').style.width = `${data.engagement}%`;
        }, 100);

        // ── BELOW VIDEO: Charts ──
        setTimeout(() => {
            renderTrendChart(
                data.newestScore || data.qualityScore,
                data.oldestScore || data.qualityScore * 0.9,
                data.trendVerdict || 'Stable'
            );
            renderDonutChart(
                data.positivePercent || 50,
                data.negativePercent || 20,
                data.neutralPercent || 30
            );
        }, 200);

        // ── BELOW VIDEO: Top Comments ──
        document.getElementById('qa-top-positive').textContent = data.mostPositiveComment
            ? `"${data.mostPositiveComment}"` : '— No strongly positive comment found';
        document.getElementById('qa-top-negative').textContent = data.mostNegativeComment
            ? `"${data.mostNegativeComment}"` : '— No strongly negative comment found';

        // ── BELOW VIDEO: Keywords ──
        const keywordsContainer = document.getElementById('qa-keywords');
        keywordsContainer.innerHTML = '';
        if (data.topKeywords && data.topKeywords.length > 0) {
            data.topKeywords.forEach(keyword => {
                const tag = document.createElement('span');
                tag.className = 'qa-keyword-tag';
                tag.textContent = keyword;
                keywordsContainer.appendChild(tag);
            });
        } else {
            keywordsContainer.innerHTML = '<span style="color:#909090; font-size:12px;">No keywords extracted</span>';
        }

        // ── BELOW VIDEO: Meta Footer ──
        document.getElementById('qa-meta-analyzed').textContent =
            `Analyzed: ${data.totalCommentsAnalyzed || 0} comments`;

        // ── Show results in BOTH panels ──
        loading.style.display = 'none';
        sidebarResults.style.display = 'flex';
        if (belowResults) belowResults.style.display = 'block';
        btn.disabled = false;
        btn.textContent = '🔄 Re-Analyze Video';

        // ── Show Learn More button ──
        const learnBtn = document.getElementById('qa-learn-btn');
        if (learnBtn) learnBtn.style.display = 'block';

    } catch (error) {
        console.error("[QA Extension] Analysis Error:", error);
        loading.style.display = 'none';
        btn.disabled = false;
        btn.textContent = '🔍 Analyze Video Quality';
        alert("Failed to analyze video. Ensure Spring Boot and Python servers are running.\n\nError: " + error.message);
    }
}


// ═══════════════════════════════════════════════════════════════════
// YOUTUBE SPA NAVIGATION DETECTION
// ═══════════════════════════════════════════════════════════════════

document.addEventListener("yt-navigate-finish", function () {
    checkAndInit();
});

let lastUrl = location.href;
new MutationObserver(() => {
    const url = location.href;
    if (url !== lastUrl) {
        lastUrl = url;
        setTimeout(checkAndInit, 500);
    }
}).observe(document.body, { subtree: true, childList: true });

function checkAndInit() {
    currentVideoId = getVideoId(window.location.href);
    if (currentVideoId) {
        const existingPanel = document.getElementById('quality-analyzer-panel');
        if (existingPanel) {
            // Already exists — reset for new video
            existingPanel.style.display = 'flex';
            const belowPanel = document.getElementById('qa-below-panel');
            if (belowPanel) belowPanel.style.display = 'block';

            const btn = document.getElementById('qa-analyze-btn');
            if (btn) { btn.textContent = '🔍 Analyze Video Quality'; btn.disabled = false; }
            const results = document.getElementById('qa-results');
            if (results) results.style.display = 'none';
            const belowResults = document.getElementById('qa-below-results');
            if (belowResults) belowResults.style.display = 'none';
            const loading = document.getElementById('qa-loading');
            if (loading) loading.style.display = 'none';
        } else {
            waitForElement('#secondary-inner, #secondary, #related', () => {
                injectUI();
            });
        }
    } else {
        const panel = document.getElementById('quality-analyzer-panel');
        if (panel) panel.style.display = 'none';
        const belowPanel = document.getElementById('qa-below-panel');
        if (belowPanel) belowPanel.style.display = 'none';
    }
}

function waitForElement(selector, callback, maxAttempts = 20) {
    let attempts = 0;
    const interval = setInterval(() => {
        const el = document.querySelector(selector);
        attempts++;
        if (el) {
            clearInterval(interval);
            callback(el);
        } else if (attempts >= maxAttempts) {
            clearInterval(interval);
            injectUI();
        }
    }, 300);
}

// ── Initial Call ──
checkAndInit();
