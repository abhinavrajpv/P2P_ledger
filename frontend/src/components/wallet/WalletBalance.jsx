import { useState, useEffect } from 'react';
import axios from 'axios';

const WalletBalance = () => {
    const [balance, setBalance] = useState(null);
    const [todaySpending, setTodaySpending] = useState(null);
    const [dailyLimit, setDailyLimit] = useState(null);
    const [status, setStatus] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const fetchBalance = async () => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/wallet/balance', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setBalance(response.data.balance);
            setTodaySpending(response.data.todaySpending);
            setDailyLimit(response.data.dailyLimit);
            setStatus(response.data.status);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch wallet balance';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchBalance();
    }, []);

    if (error) {
        return (
            <div className="wallet-balance">
                <div className="error">{error}</div>
                <button onClick={fetchBalance}>Retry</button>
            </div>
        );
    }

    if (balance === null && !loading) {
        return (
            <div className="wallet-balance">
                <button onClick={fetchBalance}>Load Wallet</button>
            </div>
        );
    }

    return (
        <div className="wallet-balance">
            <h3>Wallet Balance</h3>
            <div className="balance-info">
                <div className="balance-item">
                    <span className="label">Available Balance</span>
                    <span className="value">{balance !== null ? `₹${parseFloat(balance).toFixed(2)}` : '-'}</span>
                </div>
                <div className="balance-item">
                    <span className="label">Today's Spending</span>
                    <span className="value">{todaySpending !== null ? `₹${parseFloat(todaySpending).toFixed(2)}` : '-'}</span>
                </div>
                <div className="balance-item">
                    <span className="label">Daily Limit</span>
                    <span className="value">{dailyLimit !== null ? `₹${parseFloat(dailyLimit).toFixed(2)}` : '-'}</span>
                </div>
                <div className="balance-item">
                    <span className="label">Status</span>
                    <span className={`value status-${status?.toLowerCase()}`}>{status || '-'}</span>
                </div>
            </div>
            <button onClick={fetchBalance} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh Balance'}
            </button>
        </div>
    );
};

export default WalletBalance;
