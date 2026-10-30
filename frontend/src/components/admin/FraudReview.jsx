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
            alert(`Transaction ${actionType === 'approve' ? 'approved' : 'blocked and refunded'} successfully`);
            fetchAlerts(page);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message;
            alert(`Failed to ${actionType}: ${errorMessage}`);
            console.error(`Failed to ${actionType} fraud alert:`, errorMessage);
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

    const getStatusBadge = (status) => {
        const statusMap = {
            'OPEN': { label: 'Pending Review', class: 'pending' },
            'APPROVED': { label: 'Approved', class: 'approved' },
            'RESOLVED': { label: 'Blocked/Refunded', class: 'resolved' },
            'REFUNDED': { label: 'Blocked/Refunded', class: 'resolved' }
        };
        
        const statusInfo = statusMap[status] || { label: status, class: 'unknown' };
        return <span className={`status-badge ${statusInfo.class}`}>{statusInfo.label}</span>;
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
                            <div key={alert.id} className={`alert-item ${alert.status?.toLowerCase()}`}>
                                <div className="alert-header">
                                    <div className="alert-title">
                                        <span className="transfer-id">Transfer #{alert.transferId}</span>
                                        {getStatusBadge(alert.status)}
                                    </div>
                                    {alert.reviewedAt && (
                                        <div className="review-info">
                                            <small>Reviewed: {new Date(alert.reviewedAt).toLocaleString('en-IN')}</small>
                                        </div>
                                    )}
                                </div>
                                <div className="alert-body">
                                    <div className="alert-reason">
                                        <span className="reason-label">Reason:</span>
                                        <span className="reason-text">{alert.reason}</span>
                                    </div>
                                    {alert.createdAt && (
                                        <div className="alert-created">
                                            <small>Flagged: {new Date(alert.createdAt).toLocaleString('en-IN')}</small>
                                        </div>
                                    )}
                                </div>
                                {alert.status === 'OPEN' && (
                                    <div className="alert-actions">
                                        <button
                                            className="btn-approve"
                                            onClick={() => handleReview(alert.id, 'approve')}
                                            disabled={processingId === alert.id}
                                        >
                                            {processingId === alert.id && action === 'approve' ? 'Processing...' : '✓ Approve'}
                                        </button>
                                        <button
                                            className="btn-block"
                                            onClick={() => handleReview(alert.id, 'refund')}
                                            disabled={processingId === alert.id}
                                        >
                                            {processingId === alert.id && action === 'refund' ? 'Processing...' : '✗ Block & Refund'}
                                        </button>
                                    </div>
                                )}
                                {alert.status !== 'OPEN' && (
                                    <div className="alert-settled">
                                        <small className="settled-text">
                                            {alert.status === 'APPROVED' ? '✓ This transaction has been approved and will be settled' : '✗ This transaction was blocked and refunded'}
                                        </small>
                                    </div>
                                )}
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
