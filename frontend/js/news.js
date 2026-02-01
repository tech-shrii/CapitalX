document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadNews();
    setupLogout();
});

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.clear();
            window.location.href = '../auth/login.html';
        });
    }
}

async function loadNews() {
    try {
        const news = await apiCall('/news');
        displayNews(news);
    } catch (error) {
        console.error('Error loading news:', error);
        document.getElementById('newsContainer').innerHTML = 
            '<p>Error loading news. Please try again later.</p>';
    }
}

function displayNews(newsItems) {
    const container = document.getElementById('newsContainer');
    if (!container) return;

    if (newsItems.length === 0) {
        container.innerHTML = '<p>No news available at the moment.</p>';
        return;
    }

    container.innerHTML = newsItems.map(item => `
        <div class="news-item">
            <h3>${item.title}</h3>
            <p>${item.description || ''}</p>
            <p><small>Source: ${item.source} • ${formatDate(item.publishedAt)}</small></p>
            ${item.url ? `<a href="${item.url}" target="_blank">Read more →</a>` : ''}
        </div>
    `).join('');
}

function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
    });
}

window.loadNews = loadNews;
