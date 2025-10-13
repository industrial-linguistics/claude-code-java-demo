// API Configuration
const API_BASE = '/api/trades';
const AUTH_HEADER = 'Basic ' + btoa('admin:changeme');

let allTrades = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    loadTrades();
    setupFormHandlers();
    setupSearch();
    setDefaultDates();
});

// Set default dates to today and T+2
function setDefaultDates() {
    const today = new Date();
    const valueDate = new Date(today);
    valueDate.setDate(valueDate.getDate() + 2);

    document.getElementById('tradeDate').valueAsDate = today;
    document.getElementById('valueDate').valueAsDate = valueDate;
}

// Setup form handlers
function setupFormHandlers() {
    document.getElementById('tradeForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        await createTrade();
    });
}

// Setup search functionality
function setupSearch() {
    document.getElementById('searchInput').addEventListener('input', function(e) {
        filterTrades(e.target.value);
    });
}

// Load all trades
async function loadTrades() {
    try {
        const response = await fetch(API_BASE, {
            headers: {
                'Authorization': AUTH_HEADER
            }
        });

        if (!response.ok) throw new Error('Failed to load trades');

        allTrades = await response.json();
        displayTrades(allTrades);
    } catch (error) {
        showNotification('Error loading trades: ' + error.message, 'danger');
    }
}

// Display trades in table
function displayTrades(trades) {
    const tbody = document.getElementById('tradesTableBody');

    if (trades.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">No trades found</td></tr>';
        return;
    }

    tbody.innerHTML = trades.map(trade => `
        <tr onclick="viewTradeDetails(${trade.id})">
            <td><strong>${trade.tradeReference}</strong></td>
            <td>${formatDate(trade.tradeDate)}</td>
            <td><span class="direction-${trade.direction}">${trade.direction}</span></td>
            <td>${trade.baseCurrency}/${trade.quoteCurrency}</td>
            <td class="text-end">${formatNumber(trade.baseAmount)}</td>
            <td class="text-end">${formatRate(trade.exchangeRate)}</td>
            <td>${trade.counterparty || '-'}</td>
            <td><span class="badge status-${trade.status}">${trade.status}</span></td>
            <td onclick="event.stopPropagation()">
                <button class="btn btn-sm btn-outline-primary btn-action" onclick="viewTradeDetails(${trade.id})">
                    View
                </button>
                <button class="btn btn-sm btn-outline-secondary btn-action" onclick="viewAuditHistory(${trade.id})">
                    Audit
                </button>
            </td>
        </tr>
    `).join('');
}

// Filter trades based on search input
function filterTrades(searchTerm) {
    if (!searchTerm) {
        displayTrades(allTrades);
        return;
    }

    searchTerm = searchTerm.toLowerCase();
    const filtered = allTrades.filter(trade =>
        trade.tradeReference.toLowerCase().includes(searchTerm) ||
        trade.baseCurrency.toLowerCase().includes(searchTerm) ||
        trade.quoteCurrency.toLowerCase().includes(searchTerm) ||
        (trade.counterparty && trade.counterparty.toLowerCase().includes(searchTerm)) ||
        (trade.trader && trade.trader.toLowerCase().includes(searchTerm)) ||
        trade.status.toLowerCase().includes(searchTerm)
    );

    displayTrades(filtered);
}

// Create new trade
async function createTrade() {
    const trade = {
        tradeDate: document.getElementById('tradeDate').value,
        valueDate: document.getElementById('valueDate').value,
        direction: document.getElementById('direction').value,
        baseCurrency: document.getElementById('baseCurrency').value,
        quoteCurrency: document.getElementById('quoteCurrency').value,
        baseAmount: parseFloat(document.getElementById('baseAmount').value),
        exchangeRate: parseFloat(document.getElementById('exchangeRate').value),
        counterparty: document.getElementById('counterparty').value || null,
        trader: document.getElementById('trader').value || null,
        notes: document.getElementById('notes').value || null
    };

    try {
        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {
                'Authorization': AUTH_HEADER,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(trade)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create trade');
        }

        const createdTrade = await response.json();
        showNotification(`Trade ${createdTrade.tradeReference} created successfully!`, 'success');

        // Reset form
        document.getElementById('tradeForm').reset();
        setDefaultDates();

        // Reload trades
        await loadTrades();

    } catch (error) {
        showNotification('Error creating trade: ' + error.message, 'danger');
    }
}

// View trade details
async function viewTradeDetails(tradeId) {
    try {
        const response = await fetch(`${API_BASE}/${tradeId}`, {
            headers: {
                'Authorization': AUTH_HEADER
            }
        });

        if (!response.ok) throw new Error('Failed to load trade details');

        const trade = await response.json();
        displayTradeDetails(trade);

        const modal = new bootstrap.Modal(document.getElementById('tradeModal'));
        modal.show();

    } catch (error) {
        showNotification('Error loading trade details: ' + error.message, 'danger');
    }
}

// Display trade details in modal
function displayTradeDetails(trade) {
    const detailsDiv = document.getElementById('tradeDetails');

    detailsDiv.innerHTML = `
        <div class="trade-detail-grid">
            <div class="trade-detail-item">
                <div class="trade-detail-label">Trade Reference</div>
                <div class="trade-detail-value"><strong>${trade.tradeReference}</strong></div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Status</div>
                <div class="trade-detail-value">
                    <span class="badge status-${trade.status}">${trade.status}</span>
                </div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Direction</div>
                <div class="trade-detail-value">
                    <span class="direction-${trade.direction}">${trade.direction}</span>
                </div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Currency Pair</div>
                <div class="trade-detail-value">${trade.baseCurrency}/${trade.quoteCurrency}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Trade Date</div>
                <div class="trade-detail-value">${formatDate(trade.tradeDate)}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Value Date</div>
                <div class="trade-detail-value">${formatDate(trade.valueDate)}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Base Amount</div>
                <div class="trade-detail-value">${formatNumber(trade.baseAmount)} ${trade.baseCurrency}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Exchange Rate</div>
                <div class="trade-detail-value">${formatRate(trade.exchangeRate)}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Quote Amount</div>
                <div class="trade-detail-value">${formatNumber(trade.quoteAmount)} ${trade.quoteCurrency}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Counterparty</div>
                <div class="trade-detail-value">${trade.counterparty || '-'}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Trader</div>
                <div class="trade-detail-value">${trade.trader || '-'}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Created By</div>
                <div class="trade-detail-value">${trade.createdBy} at ${formatDateTime(trade.createdAt)}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Updated By</div>
                <div class="trade-detail-value">${trade.updatedBy} at ${formatDateTime(trade.updatedAt)}</div>
            </div>
            <div class="trade-detail-item">
                <div class="trade-detail-label">Version</div>
                <div class="trade-detail-value">${trade.version}</div>
            </div>
        </div>

        ${trade.notes ? `
            <div class="mt-3">
                <div class="trade-detail-label">Notes</div>
                <div class="alert alert-info">${trade.notes}</div>
            </div>
        ` : ''}

        <div class="mt-4">
            <h6>Update Status</h6>
            <div class="btn-group" role="group">
                <button class="btn btn-warning ${trade.status === 'PENDING' ? 'active' : ''}"
                        onclick="updateTradeStatus(${trade.id}, 'PENDING')">PENDING</button>
                <button class="btn btn-info ${trade.status === 'CONFIRMED' ? 'active' : ''}"
                        onclick="updateTradeStatus(${trade.id}, 'CONFIRMED')">CONFIRMED</button>
                <button class="btn btn-success ${trade.status === 'SETTLED' ? 'active' : ''}"
                        onclick="updateTradeStatus(${trade.id}, 'SETTLED')">SETTLED</button>
            </div>
        </div>
    `;
}

// Update trade status
async function updateTradeStatus(tradeId, newStatus) {
    try {
        const response = await fetch(`${API_BASE}/${tradeId}`, {
            method: 'PUT',
            headers: {
                'Authorization': AUTH_HEADER,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status: newStatus })
        });

        if (!response.ok) throw new Error('Failed to update trade status');

        showNotification('Trade status updated successfully!', 'success');

        // Close modal and reload
        bootstrap.Modal.getInstance(document.getElementById('tradeModal')).hide();
        await loadTrades();

    } catch (error) {
        showNotification('Error updating trade status: ' + error.message, 'danger');
    }
}

// View audit history
async function viewAuditHistory(tradeId) {
    try {
        const response = await fetch(`${API_BASE}/${tradeId}/audit`, {
            headers: {
                'Authorization': AUTH_HEADER
            }
        });

        if (!response.ok) throw new Error('Failed to load audit history');

        const audits = await response.json();
        displayAuditHistory(audits);

        const modal = new bootstrap.Modal(document.getElementById('auditModal'));
        modal.show();

    } catch (error) {
        showNotification('Error loading audit history: ' + error.message, 'danger');
    }
}

// Display audit history in modal
function displayAuditHistory(audits) {
    const auditDiv = document.getElementById('auditHistory');

    if (audits.length === 0) {
        auditDiv.innerHTML = '<p class="text-muted">No audit records found</p>';
        return;
    }

    auditDiv.innerHTML = audits.map(audit => `
        <div class="audit-entry audit-${audit.action}">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <strong>${audit.action}</strong>
                    <span class="text-muted ms-2">${audit.tradeReference}</span>
                </div>
                <small class="text-muted">${formatDateTime(audit.auditTimestamp)}</small>
            </div>
            <div class="text-muted small mt-1">
                User: ${audit.auditUser}
            </div>
            ${audit.beforeSnapshot ? `
                <div class="mt-2">
                    <strong class="small">Before:</strong>
                    <div class="json-display">${formatJson(audit.beforeSnapshot)}</div>
                </div>
            ` : ''}
            ${audit.afterSnapshot ? `
                <div class="mt-2">
                    <strong class="small">After:</strong>
                    <div class="json-display">${formatJson(audit.afterSnapshot)}</div>
                </div>
            ` : ''}
        </div>
    `).join('');
}

// Formatting helpers
function formatDate(dateStr) {
    return new Date(dateStr).toLocaleDateString('en-AU', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

function formatDateTime(dateTimeStr) {
    return new Date(dateTimeStr).toLocaleString('en-AU', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function formatNumber(num) {
    return new Intl.NumberFormat('en-AU', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(num);
}

function formatRate(rate) {
    return new Intl.NumberFormat('en-AU', {
        minimumFractionDigits: 4,
        maximumFractionDigits: 6
    }).format(rate);
}

function formatJson(jsonStr) {
    try {
        const obj = JSON.parse(jsonStr);
        return JSON.stringify(obj, null, 2);
    } catch {
        return jsonStr;
    }
}

// Show notification toast
function showNotification(message, type = 'info') {
    const toast = document.getElementById('notificationToast');
    const toastBody = document.getElementById('toastMessage');

    toastBody.textContent = message;
    toast.className = `toast bg-${type} text-white`;

    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
}
