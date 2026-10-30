import { useState, useEffect } from 'react';
import axios from 'axios';

const TransferSummary = () => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchSummary = async () => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/transfers/summary', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setSummary(response.data);
        } catch (error) {
            setError('Failed to fetch transfer summary');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSummary();
    }, []);

    if (error) {
        return <div className="error">{error}</div>;
    }

    if (!summary && !loading) {
        return <button onClick={fetchSummary}>Load Summary</button>;
    }

    return (
        <div className="transfer-summary">
            <h3>Transfer Summary</h3>
            <div className="summary-grid">
                <div className="summary-item">
                    <span className="label">Total Sent</span>
                    <span className="value">{summary?.totalSent ? `₹${parseFloat(summary.totalSent).toFixed(2)}` : '₹0.00'}</span>
                </div>
                <div className="summary-item">
                    <span className="label">Total Received</span>
                    <span className="value">{summary?.totalReceived ? `₹${parseFloat(summary.totalReceived).toFixed(2)}` : '₹0.00'}</span>
                </div>
                <div className="summary-item">
                    <span className="label">Monthly Transactions</span>
                    <span className="value">{summary?.monthlyCount || 0}</span>
                </div>
            </div>
            <button onClick={fetchSummary} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh Summary'}
            </button>
        </div>
    );
};

export default TransferSummary;
