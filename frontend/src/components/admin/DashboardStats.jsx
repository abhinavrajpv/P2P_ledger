import { useState, useEffect } from 'react';
import axios from 'axios';

const DashboardStats = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchStats = async () => {
        setLoading(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/admin/dashboard', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setStats(response.data);
        } catch (error) {
            setError('Failed to fetch dashboard statistics');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchStats();
    }, []);

    if (error) {
        return <div className="error">{error}</div>;
    }

    if (!stats && !loading) {
        return <button onClick={fetchStats}>Load Dashboard</button>;
    }

    return (
        <div className="dashboard-stats">
            <h3>Dashboard Overview</h3>
            <div className="stats-grid">
                <div className="stat-item">
                    <span className="stat-label">30-Day Volume</span>
                    <span className="stat-value">₹{parseFloat(stats?.volume30Days || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">Total Users</span>
                    <span className="stat-value">{stats?.usersCount || 0}</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">Total Wallets</span>
                    <span className="stat-value">{stats?.walletsCount || 0}</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">Pending Settlements</span>
                    <span className="stat-value">{stats?.pendingSettlements || 0}</span>
                </div>
                <div className="stat-item">
                    <span className="stat-label">Flagged Transactions</span>
                    <span className="stat-value">{stats?.flaggedTransactions || 0}</span>
                </div>
            </div>
            <button onClick={fetchStats} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh Stats'}
            </button>
        </div>
    );
};

export default DashboardStats;
