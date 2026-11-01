import { useState, useEffect } from 'react';
import axios from 'axios';

const LoginAttemptsMonitor = () => {
    const [loginAttempts, setLoginAttempts] = useState([]);
    const [filteredAttempts, setFilteredAttempts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [filterType, setFilterType] = useState('all'); // all, successful, failed
    const [searchIdentifier, setSearchIdentifier] = useState('');

    const fetchLoginAttempts = async () => {
        setLoading(true);
        setError('');
        try {
            let url = '/api/admin/login-attempts';
            if (filterType === 'failed') {
                url = '/api/admin/login-attempts/failed';
            } else if (filterType === 'successful') {
                url = '/api/admin/login-attempts?successful=true';
            }

            const response = await axios.get(url);
            setLoginAttempts(response.data);
            setFilteredAttempts(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch login attempts';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLoginAttempts();
    }, [filterType]);

    useEffect(() => {
        if (searchIdentifier.trim() === '') {
            setFilteredAttempts(loginAttempts);
        } else {
            const filtered = loginAttempts.filter(attempt => 
                attempt.identifier.toLowerCase().includes(searchIdentifier.toLowerCase())
            );
            setFilteredAttempts(filtered);
        }
    }, [searchIdentifier, loginAttempts]);

    const formatDateTime = (dateTime) => {
        return new Date(dateTime).toLocaleString('en-IN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    const getStatusBadge = (successful) => {
        return successful ? (
            <span className="badge badge-success">✓ Success</span>
        ) : (
            <span className="badge badge-danger">✗ Failed</span>
        );
    };

    const getSourceBadge = (source) => {
        const badgeClass = source === 'Login successful' ? 'badge-success' : 
                          source === 'Invalid password' ? 'badge-warning' :
                          source === 'Account locked' ? 'badge-danger' :
                          'badge-info';
        
        return <span className={`badge ${badgeClass}`}>{source}</span>;
    };

    if (error) {
        return <div className="error">{error}</div>;
    }

    return (
        <div className="login-attempts-monitor">
            <div className="monitor-header">
                <h3>Login Attempts Monitor</h3>
                <p className="subtitle">Track and monitor all login attempts to detect security threats</p>
            </div>

            <div className="monitor-controls">
                <div className="filter-group">
                    <label>Filter by Status:</label>
                    <div className="button-group">
                        <button 
                            className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
                            onClick={() => setFilterType('all')}
                        >
                            All Attempts
                        </button>
                        <button 
                            className={`filter-btn ${filterType === 'successful' ? 'active' : ''}`}
                            onClick={() => setFilterType('successful')}
                        >
                            Successful
                        </button>
                        <button 
                            className={`filter-btn ${filterType === 'failed' ? 'active' : ''}`}
                            onClick={() => setFilterType('failed')}
                        >
                            Failed
                        </button>
                    </div>
                </div>

                <div className="search-group">
                    <label>Search by Identifier:</label>
                    <input 
                        type="text"
                        placeholder="Search email or phone..."
                        value={searchIdentifier}
                        onChange={(e) => setSearchIdentifier(e.target.value)}
                        className="search-input"
                    />
                </div>

                <button onClick={fetchLoginAttempts} disabled={loading} className="refresh-button">
                    {loading ? 'Loading...' : '🔄 Refresh'}
                </button>
            </div>

            <div className="attempts-stats">
                <div className="stat-card">
                    <span className="stat-label">Total Attempts</span>
                    <span className="stat-value">{filteredAttempts.length}</span>
                </div>
                <div className="stat-card success">
                    <span className="stat-label">Successful</span>
                    <span className="stat-value">{filteredAttempts.filter(a => a.successful).length}</span>
                </div>
                <div className="stat-card danger">
                    <span className="stat-label">Failed</span>
                    <span className="stat-value">{filteredAttempts.filter(a => !a.successful).length}</span>
                </div>
            </div>

            {loading ? (
                <div className="loading-message">Loading login attempts...</div>
            ) : filteredAttempts.length === 0 ? (
                <div className="no-data-message">No login attempts found</div>
            ) : (
                <div className="attempts-table-container">
                    <table className="attempts-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>User ID</th>
                                <th>Identifier</th>
                                <th>Time</th>
                                <th>Status</th>
                                <th>Reason</th>
                            </tr>
                        </thead>
                        <tbody>
                            {filteredAttempts.map(attempt => (
                                <tr key={attempt.id} className={!attempt.successful ? 'failed-row' : ''}>
                                    <td>{attempt.id}</td>
                                    <td>{attempt.userId || <span className="text-muted">—</span>}</td>
                                    <td className="identifier-cell">{attempt.identifier}</td>
                                    <td>{formatDateTime(attempt.attemptedAt)}</td>
                                    <td>{getStatusBadge(attempt.successful)}</td>
                                    <td>{getSourceBadge(attempt.source)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default LoginAttemptsMonitor;
