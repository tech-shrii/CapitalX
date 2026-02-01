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
            localStorage.clear();
            window.location.href = '../auth/login.html';
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

async function importExcel() {
    const fileInput = document.getElementById('excelFile');
    if (!fileInput.files.length) {
        showMessage('Please select a file', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        await apiCallFormData('/excel/import', formData);
        showMessage('Excel imported successfully!', 'success');
        fileInput.value = '';
    } catch (error) {
        showMessage('Error importing Excel: ' + error.message, 'error');
    }
}

async function exportExcel() {
    try {
        const token = getAuthToken();
        const response = await fetch(`${API_BASE_URL}/excel/export`, {
            headers: {
                'Authorization': `Bearer ${token}`,
            },
        });

        if (!response.ok) {
            throw new Error('Export failed');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'portfolio_export.xlsx';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        
        showMessage('Excel exported successfully!', 'success');
    } catch (error) {
        showMessage('Error exporting Excel: ' + error.message, 'error');
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

window.importExcel = importExcel;
window.exportExcel = exportExcel;
