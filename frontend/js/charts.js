// Portfolio data structure: {symbol: quantity}
let portfolio = {};
let portfolioChart = null;
let assetCharts = {};
const UPDATE_INTERVAL = 10000; // 10 seconds
let updateIntervalId = null;

document.addEventListener('DOMContentLoaded', async () => {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }

    // Get portfolio from localStorage or load from API
    await loadPortfolio();
    
    // Initial data fetch
    await updateAllData();
    
    // Set up 10-second auto-refresh
    startAutoRefresh();
    
    // Period selector for portfolio chart
    document.getElementById('portfolioPeriod').addEventListener('change', async (e) => {
        await updatePortfolioChart(e.target.value);
    });
});

async function loadPortfolio() {
    try {
        // Get user's assets from API
        const assets = await apiCall('/assets');
        portfolio = {};
        
        // Build portfolio map: symbol -> quantity
        if (Array.isArray(assets)) {
            assets.forEach(asset => {
                if (asset.symbol && asset.quantity) {
                    portfolio[asset.symbol] = asset.quantity;
                }
            });
        }

        if (Object.keys(portfolio).length === 0) {
            document.getElementById('assetChartsContainer').innerHTML = 
                '<p class="info-text">No assets in portfolio. Add assets to see charts.</p>';
        }
    } catch (error) {
        console.error('Error loading portfolio:', error);
        showError('Failed to load portfolio');
    }
}

async function updateAllData() {
    if (Object.keys(portfolio).length === 0) return;

    try {
        // Update portfolio value and chart
        await updatePortfolioValue();
        await updatePortfolioChart();
        
        // Update individual asset prices and charts
        await updateAssetPrices();
    } catch (error) {
        console.error('Error updating data:', error);
    }
}

async function updatePortfolioValue() {
    try {
        const data = await apiCall('/pricing/portfolio/value', 'POST', portfolio);
        
        // Update display
        document.getElementById('totalValue').textContent = 
            '$' + (data.totalValue || 0).toFixed(2);
        document.getElementById('updateTime').textContent = 
            new Date().toLocaleTimeString();
        document.getElementById('lastUpdate').textContent = 
            'Last update: ' + new Date().toLocaleTimeString();
        
        // Update price table
        updatePriceTable(data.breakdown);
    } catch (error) {
        console.error('Error updating portfolio value:', error);
    }
}

async function updatePortfolioChart(period = '1mo') {
    if (Object.keys(portfolio).length === 0) return;

    try {
        const data = await apiCall(`/pricing/portfolio/chart?period=${period}&interval=1d`, 'POST', portfolio);
        
        // Prepare chart data
        const labels = data.data.map(point => new Date(point.time).toLocaleDateString());
        const values = data.data.map(point => point.value);

        // Destroy old chart if exists
        if (portfolioChart) {
            portfolioChart.destroy();
        }

        // Create new chart
        const ctx = document.getElementById('portfolioChart').getContext('2d');
        portfolioChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Portfolio Value',
                    data: values,
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.1,
                    pointRadius: 3,
                    pointBackgroundColor: 'rgb(75, 192, 192)',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    },
                    title: {
                        display: true,
                        text: `Portfolio Value History - ${period.toUpperCase()}`
                    }
                },
                scales: {
                    y: {
                        beginAtZero: false,
                        title: {
                            display: true,
                            text: 'Value ($)'
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error('Error updating portfolio chart:', error);
        document.getElementById('portfolioChartMessage').textContent = 
            'Error loading portfolio chart. Please try again.';
    }
}

async function updateAssetPrices() {
    const symbols = Object.keys(portfolio);
    if (symbols.length === 0) return;

    try {
        const data = await apiCall('/pricing/prices', 'POST', symbols);
        
        // Create/update individual asset charts
        if (data.data) {
            for (const [symbol, priceData] of Object.entries(data.data)) {
                if (priceData.price) {
                    await updateAssetChart(symbol, '1mo');
                }
            }
        }
    } catch (error) {
        console.error('Error updating asset prices:', error);
    }
}

async function updateAssetChart(symbol, period = '1mo') {
    try {
        const data = await apiCall(`/pricing/chart/${symbol}?period=${period}&interval=1d`);
        
        // Prepare chart data
        const labels = data.data.map(point => new Date(point.time).toLocaleDateString());
        const closes = data.data.map(point => point.close);

        // Create container if not exists
        let container = document.getElementById(`chart-${symbol}`);
        if (!container) {
            const html = `
                <div class="asset-chart-container">
                    <h3>${symbol}</h3>
                    <canvas id="chart-${symbol}" style="max-height: 300px;"></canvas>
                </div>
            `;
            document.getElementById('assetChartsContainer').insertAdjacentHTML('beforeend', html);
            container = document.getElementById(`chart-${symbol}`);
        }

        // Destroy old chart if exists
        if (assetCharts[symbol]) {
            assetCharts[symbol].destroy();
        }

        // Create new chart
        const ctx = container.getContext('2d');
        assetCharts[symbol] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: `${symbol} Price`,
                    data: closes,
                    borderColor: getColorForSymbol(symbol),
                    backgroundColor: getColorForSymbol(symbol, 0.1),
                    borderWidth: 2,
                    fill: true,
                    tension: 0.1,
                    pointRadius: 2
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: false,
                        title: {
                            display: true,
                            text: 'Price ($)'
                        }
                    }
                }
            }
        });
    } catch (error) {
        console.error(`Error creating chart for ${symbol}:`, error);
    }
}

function updatePriceTable(breakdown) {
    const tbody = document.getElementById('priceTableBody');
    if (!breakdown || Object.keys(breakdown).length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="center">No asset data available</td></tr>';
        return;
    }

    let html = '';
    for (const [symbol, data] of Object.entries(breakdown)) {
        if (data.error) {
            html += `
                <tr>
                    <td>${symbol}</td>
                    <td colspan="4" class="center error">${data.error}</td>
                </tr>
            `;
        } else {
            html += `
                <tr>
                    <td><strong>${symbol}</strong></td>
                    <td>$${(data.price || 0).toFixed(2)}</td>
                    <td>${data.quantity}</td>
                    <td>$${(data.value || 0).toFixed(2)}</td>
                    <td>${new Date().toLocaleTimeString()}</td>
                </tr>
            `;
        }
    }
    tbody.innerHTML = html;
}

function getColorForSymbol(symbol, alpha = 1) {
    const colors = [
        `rgba(255, 99, 132, ${alpha})`,    // Red
        `rgba(54, 162, 235, ${alpha})`,    // Blue
        `rgba(75, 192, 192, ${alpha})`,    // Teal
        `rgba(255, 206, 86, ${alpha})`,    // Yellow
        `rgba(153, 102, 255, ${alpha})`,   // Purple
        `rgba(255, 159, 64, ${alpha})`     // Orange
    ];
    let hash = 0;
    for (let i = 0; i < symbol.length; i++) {
        hash = symbol.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
}

function startAutoRefresh() {
    // Update every 10 seconds
    updateIntervalId = setInterval(() => {
        console.log('Auto-refreshing charts...');
        updateAllData();
    }, UPDATE_INTERVAL);
}

function handleLogout() {
    localStorage.clear();
    clearInterval(updateIntervalId);
    window.location.href = '../auth/login.html';
}

function showError(message) {
    const errorDiv = document.getElementById('errorMessage');
    errorDiv.textContent = message;
    errorDiv.classList.remove('hidden');
    setTimeout(() => {
        errorDiv.classList.add('hidden');
    }, 5000);
}
