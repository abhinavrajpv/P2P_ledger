import { useState, useEffect } from 'react';
import axios from 'axios';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [error, setError] = useState('');
    const [selectedUser, setSelectedUser] = useState(null);
    const [userDetail, setUserDetail] = useState(null);
    const [actionLoading, setActionLoading] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);

    const fetchUsers = async (page = 0) => {
        setLoading(true);
        setError('');
        try {
            const response = await axios.get('/api/admin/users', {
                params: { page, size: 15 }
            });
            setUsers(response.data.content || []);
            setTotalPages(response.data.totalPages || 0);
            setPage(page);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch users';
            setError(errorMessage);
            console.error('Failed to fetch users:', errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchUsers(newPage);
        }
    };

    const handleUserClick = async (user) => {
        setSelectedUser(user);
        setDetailLoading(true);
        setError('');
        try {
            const response = await axios.get(`/api/admin/users/${user.id}`);
            setUserDetail(response.data);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to fetch user details';
            setError(errorMessage);
            console.error('Failed to fetch user details:', errorMessage);
        } finally {
            setDetailLoading(false);
        }
    };

    const handleFreezeWallet = async (userId) => {
        if (!window.confirm('Are you sure you want to freeze this user\'s wallet?')) {
            return;
        }
        
        setActionLoading(true);
        setError('');
        try {
            await axios.post(`/api/admin/users/${userId}/freeze`);
            alert('Wallet frozen successfully');
            fetchUsers(page); // Refresh the list
            setSelectedUser(null);
            setUserDetail(null);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to freeze wallet';
            setError(errorMessage);
            alert('Error: ' + errorMessage);
        } finally {
            setActionLoading(false);
        }
    };

    const handleUnfreezeWallet = async (userId) => {
        if (!window.confirm('Are you sure you want to unfreeze this user\'s wallet?')) {
            return;
        }
        
        setActionLoading(true);
        setError('');
        try {
            await axios.post(`/api/admin/users/${userId}/unfreeze`);
            alert('Wallet unfrozen successfully');
            fetchUsers(page); // Refresh the list
            setSelectedUser(null);
            setUserDetail(null);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to unfreeze wallet';
            setError(errorMessage);
            alert('Error: ' + errorMessage);
        } finally {
            setActionLoading(false);
        }
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString('en-IN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

    const formatCurrency = (amount) => {
        if (amount === null || amount === undefined) return '₹0.00';
        return `₹${parseFloat(amount).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    };

    if (selectedUser) {
        const displayData = userDetail || selectedUser;
        
        return (
            <div className="user-detail-modal">
                <div className="user-detail-content">
                    <h3>User Details</h3>
                    <button className="close-btn" onClick={() => {
                        setSelectedUser(null);
                        setUserDetail(null);
                    }}>✕</button>
                    
                    {detailLoading && <div className="loading">Loading details...</div>}
                    
                    {!detailLoading && (
                        <>
                            <div className="user-info-grid">
                                <div className="info-item">
                                    <label>Name:</label>
                                    <span>{displayData.name}</span>
                                </div>
                                <div className="info-item">
                                    <label>Email:</label>
                                    <span>{displayData.email}</span>
                                </div>
                                <div className="info-item">
                                    <label>Phone:</label>
                                    <span>{displayData.phone}</span>
                                </div>
                                <div className="info-item">
                                    <label>Role:</label>
                                    <span className={`role-badge ${displayData.role?.toLowerCase()}`}>
                                        {displayData.role}
                                    </span>
                                </div>
                                <div className="info-item">
                                    <label>Wallet Balance:</label>
                                    <span className="wallet-balance">{formatCurrency(displayData.walletBalance)}</span>
                                </div>
                                <div className="info-item">
                                    <label>Wallet Status:</label>
                                    <span className={`wallet-status-badge ${displayData.walletStatus?.toLowerCase()}`}>
                                        {displayData.walletStatus || 'N/A'}
                                    </span>
                                </div>
                                <div className="info-item">
                                    <label>Masking:</label>
                                    <span>{displayData.maskingEnabled ? '✓ Enabled' : '✗ Disabled'}</span>
                                </div>
                                <div className="info-item">
                                    <label>Created:</label>
                                    <span>{formatDate(displayData.createdAt)}</span>
                                </div>
                            </div>

                            {/* Recent Transactions Section */}
                            {userDetail && userDetail.recentTransfers && userDetail.recentTransfers.length > 0 && (
                                <div className="recent-transactions-section">
                                    <h4>Recent Transactions (Last 10)</h4>
                                    <div className="transactions-list-modal">
                                        {userDetail.recentTransfers.map(transfer => (
                                            <div key={transfer.id} className={`transaction-item-modal ${transfer.status?.toLowerCase()}`}>
                                                <div className="transaction-row">
                                                    <span className="tx-id">ID: {transfer.id}</span>
                                                    <span className={`tx-status ${transfer.status?.toLowerCase()}`}>
                                                        {transfer.status}
                                                    </span>
                                                </div>
                                                <div className="transaction-row">
                                                    <span className="tx-amount">₹{transfer.amount?.toFixed(2)}</span>
                                                    <span className="tx-direction">
                                                        {transfer.senderId === displayData.id ? 
                                                            `→ To User ${transfer.receiverId}` : 
                                                            `← From User ${transfer.senderId}`}
                                                    </span>
                                                </div>
                                                <div className="transaction-row">
                                                    <span className="tx-date">{formatDate(transfer.createdAt)}</span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {userDetail && (!userDetail.recentTransfers || userDetail.recentTransfers.length === 0) && (
                                <div className="recent-transactions-section">
                                    <h4>Recent Transactions</h4>
                                    <p className="no-transactions">No transactions found for this user</p>
                                </div>
                            )}

                            <div className="user-actions">
                                <h4>Wallet Actions</h4>
                                {displayData.walletStatus === 'FROZEN' ? (
                                    <button 
                                        className="btn-success" 
                                        onClick={() => handleUnfreezeWallet(displayData.id)}
                                        disabled={actionLoading}
                                    >
                                        {actionLoading ? 'Processing...' : '🔓 Unfreeze Wallet'}
                                    </button>
                                ) : (
                                    <button 
                                        className="btn-danger" 
                                        onClick={() => handleFreezeWallet(displayData.id)}
                                        disabled={actionLoading || displayData.role === 'ADMIN'}
                                    >
                                        {actionLoading ? 'Processing...' : '🔒 Freeze Wallet'}
                                    </button>
                                )}
                                {displayData.role === 'ADMIN' && (
                                    <p className="note">* Cannot freeze admin wallets</p>
                                )}
                            </div>
                        </>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="user-list">
            <h3>User Management</h3>
            
            {error && <div className="error-message">{error}</div>}
            {loading && <div className="loading">Loading users...</div>}
            {!loading && users.length === 0 && <p>No users found</p>}
            {!loading && users.length > 0 && (
                <>
                    <div className="users-table-container">
                        <table className="users-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Phone</th>
                                    <th>Wallet Balance</th>
                                    <th>Wallet Status</th>
                                    <th>Role</th>
                                    <th>Masking</th>
                                    <th>Created</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(user => (
                                    <tr key={user.id} onClick={() => handleUserClick(user)} className="clickable-row">
                                        <td>{user.id}</td>
                                        <td>{user.name}</td>
                                        <td className="email-cell">{user.email}</td>
                                        <td>{user.phone}</td>
                                        <td className="wallet-balance-cell">{formatCurrency(user.walletBalance)}</td>
                                        <td>
                                            <span className={`wallet-status-badge ${user.walletStatus?.toLowerCase()}`}>
                                                {user.walletStatus || 'N/A'}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`role-badge ${user.role?.toLowerCase()}`}>
                                                {user.role}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`masking-status ${user.maskingEnabled ? 'enabled' : 'disabled'}`}>
                                                {user.maskingEnabled ? '✓' : '✗'}
                                            </span>
                                        </td>
                                        <td>{formatDate(user.createdAt)}</td>
                                        <td>
                                            <button 
                                                className="btn-view" 
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleUserClick(user);
                                                }}
                                            >
                                                View
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button onClick={() => handlePageChange(page - 1)} disabled={page === 0}>
                                Previous
                            </button>
                            <span>Page {page + 1} of {totalPages}</span>
                            <button onClick={() => handlePageChange(page + 1)} disabled={page >= totalPages - 1}>
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default UserList;
