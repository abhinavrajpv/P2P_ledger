import { useState, useEffect } from 'react';
import axios from 'axios';
import MarkAsRead from './MarkAsRead';

const NotificationList = () => {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchNotifications = async (pageNum = 0) => {
        setLoading(true);
        setError('');
        try {
            const response = await axios.get('/api/notifications', {
                params: { page: pageNum, size: 10 }
            });
            
            // Handle different response structures
            if (response.data.content) {
                setNotifications(response.data.content || []);
                setTotalPages(response.data.totalPages || 0);
            } else if (Array.isArray(response.data)) {
                setNotifications(response.data);
                setTotalPages(1);
            } else {
                setNotifications([]);
                setTotalPages(0);
            }
            setPage(pageNum);
        } catch (err) {
            const errorMessage = err.response?.data?.message || err.message || 'Failed to load notifications';
            setError(errorMessage);
            console.error('Notification fetch error:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications(0);
    }, []);

    const handleMarkAsRead = async (notificationId) => {
        // Optimistically update UI to mark as read
        setNotifications(prev => 
            prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
        );
        
        // Trigger a custom event to notify UnreadCountBadge to refresh
        window.dispatchEvent(new CustomEvent('notificationRead'));
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchNotifications(newPage);
        }
    };

    return (
        <div className="notification-list" style={{ maxWidth: '800px', margin: '0 auto' }}>
            <h3>Notifications</h3>
            
            {loading && (
                <div className="loading">
                    <p>Loading notifications...</p>
                </div>
            )}

            {error && !loading && (
                <div className="error" style={{ 
                    padding: '1rem', 
                    backgroundColor: '#ffebee', 
                    color: '#c62828', 
                    borderRadius: '4px',
                    marginBottom: '1rem'
                }}>
                    <p>{error}</p>
                    <button 
                        onClick={() => fetchNotifications(page)}
                        style={{
                            marginTop: '0.5rem',
                            padding: '0.5rem 1rem',
                            backgroundColor: '#1976d2',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                        }}
                    >
                        Retry
                    </button>
                </div>
            )}

            {!loading && !error && notifications.length === 0 && (
                <div style={{ 
                    padding: '2rem', 
                    textAlign: 'center', 
                    color: '#666',
                    backgroundColor: '#f5f5f5',
                    borderRadius: '8px'
                }}>
                    <p>No notifications yet</p>
                    <p style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
                        You'll receive notifications when transfers are initiated, settled, or require attention.
                    </p>
                </div>
            )}
            
            {!loading && !error && notifications.length > 0 && (
                <>
                    <div className="notifications-container">
                        {notifications.map((notification) => (
                            <div
                                key={notification.id}
                                className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
                            >
                                <div className="notification-type">
                                    <span className="type-label">{notification.type}</span>
                                </div>
                                <div className="notification-message">{notification.message}</div>
                                <div className="notification-time">
                                    {new Date(notification.createdAt).toLocaleString()}
                                </div>
                                {!notification.isRead && (
                                    <MarkAsRead 
                                        notificationId={notification.id} 
                                        onMarked={() => handleMarkAsRead(notification.id)} 
                                    />
                                )}
                            </div>
                        ))}
                    </div>
                    
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button
                                onClick={() => handlePageChange(page - 1)}
                                disabled={page === 0}
                            >
                                Previous
                            </button>
                            <span>Page {page + 1} of {totalPages}</span>
                            <button
                                onClick={() => handlePageChange(page + 1)}
                                disabled={page >= totalPages - 1}
                            >
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
            
            <button 
                onClick={() => fetchNotifications(page)} 
                disabled={loading}
                style={{ 
                    marginTop: '1rem',
                    padding: '0.5rem 1rem',
                    backgroundColor: '#1976d2',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: loading ? 'not-allowed' : 'pointer',
                    opacity: loading ? 0.6 : 1
                }}
            >
                {loading ? 'Refreshing...' : 'Refresh Notifications'}
            </button>
        </div>
    );
};

export default NotificationList;
