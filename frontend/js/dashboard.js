let currentClientId = null;

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadDashboard();
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

    const userName = document.getElementById('userName');
    if (userName) {
        userName.textContent = localStorage.getItem('userName') || 'User';
    }
}

async function loadDashboard() {
    try {
        const data = await apiCall('/dashboard/summary');
        
        document.getElementById('totalClients').textContent = data.totalClients || 0;
        document.getElementById('totalAssets').textContent = data.totalAssets || 0;
        document.getElementById('totalInvested').textContent = formatCurrency(data.totalInvested || 0);
        document.getElementById('currentValue').textContent = formatCurrency(data.totalCurrentValue || 0);
        
        const pnl = data.totalProfitLoss || 0;
        const pnlPercent = data.totalProfitLossPercent || 0;
        const pnlElement = document.getElementById('totalPnL');
        const pnlPercentElement = document.getElementById('totalPnLPercent');
        
        pnlElement.textContent = formatCurrency(pnl);
        pnlElement.className = 'summary-value ' + (pnl >= 0 ? 'positive' : 'negative');
        pnlPercentElement.textContent = formatPercent(pnlPercent);
        pnlPercentElement.className = 'summary-percent ' + (pnl >= 0 ? 'positive' : 'negative');

        if (data.recentClients) {
            displayRecentClients(data.recentClients);
        }
    } catch (error) {
        console.error('Error loading dashboard:', error);
    }
}

function displayRecentClients(clients) {
    const container = document.getElementById('recentClients');
    if (!container) return;

    if (clients.length === 0) {
        container.innerHTML = '<p>No clients yet</p>';
        return;
    }

    container.innerHTML = clients.map(client => `
        <div class="client-item">
            <div>
                <h3>${client.name}</h3>
                <p>${client.email} • ${client.assetCount} assets</p>
            </div>
            <div>
                <strong class="${client.profitLoss >= 0 ? 'positive' : 'negative'}">
                    ${formatCurrency(client.profitLoss)}
                </strong>
            </div>
        </div>
    `).join('');
}

function formatCurrency(amount) {
    return '₹' + parseFloat(amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatPercent(percent) {
    const sign = percent >= 0 ? '+' : '';
    return sign + parseFloat(percent).toFixed(2) + '%';
}
