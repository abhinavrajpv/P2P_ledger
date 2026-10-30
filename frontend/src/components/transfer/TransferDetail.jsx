import { useState, useEffect } from 'react';
import axios from 'axios';

const TransferDetail = ({ transferId, onBack }) => {
    const [transfer, setTransfer] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [maskingEnabled, setMaskingEnabled] = useState(false);

    useEffect(() => {
        if (transferId) {
            fetchTransfer();
            fetchMaskingStatus();
        }
    }, [transferId]);

    const fetchMaskingStatus = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/masking/status', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setMaskingEnabled(response.data.enabled);
        } catch (error) {
            // Silently fail - masking status not critical
        }
    };

    const fetchTransfer = async () => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get(`/api/transfers/${transferId}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            setTransfer(response.data);
        } catch (error) {
            setError('Failed to fetch transfer details');
        } finally {
            setLoading(false);
        }
    };

    const formatAmount = (amount) => {
        return `₹${parseFloat(amount).toFixed(2)}`;
    };

    // Format counterparty info - masked if masking is enabled
    const formatCounterpartyInfo = (transfer) => {
        const isSender = transfer.senderId.toString() === localStorage.getItem('userId');
        
        if (maskingEnabled) {
            return isSender 
                ? 'Counterparty: *** (masked)'
                : 'Counterparty: *** (masked)';
        }

        if (isSender) {
            // User is sender - show receiver info
            if (transfer.receiverName) {
                return `Receiver: ${transfer.receiverName} (${transfer.receiverEmail || transfer.receiverPhone})`;
            }
            return `Receiver: ${transfer.receiverId}`;
        } else {
            // User is receiver - show sender info
            if (transfer.senderName) {
                return `Sender: ${transfer.senderName} (${transfer.senderEmail || transfer.senderPhone})`;
            }
            return `Sender: ${transfer.senderId}`;
        }
    };

    if (error) {
        return (
            <div className="transfer-detail">
                <h3>Transfer Detail</h3>
                <div className="error">{error}</div>
                {onBack && <button onClick={onBack}>Back to History</button>}
            </div>
        );
    }

    if (!transfer && !loading) {
        return <div>No transfer selected</div>;
    }

    if (loading) {
        return <div className="loading">Loading transfer details...</div>;
    }

    return (
        <div className="transfer-detail">
            <div className="transfer-header">
                <h3>Transfer Detail</h3>
                <div className="masking-indicator-container">
                    {maskingEnabled && <span className="masking-indicator" title="Counterparty details are hidden">👤</span>}
                </div>
            </div>
            <div className="transfer-header">
                <div className="header-row">
                    <span className="label">Transfer ID:</span>
                    <span className="value">{transfer.id}</span>
                </div>
                <div className="header-row">
                    <span className="label">Amount:</span>
                    <span className="value">{formatAmount(transfer.amount)}</span>
                </div>
                <div className="header-row">
                    <span className="label">Status:</span>
                    <span className={`status status-${transfer.status?.toLowerCase()}`}>
                        {transfer.status}
                    </span>
                </div>
                <div className="header-row">
                    <span className="label">Request ID:</span>
                    <span className="value">{transfer.requestId}</span>
                </div>
                <div className="header-row">
                    <span className="label">Created:</span>
                    <span className="value">{new Date(transfer.createdAt).toLocaleString()}</span>
                </div>
                <div className="header-row">
                    <span className="label">Settlement Date:</span>
                    <span className="value">{transfer.settlementDate ? new Date(transfer.settlementDate).toLocaleString() : '-'}</span>
                </div>
                {transfer.fraudFlagged && (
                    <div className="header-row">
                        <span className="label">Fraud Flagged:</span>
                        <span className="value">Yes</span>
                    </div>
                )}
                {transfer.adminReviewStatus && (
                    <div className="header-row">
                        <span className="label">Admin Review:</span>
                        <span className="value">{transfer.adminReviewStatus}</span>
                    </div>
                )}
                <div className="header-row">
                    <span className="label">Counterparty:</span>
                    <span className="value">{formatCounterpartyInfo(transfer)}</span>
                </div>
            </div>

            <div className="ledger-section">
                <h4>Ledger Entries</h4>
                {transfer.ledgerEntries && transfer.ledgerEntries.length > 0 ? (
                    <div className="ledger-list">
                        {transfer.ledgerEntries.map((entry) => (
                            <div key={entry.id} className="ledger-item">
                                <div className="ledger-row">
                                    <span className="type">{entry.type}</span>
                                    <span className={`amount ${entry.type?.toLowerCase()}`}>
                                        {entry.type === 'DEBIT' ? '-' : '+'}₹{entry.amount.toFixed(2)}
                                    </span>
                                </div>
                                <div className="ledger-balance">
                                    <span className="balance-before">Balance Before: ₹{entry.balanceBefore.toFixed(2)}</span>
                                    <span className="balance-after">Balance After: ₹{entry.balanceAfter.toFixed(2)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p>No ledger entries available</p>
                )}
            </div>

            {onBack && (
                <button onClick={onBack} style={{ marginTop: '15px' }}>
                    Back to Transaction History
                </button>
            )}
        </div>
    );
};

export default TransferDetail;
