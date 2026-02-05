let currentEmail = '';

document.addEventListener('DOMContentLoaded', () => {
    const signupForm = document.getElementById('signupForm');
    const loginForm = document.getElementById('loginForm');
    const otpForm = document.getElementById('otpForm');
    const logoutBtn = document.getElementById('logoutBtn');

    if (signupForm) {
        signupForm.addEventListener('submit', handleSignup);
    }

    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    if (otpForm) {
        otpForm.addEventListener('submit', handleOtpVerification);
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }
});

async function handleSignup(e) {
    e.preventDefault();
    const formData = {
        name: document.getElementById('name').value,
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
        confirmPassword: document.getElementById('confirmPassword').value,
    };

    if (formData.password !== formData.confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    try {
        await apiCall('/auth/signup', 'POST', formData);
        currentEmail = formData.email;
        document.getElementById('signupSection').classList.add('hidden');
        document.getElementById('otpSection').classList.remove('hidden');
        showMessage('OTP sent to your email', 'success');
    } catch (error) {
        showError(error.message);
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const formData = {
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
    };

    try {
        await apiCall('/auth/login', 'POST', formData);
        currentEmail = formData.email;
        document.getElementById('loginSection').classList.add('hidden');
        document.getElementById('otpSection').classList.remove('hidden');
        showMessage('OTP sent to your email', 'success');
    } catch (error) {
        showError(error.message);
    }
}

async function handleOtpVerification(e) {
    e.preventDefault();
    const otp = document.getElementById('otp').value;
    const isSignup = window.location.pathname.includes('signup');

    try {
        const response = await apiCall(
            isSignup ? '/auth/verify-signup-otp' : '/auth/verify-login-otp',
            'POST',
            { email: currentEmail, otp }
        );

        localStorage.setItem('token', response.token);
        localStorage.setItem('userId', response.userId);
        localStorage.setItem('userName', response.name);
        localStorage.setItem('userEmail', response.email);

        if (isSignup) {
            window.location.href = '../dashboard/import-data.html';
        } else {
            window.location.href = '../dashboard/home.html';
        }
    } catch (error) {
        showError(error.message);
    }
}

function handleLogout() {
    localStorage.clear();
    window.location.href = '../auth/login.html';
}

function showError(message) {
    const messageDiv = document.getElementById('message');
    if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = 'message error';
        messageDiv.classList.remove('hidden');
    }
}

function showMessage(message, type = 'success') {
    const messageDiv = document.getElementById('message');
    if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = `message ${type}`;
        messageDiv.classList.remove('hidden');
    }
}
