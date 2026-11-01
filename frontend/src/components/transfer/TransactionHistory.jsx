import { useState, useEffect } from 'react';
import axios from 'axios';

const TransactionHistory = ({ onTransferSelect }) => {
    const [transfers, setTransfers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [maskingEnabled, setMaskingEnabled] = useState(false);
    
    // Filter states
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [typeFilter, setTypeFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [showFilters, setShowFilters] = useState(false);

    const fetchHistory = async (page = 0) => {
        setLoading(true);
        setError('');
        try {
            // Fetch masking status first
            const maskingResponse = await axios.get('/api/masking/status');
            setMaskingEnabled(maskingResponse.data.enabled);
            
            // Build query params
            const params = {
                page,
                size: 10
            };
            
            if (startDate) params.startDate = startDate;
            if (endDate) params.endDate = endDate;
            if (typeFilter) params.type = typeFilter;
            if (statusFilter) params.status = statusFilter;
            
            // Fetch transaction history
            const response = await axios.get('/api/transfers', { params });
            setTransfers(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch transaction history';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchHistory();
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchHistory(newPage);
        }
    };

    const handleApplyFilters = () => {
        fetchHistory(0);
    };

    const handleClearFilters = () => {
        setStartDate('');
        setEndDate('');
        setTypeFilter('');
        setStatusFilter('');
        // Trigger fetch with cleared filters
        setTimeout(() => fetchHistory(0), 0);
    };

    // Format counterparty info - masked if masking is enabled
    const formatCounterpartyInfo = (transfer) => {
        if (maskingEnabled) {
            return transfer.type === 'DEBIT' 
                ? 'To: ***'
                : 'From: ***';
        }
        
        // Use counterparty details from the response
        const isDebit = transfer.type === 'DEBIT';
        const counterpartyName = isDebit ? transfer.receiverName : transfer.senderName;
        const counterpartyEmail = isDebit ? transfer.receiverEmail : transfer.senderEmail;
        
        if (counterpartyName) {
            return isDebit 
                ? `To: ${counterpartyName}`
                : `From: ${counterpartyName}`;
        }
        
        return isDebit
            ? `To: ${transfer.receiverPhone || transfer.receiverId}`
            : `From: ${transfer.senderPhone || transfer.senderId}`;
    };

    return (
        <div className="transaction-history">
            <div className="transaction-header">
                <h3>Transaction History</h3>
                <div className="header-actions">
                    <button 
                        className="filter-toggle-btn"
                        onClick={() => setShowFilters(!showFilters)}
                    >
                        {showFilters ? '▼' : '▶'} Filters
                    </button>
                    {maskingEnabled && <span className="masking-indicator" title="Counterparty details are hidden">👤</span>}
                </div>
            </div>

            {/* Filters Section */}
            {showFilters && (
                <div className="filters-panel">
                    <div className="filter-group">
                        <label>Date Range:</label>
                        <div className="date-inputs">
                            <input
                                type="date"
                                value={startDate}
                                onChange={(e) => setStartDate(e.target.value)}
                                placeholder="Start Date"
                            />
                            <span>to</span>
                            <input
                                type="date"
                                value={endDate}
                                onChange={(e) => setEndDate(e.target.value)}
                                placeholder="End Date"
                            />
                        </div>
                    </div>
                    
                    <div className="filter-group">
                        <label>Type:</label>
                        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
                            <option value="">All</option>
                            <option value="DEBIT">Sent</option>
                            <option value="CREDIT">Received</option>
                        </select>
                    </div>
                    
                    <div className="filter-group">
                        <label>Status:</label>
                        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                            <option value="">All</option>
                            <option value="SUCCESS">Success</option>
                            <option value="PENDING">Pending</option>
                            <option value="FAILED">Failed</option>
                            <option value="DECLINED">Declined</option>
                        </select>
                    </div>
                    
                    <div className="filter-actions">
                        <button onClick={handleApplyFilters}>Apply Filters</button>
                        <button onClick={handleClearFilters} className="clear-btn">Clear</button>
                    </div>
                </div>
            )}

            {error && <div className="error">{error}</div>}
            {loading && <div className="loading">Loading transactions...</div>}

            {!error && !loading && transfers.length === 0 && (
                <p>No transactions yet. Make your first transfer!</p>
            )}

            {!error && !loading && transfers.length > 0 && (
                <>
                    <div className="transfers-list">
                        {transfers.map((transfer) => (
                            <div
                                key={transfer.id}
                                className={`transfer-item ${transfer.status}`}
                                onClick={() => onTransferSelect?.(transfer)}
                            >
                                <div className="transfer-amount">
                                    {transfer.type === 'DEBIT' ? '-' : '+'}₹{parseFloat(transfer.amount).toFixed(2)}
                                </div>
                                <div className="transfer-details">
                                    <div className="counterparty-info">
                                        {formatCounterpartyInfo(transfer)}
                                    </div>
                                    <div className="transfer-status-row">
                                        <span className={`status status-${transfer.status?.toLowerCase()}`}>
                                            {transfer.status}
                                        </span>
                                        <span className="date">
                                            {new Date(transfer.createdAt).toLocaleDateString()}
                                        </span>
                                        {transfer.fraudFlagged && (
                                            <span className="fraud-flagged" title="Fraud Flagged">⚠️</span>
                                        )}
                                    </div>
                                </div>
                                {transfer.status === 'PENDING' && (
                                    <div className="transfer-meta">
                                        <span>Settlement: {transfer.settlementDate ? new Date(transfer.settlementDate).toLocaleDateString() : '-'}</span>
                                        <span>Days Remaining: {transfer.daysUntilSettlement || 0}</span>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                    <div className="pagination">
                        <button
                            onClick={() => handlePageChange(page - 1)}
                            disabled={page === 0}
                        >
                            Previous
                        </button>
                        <span>Page {page + 1} of {totalPages}</span>
                        <button
                            onClick={() => handlePageChange(page + 1)}
                            disabled={page >= totalPages - 1}
                        >
                            Next
                        </button>
                    </div>
                </>
            )}
        </div>
    );
};

export default TransactionHistory;
