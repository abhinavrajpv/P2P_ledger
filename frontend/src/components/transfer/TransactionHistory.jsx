import { useState, useEffect } from 'react';
import axios from 'axios';

const TransactionHistory = ({ onTransferSelect }) => {
    const [transfers, setTransfers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [maskingEnabled, setMaskingEnabled] = useState(false);

    const fetchHistory = async (page = 0) => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            
            // Fetch masking status first
            const maskingResponse = await axios.get('/api/masking/status', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setMaskingEnabled(maskingResponse.data.enabled);
            
            // Fetch transaction history
            const response = await axios.get('/api/transfers', {
                headers: { Authorization: `Bearer ${token}` },
                params: { page, size: 10 }
            });
            setTransfers(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
        } catch (error) {
            setError('Failed to fetch transaction history');
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
                {maskingEnabled && <span className="masking-indicator" title="Counterparty details are hidden">👤</span>}
            </div>
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
                                    {transfer.type === 'DEBIT' ? '-' : '+'}₹{transfer.amount.toFixed(2)}
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
                                <div className="transfer-meta">
                                    <span>Settlement: {transfer.settlementDate ? new Date(transfer.settlementDate).toLocaleDateString() : '-'}</span>
                                    <span>Days Remaining: {transfer.daysUntilSettlement || 0}</span>
                                </div>
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
