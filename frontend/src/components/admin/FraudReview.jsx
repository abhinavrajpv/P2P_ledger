import { useState, useEffect } from 'react';
import axios from 'axios';

const FraudReview = () => {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [processingId, setProcessingId] = useState(null);
    const [action, setAction] = useState(null);

    const fetchAlerts = async (page = 0) => {
        setLoading(true);
        try {
            const response = await axios.get('/api/admin/fraud', {
                params: { page, size: 10 }
            });
            setAlerts(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
        } catch (error) {
            console.error('Failed to fetch fraud alerts:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAlerts();
    }, []);

    const handleReview = async (fraudAlertId, actionType) => {
        setProcessingId(fraudAlertId);
        setAction(actionType);
        try {
            const endpoint = actionType === 'approve' 
                ? `/api/admin/fraud/${fraudAlertId}/approve`
                : `/api/admin/fraud/${fraudAlertId}/refund`;
            await axios.post(endpoint, {});
            fetchAlerts(page);
        } catch (error) {
            console.error(`Failed to ${actionType} fraud alert:`, error.response?.data?.message || error.message);
        } finally {
            setProcessingId(null);
            setAction(null);
        }
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchAlerts(newPage);
        }
    };

    return (
        <div className="fraud-review">
            <h3>Fraud Review</h3>
            {loading && <div className="loading">Loading fraud alerts...</div>}
            {!loading && alerts.length === 0 && <p>No flagged transactions for review</p>}
            {!loading && alerts.length > 0 && (
                <>
                    <div className="alerts-list">
                        {alerts.map(alert => (
                            <div key={alert.id} className="alert-item">
                                <div className="alert-info">
                                    <span>Transfer ID: {alert.transferId}</span>
                                    <span>Reason: {alert.reason}</span>
                                    <span className={`status-${alert.status?.toLowerCase()}`}>{alert.status}</span>
                                </div>
                                <div className="alert-actions">
                                    <button
                                        onClick={() => handleReview(alert.id, 'approve')}
                                        disabled={processingId === alert.id}
                                    >
                                        {processingId === alert.id ? 'Processing...' : 'Approve'}
                                    </button>
                                    <button
                                        onClick={() => handleReview(alert.id, 'refund')}
                                        disabled={processingId === alert.id}
                                    >
                                        {processingId === alert.id ? 'Processing...' : 'Refund'}
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="pagination">
                        <button onClick={() => handlePageChange(page - 1)} disabled={page === 0}>Previous</button>
                        <span>Page {page + 1} of {totalPages}</span>
                        <button onClick={() => handlePageChange(page + 1)} disabled={page >= totalPages - 1}>Next</button>
                    </div>
                </>
            )}
        </div>
    );
};

export default FraudReview;
