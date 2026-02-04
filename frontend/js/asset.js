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

    const purchaseDateTimeValue = document.getElementById('assetPurchaseDateTime').value;
    const sellingDateTimeValue = document.getElementById('assetSellingDateTime').value;

    const assetData = {
        name: document.getElementById('assetName').value,
        category: document.getElementById('assetCategory').value,
        symbol: document.getElementById('assetSymbol').value,
        quantity: parseFloat(document.getElementById('assetQuantity').value),
        buyingRate: parseFloat(document.getElementById('assetBuyingRate').value),
        purchaseDateTime: purchaseDateTimeValue ? `${purchaseDateTimeValue}:00Z` : null,
        currency: document.getElementById('assetCurrency')?.value || 'USD',
        sold: document.getElementById('assetSold').checked,
        sellingRate: parseFloat(document.getElementById('assetSellingRate').value) || null,
        sellingDateTime: sellingDateTimeValue ? `${sellingDateTimeValue}:00Z` : null,
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
        // Setup checkbox toggle for selling info
        const soldCheckbox = document.getElementById('assetSold');
        const sellingInfo = document.getElementById('selling-info-client');
        const sellingRateInput = document.getElementById('assetSellingRate');
        const sellingDateTimeInput = document.getElementById('assetSellingDateTime');
        
        if (soldCheckbox && sellingInfo) {
            // Ensure selling section is hidden initially
            sellingInfo.classList.add('hidden');
            soldCheckbox.checked = false;
            if (sellingRateInput) sellingRateInput.value = '';
            if (sellingDateTimeInput) sellingDateTimeInput.value = '';
            
            // Add event listener for the checkbox to toggle selling info visibility
            soldCheckbox.onchange = () => {
                sellingInfo.classList.toggle('hidden', !soldCheckbox.checked);
                if (!soldCheckbox.checked) {
                    if (sellingRateInput) sellingRateInput.value = '';
                    if (sellingDateTimeInput) sellingDateTimeInput.value = '';
                }
            };
        }
        
        modal.classList.remove('hidden');
    }
};

window.closeAddAssetModal = closeAddAssetModal;
