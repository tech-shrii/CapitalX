let pieChart = null;
let lineChart = null;
let refreshIntervalId = null;
let isModalOpen = false;

document.addEventListener('DOMContentLoaded', () => {
    console.log('[DEBUG] DOMContentLoaded - Initializing dashboard');
    checkAuth();
    setupUser();
    setupTabs();
    loadTab('dashboard');
    loadClientsForSwitcher();
    setupClientSwitcher();
    
    // Start auto-refresh only if no modal is open
    /* refreshIntervalId = setInterval(() => {
        if (!isModalOpen) {
            refreshData();
        }
    }, 10000); // Auto-refresh every 10 seconds */
    console.log('[DEBUG] Auto-refresh interval set:', refreshIntervalId);

    const addClientBtnHeader = document.getElementById('addClientBtnHeader');
    console.log('[DEBUG] addClientBtnHeader found:', !!addClientBtnHeader);
    if (addClientBtnHeader) {
        addClientBtnHeader.addEventListener('click', (e) => {
            console.log('[DEBUG] Add Client header button clicked');
            e.preventDefault();
            loadTab('setting'); // Switch to settings tab
            // Now, within settings, open the client management modal
            // This will be handled by the setupSettings function
        });
    }
    
    // Close modal handler for edit asset modal
    const closeEditAssetModal = document.getElementById('closeEditAssetModal');
    if (closeEditAssetModal) {
        closeEditAssetModal.addEventListener('click', () => {
            const modal = document.getElementById('editAssetModal');
            if (modal) {
                modal.classList.add('hidden');
                isModalOpen = false;
            }
        });
    }
    
    // Close modal when clicking outside
    window.addEventListener('click', (e) => {
        const editModal = document.getElementById('editAssetModal');
        if (editModal && e.target === editModal) {
            console.log('[DEBUG] Clicked outside editAssetModal');
            editModal.classList.add('hidden');
            editModal.style.removeProperty('display');
            editModal.style.removeProperty('visibility');
            editModal.style.removeProperty('opacity');
            editModal.style.removeProperty('z-index');
            isModalOpen = false;
        }
    });
});

let currentClientId = 'main'; // 'main' for the user's own portfolio

function setupClientSwitcher() {
    const clientSwitcher = document.getElementById('clientSwitcher');
    clientSwitcher.addEventListener('change', (e) => {
        currentClientId = e.target.value;
        refreshData();
    });
}

function refreshData() {
    console.log('[DEBUG] refreshData called, isModalOpen:', isModalOpen);
    // Don't refresh if a modal is open
    if (isModalOpen) {
        console.log('[DEBUG] Skipping refresh - modal is open');
        return;
    }
    
    const activeTab = document.querySelector('.nav a.active')?.getAttribute('data-tab');
    console.log('[DEBUG] Active tab for refresh:', activeTab);
    if (activeTab === 'dashboard') {
        console.log('[DEBUG] Refreshing dashboard data only');
        loadDashboardData();
    } else if (activeTab) {
        console.log('[DEBUG] Loading tab:', activeTab);
        loadTab(activeTab);
    } else {
        console.warn('[WARN] No active tab found for refresh');
    }
}


function destroyCharts() {
    if (pieChart) { pieChart.destroy(); pieChart = null; }
    if (lineChart) { lineChart.destroy(); lineChart = null; }
}

function setupUser() {
    const userEmail = document.getElementById('userEmail');
    if (userEmail) {
        userEmail.textContent = localStorage.getItem('userEmail') || 'user@example.com';
    }
}

function setupTabs() {
    const navLinks = document.querySelectorAll('.nav a');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            const tab = link.getAttribute('data-tab');
            loadTab(tab);
        });
    });
}

async function loadTab(tab) {
    const tabContent = document.getElementById('tab-content');
    if (!tabContent) return;

    const assetCategories = ['stock', 'mutual_fund', 'bond', 'crypto', 'commodity', 'forex'];

    if (tab === 'dashboard') {
        destroyCharts();
        const response = await fetch('dashboard-content.html');
        tabContent.innerHTML = await response.text();
        loadDashboardData();
    } else if (tab === 'clients') {
        tabContent.innerHTML = `
            <div class="card">
                <h2>Client Management</h2>
                <div id="client-list" class="clients-grid"></div>
            </div>
        `;
        setupClientManagement();
    } else if (assetCategories.includes(tab)) {
        loadAssetCategoryTab(tab);
    } else if (tab === 'news') {
        loadNewsTab();
    } else if (tab === 'statement') {
        loadStatementTab();
    } else if (tab === 'setting') {
        tabContent.innerHTML = `
            <div class="card">
                <h2>Settings</h2>
                <div id="settings-content"></div>
                <button id="logoutBtn" class="btn btn-danger" style="margin-top: 20px;">Logout</button>
            </div>
        `;
        setupSettings();
    } else {
        tabContent.innerHTML = `<div class="card"><h2>${tab.replace('_', ' ')}</h2><p>Content for ${tab.replace('_', ' ')} will be loaded here.</p></div>`;
    }
}

async function loadDashboardData() {
    try {
        const url = currentClientId === 'main' ? '/dashboard/summary' : `/clients/${currentClientId}/dashboard/summary`;
        console.log('[DEBUG] loadDashboardData - URL:', url);
        
        const data = await apiCall(url);
        console.log('[DEBUG] Dashboard data received:', data);
        
        // Update summary cards
        const defaultCurrency = data.currency || 'USD';
        document.getElementById('totalPortfolioValue').textContent = formatCurrency(data.totalCurrentValue || 0, defaultCurrency);
        document.getElementById('totalReturns').textContent = formatCurrency(data.totalProfitLoss || 0, defaultCurrency);
        document.getElementById('totalInvestment').textContent = formatCurrency(data.totalInvested || 0, defaultCurrency);
        document.getElementById('assetCategoryCount').textContent = data.assetCategoryCount || 0;

        const todaysPLValue = data.todaysPL || 0;
        const todaysPL = document.getElementById('todaysPL');
        if (todaysPL) {
            todaysPL.textContent = formatCurrency(todaysPLValue, defaultCurrency);
            todaysPL.classList.toggle('positive', todaysPLValue >= 0);
            todaysPL.classList.toggle('negative', todaysPLValue < 0);
        }
        
        const todaysPLPercentageValue = data.todaysPLPercentage || 0;
        const todaysPLPercentage = document.getElementById('todaysPLPercentage');
        if (todaysPLPercentage) {
            todaysPLPercentage.textContent = formatPercent(todaysPLPercentageValue);
            todaysPLPercentage.classList.toggle('positive', todaysPLPercentageValue >= 0);
            todaysPLPercentage.classList.toggle('negative', todaysPLPercentageValue < 0);
        }
        
        const pnl = data.totalProfitLoss || 0;
        const pnlPercent = data.totalProfitLossPercent || 0;
        const totalReturnsPercentage = document.getElementById('totalReturnsPercentage');
        totalReturnsPercentage.textContent = `${formatPercent(pnlPercent)} overall return`;
        totalReturnsPercentage.classList.toggle('positive', pnl >= 0);
        totalReturnsPercentage.classList.toggle('negative', pnl < 0);

        // Update charts
        updatePieChart(data.assetAllocation);
        updateLineChart(data.portfolioPerformance);

        // Update Top Performing Assets Table
        const topAssetsTable = document.getElementById('topAssetsTable');
        if (topAssetsTable) {
            const tbody = topAssetsTable.getElementsByTagName('tbody')[0];
            if (tbody) {
                if (data.topAssets && data.topAssets.length > 0) {
                    tbody.innerHTML = data.topAssets.map(asset => `
                        <tr>
                            <td>${asset.name || 'N/A'}</td>
                            <td><span class="badge badge-blue">${asset.category || 'N/A'}</span></td>
                            <td class="right">${formatCurrency(asset.currentValue || 0, asset.currency || defaultCurrency)}</td>
                            <td class="right ${(asset.returns || 0) >= 0 ? 'positive' : 'negative'}">${formatPercent(asset.returns || 0)}</td>
                        </tr>
                    `).join('');
                } else {
                    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No assets found</td></tr>';
                }
            }
        }
        
        // Update Worst Performing Assets Table
        const worstAssetsTable = document.getElementById('worstAssetsTable');
        if (worstAssetsTable) {
            const tbody = worstAssetsTable.getElementsByTagName('tbody')[0];
            if (tbody) {
                if (data.worstAssets && data.worstAssets.length > 0) {
                    tbody.innerHTML = data.worstAssets.map(asset => `
                        <tr>
                            <td>${asset.name || 'N/A'}</td>
                            <td><span class="badge badge-red">${asset.category || 'N/A'}</span></td>
                            <td class="right">${formatCurrency(asset.currentValue || 0, asset.currency || defaultCurrency)}</td>
                            <td class="right ${(asset.returns || 0) >= 0 ? 'positive' : 'negative'}">${formatPercent(asset.returns || 0)}</td>
                        </tr>
                    `).join('');
                } else {
                    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No assets found</td></tr>';
                }
            }
        }

    } catch (error) {
        console.error('[ERROR] Error loading dashboard data:', error);
    }
}

function updatePieChart(assetAllocation) {
    const pieCtx = document.getElementById('pie');
    if (!pieCtx) return;

    const labels = Object.keys(assetAllocation || {});
    const data = Object.values(assetAllocation || {}).map(v => Number(v));

    if (pieChart) {
        pieChart.data.labels = labels;
        pieChart.data.datasets[0].data = data;
        pieChart.update();
        console.log('[DEBUG] Pie chart updated');
    } else {
        pieChart = new Chart(pieCtx.getContext('2d'), {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: ['#3b82f6','#10b981','#f59e0b','#8b5cf6','#ef4444', '#64748b'],
                }]
            }
        });
        console.log('[DEBUG] Pie chart created');
    }
}

function updateLineChart(portfolioPerformance) {
    const lineCtx = document.getElementById('line');
    if (!lineCtx) return;

    const labels = portfolioPerformance?.labels || [];
    const data = portfolioPerformance?.data?.map(v => Number(v)) || [];

    if (lineChart) {
        lineChart.data.labels = labels;
        lineChart.data.datasets[0].data = data;
        lineChart.update();
        console.log('[DEBUG] Line chart updated');
    } else {
        lineChart = new Chart(lineCtx.getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Portfolio Value',
                    data: data,
                    borderColor: '#2563eb',
                    tension: .4,
                    fill: false
                }]
            },
            options: {
                aspectRatio: 1 // Ensure the chart maintains a square aspect ratio
            }
        });
        console.log('[DEBUG] Line chart created');
    }
}



function setupClientManagement() {
    loadClients();
}

async function deleteClient(clientId) {
    if (!confirm('Are you sure you want to delete this client?')) return;

    try {
        await apiCall(`/clients/${clientId}`, 'DELETE');

        // If the currently viewed client is the one being deleted, reset to main view
        if (currentClientId === clientId) {
            currentClientId = 'main';
            document.getElementById('clientSwitcher').value = 'main';
            refreshData();
        }

        // Refresh client lists in UI
        loadClientsForSwitcher();
        loadClientsForSettings();
    } catch (error) {
        alert('Error deleting client: ' + error.message);
    }
}

async function editClient(clientId) {
    console.log('[DEBUG] editClient called with clientId:', clientId);
    console.log('[DEBUG] isModalOpen before:', isModalOpen);
    
    isModalOpen = true; // Prevent refresh while modal is open
    console.log('[DEBUG] isModalOpen after setting:', isModalOpen);
    
    try {
        console.log('[DEBUG] Fetching client from API');
        const client = await apiCall(`/clients/${clientId}`);
        console.log('[DEBUG] Client fetched:', client);
        
        const modal = document.getElementById('addClientModal');
        console.log('[DEBUG] Modal element found:', !!modal);
        if (!modal) {
            console.error('[ERROR] addClientModal not found in DOM');
            isModalOpen = false;
            return;
        }
        
        console.log('[DEBUG] Modal classes before:', modal.className);
        modal.classList.remove('hidden');
        console.log('[DEBUG] Modal classes after removing hidden:', modal.className);
        console.log('[DEBUG] Modal computed display:', window.getComputedStyle(modal).display);
        
        document.getElementById('clientName').value = client.name;
        document.getElementById('clientEmail').value = client.email;
        document.getElementById('clientPhone').value = client.phone || '';
        document.getElementById('clientCurrency').value = client.currency || 'USD';

        const form = document.getElementById('addClientForm');
        form.onsubmit = async (e) => {
            e.preventDefault();
            const clientData = {
                name: document.getElementById('clientName').value,
                email: document.getElementById('clientEmail').value,
                phone: document.getElementById('clientPhone').value,
                currency: document.getElementById('clientCurrency').value || null,
            };

            try {
                await apiCall(`/clients/${clientId}`, 'PUT', clientData);
                modal.classList.add('hidden');
                isModalOpen = false;
                form.reset();
                form.onsubmit = null; // Reset submit handler
                loadClientsForSwitcher();
                loadClientsForSettings(); // Refresh client list in settings
            } catch (error) {
                alert('Error updating client: ' + error.message);
            }
        };

    } catch (error) {
        alert('Error fetching client data: ' + error.message);
        isModalOpen = false;
    }
}

async function loadClients() {
    console.log('Loading clients...'); // Debugging
    try {
        const clients = await apiCall('/clients');
        const clientList = document.getElementById('client-list');
        if (clients.length === 0) {
            clientList.innerHTML = '<p>No clients yet. Add your first client!</p>';
            return;
        }
        clientList.innerHTML = clients.map(client => `
            <div class="card client-card" data-client-id="${client.id}">
                <h3>${client.name}</h3>
                <p>${client.email}</p>
                <div class="client-card-actions">
                    <button class="btn btn-secondary btn-sm edit-client-btn">Edit</button>
                    <button class="btn btn-danger btn-sm delete-client-btn">Delete</button>
                </div>
            </div>
        `).join('');

        // Add event listeners for edit and delete buttons
        document.querySelectorAll('.edit-client-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const clientId = e.target.closest('.client-card')?.dataset.clientId;
                if (clientId) {
                    editClient(clientId);
                }
            });
        });

        document.querySelectorAll('.delete-client-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const clientId = e.target.closest('.client-card')?.dataset.clientId;
                if (clientId) {
                    deleteClient(clientId);
                }
            });
        });

    } catch (error) {
        console.error('Error loading clients:', error);
    }
}

async function loadClientsForSwitcher() {
    try {
        const clients = await apiCall('/clients');
        const clientSwitcher = document.getElementById('clientSwitcher');
        clientSwitcher.innerHTML = '<option value="main">Combined Portfolio</option>';
        clients.forEach(client => {
            const option = document.createElement('option');
            option.value = client.id;
            option.textContent = client.name;
            clientSwitcher.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading clients for switcher:', error);
    }
}

async function editAsset(assetId) {
    console.log('[DEBUG] editAsset called with assetId:', assetId);
    isModalOpen = true;
    
    try {
        const url = `/assets/${assetId}`; // Simplified URL, assuming API gateway handles client scope
        console.log('[DEBUG] Fetching asset from URL:', url);
        const asset = await apiCall(url);
        console.log('[DEBUG] Asset fetched:', asset);
        
        const modal = document.getElementById('editAssetModal');
        if (!modal) {
            console.error('[ERROR] editAssetModal not found in DOM');
            isModalOpen = false;
            return;
        }

        modal.classList.remove('hidden');

        // Populate form fields
        document.getElementById('editAssetId').value = asset.id;
        document.getElementById('editAssetName').value = asset.name || '';
        document.getElementById('editAssetCategory').value = asset.category || 'STOCK';
        document.getElementById('editAssetSymbol').value = asset.symbol || '';
        document.getElementById('editAssetQuantity').value = asset.quantity || '';
        document.getElementById('editAssetBuyingRate').value = asset.buyingRate || '';
        document.getElementById('editAssetCurrency').value = asset.currency || 'USD';
        
        // Handle purchase date-time
        if (asset.purchaseDateTime) {
            // Convert ISO string (e.g., 2023-10-27T10:00:00Z) to format for datetime-local (YYYY-MM-DDTHH:MM)
            document.getElementById('editAssetPurchaseDateTime').value = asset.purchaseDateTime.slice(0, 16);
        }

        // Handle selling info
        const soldCheckbox = document.getElementById('editAssetSold');
        const sellingInfo = document.getElementById('selling-info');
        const sellingRateInput = document.getElementById('editAssetSellingRate');
        const sellingDateTimeInput = document.getElementById('editAssetSellingDateTime');

        soldCheckbox.checked = asset.sold;
        sellingRateInput.value = asset.sellingRate || '';
        if (asset.sellingDateTime) {
            sellingDateTimeInput.value = asset.sellingDateTime.slice(0, 16);
        } else {
            sellingDateTimeInput.value = '';
        }

        // Toggle visibility based on initial 'sold' status
        if (asset.sold) {
            sellingInfo.classList.remove('hidden');
        } else {
            sellingInfo.classList.add('hidden');
        }

        // Add event listener for the checkbox
        soldCheckbox.onchange = () => {
            sellingInfo.classList.toggle('hidden', !soldCheckbox.checked);
        };

        // Handle form submission
        const form = document.getElementById('editAssetForm');
        form.onsubmit = async (e) => {
            e.preventDefault();
            
            const purchaseDateTimeValue = document.getElementById('editAssetPurchaseDateTime').value;
            const sellingDateTimeValue = document.getElementById('editAssetSellingDateTime').value;

            const assetData = {
                name: document.getElementById('editAssetName').value,
                category: document.getElementById('editAssetCategory').value,
                symbol: document.getElementById('editAssetSymbol').value,
                quantity: parseFloat(document.getElementById('editAssetQuantity').value),
                buyingRate: parseFloat(document.getElementById('editAssetBuyingRate').value),
                purchaseDateTime: purchaseDateTimeValue ? `${purchaseDateTimeValue}:00Z` : null,
                currency: document.getElementById('editAssetCurrency').value,
                sold: document.getElementById('editAssetSold').checked,
                sellingRate: parseFloat(document.getElementById('editAssetSellingRate').value) || null,
                sellingDateTime: sellingDateTimeValue ? `${sellingDateTimeValue}:00Z` : null,
            };

            try {
                const updateUrl = `/assets/${assetId}`;
                await apiCall(updateUrl, 'PUT', assetData);
                modal.classList.add('hidden');
                isModalOpen = false;
                form.reset();
                form.onsubmit = null;
                refreshData(); 
            } catch (error) {
                console.error('[ERROR] Backend error updating asset:', error);
                alert('Error updating asset: ' + error.message);
            }
        };

    } catch (error) {
        console.error('[ERROR] Error fetching asset data for edit:', error);
        alert('Error fetching asset data: ' + error.message);
        isModalOpen = false;
    }
}

function setupSettings() {
    const settingsContent = document.getElementById('settings-content');
    settingsContent.innerHTML = `
        <div class="grid2">
            <div class="card">
                <h3>Profile</h3>
                <form id="profileForm">
                    <div class="form-group">
                        <label>Name</label>
                        <input type="text" id="profileName" required class="form-control">
                    </div>
                    <div class="form-group">
                        <label>Email</label>
                        <input type="email" id="profileEmail" disabled class="form-control">
                    </div>
                    <button type="submit" class="btn btn-primary">Update Profile</button>
                </form>
                <div id="profileMessage" class="message"></div>
            </div>
            <div class="card">
                <h3>Reset Password</h3>
                <form id="resetPasswordForm">
                    <div class="form-group">
                        <label>Current Password</label>
                        <input type="password" id="currentPassword" required class="form-control">
                    </div>
                    <div class="form-group">
                        <label>New Password</label>
                        <input type="password" id="newPassword" required minlength="6" class="form-control">
                    </div>
                    <button type="submit" class="btn btn-primary">Reset Password</button>
                </form>
                <div id="passwordMessage" class="message"></div>
            </div>
        </div>
        <div class="card" style="margin-top: 20px;">
            <h3>Client Management</h3>
            <button id="addClientBtn" class="btn btn-primary">+ Add Client</button>
            <div id="clientListSettings" class="clients-grid" style="margin-top: 20px;"></div>
        </div>
        <div id="addClientModal" class="modal hidden">
            <div class="modal-content">
                <span class="close" id="closeClientModal">&times;</span>
                <h2>Add Client</h2>
                <form id="addClientForm">
                    <div class="form-group">
                        <label>Name</label>
                        <input type="text" id="clientName" required placeholder="Client name">
                    </div>
                    <div class="form-group">
                        <label>Email</label>
                        <input type="email" id="clientEmail" required placeholder="email@example.com">
                    </div>
                    <div class="form-group">
                        <label>Phone</label>
                        <input type="tel" id="clientPhone" placeholder="+1 234 567 8900">
                    </div>
                    <div class="form-group">
                        <label>Currency</label>
                        <select id="clientCurrency">
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                            <option value="GBP">GBP</option>
                            <option value="INR">INR</option>
                            <option value="JPY">JPY</option>
                            <option value="CHF">CHF</option>
                            <option value="CAD">CAD</option>
                            <option value="AUD">AUD</option>
                        </select>
                    </div>
                    <div id="csvImportSection" class="form-group hidden">
                        <label>Import from CSV</label>
                        <input type="file" id="csvFile" accept=".csv">
                    </div>
                    <button type="submit" class="btn btn-primary" id="addClientSubmitBtn">Add Client</button>
                </form>
            </div>
        </div>
    `;

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.clear();
            window.location.href = '../index.html';
        });
    }

    const addClientBtn = document.getElementById('addClientBtn');
    const addClientModal = document.getElementById('addClientModal');
    const closeClientModal = document.getElementById('closeClientModal');
    const addClientForm = document.getElementById('addClientForm');
    const addClientSubmitBtn = document.getElementById('addClientSubmitBtn');
    const csvImportSection = document.getElementById('csvImportSection');

    addClientBtn.addEventListener('click', (e) => {
        console.log('[DEBUG] Add Client button clicked!', e);
        e.preventDefault();
        e.stopPropagation();
        isModalOpen = true; // Prevent refresh while modal is open
        console.log('[DEBUG] isModalOpen set to true');
        console.log('[DEBUG] Modal classes before:', addClientModal.className);
        
        // Remove hidden class AND force display with inline style to override !important
        addClientModal.classList.remove('hidden');
        addClientModal.style.setProperty('display', 'block', 'important');
        addClientModal.style.setProperty('visibility', 'visible', 'important');
        addClientModal.style.setProperty('opacity', '1', 'important');
        addClientModal.style.setProperty('z-index', '10000', 'important');
        
        console.log('[DEBUG] Modal classes after:', addClientModal.className);
        
        // Force display and check visibility
        const computedStyle = window.getComputedStyle(addClientModal);
        console.log('[DEBUG] Modal computed display:', computedStyle.display);
        console.log('[DEBUG] Modal computed visibility:', computedStyle.visibility);
        console.log('[DEBUG] Modal computed z-index:', computedStyle.zIndex);
        console.log('[DEBUG] Modal computed opacity:', computedStyle.opacity);
        console.log('[DEBUG] Modal offsetParent:', addClientModal.offsetParent);
        console.log('[DEBUG] Modal offsetWidth:', addClientModal.offsetWidth);
        console.log('[DEBUG] Modal offsetHeight:', addClientModal.offsetHeight);
        addClientForm.reset();
        csvImportSection.classList.add('hidden'); // Hide CSV import initially
        addClientSubmitBtn.textContent = 'Add Client';
        addClientForm.onsubmit = handleAddClientFormSubmit; // Set initial submit handler
    });

    closeClientModal.addEventListener('click', () => {
        console.log('[DEBUG] Close client modal button clicked');
        addClientModal.classList.add('hidden');
        addClientModal.style.removeProperty('display');
        addClientModal.style.removeProperty('visibility');
        addClientModal.style.removeProperty('opacity');
        addClientModal.style.removeProperty('z-index');
        isModalOpen = false;
    });
    
    // Close modal when clicking outside
    window.addEventListener('click', (e) => {
        if (e.target === addClientModal) {
            console.log('[DEBUG] Clicked outside addClientModal');
            addClientModal.classList.add('hidden');
            addClientModal.style.removeProperty('display');
            addClientModal.style.removeProperty('visibility');
            addClientModal.style.removeProperty('opacity');
            addClientModal.style.removeProperty('z-index');
            isModalOpen = false;
        }
    });

    async function handleAddClientFormSubmit(e) {
        e.preventDefault();
        const clientData = {
            name: document.getElementById('clientName').value,
            email: document.getElementById('clientEmail').value,
            phone: document.getElementById('clientPhone').value,
            currency: document.getElementById('clientCurrency').value || null,
        };

        try {
            const newClient = await apiCall('/clients', 'POST', clientData);
            // After adding client, show CSV import section
            csvImportSection.classList.remove('hidden');
            addClientSubmitBtn.textContent = 'Import CSV & Finish';
            addClientForm.onsubmit = (e) => handleCsvImportSubmit(e, newClient.id); // Change submit handler
            loadClientsForSettings(); // Refresh client list in settings
        } catch (error) {
            alert('Error adding client: ' + error.message);
            isModalOpen = false;
        }
    }

    async function handleCsvImportSubmit(e, clientId) {
        e.preventDefault();
        console.log('[DEBUG] handleCsvImportSubmit called with clientId:', clientId);
        
        const fileInput = document.getElementById('csvFile');
        console.log('[DEBUG] File input found:', !!fileInput);
        console.log('[DEBUG] Files selected:', fileInput?.files?.length || 0);
        
        if (fileInput && fileInput.files.length > 0) {
            const file = fileInput.files[0];
            console.log('[DEBUG] File details:', {
                name: file.name,
                size: file.size,
                type: file.type,
                lastModified: new Date(file.lastModified)
            });
            
            const formData = new FormData();
            formData.append('file', file);
            console.log('[DEBUG] FormData created, calling API...');
            
            try {
                const result = await apiCallFormData(`/clients/${clientId}/assets/import-csv`, formData);
                console.log('[DEBUG] CSV import successful:', result);
                alert(`Successfully imported ${result.count || 0} assets!`);
                addClientModal.classList.add('hidden');
                isModalOpen = false;
                addClientForm.reset();
                loadClientsForSettings(); // Refresh client list in settings
            } catch (error) {
                console.error('[ERROR] CSV import failed:', error);
                console.error('[ERROR] Error message:', error.message);
                console.error('[ERROR] Error stack:', error.stack);
                alert('Error importing CSV: ' + error.message);
            }
        } else {
            console.warn('[WARN] No CSV file selected');
            alert('No CSV file selected. Client added without assets.');
            addClientModal.classList.add('hidden');
            isModalOpen = false;
            addClientForm.reset();
            loadClientsForSettings(); // Refresh client list in settings
        }
    }

    loadClientsForSettings(); // Load clients initially for settings page

    // Load profile data
    apiCall('/profile').then(profile => {
        document.getElementById('profileName').value = profile.name;
        document.getElementById('profileEmail').value = profile.email;
    });

    // Profile form submission
    const profileForm = document.getElementById('profileForm');
    profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const profileData = { name: document.getElementById('profileName').value };
        try {
            await apiCall('/profile', 'PUT', profileData);
            const msg = document.getElementById('profileMessage');
            msg.textContent = 'Profile updated successfully!';
            msg.className = 'message success';
        } catch (error) {
            const msg = document.getElementById('profileMessage');
            msg.textContent = 'Error updating profile: ' + error.message;
            msg.className = 'message error';
        }
    });

    // Password reset form submission
    const resetPasswordForm = document.getElementById('resetPasswordForm');
    resetPasswordForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const passwordData = {
            currentPassword: document.getElementById('currentPassword').value,
            newPassword: document.getElementById('newPassword').value,
        };
        try {
            await apiCall('/profile/reset-password', 'POST', passwordData);
            const msg = document.getElementById('passwordMessage');
            msg.textContent = 'Password reset successfully!';
            msg.className = 'message success';
            resetPasswordForm.reset();
        } catch (error) {
            const msg = document.getElementById('passwordMessage');
            msg.textContent = 'Error resetting password: ' + error.message;
            msg.className = 'message error';
        }
    });
}

async function loadClientsForSettings() {
    try {
        const clients = await apiCall('/clients');
        const clientListSettings = document.getElementById('clientListSettings');
        if (clients.length === 0) {
            clientListSettings.innerHTML = '<p>No clients yet. Add your first client!</p>';
            return;
        }
        clientListSettings.innerHTML = clients.map(client => `
            <div class="card client-card" data-client-id="${client.id}">
                <h3>${client.name}</h3>
                <p>${client.email}</p>
                <div class="client-card-actions">
                    <button class="btn btn-secondary btn-sm edit-client-btn">Edit</button>
                    <button class="btn btn-danger btn-sm delete-client-btn">Delete</button>
                </div>
            </div>
        `).join('');

        // Add event listeners for edit and delete buttons
        document.querySelectorAll('#clientListSettings .edit-client-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const clientId = e.target.closest('.client-card')?.dataset.clientId;
                if (clientId) {
                    editClient(clientId);
                }
            });
        });

        document.querySelectorAll('#clientListSettings .delete-client-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const clientId = e.target.closest('.client-card')?.dataset.clientId;
                if (clientId) {
                    deleteClient(clientId);
                }
            });
        });

    } catch (error) {
        console.error('Error loading clients for settings:', error);
    }
}

async function loadAssetCategoryTab(category) {
    console.log('[DEBUG] loadAssetCategoryTab called for category:', category);
    const tabContent = document.getElementById('tab-content');
    console.log('[DEBUG] tab-content element found:', !!tabContent);
    if (!tabContent) {
        console.error('[ERROR] tab-content not found');
        return;
    }
    console.log('[DEBUG] Setting innerHTML for category tab');
    tabContent.innerHTML = `
        <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <h2>${category.replace('_', ' ').toUpperCase()}</h2>
                <button id="addAssetBtn" class="btn btn-primary">+ Add Asset</button>
            </div>
            <table class="assets-table">
                <thead>
                    <tr>
                        <th>Asset Name</th>
                        <th>Symbol</th>
                        <th>Quantity</th>
                        <th>Buying Rate</th>
                        <th>Current Price</th>
                        <th>P&L</th>
                        <th>P&L %</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="asset-table-body">
                    <tr><td colspan="8" style="text-align:center;">Loading...</td></tr>
                </tbody>
            </table>
            <div id="pagination-controls" style="display: flex; justify-content: center; margin-top: 20px;"></div>
        </div>
    `;

        // Use setTimeout to ensure DOM is ready
        setTimeout(() => {
            const addAssetBtn = document.getElementById('addAssetBtn');
            console.log('[DEBUG] loadAssetCategoryTab - addAssetBtn found:', !!addAssetBtn, 'for category:', category);
            if (addAssetBtn) {
                console.log('[DEBUG] Attaching click listener to addAssetBtn');
                // Remove any existing listeners first
                const newBtn = addAssetBtn.cloneNode(true);
                addAssetBtn.parentNode.replaceChild(newBtn, addAssetBtn);
                
                newBtn.addEventListener('click', (e) => {
                    console.log('[DEBUG] Add Asset button clicked!', e);
                    e.preventDefault();
                    e.stopPropagation();
                    console.log('[DEBUG] Calling addAsset with category:', category);
                    addAsset(category);
                });
                console.log('[DEBUG] Click listener attached successfully');
            } else {
                console.error('[ERROR] addAssetBtn not found for category:', category);
                console.error('[ERROR] Available buttons:', document.querySelectorAll('button').length);
            }
        }, 100);

    try {
        const url = currentClientId === 'main' ? '/assets' : `/clients/${currentClientId}/assets`;
        const assets = await apiCall(url);
        const filteredAssets = assets.filter(asset => asset.category.toLowerCase() === category.toLowerCase());
        
        // Pagination variables
        const pageSize = 5;
        let currentPage = 0;
        const totalPages = Math.ceil(filteredAssets.length / pageSize);
        
        const assetTableBody = document.getElementById('asset-table-body');
        const paginationControls = document.getElementById('pagination-controls');
        
        function renderPage(page) {
            currentPage = page;
            const start = currentPage * pageSize;
            const end = start + pageSize;
            const pageAssets = filteredAssets.slice(start, end);
            
            if (pageAssets.length === 0) {
                assetTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;">No assets in this category.</td></tr>`;
                paginationControls.innerHTML = '';
                return;
            }

            assetTableBody.innerHTML = pageAssets.map(asset => {
                const currency = asset.currency || 'USD';
                return `
                <tr data-asset-id="${asset.id}">
                    <td>${asset.name}</td>
                    <td>${asset.symbol}</td>
                    <td>${asset.quantity}</td>
                    <td>${formatCurrency(asset.buyingRate, currency)}</td>
                    <td>${formatCurrency(asset.currentPrice, currency)}</td>
                    <td class="${asset.profitLoss >= 0 ? 'positive' : 'negative'}">${formatCurrency(asset.profitLoss, currency)}</td>
                    <td class="${asset.profitLossPercent >= 0 ? 'positive' : 'negative'}">${formatPercent(asset.profitLossPercent)}</td>
                    <td>
                        <button class="btn btn-secondary btn-sm edit-asset-btn">Edit</button>
                        <button class="btn btn-danger btn-sm delete-asset-btn">Delete</button>
                    </td>
                </tr>
            `;
            }).join('');

            // Render pagination controls
            let controlsHTML = '';
            if (totalPages > 1) {
                controlsHTML += `<button class="btn btn-secondary" id="prev-page" ${currentPage === 0 ? 'disabled' : ''}>Previous</button>`;
                for (let i = 0; i < totalPages; i++) {
                    controlsHTML += `<button class="btn ${i === currentPage ? 'btn-primary' : 'btn-secondary'}" data-page="${i}">${i + 1}</button>`;
                }
                controlsHTML += `<button class="btn btn-secondary" id="next-page" ${currentPage === totalPages - 1 ? 'disabled' : ''}>Next</button>`;
            }
            paginationControls.innerHTML = controlsHTML;

            // Add event listeners for pagination
            document.getElementById('prev-page')?.addEventListener('click', () => renderPage(currentPage - 1));
            document.getElementById('next-page')?.addEventListener('click', () => renderPage(currentPage + 1));
            document.querySelectorAll('[data-page]').forEach(btn => {
                btn.addEventListener('click', (e) => renderPage(parseInt(e.target.dataset.page)));
            });

            // Add event listeners for edit and delete buttons
            const editButtons = document.querySelectorAll('.edit-asset-btn');
            editButtons.forEach((btn) => {
                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const assetId = e.target.closest('tr')?.dataset.assetId;
                    if (assetId) {
                        editAsset(assetId);
                    }
                });
            });

            const deleteButtons = document.querySelectorAll('.delete-asset-btn');
            deleteButtons.forEach((btn) => {
                btn.addEventListener('click', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    const assetId = e.target.closest('tr')?.dataset.assetId;
                    if (assetId) {
                        deleteAsset(assetId);
                    }
                });
            });
        }
        
        renderPage(0);

    } catch (error) {
        console.error(`Error loading assets for ${category}:`, error);
        const assetTableBody = document.getElementById('asset-table-body');
        assetTableBody.innerHTML = `<tr><td colspan="8" style="text-align:center;">Error loading assets.</td></tr>`;
    }
}

async function loadNewsTab() {
    const tabContent = document.getElementById('tab-content');
    tabContent.innerHTML = `
        <div class="card">
            <h2>Financial News</h2>
            <div id="news-container" class="news-container"></div>
        </div>
    `;

    try {
        const news = await apiCall('/news');
        const newsContainer = document.getElementById('news-container');
        if (news.length === 0) {
            newsContainer.innerHTML = '<p>No news available at the moment.</p>';
            return;
        }

        newsContainer.innerHTML = news.map(item => `
            <div class="news-item">
                <h3>${item.title}</h3>
                <p>${item.description || ''}</p>
                <p><small>Source: ${item.source} • ${new Date(item.publishedAt).toLocaleDateString()}</small></p>
                ${item.url ? `<a href="${item.url}" target="_blank" class="btn btn-secondary btn-sm">Read more</a>` : ''}
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading news:', error);
        const newsContainer = document.getElementById('news-container');
        newsContainer.innerHTML = '<p>Error loading news. Please try again later.</p>';
    }
}

async function loadStatementTab() {
    const tabContent = document.getElementById('tab-content');
    tabContent.innerHTML = `
        <div class="card">
            <h2>Generate Statements</h2>
            <form id="statementForm">
                <div class="form-group">
                    <label>Select Client</label>
                    <select id="statementClientId" required class="form-control"></select>
                </div>
                <div class="form-group">
                    <label>Statement Type</label>
                    <select id="statementType" required class="form-control">
                        <option value="BASIC_PNL">Basic P&L</option>
                        <option value="DETAILED">Detailed</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Email To</label>
                    <input type="email" id="statementEmail" placeholder="Leave empty to use client email" class="form-control">
                </div>
                <button type="submit" class="btn btn-primary">Generate & Send Statement</button>
            </form>
            <div id="statementMessage" class="message"></div>
        </div>
    `;

    try {
        const clients = await apiCall('/clients');
        const select = document.getElementById('statementClientId');
        if (select) {
            select.innerHTML = '<option value="">Select a client</option>' +
                clients.map(client => 
                    `<option value="${client.id}">${client.name} (${client.email})</option>`
                ).join('');
        }
    } catch (error) {
        console.error('Error loading clients for statement form:', error);
    }

    const statementForm = document.getElementById('statementForm');
    statementForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const statementData = {
            clientId: document.getElementById('statementClientId').value,
            statementType: document.getElementById('statementType').value,
            emailTo: document.getElementById('statementEmail').value,
        };

        try {
            await apiCall('/statements/generate', 'POST', statementData);
            const msg = document.getElementById('statementMessage');
            msg.textContent = 'Statement generated and sent successfully!';
            msg.className = 'message success';
        } catch (error) {
            const msg = document.getElementById('statementMessage');
            msg.textContent = 'Error generating statement: ' + error.message;
            msg.className = 'message error';
        }
    });
}


async function addAsset(category) {
    console.log('[DEBUG] addAsset called with category:', category);
    console.log('[DEBUG] isModalOpen before:', isModalOpen);
    
    isModalOpen = true; // Prevent refresh while modal is open
    console.log('[DEBUG] isModalOpen after setting:', isModalOpen);
    
    const modal = document.getElementById('editAssetModal');
    console.log('[DEBUG] Modal element found:', !!modal);
    if (!modal) {
        console.error('[ERROR] editAssetModal not found in DOM');
        isModalOpen = false;
        return;
    }
    
    console.log('[DEBUG] Modal classes before:', modal.className);
    modal.classList.remove('hidden');
    console.log('[DEBUG] Modal classes after removing hidden:', modal.className);
    
    // Force display and check visibility
    const computedStyle = window.getComputedStyle(modal);
    console.log('[DEBUG] Modal computed display:', computedStyle.display);
    console.log('[DEBUG] Modal computed visibility:', computedStyle.visibility);
    console.log('[DEBUG] Modal computed z-index:', computedStyle.zIndex);
    console.log('[DEBUG] Modal computed opacity:', computedStyle.opacity);
    console.log('[DEBUG] Modal offsetParent:', modal.offsetParent);
    console.log('[DEBUG] Modal offsetWidth:', modal.offsetWidth);
    console.log('[DEBUG] Modal offsetHeight:', modal.offsetHeight);
    
    const form = document.getElementById('editAssetForm');
    console.log('[DEBUG] Form element found:', !!form);
    if (!form) {
        console.error('[ERROR] editAssetForm not found in DOM');
        isModalOpen = false;
        modal.classList.add('hidden');
        return;
    }
    form.reset();
    document.getElementById('editAssetId').value = ''; // Clear ID for new asset
    document.getElementById('editAssetCategory').value = category.toUpperCase();
    console.log('[DEBUG] Form reset and category set');

    form.onsubmit = async (e) => {
        e.preventDefault();
        const assetData = {
            name: document.getElementById('editAssetName').value,
            category: document.getElementById('editAssetCategory').value,
            symbol: document.getElementById('editAssetSymbol').value,
            quantity: parseFloat(document.getElementById('editAssetQuantity').value),
            buyingRate: parseFloat(document.getElementById('editAssetBuyingRate').value),
            purchaseDate: document.getElementById('editAssetPurchaseDate').value,
            currency: document.getElementById('editAssetCurrency').value,
        };

        try {
            console.log('[DEBUG] Submitting asset data:', assetData);
            const url = currentClientId === 'main' ? '/assets' : `/clients/${currentClientId}/assets`;
            console.log('[DEBUG] POST URL:', url);
            const result = await apiCall(url, 'POST', assetData);
            console.log('[DEBUG] Asset added successfully:', result);
            modal.classList.add('hidden');
            modal.style.removeProperty('display');
            modal.style.removeProperty('visibility');
            modal.style.removeProperty('opacity');
            modal.style.removeProperty('z-index');
            isModalOpen = false;
            form.reset();
            form.onsubmit = null; // Reset submit handler
            refreshData();
        } catch (error) {
            console.error('[ERROR] Backend error adding asset:', error);
            console.error('[ERROR] Error stack:', error.stack);
            alert('Error adding asset: ' + error.message);
        }
    };

    const closeModalBtn = document.getElementById('closeEditAssetModal');
    if (closeModalBtn) {
        closeModalBtn.onclick = () => {
            console.log('[DEBUG] Close modal button clicked (addAsset)');
            modal.classList.add('hidden');
            modal.style.removeProperty('display');
            modal.style.removeProperty('visibility');
            modal.style.removeProperty('opacity');
            modal.style.removeProperty('z-index');
            isModalOpen = false;
            form.reset();
            form.onsubmit = null;
        };
    }
}

async function deleteAsset(assetId) {
    console.log('Deleting asset:', assetId); // Debugging
    if (!confirm('Are you sure you want to delete this asset?')) return;

    try {
        const url = currentClientId === 'main' ? `/assets/${assetId}` : `/clients/${currentClientId}/assets/${assetId}`;
        await apiCall(url, 'DELETE');
        refreshData();
    } catch (error) {
        alert('Error deleting asset: ' + error.message);
    }
}


// Make formatCurrency globally available
window.formatCurrency = function(amount, currency = 'USD') {
    if (amount == null || amount === undefined || amount === '') {
        return 'N/A';
    }
    
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount === 0) {
        return 'N/A';
    }
    
    // Currency symbol mapping
    const currencySymbols = {
        'USD': '$',
        'EUR': '€',
        'GBP': '£',
        'INR': '₹',
        'JPY': '¥',
        'CHF': 'CHF ',
        'CAD': 'C$',
        'AUD': 'A$',
        'CNY': '¥',
        'SGD': 'S$',
        'HKD': 'HK$'
    };
    
    const symbol = currencySymbols[currency?.toUpperCase()] || currency?.toUpperCase() + ' ';
    const formatted = numAmount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    
    return symbol + formatted;
};

// Make formatPercent globally available
window.formatPercent = function(percent) {
    if (percent == null || percent === undefined || percent === '') {
        return 'N/A';
    }
    const numPercent = parseFloat(percent);
    if (isNaN(numPercent)) {
        return 'N/A';
    }
    const sign = numPercent >= 0 ? '+' : '';
    return sign + numPercent.toFixed(2) + '%';
}

// Debug function to test modal visibility
window.testModal = function() {
    console.log('[TEST] Testing modal visibility...');
    const modal = document.getElementById('editAssetModal');
    console.log('[TEST] Modal found:', !!modal);
    if (modal) {
        console.log('[TEST] Modal classes:', modal.className);
        console.log('[TEST] Modal has hidden class:', modal.classList.contains('hidden'));
        modal.classList.remove('hidden');
        console.log('[TEST] Modal classes after remove:', modal.className);
        const style = window.getComputedStyle(modal);
        console.log('[TEST] Modal display:', style.display);
        console.log('[TEST] Modal visibility:', style.visibility);
        console.log('[TEST] Modal z-index:', style.zIndex);
        console.log('[TEST] Modal position:', style.position);
        console.log('[TEST] Modal width:', style.width);
        console.log('[TEST] Modal height:', style.height);
        console.log('[TEST] Modal offsetParent:', modal.offsetParent);
        console.log('[TEST] Modal offsetWidth:', modal.offsetWidth);
        console.log('[TEST] Modal offsetHeight:', modal.offsetHeight);
        
        // Try to force show
        modal.style.display = 'block';
        modal.style.visibility = 'visible';
        modal.style.zIndex = '10000';
        console.log('[TEST] Modal forced to display');
        
        setTimeout(() => {
            modal.classList.add('hidden');
            console.log('[TEST] Modal hidden again after 3 seconds');
        }, 3000);
    }
};

// Debug function to test button clicks
window.testAddAssetButton = function() {
    console.log('[TEST] Testing Add Asset button...');
    const btn = document.getElementById('addAssetBtn');
    console.log('[TEST] Button found:', !!btn);
    if (btn) {
        console.log('[TEST] Button text:', btn.textContent);
        console.log('[TEST] Button classes:', btn.className);
        console.log('[TEST] Button disabled:', btn.disabled);
        console.log('[TEST] Simulating click...');
        btn.click();
    } else {
        console.error('[TEST] Button not found!');
    }
};

// Make functions globally available for debugging
window.debugModal = window.testModal;
window.debugAddAsset = window.testAddAssetButton;