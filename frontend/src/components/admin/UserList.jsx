import { useState, useEffect } from 'react';
import axios from 'axios';

const UserList = () => {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [error, setError] = useState('');

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

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString('en-IN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    };

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
                                    <th>Role</th>
                                    <th>Masking</th>
                                    <th>Created</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map(user => (
                                    <tr key={user.id}>
                                        <td>{user.id}</td>
                                        <td>{user.name}</td>
                                        <td className="email-cell">{user.email}</td>
                                        <td>{user.phone}</td>
                                        <td>
                                            <span className={`role-badge ${user.role?.toLowerCase()}`}>
                                                {user.role}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`masking-status ${user.maskingEnabled ? 'enabled' : 'disabled'}`}>
                                                {user.maskingEnabled ? '✓ Enabled' : '✗ Disabled'}
                                            </span>
                                        </td>
                                        <td>{formatDate(user.createdAt)}</td>
                                        <td>
                                            {user.lockedUntil && new Date(user.lockedUntil) > new Date() ? (
                                                <span className="status-locked">🔒 Locked</span>
                                            ) : (
                                                <span className="status-active">✓ Active</span>
                                            )}
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
