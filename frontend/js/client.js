let currentClientId = null;

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadClients();
    setupLogout();
    setupForms();
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

function setupForms() {
    const addClientForm = document.getElementById('addClientForm');
    if (addClientForm) {
        addClientForm.addEventListener('submit', handleAddClient);
    }
}

async function loadClients() {
    try {
        const clients = await apiCall('/clients');
        displayClients(clients);
    } catch (error) {
        console.error('Error loading clients:', error);
    }
}

function displayClients(clients) {
    const container = document.getElementById('clientsContainer');
    if (!container) return;

    if (clients.length === 0) {
        container.innerHTML = '<p>No clients yet. Add your first client!</p>';
        return;
    }

    container.innerHTML = clients.map(client => `
        <div class="client-card" onclick="viewClientDetail(${client.id})">
            <h3>${client.name}</h3>
            <p>${client.email}</p>
            ${client.phone ? `<p>${client.phone}</p>` : ''}
        </div>
    `).join('');
}

async function viewClientDetail(clientId) {
    currentClientId = clientId;
    try {
        const client = await apiCall(`/clients/${clientId}`);
        const assets = await apiCall(`/clients/${clientId}/assets`);
        
        displayClientDetail(client, assets);
        document.getElementById('clientDetailModal').classList.remove('hidden');
    } catch (error) {
        console.error('Error loading client detail:', error);
        alert('Error loading client details');
    }
}

function displayClientDetail(client, assets) {
    document.getElementById('clientDetailTitle').textContent = client.name + ' - Assets';
    
    // Set up add asset button
    const addAssetBtn = document.getElementById('addAssetBtn');
    if (addAssetBtn) {
        addAssetBtn.onclick = () => showAddAssetModalForClient(client.id);
    }
    
    const container = document.getElementById('clientAssetsContainer');
    if (!container) return;

    if (assets.length === 0) {
        container.innerHTML = '<p>No assets for this client yet.</p>';
        return;
    }

    container.innerHTML = `
        <table class="assets-table">
            <thead>
                <tr>
                    <th>Asset Name</th>
                    <th>Category</th>
                    <th>Quantity</th>
                    <th>Buying Rate</th>
                    <th>Current Price</th>
                    <th>P&L</th>
                    <th>P&L %</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${assets.map(asset => `
                    <tr>
                        <td>${asset.name}</td>
                        <td>${asset.category}</td>
                        <td>${asset.quantity}</td>
                        <td>₹${asset.buyingRate}</td>
                        <td>₹${asset.currentPrice || asset.buyingRate}</td>
                        <td class="${asset.profitLoss >= 0 ? 'positive' : 'negative'}">
                            ₹${(asset.profitLoss || 0).toFixed(2)}
                        </td>
                        <td class="${asset.profitLossPercent >= 0 ? 'positive' : 'negative'}">
                            ${(asset.profitLossPercent || 0).toFixed(2)}%
                        </td>
                        <td>
                            <button onclick="editAsset(${asset.id})" class="btn btn-secondary">Edit</button>
                            <button onclick="deleteAsset(${asset.id})" class="btn btn-secondary">Delete</button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function closeClientDetailModal() {
    document.getElementById('clientDetailModal').classList.add('hidden');
    currentClientId = null;
}

function showAddClientModal() {
    document.getElementById('addClientModal').classList.remove('hidden');
}

function closeAddClientModal() {
    document.getElementById('addClientModal').classList.add('hidden');
    document.getElementById('addClientForm').reset();
}

async function handleAddClient(e) {
    e.preventDefault();
    const clientData = {
        name: document.getElementById('clientName').value,
        email: document.getElementById('clientEmail').value,
        phone: document.getElementById('clientPhone').value,
    };

    try {
        await apiCall('/clients', 'POST', clientData);
        closeAddClientModal();
        loadClients();
    } catch (error) {
        alert('Error adding client: ' + error.message);
    }
}

async function editAsset(assetId) {
    // Implementation for editing asset
    alert('Edit asset functionality - to be implemented');
}

function showAddAssetModalForClient(clientId) {
    currentClientId = clientId;
    const modal = document.getElementById('addAssetModal');
    if (modal) {
        modal.classList.remove('hidden');
    }
}

async function deleteAsset(assetId) {
    if (!confirm('Are you sure you want to delete this asset?')) return;

    try {
        await apiCall(`/assets/${assetId}`, 'DELETE');
        if (currentClientId) {
            viewClientDetail(currentClientId);
        } else {
            loadClients();
        }
    } catch (error) {
        alert('Error deleting asset: ' + error.message);
    }
}
