import { useState, useEffect } from 'react';
import axios from '../../axiosConfig';

const MiniStatement = () => {
    const [transfers, setTransfers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [maskingEnabled, setMaskingEnabled] = useState(false);

    const fetchMiniStatement = async () => {
        setLoading(true);
        setError('');
        try {
            // Fetch masking status first
            const maskingResponse = await axios.get('/api/masking/status');
            setMaskingEnabled(maskingResponse.data.enabled);
            
            // Fetch last 10 transactions
            const response = await axios.get('/api/transfers', {
                params: { page: 0, size: 10 }
            });
            setTransfers(response.data.content || []);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch mini statement';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchMiniStatement();
    }, []);

    // Format counterparty info - masked if masking is enabled
    const formatCounterpartyInfo = (transfer) => {
        if (maskingEnabled) {
            return transfer.type === 'DEBIT' ? '***' : '***';
        }
        
        const isDebit = transfer.type === 'DEBIT';
        const counterpartyName = isDebit ? transfer.receiverName : transfer.senderName;
        
        if (counterpartyName) {
            return counterpartyName;
        }
        
        return isDebit
            ? (transfer.receiverPhone || `User ${transfer.receiverId}`)
            : (transfer.senderPhone || `User ${transfer.senderId}`);
    };

    return (
        <div className="mini-statement">
            <div className="mini-statement-header">
                <h3>Mini Statement</h3>
                <span className="subtitle">Last 10 Transactions</span>
                {maskingEnabled && <span className="masking-indicator" title="Counterparty details are hidden">👤</span>}
            </div>

            {error && <div className="error">{error}</div>}
            {loading && <div className="loading">Loading...</div>}

            {!error && !loading && transfers.length === 0 && (
                <p className="no-data">No transactions yet</p>
            )}

            {!error && !loading && transfers.length > 0 && (
                <div className="mini-statement-list">
                    {transfers.map((transfer) => (
                        <div key={transfer.id} className={`mini-item ${transfer.status?.toLowerCase()}`}>
                            <div className="mini-item-left">
                                <span className={`mini-amount ${transfer.type === 'DEBIT' ? 'negative' : 'positive'}`}>
                                    {transfer.type === 'DEBIT' ? '-' : '+'}₹{parseFloat(transfer.amount).toFixed(2)}
                                </span>
                                <span className="mini-counterparty">
                                    {transfer.type === 'DEBIT' ? 'To: ' : 'From: '}
                                    {formatCounterpartyInfo(transfer)}
                                </span>
                            </div>
                            <div className="mini-item-right">
                                <span className={`mini-status status-${transfer.status?.toLowerCase()}`}>
                                    {transfer.status}
                                </span>
                                <span className="mini-date">
                                    {new Date(transfer.createdAt).toLocaleDateString('en-IN', {
                                        day: '2-digit',
                                        month: 'short'
                                    })}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <button onClick={fetchMiniStatement} disabled={loading} className="refresh-btn">
                {loading ? 'Loading...' : 'Refresh'}
            </button>
        </div>
    );
};

export default MiniStatement;
