// Chatbot functionality
let conversationId = null;
let isChatbotInitialized = false;

/**
 * Initialize chatbot on page load
 */
function initializeChatbot() {
    if (isChatbotInitialized) {
        return;
    }

    const chatbotWidget = document.getElementById('chatbotWidget');
    const chatbotToggle = document.getElementById('chatbotToggle');
    const chatbotClose = document.getElementById('chatbotClose');
    const chatbotInput = document.getElementById('chatbotInput');
    const chatbotSend = document.getElementById('chatbotSend');
    const quickPromptBtns = document.querySelectorAll('.quick-prompt-btn');

    if (!chatbotWidget || !chatbotToggle || !chatbotInput || !chatbotSend) {
        console.warn('[Chatbot] Required elements not found');
        return;
    }

    // Load greeting immediately when widget is open on page load
    if (typeof loadGreeting === 'function') {
        loadGreeting();
    }

    // Open chatbot when bubble button is clicked
    chatbotToggle.addEventListener('click', (e) => {
        e.stopPropagation();
        chatbotWidget.classList.remove('collapsed');
    });

    // Close chatbot when close button is clicked
    if (chatbotClose) {
        chatbotClose.addEventListener('click', (e) => {
            e.stopPropagation();
            chatbotWidget.classList.add('collapsed');
        });
    }

    // Close chatbot when clicking outside (optional - disabled for better UX)
    // Uncomment below if you want click-outside-to-close functionality
    /*
    document.addEventListener('click', (e) => {
        if (!chatbotWidget.contains(e.target) && !chatbotWidget.classList.contains('collapsed')) {
            chatbotWidget.classList.add('collapsed');
        }
    });
    */

    // Send message on Enter key
    chatbotInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Send message on button click
    chatbotSend.addEventListener('click', () => {
        sendMessage();
    });

    // Quick prompt buttons
    quickPromptBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const promptType = btn.getAttribute('data-prompt');
            handleQuickPrompt(promptType);
        });
    });

    isChatbotInitialized = true;
    console.log('[Chatbot] Initialized successfully');
}

/**
 * Send user message to chatbot
 */
async function sendMessage() {
    const chatbotInput = document.getElementById('chatbotInput');
    const chatbotSend = document.getElementById('chatbotSend');
    const message = chatbotInput.value.trim();

    if (!message) {
        return;
    }

    // Clear input
    chatbotInput.value = '';
    chatbotSend.disabled = true;

    // Display user message
    displayMessage('user', message);

    // Show loading indicator
    showLoadingIndicator();

    try {
        const response = await apiCall('/chatbot/chat', 'POST', {
            message: message,
            conversationId: conversationId
        });

        // Hide loading indicator
        hideLoadingIndicator();

        // Update conversation ID if provided
        if (response.conversationId) {
            conversationId = response.conversationId;
        }

        // Display assistant response
        displayMessage('assistant', response.response);

        chatbotSend.disabled = false;
    } catch (error) {
        console.error('[Chatbot] Error sending message:', error);
        hideLoadingIndicator();
        displayError('Failed to send message. Please try again.');
        chatbotSend.disabled = false;
    }
}

/**
 * Display a message in the chat
 */
function displayMessage(role, content) {
    const chatbotMessages = document.getElementById('chatbotMessages');
    if (!chatbotMessages) {
        return;
    }

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${role}`;

    const bubbleDiv = document.createElement('div');
    bubbleDiv.className = 'message-bubble';

    // If it's markdown content, parse it
    if (role === 'assistant') {
        bubbleDiv.innerHTML = parseMarkdown(content);
    } else {
        bubbleDiv.textContent = content;
    }

    const timestampDiv = document.createElement('div');
    timestampDiv.className = 'message-timestamp';
    timestampDiv.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    messageDiv.appendChild(bubbleDiv);
    messageDiv.appendChild(timestampDiv);
    chatbotMessages.appendChild(messageDiv);

    // Scroll to bottom
    chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
}

/**
 * Simple markdown parser for basic formatting
 */
function parseMarkdown(text) {
    if (!text) return '';

    let html = text;

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold
    html = html.replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>');

    // Italic
    html = html.replace(/\*(.*?)\*/gim, '<em>$1</em>');

    // Lists
    html = html.replace(/^\* (.*$)/gim, '<li>$1</li>');
    html = html.replace(/^- (.*$)/gim, '<li>$1</li>');
    html = html.replace(/^(\d+)\. (.*$)/gim, '<li>$2</li>');

    // Wrap consecutive list items in ul
    html = html.replace(/(<li>.*<\/li>\n?)+/gim, '<ul>$&</ul>');

    // Line breaks
    html = html.replace(/\n/gim, '<br>');

    return html;
}

/**
 * Show loading indicator
 */
function showLoadingIndicator() {
    const chatbotMessages = document.getElementById('chatbotMessages');
    if (!chatbotMessages) {
        return;
    }

    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'loading-indicator';
    loadingDiv.id = 'chatbotLoading';
    loadingDiv.innerHTML = `
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
        <div class="loading-dot"></div>
    `;
    chatbotMessages.appendChild(loadingDiv);
    chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
}

/**
 * Hide loading indicator
 */
function hideLoadingIndicator() {
    const loadingDiv = document.getElementById('chatbotLoading');
    if (loadingDiv) {
        loadingDiv.remove();
    }
}

/**
 * Display error message
 */
function displayError(message) {
    const chatbotMessages = document.getElementById('chatbotMessages');
    if (!chatbotMessages) {
        return;
    }

    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    chatbotMessages.appendChild(errorDiv);
    chatbotMessages.scrollTop = chatbotMessages.scrollHeight;

    // Remove error after 5 seconds
    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

/**
 * Load greeting message on login
 */
async function loadGreeting() {
    try {
        showLoadingIndicator();
        const response = await apiCall('/chatbot/greeting', 'POST');
        hideLoadingIndicator();

        if (response.conversationId) {
            conversationId = response.conversationId;
        }

        displayMessage('assistant', response.response);
    } catch (error) {
        console.error('[Chatbot] Error loading greeting:', error);
        hideLoadingIndicator();
        displayMessage('assistant', 'Welcome to CapitalX! I\'m your investment expert assistant. How can I help you today?');
    }
}

/**
 * Handle quick prompt buttons
 */
async function handleQuickPrompt(promptType) {
    const chatbotInput = document.getElementById('chatbotInput');
    const chatbotSend = document.getElementById('chatbotSend');

    let message = '';
    let endpoint = '/chatbot/chat';

    switch (promptType) {
        case 'analyze':
            endpoint = '/chatbot/analysis';
            break;
        case 'suggestions':
            message = 'Provide investment suggestions: rebalancing recommendations, new investment opportunities, assets to consider selling, and diversification opportunities.';
            break;
        case 'risk':
            message = 'Assess the portfolio risk: overall risk level, risk factors identified, concentration risks, and recommendations to reduce risk.';
            break;
        default:
            return;
    }

    chatbotSend.disabled = true;

    if (message) {
        // Display user message
        displayMessage('user', message);
    }

    showLoadingIndicator();

    try {
        let response;
        if (endpoint === '/chatbot/analysis') {
            response = await apiCall(endpoint, 'POST');
        } else {
            response = await apiCall(endpoint, 'POST', {
                message: message,
                conversationId: conversationId
            });
        }

        hideLoadingIndicator();

        if (response.conversationId) {
            conversationId = response.conversationId;
        }

        displayMessage('assistant', response.response);
        chatbotSend.disabled = false;
    } catch (error) {
        console.error('[Chatbot] Error handling quick prompt:', error);
        hideLoadingIndicator();
        displayError('Failed to process request. Please try again.');
        chatbotSend.disabled = false;
    }
}

// Initialize chatbot when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeChatbot);
} else {
    initializeChatbot();
}

// Make functions available globally for integration
window.initializeChatbot = initializeChatbot;
window.loadChatbotGreeting = loadGreeting;
