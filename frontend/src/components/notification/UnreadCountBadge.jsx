import { useState, useEffect } from 'react';
import axios from 'axios';
import { getToken } from '../../auth';

const UnreadCountBadge = ({ inline = false }) => {
    const [unreadCount, setUnreadCount] = useState(0);

    const fetchUnreadCount = async () => {
        try {
            if (!getToken()) return;
            const response = await axios.get('/api/notifications/unread-count');
            setUnreadCount(response.data || 0);
        } catch (error) {
            console.error('Failed to fetch unread count:', error.response?.data?.message || error.message);
        }
    };

    useEffect(() => {
        fetchUnreadCount();
        // Poll every 30 seconds
        const interval = setInterval(fetchUnreadCount, 30000);
        return () => clearInterval(interval);
    }, []);

    if (unreadCount === 0) return null;

    if (inline) {
        return <span className="unread-count-inline">{unreadCount}</span>;
    }

    return (
        <div className="unread-badge">
            <span className="badge-count">{unreadCount}</span>
            <span className="badge-text">Unread Notification{unreadCount !== 1 ? 's' : ''}</span>
        </div>
    );
};

export default UnreadCountBadge;
