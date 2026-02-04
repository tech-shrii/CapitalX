document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadProfile();
    setupLogout();
    setupForms();
});

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            if (confirm('Are you sure you want to logout?')) {
                localStorage.clear();
                window.location.href = '../auth/login.html';
            }
        });
    }
}

function setupForms() {
    const profileForm = document.getElementById('profileForm');
    const resetPasswordForm = document.getElementById('resetPasswordForm');

    if (profileForm) {
        profileForm.addEventListener('submit', handleUpdateProfile);
    }

    if (resetPasswordForm) {
        resetPasswordForm.addEventListener('submit', handleResetPassword);
    }
}

async function loadProfile() {
    try {
        const profile = await apiCall('/profile');
        document.getElementById('profileName').value = profile.name;
        document.getElementById('profileEmail').value = profile.email;
    } catch (error) {
        console.error('Error loading profile:', error);
    }
}

async function handleUpdateProfile(e) {
    e.preventDefault();
    const profileData = {
        name: document.getElementById('profileName').value,
    };

    try {
        await apiCall('/profile', 'PUT', profileData);
        localStorage.setItem('userName', profileData.name);
        showMessage('Profile updated successfully!', 'success');
    } catch (error) {
        showMessage('Error updating profile: ' + error.message, 'error');
    }
}

async function handleResetPassword(e) {
    e.preventDefault();
    const passwordData = {
        currentPassword: document.getElementById('currentPassword').value,
        newPassword: document.getElementById('newPassword').value,
    };

    try {
        await apiCall('/profile/reset-password', 'POST', passwordData);
        showMessage('Password reset successfully!', 'success');
        document.getElementById('resetPasswordForm').reset();
    } catch (error) {
        showMessage('Error resetting password: ' + error.message, 'error');
    }
}

async function importCSV() {
    const fileInput = document.getElementById('csvFile');
    const dataMessage = document.getElementById('dataMessage');

    if (!fileInput.files.length) {
        showMessage('Please select a CSV file', 'error');
        return;
    }

    const file = fileInput.files[0];

    if (!file.name.endsWith('.csv')) {
        showMessage('Please select a valid CSV file (.csv)', 'error');
        return;
    }

    try {
        // Read CSV file
        const text = await file.text();
        const rows = text.trim().split('\n');

        if (rows.length < 2) {
            showMessage('CSV file must have headers and at least one data row', 'error');
            return;
        }

        // Parse headers
        const headers = rows[0].split(',').map(h => h.trim().toLowerCase());
        
        // Validate required columns (symbol, quantity - currency is optional, defaults to USD)
        const requiredColumns = ['symbol', 'quantity'];
        const hasRequiredColumns = requiredColumns.every(col => headers.includes(col));
        
        if (!hasRequiredColumns) {
            showMessage(`CSV must have columns: ${requiredColumns.join(', ')}. Currency is optional (defaults to USD).`, 'error');
            return;
        }
        
        const hasCurrency = headers.includes('currency');
        if (!hasCurrency) {
            console.log('[DEBUG] CSV missing currency column, will default to USD');
        }

        // Parse data rows
        const assets = [];
        for (let i = 1; i < rows.length; i++) {
            const row = rows[i].split(',').map(cell => cell.trim());
            if (row.length < 2 || !row[0]) continue;

            const asset = {};
            headers.forEach((header, index) => {
                asset[header] = row[index] || '';
            });

            // Currency is optional - default to USD if not provided
            if (asset.symbol && asset.quantity) {
                assets.push({
                    symbol: asset.symbol.toUpperCase(),
                    quantity: parseFloat(asset.quantity),
                    buyingRate: asset.buyingrate ? parseFloat(asset.buyingrate) : null,
                    assetType: asset.assettype || 'STOCK',
                    purchaseDate: asset.purchasedate || new Date().toISOString().split('T')[0],
                    currency: (asset.currency || 'USD').toString().toUpperCase().substring(0, 3)
                });
            }
        }

        if (assets.length === 0) {
            showMessage('No valid assets found in CSV', 'error');
            return;
        }

        // Send to backend
        const result = await apiCall('/assets/import', 'POST', assets);
        showMessage(`Successfully imported ${result.count || assets.length} assets!`, 'success');
        fileInput.value = '';
    } catch (error) {
        showMessage('Error importing CSV: ' + error.message, 'error');
    }
}

async function exportCSV() {
    try {
        const response = await fetch(`${window.API_BASE_URL}/assets/export`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
            },
        });

        if (!response.ok) {
            throw new Error('Export failed');
        }

        const text = await response.text();
        const blob = new Blob([text], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        
        link.setAttribute('href', url);
        link.setAttribute('download', `portfolio_export_${new Date().getTime()}.csv`);
        link.style.visibility = 'hidden';
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        showMessage('CSV exported successfully!', 'success');
    } catch (error) {
        showMessage('Error exporting CSV: ' + error.message, 'error');
    }
}

function showMessage(message, type) {
    const messageDiv = document.getElementById('dataMessage');
    if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = `message ${type}`;
        setTimeout(() => {
            messageDiv.textContent = '';
            messageDiv.className = 'message';
        }, 5000);
    }
}

window.importCSV = importCSV;
window.exportCSV = exportCSV;
