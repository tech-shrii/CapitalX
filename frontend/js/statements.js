document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    loadClients();
    setupLogout();
    setupStatementForm();
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

function setupStatementForm() {
    const form = document.getElementById('statementForm');
    if (form) {
        form.addEventListener('submit', handleGenerateStatement);
    }
}

async function loadClients() {
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
        console.error('Error loading clients:', error);
    }
}

async function handleGenerateStatement(e) {
    e.preventDefault();
    
    const clientId = document.getElementById('statementClientId').value;
    const statementType = document.getElementById('statementType').value;
    const emailTo = document.getElementById('statementEmail').value;

    if (!clientId) {
        showMessage('Please select a client', 'error');
        return;
    }

    const statementData = {
        clientId: parseInt(clientId),
        statementType: statementType,
    };

    if (emailTo) {
        statementData.emailTo = emailTo;
    }

    try {
        await apiCall('/statements/generate', 'POST', statementData);
        showMessage('Statement generated and sent successfully!', 'success');
        document.getElementById('statementForm').reset();
    } catch (error) {
        showMessage('Error generating statement: ' + error.message, 'error');
    }
}

function showMessage(message, type) {
    const messageDiv = document.getElementById('statementMessage');
    if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = `message ${type}`;
        setTimeout(() => {
            messageDiv.textContent = '';
            messageDiv.className = 'message';
        }, 5000);
    }
}
