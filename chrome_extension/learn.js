// ═══════════════════════════════════════════════════════════
// Learn More Page — JavaScript Logic
// Reads title & videoId from URL params, calls Spring Boot,
// and renders all 4 sections.
// ═══════════════════════════════════════════════════════════

// --- Environment Configuration ---
const IS_PRODUCTION = false; // Toggle to true when deploying/publishing
const BACKEND_BASE_URL = IS_PRODUCTION 
    ? 'https://your-spring-backend-domain.onrender.com' // Replace with your Render Spring Boot URL
    : 'http://localhost:8080';

document.addEventListener('DOMContentLoaded', async () => {
    // Read URL params
    const params = new URLSearchParams(window.location.search);
    const title = params.get('title') || 'Unknown Video';
    const videoId = params.get('videoId') || '';

    // Set header
    document.getElementById('learn-title').textContent = 'Learn More';
    document.getElementById('learn-subtitle').textContent = title;
    document.title = `Learn More: ${title}`;

    try {
        // Call Spring Boot /learnMore endpoint
        const response = await fetch(`${BACKEND_BASE_URL}/learnMore`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title, videoId })
        });

        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }

        const data = await response.json();
        console.log('[Learn More] Response:', data);

        // Hide loading, show content
        document.getElementById('learn-loading').style.display = 'none';
        document.getElementById('learn-content').style.display = 'block';

        // Render all sections
        renderOverview(data);
        renderRelatedVideos(data.relatedVideos);
        renderRoadmap(data.roadmapSteps);
        renderResources(data.officialResources);

    } catch (error) {
        console.error('[Learn More] Error:', error);
        document.getElementById('learn-loading').innerHTML = `
            <div style="color: #f28b82; font-size: 16px; margin-bottom: 8px;">⚠️ Failed to load</div>
            <p>Ensure Spring Boot and Python servers are running.</p>
            <p style="font-size: 12px; color: #666; margin-top: 8px;">${error.message}</p>
        `;
    }
});


// ═══════════════════════════════════════════
// Section 1: Topic Overview (Wikipedia)
// ═══════════════════════════════════════════
function renderOverview(data) {
    const titleEl = document.getElementById('topic-title');
    const summaryEl = document.getElementById('topic-summary');
    const imageEl = document.getElementById('topic-image');
    const wikiLink = document.getElementById('wiki-link');

    titleEl.textContent = data.topicTitle || 'Topic';
    summaryEl.textContent = data.topicSummary || 'No overview available.';

    if (data.topicImageUrl) {
        imageEl.src = data.topicImageUrl;
        imageEl.alt = data.topicTitle;
        imageEl.style.display = 'block';
    }

    if (data.wikipediaUrl) {
        wikiLink.href = data.wikipediaUrl;
        wikiLink.style.display = 'inline-block';
    }
}


// ═══════════════════════════════════════════
// Section 2: Related Videos
// ═══════════════════════════════════════════
function renderRelatedVideos(videos) {
    const grid = document.getElementById('videos-grid');
    grid.innerHTML = '';

    if (!videos || videos.length === 0) {
        grid.innerHTML = '<div class="no-videos">No related videos found</div>';
        return;
    }

    videos.forEach(video => {
        let badgeClass, badgeText;
        if (video.qualityScore >= 70) {
            badgeClass = 'badge-high';
            badgeText = `🟢 ${Math.round(video.qualityScore)} — ${video.verdict}`;
        } else if (video.qualityScore >= 50) {
            badgeClass = 'badge-medium';
            badgeText = `🟡 ${Math.round(video.qualityScore)} — ${video.verdict}`;
        } else {
            badgeClass = 'badge-low';
            badgeText = `🔴 ${Math.round(video.qualityScore)} — ${video.verdict}`;
        }

        const card = document.createElement('a');
        card.className = 'video-card';
        card.href = `https://www.youtube.com/watch?v=${video.videoId}`;
        card.target = '_blank';
        card.innerHTML = `
            <img class="video-thumbnail" 
                 src="${video.thumbnailUrl || ''}" 
                 alt="${escapeHtml(video.title)}"
                 onerror="this.style.display='none'">
            <div class="video-info">
                <h4>${escapeHtml(video.title)}</h4>
                <div class="video-channel">${escapeHtml(video.channelName || '')}</div>
                <span class="video-score-badge ${badgeClass}">${badgeText}</span>
            </div>
        `;
        grid.appendChild(card);
    });
}


// ═══════════════════════════════════════════
// Section 3: Learning Roadmap (Gemini AI)
// ═══════════════════════════════════════════
function renderRoadmap(steps) {
    const container = document.getElementById('roadmap-container');
    container.innerHTML = '';

    if (!steps || steps.length === 0) {
        container.innerHTML = '<div class="empty-state">Roadmap generation in progress...</div>';
        return;
    }

    steps.forEach((step, index) => {
        const stepEl = document.createElement('div');
        stepEl.className = 'roadmap-step';

        stepEl.innerHTML = `
            <div class="roadmap-node">
                <div class="roadmap-number">${index + 1}</div>
                <div class="roadmap-text">${escapeHtml(step)}</div>
            </div>
            ${index < steps.length - 1 ? '<div class="roadmap-arrow">→</div>' : ''}
        `;

        container.appendChild(stepEl);
    });
}


// ═══════════════════════════════════════════
// Section 4: Official Resources (Gemini AI)
// ═══════════════════════════════════════════
function renderResources(resources) {
    const list = document.getElementById('resources-list');
    list.innerHTML = '';

    if (!resources || resources.length === 0) {
        list.innerHTML = '<div class="empty-state">No resources found</div>';
        return;
    }

    const typeIcons = {
        'documentation': '📄',
        'github': '💻',
        'tutorial': '🎓',
        'article': '📰',
        'course': '🎥',
    };

    resources.forEach(resource => {
        const icon = typeIcons[resource.type] || '📄';

        const card = document.createElement('a');
        card.className = 'resource-card';
        card.href = resource.url;
        card.target = '_blank';
        card.innerHTML = `
            <span class="resource-icon">${icon}</span>
            <div class="resource-info">
                <h4>${escapeHtml(resource.title)}</h4>
                <p>${escapeHtml(resource.description || '')}</p>
            </div>
            <span class="resource-type-badge">${escapeHtml(resource.type || 'link')}</span>
            <span class="resource-arrow">→</span>
        `;
        list.appendChild(card);
    });
}


// ── Utility: safely decode HTML entities (like &#39;) and re-escape to prevent XSS ──
function escapeHtml(text) {
    if (!text) return '';
    // 1. Safely decode entities like &#39; using DOMParser without executing scripts
    const doc = new DOMParser().parseFromString(text, 'text/html');
    const decodedText = doc.documentElement.textContent || "";
    
    // 2. Re-encode safely
    const div = document.createElement('div');
    div.textContent = decodedText;
    return div.innerHTML;
}
