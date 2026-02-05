(function () {
    const urlParams = new URLSearchParams(window.location.search);
    const clientId = urlParams.get('clientId');

    let client = null;
    let assets = [];
    let portfolio = {}; // { symbol: quantity }
    let currentPeriod = '1mo';
    let combinedChart = null;
    let singleAssetChart = null;
    let singleAssetChartSymbol = null;

    const PERIOD_TO_INTERVAL = {
        '1d': '5m',
        '5d': '1d',
        '1mo': '1d',
        '6mo': '1d',
        '1y': '1d',
        '5y': '1wk'
    };

    document.addEventListener('DOMContentLoaded', async () => {
        checkAuth();
        if (!clientId) {
            showError('No client selected.');
            document.getElementById('clientChartsTitle').textContent = 'Invalid client';
            return;
        }

        document.getElementById('backLink').href = 'client-detail.html';
        setupTimelineButtons();
        setupCloseAssetChart();
        await loadClientAndAssets();
        if (Object.keys(portfolio).length > 0) {
            await updateCombinedChart();
            await updatePortfolioValue();
        }
        renderAssetCards();
    });

    function setupTimelineButtons() {
        document.querySelectorAll('.timeline-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('.timeline-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentPeriod = btn.getAttribute('data-period');
                updateCombinedChart();
                if (singleAssetChartSymbol) {
                    loadSingleAssetChart(singleAssetChartSymbol);
                }
            });
        });
    }

    function setupCloseAssetChart() {
        document.getElementById('closeAssetChart').addEventListener('click', () => {
            document.getElementById('assetChartSection').classList.add('hidden');
            singleAssetChartSymbol = null;
            if (singleAssetChart) {
                singleAssetChart.destroy();
                singleAssetChart = null;
            }
        });
    }

    async function loadClientAndAssets() {
        try {
            client = await apiCall(`/clients/${clientId}`);
            assets = await apiCall(`/clients/${clientId}/assets`);
            document.getElementById('clientChartsTitle').textContent = client.name + ' – Portfolio Charts';

            portfolio = {};
            if (Array.isArray(assets)) {
                assets.forEach(a => {
                    if (a.symbol && a.quantity != null) {
                        portfolio[a.symbol] = Number(a.quantity);
                    }
                });
            }

            if (Object.keys(portfolio).length === 0) {
                document.getElementById('combinedChartMessage').textContent = 'No assets in this portfolio. Add assets to see charts.';
            }
        } catch (err) {
            console.error(err);
            showError('Failed to load client or assets.');
        }
    }

    async function updatePortfolioValue() {
        if (Object.keys(portfolio).length === 0) return;
        try {
            const data = await apiCall('/pricing/portfolio/value', 'POST', portfolio);
            const totalEl = document.getElementById('totalPortfolioValue');
            const timeEl = document.getElementById('lastUpdateTime');
            if (data && data.totalValue != null) {
                totalEl.textContent = '$' + Number(data.totalValue).toFixed(2);
            }
            if (timeEl) {
                timeEl.textContent = new Date().toLocaleTimeString();
            }
        } catch (err) {
            console.error(err);
        }
    }

    async function updateCombinedChart() {
        if (Object.keys(portfolio).length === 0) return;
        const msgEl = document.getElementById('combinedChartMessage');
        try {
            // Use backend endpoint that fetches from database (MANUAL source only)
            const data = await apiCall(
                `/clients/${clientId}/portfolio/chart?period=${currentPeriod}`,
                'POST',
                portfolio
            );
            if (!data || !data.data || data.data.length === 0) {
                msgEl.textContent = 'No chart data available for this period.';
                if (combinedChart) {
                    combinedChart.destroy();
                    combinedChart = null;
                }
                return;
            }
            msgEl.textContent = '';

            const labels = data.data.map(p => new Date(p.time).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: '2-digit' }));
            const values = data.data.map(p => p.value);

            const canvas = document.getElementById('combinedPortfolioChart');
            if (combinedChart) combinedChart.destroy();
            combinedChart = new Chart(canvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: 'Combined Portfolio Value',
                        data: values,
                        borderColor: 'rgb(37, 99, 235)',
                        backgroundColor: 'rgba(37, 99, 235, 0.08)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.2,
                        pointRadius: 2,
                        pointBackgroundColor: 'rgb(37, 99, 235)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: { display: true, position: 'top' },
                        title: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: { display: true, text: 'Value ($)' }
                        }
                    }
                }
            });
        } catch (err) {
            console.error(err);
            msgEl.textContent = 'Failed to load combined chart.';
            if (combinedChart) {
                combinedChart.destroy();
                combinedChart = null;
            }
        }
    }

    function renderAssetCards() {
        const grid = document.getElementById('assetsGrid');
        if (!assets || assets.length === 0) {
            grid.innerHTML = '<p class="section-hint">No assets for this client.</p>';
            return;
        }
        grid.innerHTML = assets.map(asset => {
            const pnl = asset.profitLoss != null ? Number(asset.profitLoss).toFixed(2) : '--';
            const pnlClass = (asset.profitLoss != null && asset.profitLoss >= 0) ? 'positive' : 'negative';
            const value = asset.currentPrice != null && asset.quantity != null
                ? (Number(asset.currentPrice) * Number(asset.quantity)).toFixed(2)
                : '--';
            const currency = asset.currency || 'USD';
            return `
                <div class="asset-card" data-symbol="${escapeHtml(asset.symbol)}" data-name="${escapeHtml(asset.name || asset.symbol)}">
                    <span class="asset-symbol">${escapeHtml(asset.symbol)}</span>
                    <span class="asset-name">${escapeHtml(asset.name || asset.symbol)}</span>
                    <span class="asset-value">${currency} ${value}</span>
                    <span class="asset-pnl ${pnlClass}">P&L: ${currency} ${pnl}</span>
                    <span class="asset-meta">Qty: ${escapeHtml(String(asset.quantity))}</span>
                </div>
            `;
        }).join('');

        grid.querySelectorAll('.asset-card').forEach(card => {
            card.addEventListener('click', () => {
                const symbol = card.getAttribute('data-symbol');
                openAssetChart(symbol, card.getAttribute('data-name'));
            });
        });
    }

    function openAssetChart(symbol, name) {
        singleAssetChartSymbol = symbol;
        document.getElementById('assetChartTitle').textContent = name || symbol + ' – Historical';
        document.getElementById('assetChartSection').classList.remove('hidden');
        loadSingleAssetChart(symbol);
    }

    async function loadSingleAssetChart(symbol) {
        const canvas = document.getElementById('singleAssetChart');
        if (singleAssetChart) {
            singleAssetChart.destroy();
            singleAssetChart = null;
        }
        try {
            const interval = PERIOD_TO_INTERVAL[currentPeriod] || '1d';
            const data = await apiCall(`/pricing/chart/${symbol}?period=${currentPeriod}&interval=${interval}`);
            if (!data || !data.data || data.data.length === 0) {
                return;
            }
            const labels = data.data.map(p => new Date(p.time).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: '2-digit' }));
            const closes = data.data.map(p => p.close);

            singleAssetChart = new Chart(canvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: symbol + ' Price',
                        data: closes,
                        borderColor: 'rgb(37, 99, 235)',
                        backgroundColor: 'rgba(37, 99, 235, 0.08)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.2,
                        pointRadius: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: true,
                    plugins: {
                        legend: { display: true, position: 'top' }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: { display: true, text: 'Price ($)' }
                        }
                    }
                }
            });
        } catch (err) {
            console.error('Failed to load chart for ' + symbol, err);
        }
    }

    function showError(message) {
        const el = document.getElementById('errorBanner');
        if (el) {
            el.textContent = message;
            el.classList.remove('hidden');
            setTimeout(() => el.classList.add('hidden'), 5000);
        }
    }

    function escapeHtml(text) {
        if (text == null) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
})();
