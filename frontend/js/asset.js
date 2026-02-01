let currentClientId = null;

document.addEventListener('DOMContentLoaded', () => {
    const addAssetForm = document.getElementById('addAssetForm');
    if (addAssetForm) {
        addAssetForm.addEventListener('submit', handleAddAsset);
    }
});

function showAddAssetModal() {
    const modal = document.getElementById('addAssetModal');
    if (!modal) return;
    
    // Get current client ID from the detail modal
    const clientDetailTitle = document.getElementById('clientDetailTitle');
    if (clientDetailTitle && clientDetailTitle.textContent.includes('-')) {
        // Extract client ID from context or use a global variable
        // For now, we'll need to pass it when opening the modal
    }
    
    modal.classList.remove('hidden');
}

function closeAddAssetModal() {
    const modal = document.getElementById('addAssetModal');
    if (modal) {
        modal.classList.add('hidden');
        document.getElementById('addAssetForm').reset();
    }
}

async function handleAddAsset(e) {
    e.preventDefault();
    
    if (!currentClientId) {
        alert('Please select a client first');
        return;
    }

    const assetData = {
        name: document.getElementById('assetName').value,
        category: document.getElementById('assetCategory').value,
        symbol: document.getElementById('assetSymbol').value,
        quantity: parseFloat(document.getElementById('assetQuantity').value),
        buyingRate: parseFloat(document.getElementById('assetBuyingRate').value),
        purchaseDate: document.getElementById('assetPurchaseDate').value,
        currency: document.getElementById('assetCurrency')?.value || 'USD',
    };

    try {
        await apiCall(`/clients/${currentClientId}/assets`, 'POST', assetData);
        closeAddAssetModal();
        if (typeof viewClientDetail === 'function' && currentClientId) {
            viewClientDetail(currentClientId);
        } else {
            // Reload page or refresh client list
            window.location.reload();
        }
    } catch (error) {
        alert('Error adding asset: ' + error.message);
    }
}

// Update the showAddAssetModal to accept clientId
window.showAddAssetModal = function(clientId) {
    currentClientId = clientId;
    window.currentClientIdForAsset = clientId;
    const modal = document.getElementById('addAssetModal');
    if (modal) {
        modal.classList.remove('hidden');
    }
};

window.closeAddAssetModal = closeAddAssetModal;
