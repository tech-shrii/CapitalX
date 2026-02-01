async function importCSV() {
    const fileInput = document.getElementById('csvFile');
    const dataMessage = document.getElementById('dataMessage');

    if (!fileInput.files.length) {
        dataMessage.textContent = 'Please select a CSV file';
        dataMessage.className = 'message error';
        return;
    }

    const file = fileInput.files[0];

    // Check file type
    if (!file.name.endsWith('.csv')) {
        dataMessage.textContent = 'Please select a valid CSV file (.csv)';
        dataMessage.className = 'message error';
        return;
    }

    try {
        // Read CSV file
        const text = await file.text();
        const rows = text.trim().split('\n');

        if (rows.length < 2) {
            dataMessage.textContent = 'CSV file must have headers and at least one data row';
            dataMessage.className = 'message error';
            return;
        }

        // Parse headers
        const headers = rows[0].split(',').map(h => h.trim().toLowerCase());
        
        // Validate required columns (symbol, quantity, currency)
        const requiredColumns = ['symbol', 'quantity', 'currency'];
        const hasRequiredColumns = requiredColumns.every(col => headers.includes(col));
        
        if (!hasRequiredColumns) {
            dataMessage.textContent = `CSV must have columns: ${requiredColumns.join(', ')}`;
            dataMessage.className = 'message error';
            return;
        }

        // Parse data rows
        const assets = [];
        for (let i = 1; i < rows.length; i++) {
            const row = rows[i].split(',').map(cell => cell.trim());
            if (row.length < 2 || !row[0]) continue; // Skip empty rows

            const asset = {};
            headers.forEach((header, index) => {
                asset[header] = row[index] || '';
            });

            // Validate asset data (currency required)
            if (asset.symbol && asset.quantity && asset.currency) {
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
            dataMessage.textContent = 'No valid assets found in CSV';
            dataMessage.className = 'message error';
            return;
        }

        // Send to backend
        const result = await apiCall('/assets/import', 'POST', assets);
        dataMessage.textContent = `Successfully imported ${result.count || assets.length} assets!`;
        dataMessage.className = 'message success';
        
        setTimeout(() => {
            window.location.href = 'home.html';
        }, 2000);
    } catch (error) {
        console.error('Import error:', error);
        dataMessage.textContent = `Error: ${error.message}`;
        dataMessage.className = 'message error';
    }
}

function skipImport() {
    window.location.href = 'home.html';
}
