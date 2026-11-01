import { useState, useEffect } from 'react';
import axios from 'axios';

const FraudReview = () => {
    const [alerts, setAlerts] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [processingId, setProcessingId] = useState(null);
    const [action, setAction] = useState(null);
    const [transferDetails, setTransferDetails] = useState({});

    const fetchAlerts = async (page = 0) => {
        setLoading(true);
        try {
            const response = await axios.get('/api/admin/fraud', {
                params: { page, size: 10 }
            });
            const alertsData = response.data.content;
            setAlerts(alertsData);
            setTotalPages(response.data.totalPages);
            setPage(page);
            
            // Fetch transfer details for each alert to check if settled
            const details = {};
            await Promise.all(alertsData.map(async (alert) => {
                try {
                    const transferResponse = await axios.get(`/api/admin/transactions`, {
                        params: { page: 0, size: 1 }
                    });
                    // Find the specific transfer
                    const allTransfers = await axios.get(`/api/admin/transactions?page=0&size=100`);
                    const transfer = allTransfers.data.content.find(t => t.id === alert.transferId);
                    if (transfer) {
                        details[alert.transferId] = transfer;
                    }
                } catch (error) {
                    console.error('Failed to fetch transfer details:', error);
                }
            }));
            setTransferDetails(details);
        } catch (error) {
            console.error('Failed to fetch fraud alerts:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAlerts();
    }, []);

    const handleReview = async (fraudAlert, actionType) => {
        // Check if transaction is already settled
        const transfer = transferDetails[fraudAlert.transferId];
        if (transfer && transfer.status === 'SUCCESS') {
            window.alert('Cannot modify alert: Transaction has already been settled and is read-only');
            return;
        }

        setProcessingId(fraudAlert.id);
        setAction(actionType);
        try {
            const endpoint = actionType === 'dismiss' 
                ? `/api/admin/fraud/${fraudAlert.id}/approve`
                : `/api/admin/fraud/${fraudAlert.id}/refund`;
            await axios.post(endpoint, {});
            window.alert(`Transaction ${actionType === 'dismiss' ? 'dismissed (approved)' : 'blocked and refunded'} successfully`);
            fetchAlerts(page);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message;
            window.alert(`Failed to ${actionType}: ${errorMessage}`);
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

    const getStatusBadge = (fraudAlert) => {
        const transfer = transferDetails[fraudAlert.transferId];
        const isSettled = transfer && transfer.status === 'SUCCESS';
        
        const statusMap = {
            'OPEN': { label: isSettled ? 'Settled (Read-Only)' : 'Pending Review', class: isSettled ? 'settled' : 'pending' },
            'APPROVED': { label: 'Dismissed', class: 'approved' },
            'RESOLVED': { label: 'Blocked & Refunded', class: 'resolved' }
        };
        
        const statusInfo = statusMap[fraudAlert.status] || { label: fraudAlert.status, class: 'unknown' };
        return <span className={`status-badge ${statusInfo.class}`}>{statusInfo.label}</span>;
    };

    const isReadOnly = (fraudAlert) => {
        const transfer = transferDetails[fraudAlert.transferId];
        return (transfer && transfer.status === 'SUCCESS') || fraudAlert.status !== 'OPEN';
    };

    return (
        <div className="fraud-review">
            {loading && <div className="loading">Loading fraud alerts...</div>}
            {!loading && alerts.length === 0 && <p>No fraud alerts found</p>}
            {!loading && alerts.length > 0 && (
                <>
                    <div className="alerts-list">
                        {alerts.map(fraudAlert => {
                            const transfer = transferDetails[fraudAlert.transferId];
                            const readOnly = isReadOnly(fraudAlert);
                            
                            return (
                                <div key={fraudAlert.id} className={`alert-item ${fraudAlert.status?.toLowerCase()} ${readOnly ? 'read-only' : ''}`}>
                                    <div className="alert-header">
                                        <div className="alert-title">
                                            <span className="transfer-id">Transfer #{fraudAlert.transferId}</span>
                                            {getStatusBadge(fraudAlert)}
                                        </div>
                                        {fraudAlert.reviewedAt && (
                                            <div className="review-info">
                                                <small>Reviewed: {new Date(fraudAlert.reviewedAt).toLocaleString('en-IN')}</small>
                                            </div>
                                        )}
                                    </div>
                                    <div className="alert-body">
                                        <div className="alert-reason">
                                            <span className="reason-label">Reason:</span>
                                            <span className="reason-text">{fraudAlert.reason}</span>
                                        </div>
                                        {transfer && (
                                            <div className="transfer-info">
                                                <div className="info-row">
                                                    <span className="info-label">Amount:</span>
                                                    <span className="info-value">₹{transfer.amount?.toFixed(2)}</span>
                                                </div>
                                                <div className="info-row">
                                                    <span className="info-label">From User:</span>
                                                    <span className="info-value">{transfer.senderId}</span>
                                                </div>
                                                <div className="info-row">
                                                    <span className="info-label">To User:</span>
                                                    <span className="info-value">{transfer.receiverId}</span>
                                                </div>
                                                <div className="info-row">
                                                    <span className="info-label">Transaction Status:</span>
                                                    <span className={`tx-status ${transfer.status?.toLowerCase()}`}>
                                                        {transfer.status}
                                                    </span>
                                                </div>
                                            </div>
                                        )}
                                        {fraudAlert.createdAt && (
                                            <div className="alert-created">
                                                <small>Flagged: {new Date(fraudAlert.createdAt).toLocaleString('en-IN')}</small>
                                            </div>
                                        )}
                                    </div>
                                    
                                    {fraudAlert.status === 'OPEN' && !readOnly && (
                                        <div className="alert-actions">
                                            <button
                                                className="btn-dismiss"
                                                onClick={() => handleReview(fraudAlert, 'dismiss')}
                                                disabled={processingId === fraudAlert.id}
                                            >
                                                {processingId === fraudAlert.id && action === 'dismiss' ? 'Processing...' : '✓ Dismiss Alert'}
                                            </button>
                                            <button
                                                className="btn-block"
                                                onClick={() => handleReview(fraudAlert, 'block')}
                                                disabled={processingId === fraudAlert.id}
                                            >
                                                {processingId === fraudAlert.id && action === 'block' ? 'Processing...' : '✗ Block Transaction'}
                                            </button>
                                        </div>
                                    )}
                                    
                                    {readOnly && fraudAlert.status === 'OPEN' && (
                                        <div className="alert-readonly">
                                            <small className="readonly-text">
                                                ⚠️ This transaction has already been settled. Alert is read-only.
                                            </small>
                                        </div>
                                    )}
                                    
                                    {fraudAlert.status === 'APPROVED' && (
                                        <div className="alert-settled approved-settled">
                                            <small className="settled-text">
                                                ✓ Alert dismissed - Transaction was approved and settled
                                            </small>
                                        </div>
                                    )}
                                    
                                    {fraudAlert.status === 'RESOLVED' && (
                                        <div className="alert-settled resolved-settled">
                                            <small className="settled-text">
                                                ✗ Transaction was blocked and sender refunded
                                            </small>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
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
