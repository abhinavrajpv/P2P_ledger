import { useState, useEffect } from 'react';
import axios from 'axios';

const DashboardStats = () => {
    const [stats, setStats] = useState(null);
    const [chartData, setChartData] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const fetchStats = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await axios.get('/api/admin/dashboard');
            setStats(response.data);
            
            // Generate 30-day chart data (mock data for now - can be replaced with real API data)
            const last30Days = [];
            for (let i = 29; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                last30Days.push({
                    date: date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }),
                    volume: Math.random() * 50000 // This should come from backend
                });
            }
            setChartData(last30Days);
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

    const maxVolume = Math.max(...chartData.map(d => d.volume), 1);

    if (error) {
        return <div className="error">{error}</div>;
    }

    if (!stats && !loading) {
        return <button onClick={fetchStats}>Load Dashboard</button>;
    }

    return (
        <div className="dashboard-stats">
            <h3>Dashboard</h3>
            
            {/* Main Stats Grid */}
            <div className="main-stats-grid">
                <div className="stat-card">
                    <span className="stat-label">Total Registered Users</span>
                    <span className="stat-value large">{stats?.usersCount || 0}</span>
                </div>
                <div className="stat-card">
                    <span className="stat-label">Today's Transfers Initiated Count</span>
                    <span className="stat-value large">{stats?.todayCount || 0}</span>
                </div>
                <div className="stat-card success">
                    <span className="stat-label">Today's Transfers Settled Count</span>
                    <span className="stat-value large">{stats?.todaySuccessCount || 0}</span>
                </div>
                <div className="stat-card highlight">
                    <span className="stat-label">Today's Transaction Volume (₹)</span>
                    <span className="stat-value large">₹{parseFloat(stats?.todayVolume || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                </div>
                <div className="stat-card attention">
                    <span className="stat-label">Total Pending Settlement Amount</span>
                    <span className="stat-value large">₹{parseFloat(stats?.pendingSettlementAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                </div>
                <div className="stat-card alert">
                    <span className="stat-label">Pending Fraud Alerts Count</span>
                    <span className="stat-value large">{stats?.flaggedTransactions || 0}</span>
                </div>
            </div>

            {/* Line Chart - Daily Transaction Volume (Last 30 Days) */}
            <div className="chart-section">
                <h4>Daily Transaction Volume Over Last 30 Days</h4>
                <div className="line-chart">
                    <div className="chart-y-axis">
                        <span className="y-label">₹{(maxVolume).toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
                        <span className="y-label">₹{(maxVolume * 0.75).toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
                        <span className="y-label">₹{(maxVolume * 0.5).toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
                        <span className="y-label">₹{(maxVolume * 0.25).toLocaleString('en-IN', { maximumFractionDigits: 0 })}</span>
                        <span className="y-label">₹0</span>
                    </div>
                    <div className="chart-area">
                        <svg viewBox="0 0 800 300" className="chart-svg">
                            {/* Grid lines */}
                            <line x1="0" y1="0" x2="800" y2="0" stroke="#e0e0e0" strokeWidth="1" />
                            <line x1="0" y1="75" x2="800" y2="75" stroke="#e0e0e0" strokeWidth="1" />
                            <line x1="0" y1="150" x2="800" y2="150" stroke="#e0e0e0" strokeWidth="1" />
                            <line x1="0" y1="225" x2="800" y2="225" stroke="#e0e0e0" strokeWidth="1" />
                            <line x1="0" y1="300" x2="800" y2="300" stroke="#e0e0e0" strokeWidth="1" />
                            
                            {/* Line path */}
                            <polyline
                                fill="none"
                                stroke="#1976d2"
                                strokeWidth="2"
                                points={chartData.map((d, i) => {
                                    const x = (i / (chartData.length - 1)) * 800;
                                    const y = 300 - (d.volume / maxVolume) * 300;
                                    return `${x},${y}`;
                                }).join(' ')}
                            />
                            
                            {/* Fill area under line */}
                            <polygon
                                fill="rgba(25, 118, 210, 0.1)"
                                points={`0,300 ${chartData.map((d, i) => {
                                    const x = (i / (chartData.length - 1)) * 800;
                                    const y = 300 - (d.volume / maxVolume) * 300;
                                    return `${x},${y}`;
                                }).join(' ')} 800,300`}
                            />
                            
                            {/* Data points */}
                            {chartData.map((d, i) => {
                                const x = (i / (chartData.length - 1)) * 800;
                                const y = 300 - (d.volume / maxVolume) * 300;
                                return (
                                    <circle
                                        key={i}
                                        cx={x}
                                        cy={y}
                                        r="3"
                                        fill="#1976d2"
                                    />
                                );
                            })}
                        </svg>
                        <div className="chart-x-axis">
                            {chartData.filter((_, i) => i % 5 === 0).map((d, i) => (
                                <span key={i} className="x-label">{d.date}</span>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            <button onClick={fetchStats} disabled={loading} className="refresh-button">
                {loading ? 'Loading...' : 'Refresh Dashboard'}
            </button>
        </div>
    );
};

export default DashboardStats;
