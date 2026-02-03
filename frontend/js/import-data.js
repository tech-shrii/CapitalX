document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    initializeStepLogic();
});

let currentStep = 1;
let currentClient = null;

function checkAuth() {
    if (!localStorage.getItem('token')) {
        window.location.href = '../auth/login.html';
    }
}

function initializeStepLogic() {
    const addClientForm = document.getElementById('addClientForm');
    addClientForm.addEventListener('submit', handleAddClient);

    // Initial state
    goToStep(1);
}

function goToStep(stepNumber) {
    // Hide all steps
    document.getElementById('step-1').classList.add('hidden');
    document.getElementById('step-2').classList.add('hidden');
    document.getElementById('step-3').classList.add('hidden');

    // Deactivate all step indicators
    document.getElementById('step-1-indicator').classList.remove('active');
    document.getElementById('step-2-indicator').classList.remove('active');
    document.getElementById('step-3-indicator').classList.remove('active');

    // Show the current step and activate the indicator
    const currentStepElement = document.getElementById(`step-${stepNumber}`);
    const currentStepIndicator = document.getElementById(`step-${stepNumber}-indicator`);
    
    if (currentStepElement) {
        currentStepElement.classList.remove('hidden');
    }
    if (currentStepIndicator) {
        currentStepIndicator.classList.add('active');
    }
    
    currentStep = stepNumber;
}

async function handleAddClient(e) {
    e.preventDefault();
    const clientData = {
        name: document.getElementById('clientName').value,
        email: document.getElementById('clientEmail').value,
        phone: document.getElementById('clientPhone').value,
        currency: document.getElementById('clientCurrency').value,
    };

    try {
        const newClient = await apiCall('/clients', 'POST', clientData);
        currentClient = newClient;
        document.getElementById('csvClientName').textContent = newClient.name;
        goToStep(2);
    } catch (error) {
        alert('Error adding client: ' + error.message);
    }
}

async function importCSV() {
    if (!currentClient) {
        alert('No client selected for import.');
        return;
    }

    const fileInput = document.getElementById('csvFile');
    const messageDiv = document.getElementById('dataMessage');

    if (fileInput.files.length === 0) {
        messageDiv.textContent = 'Please select a CSV file.';
        messageDiv.className = 'message error';
        return;
    }

    const file = fileInput.files[0];
    const formData = new FormData();
    formData.append('file', file);

    try {
        const result = await apiCallFormData(`/clients/${currentClient.id}/assets/import-csv`, formData);
        messageDiv.textContent = `Successfully imported ${result.count || 0} assets!`;
        messageDiv.className = 'message success';
        
        // Wait a bit so the user can see the message, then move to the next step
        setTimeout(() => {
            goToStep(3);
        }, 1500);

    } catch (error) {
        messageDiv.textContent = 'Error importing CSV: ' + error.message;
        messageDiv.className = 'message error';
    }
}

function skipToDashboard() {
    window.location.href = 'home.html';
}

// Make functions globally available for onclick handlers
window.goToStep = goToStep;
window.importCSV = importCSV;
window.skipToDashboard = skipToDashboard;