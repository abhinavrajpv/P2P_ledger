import { useState, useEffect } from 'react';
import axios from 'axios';

const UnreadCountBadge = () => {
    const [unreadCount, setUnreadCount] = useState(0);
    const [loading, setLoading] = useState(false);

    const fetchUnreadCount = async () => {
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/notifications/unread-count', {
                headers: { Authorization: `Bearer ${token}` }
            });
            setUnreadCount(response.data);
        } catch (error) {
            console.error('Failed to fetch unread count');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUnreadCount();
    }, []);

    if (unreadCount === 0 && !loading) {
        return null;
    }

    return (
        <div className="unread-badge">
            <span className="badge-count">{unreadCount}</span>
            <span className="badge-text">Unread Notification{unreadCount !== 1 ? 's' : ''}</span>
        </div>
    );
};

export default UnreadCountBadge;
