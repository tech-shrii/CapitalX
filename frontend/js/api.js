const API_BASE_URL = 'http://localhost:8080/api';

function getAuthToken() {
    return localStorage.getItem('token');
}

function getUserId() {
    return localStorage.getItem('userId');
}

async function apiCall(endpoint, method = 'GET', body = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const token = getAuthToken();
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(url, options);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'An error occurred');
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Make apiCall globally available
window.apiCall = apiCall;

async function apiCallFormData(endpoint, formData) {
    const url = `${API_BASE_URL}${endpoint}`;
    const token = getAuthToken();
    
    const options = {
        method: 'POST',
        headers: {},
    };

    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }

    options.body = formData;

    try {
        const response = await fetch(url, options);
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'An error occurred');
        }

        return data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Make apiCallFormData globally available
window.apiCallFormData = apiCallFormData;

function redirectToLogin() {
    window.location.href = '../auth/login.html';
}

function checkAuth() {
    const token = getAuthToken();
    if (!token) {
        redirectToLogin();
    }
}

// Make utility functions globally available
window.getAuthToken = getAuthToken;
window.getUserId = getUserId;
window.redirectToLogin = redirectToLogin;
window.checkAuth = checkAuth;
