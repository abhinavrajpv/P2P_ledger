import { useState } from 'react';
import axios from 'axios';

const MarkAsRead = ({ notificationId, onMarked }) => {
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);

    const markAsRead = async () => {
        setLoading(true);
        try {
            await axios.patch(`/api/notifications/${notificationId}/read`, {});
            setSuccess(true);
            onMarked?.();
        } catch (error) {
            console.error('Failed to mark notification as read');
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return <span className="marked-read">Read</span>;
    }

    return (
        <button onClick={markAsRead} disabled={loading}>
            {loading ? 'Marking...' : 'Mark as Read'}
        </button>
    );
};

export default MarkAsRead;
