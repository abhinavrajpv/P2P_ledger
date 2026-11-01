import { useState } from 'react';
import axios from 'axios';

const MarkAsRead = ({ notificationId, onMarked }) => {
    const [loading, setLoading] = useState(false);

    const markAsRead = async () => {
        setLoading(true);
        try {
            await axios.patch(`/api/notifications/${notificationId}/read`);
            // Call parent to update the notification state
            if (onMarked) {
                onMarked();
            }
        } catch (error) {
            console.error('Failed to mark notification as read:', error);
            const errorMsg = error.response?.data?.message || 'Failed to mark as read. Please try again.';
            alert(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <button onClick={markAsRead} disabled={loading} className="mark-read-btn">
            {loading ? 'Marking...' : 'Mark as Read'}
        </button>
    );
};

export default MarkAsRead;
