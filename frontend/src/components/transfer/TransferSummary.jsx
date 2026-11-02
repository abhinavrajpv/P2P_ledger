import { useState, useEffect } from 'react';
import axios from '../../axiosConfig';
import './TransferSummary.css';

const TransferSummary = () => {
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchSummary = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await axios.get('/api/transfers/summary');
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
        return <div className="summary-error">
            <span className="error-icon">⚠️</span>
            {error}
        </div>;
    }

    if (!summary && !loading) {
        return (
            <div className="transfer-summary-card">
                <button className="btn-load-summary" onClick={fetchSummary}>
                    Load Summary
                </button>
            </div>
        );
    }

    const totalSent = summary?.totalSent ? parseFloat(summary.totalSent) : 0;
    const totalReceived = summary?.totalReceived ? parseFloat(summary.totalReceived) : 0;
    const netBalance = totalReceived - totalSent;

    return (
        <div className="transfer-summary-card">
            <div className="summary-header">
                <div className="summary-title">
                    <h3>💸 Transfer Summary</h3>
                    <p className="summary-subtitle">Your transaction overview</p>
                </div>
                <button 
                    className={`btn-refresh ${loading ? 'loading' : ''}`}
                    onClick={fetchSummary} 
                    disabled={loading}
                    title="Refresh Summary"
                >
                    <span className="refresh-icon">🔄</span>
                </button>
            </div>

            <div className="summary-stats">
                <div className="stat-card sent">
                    <div className="stat-icon">📤</div>
                    <div className="stat-content">
                        <span className="stat-label">Total Sent</span>
                        <span className="stat-value">₹{totalSent.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                </div>

                <div className="stat-card received">
                    <div className="stat-icon">📥</div>
                    <div className="stat-content">
                        <span className="stat-label">Total Received</span>
                        <span className="stat-value">₹{totalReceived.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                </div>

                <div className="stat-card transactions">
                    <div className="stat-icon">📊</div>
                    <div className="stat-content">
                        <span className="stat-label">Monthly Transactions</span>
                        <span className="stat-value">{summary?.monthlyCount || 0}</span>
                    </div>
                </div>
            </div>

            <div className={`net-balance ${netBalance >= 0 ? 'positive' : 'negative'}`}>
                <span className="balance-label">Net Balance</span>
                <span className="balance-value">
                    {netBalance >= 0 ? '+' : ''}₹{Math.abs(netBalance).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
            </div>
        </div>
    );
};

export default TransferSummary;
