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
            const response = await axios.get('/api/admin/dashboard');
            setStats(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch dashboard statistics';
            setError(errorMessage);
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
            
            {/* Today's Stats Section */}
            <div className="today-stats">
                <h4>Today's Activity</h4>
                <div className="stats-grid today-grid">
                    <div className="stat-item highlight">
                        <span className="stat-label">Today's Volume</span>
                        <span className="stat-value">₹{parseFloat(stats?.todayVolume || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                    <div className="stat-item">
                        <span className="stat-label">Transactions Today</span>
                        <span className="stat-value">{stats?.todayCount || 0}</span>
                    </div>
                    <div className="stat-item success">
                        <span className="stat-label">Successful</span>
                        <span className="stat-value">{stats?.todaySuccessCount || 0}</span>
                    </div>
                    <div className="stat-item warning">
                        <span className="stat-label">Failed</span>
                        <span className="stat-value">{stats?.todayFailedCount || 0}</span>
                    </div>
                </div>
            </div>

            {/* Overall Stats Section */}
            <div className="overall-stats">
                <h4>System Overview</h4>
                <div className="stats-grid">
                    <div className="stat-item">
                        <span className="stat-label">30-Day Volume</span>
                        <span className="stat-value">₹{parseFloat(stats?.volume30Day || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                    </div>
                    <div className="stat-item">
                        <span className="stat-label">Total Users</span>
                        <span className="stat-value">{stats?.usersCount || 0}</span>
                    </div>
                    <div className="stat-item">
                        <span className="stat-label">Total Wallets</span>
                        <span className="stat-value">{stats?.walletsCount || 0}</span>
                    </div>
                    <div className="stat-item attention">
                        <span className="stat-label">Pending Settlements</span>
                        <span className="stat-value">{stats?.pendingSettlements || 0}</span>
                    </div>
                    <div className="stat-item alert">
                        <span className="stat-label">Flagged Transactions</span>
                        <span className="stat-value">{stats?.flaggedTransactions || 0}</span>
                    </div>
                </div>
            </div>

            <button onClick={fetchStats} disabled={loading}>
                {loading ? 'Loading...' : 'Refresh Stats'}
            </button>
        </div>
    );
};

export default DashboardStats;
