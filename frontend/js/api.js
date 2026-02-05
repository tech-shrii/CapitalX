const API_BASE_URL = 'https://captialx.shreyas.in/api';

function getAuthToken() {
    return localStorage.getItem('token');
}

function getUserId() {
    return localStorage.getItem('userId');
}

async function apiCall(endpoint, method = 'GET', body = null) {
    const url = `${API_BASE_URL}${endpoint}`;
    console.log(`[API] ${method} ${url}`, body ? { body } : '');
    
    const options = {
        method,
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const token = getAuthToken();
    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
        console.log(`[API] Authorization header added`);
    } else {
        console.warn(`[API] No auth token found`);
    }

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        console.log(`[API] Sending request...`);
        const response = await fetch(url, options);
        console.log(`[API] Response status: ${response.status} ${response.statusText}`);
        console.log(`[API] Response headers:`, Object.fromEntries(response.headers.entries()));
        
        // Check content type before parsing JSON
        const contentType = response.headers.get('content-type');
        console.log(`[API] Response content-type: ${contentType}`);
        
        let data;
        if (contentType && contentType.includes('application/json')) {
            try {
                data = await response.json();
                console.log(`[API] Response data:`, data);
            } catch (jsonError) {
                console.error(`[API] JSON parse error:`, jsonError);
                const text = await response.text();
                console.error(`[API] Response text:`, text);
                throw new Error(`Invalid JSON response: ${text.substring(0, 200)}`);
            }
        } else {
            // Not JSON - read as text
            const text = await response.text();
            console.warn(`[API] Non-JSON response:`, text.substring(0, 500));
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${text.substring(0, 200)}`);
            }
            // Try to parse as JSON anyway
            try {
                data = JSON.parse(text);
            } catch {
                throw new Error(`Expected JSON but got: ${contentType || 'unknown'}`);
            }
        }

        if (!response.ok) {
            const errorMsg = data?.message || data?.error || `HTTP ${response.status}: ${response.statusText}`;
            console.error(`[API] Error response:`, errorMsg);
            throw new Error(errorMsg);
        }

        console.log(`[API] Request successful`);
        return data;
    } catch (error) {
        console.error(`[API] Request failed:`, error);
        console.error(`[API] Error stack:`, error.stack);
        throw error;
    }
}

// Make apiCall globally available
window.apiCall = apiCall;

async function apiCallFormData(endpoint, formData) {
    const url = `${API_BASE_URL}${endpoint}`;
    console.log(`[API] POST (FormData) ${url}`);
    
    const token = getAuthToken();
    
    const options = {
        method: 'POST',
        headers: {},
    };

    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
        console.log(`[API] Authorization header added`);
    } else {
        console.warn(`[API] No auth token found`);
    }

    // Don't set Content-Type for FormData - browser will set it with boundary
    options.body = formData;
    
    // Log file info if available
    if (formData instanceof FormData) {
        for (const [key, value] of formData.entries()) {
            if (value instanceof File) {
                console.log(`[API] FormData file: ${key} = ${value.name} (${value.size} bytes, type: ${value.type})`);
            } else {
                console.log(`[API] FormData field: ${key} = ${value}`);
            }
        }
    }

    try {
        console.log(`[API] Sending FormData request...`);
        const response = await fetch(url, options);
        console.log(`[API] Response status: ${response.status} ${response.statusText}`);
        console.log(`[API] Response headers:`, Object.fromEntries(response.headers.entries()));
        
        // Check content type before parsing JSON
        const contentType = response.headers.get('content-type');
        console.log(`[API] Response content-type: ${contentType}`);
        
        let data;
        if (contentType && contentType.includes('application/json')) {
            try {
                data = await response.json();
                console.log(`[API] Response data:`, data);
            } catch (jsonError) {
                console.error(`[API] JSON parse error:`, jsonError);
                const text = await response.text();
                console.error(`[API] Response text:`, text);
                throw new Error(`Invalid JSON response: ${text.substring(0, 200)}`);
            }
        } else {
            // Not JSON - read as text
            const text = await response.text();
            console.warn(`[API] Non-JSON response:`, text.substring(0, 500));
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${text.substring(0, 200)}`);
            }
            // Try to parse as JSON anyway
            try {
                data = JSON.parse(text);
            } catch {
                throw new Error(`Expected JSON but got: ${contentType || 'unknown'}`);
            }
        }

        if (!response.ok) {
            const errorMsg = data?.message || data?.error || `HTTP ${response.status}: ${response.statusText}`;
            console.error(`[API] Error response:`, errorMsg);
            throw new Error(errorMsg);
        }

        console.log(`[API] FormData request successful`);
        return data;
    } catch (error) {
        console.error(`[API] FormData request failed:`, error);
        console.error(`[API] Error stack:`, error.stack);
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
window.API_BASE_URL = API_BASE_URL;
