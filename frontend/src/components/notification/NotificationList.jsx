import { useState, useEffect } from 'react';
import axios from 'axios';
import MarkAsRead from './MarkAsRead';

const NotificationList = () => {
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [updated, setUpdated] = useState(false);

    const fetchNotifications = async (page = 0) => {
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/notifications', {
                headers: { Authorization: `Bearer ${token}` },
                params: { page, size: 10 }
            });
            setNotifications(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
            setUpdated(false);
        } catch (error) {
            console.error('Failed to fetch notifications:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchNotifications();
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchNotifications(newPage);
        }
    };

    return (
        <div className="notification-list">
            <h3>Notifications</h3>
            {loading && <div className="loading">Loading notifications...</div>}
            {!loading && notifications.length === 0 && <p>No notifications</p>}
            
            {!loading && notifications.length > 0 && (
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
                                <MarkAsRead notificationId={notification.id} onMarked={() => setUpdated(true)} />
                            </div>
                        ))}
                    </div>
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
                </>
            )}
        </div>
    );
};

export default NotificationList;
