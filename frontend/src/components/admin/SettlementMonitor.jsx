import { useState, useEffect } from 'react';
import axios from 'axios';

const SettlementMonitor = () => {
    const [settlements, setSettlements] = useState([]);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [processing, setProcessing] = useState(false);
    const [processingId, setProcessingId] = useState(null);

    const fetchSettlements = async (page = 0) => {
        setLoading(true);
        try {
            const [settlementsRes, summaryRes] = await Promise.all([
                axios.get('/api/admin/settlements', {
                    params: { page, size: 10 }
                }),
                axios.get('/api/admin/settlements/summary')
            ]);
            
            // Handle paginated response
            if (settlementsRes.data.content) {
                setSettlements(settlementsRes.data.content || []);
                setTotalPages(settlementsRes.data.totalPages || 0);
            } else if (Array.isArray(settlementsRes.data)) {
                setSettlements(settlementsRes.data);
                setTotalPages(1);
            } else {
                setSettlements([]);
                setTotalPages(0);
            }
            
            setSummary(summaryRes.data);
            setPage(page);
        } catch (error) {
            console.error('Failed to fetch settlements:', error.response?.data?.message || error.message);
            setSettlements([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSettlements();
    }, []);

    const processSettlement = async (transferId) => {
        setProcessingId(transferId);
        setProcessing(true);
        try {
            await axios.post('/api/admin/settlements/process', { transferId });
            alert('Settlement processed successfully');
            fetchSettlements(page);
        } catch (error) {
            const errorMessage = error.response?.data?.message || error.message || 'Failed to process settlement';
            alert('Error: ' + errorMessage);
            console.error('Failed to process settlement:', errorMessage);
        } finally {
            setProcessingId(null);
            setProcessing(false);
        }
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchSettlements(newPage);
        }
    };

    const getSettlementCategory = (settlementDate) => {
        const now = new Date();
        const settlement = new Date(settlementDate);
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);
        
        if (settlement < now) return 'overdue';
        if (settlement < today.setHours(23, 59, 59)) return 'due-today';
        if (settlement < tomorrow.setHours(23, 59, 59)) return 'due-tomorrow';
        return 'future';
    };

    return (
        <div className="settlement-monitor">
            <h3>Settlement Monitor</h3>
            
            {/* Settlement Summary */}
            {summary && (
                <div className="settlement-summary">
                    <div className="summary-card overdue">
                        <span className="summary-label">⚠️ Overdue</span>
                        <span className="summary-value">{summary.overdue}</span>
                    </div>
                    <div className="summary-card due-today">
                        <span className="summary-label">🔔 Due Today</span>
                        <span className="summary-value">{summary.dueToday}</span>
                    </div>
                    <div className="summary-card due-tomorrow">
                        <span className="summary-label">📅 Due Tomorrow</span>
                        <span className="summary-value">{summary.dueTomorrow}</span>
                    </div>
                    <div className="summary-card total">
                        <span className="summary-label">Total Pending</span>
                        <span className="summary-value">{summary.totalPending}</span>
                    </div>
                </div>
            )}
            
            {loading && <div className="loading">Loading settlements...</div>}
            {!loading && settlements.length === 0 && <p>No pending settlements</p>}
            {!loading && settlements.length > 0 && (
                <>
                    <div className="settlements-list">
                        {settlements.map(s => {
                            const category = getSettlementCategory(s.settlementDate);
                            return (
                                <div key={s.id} className={`settlement-item ${category}`}>
                                    <div className="settlement-info">
                                        <div className="info-row">
                                            <span className="label">Transfer ID:</span>
                                            <span className="value">{s.id}</span>
                                        </div>
                                        <div className="info-row">
                                            <span className="label">Amount:</span>
                                            <span className="value amount">₹{s.amount?.toFixed(2) || '0.00'}</span>
                                        </div>
                                        <div className="info-row">
                                            <span className="label">From:</span>
                                            <span className="value">User {s.senderId}</span>
                                        </div>
                                        <div className="info-row">
                                            <span className="label">To:</span>
                                            <span className="value">User {s.receiverId}</span>
                                        </div>
                                        <div className="info-row">
                                            <span className="label">Settlement Date:</span>
                                            <span className={`value ${category}-text`}>
                                                {new Date(s.settlementDate).toLocaleString()}
                                                {category === 'overdue' && ' ⚠️ OVERDUE'}
                                                {category === 'due-today' && ' 🔔 TODAY'}
                                                {category === 'due-tomorrow' && ' 📅 TOMORROW'}
                                            </span>
                                        </div>
                                        {s.fraudFlagged && (
                                            <div className="info-row">
                                                <span className="label">Status:</span>
                                                <span className="value warning">⚠️ Flagged for Review</span>
                                            </div>
                                        )}
                                    </div>
                                    <button
                                        className={`process-btn ${category}`}
                                        onClick={() => processSettlement(s.id)}
                                        disabled={processingId === s.id}
                                    >
                                        {processingId === s.id ? 'Processing...' : 'Process Now'}
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                    {totalPages > 1 && (
                        <div className="pagination">
                            <button onClick={() => handlePageChange(page - 1)} disabled={page === 0}>Previous</button>
                            <span>Page {page + 1} of {totalPages}</span>
                            <button onClick={() => handlePageChange(page + 1)} disabled={page >= totalPages - 1}>Next</button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default SettlementMonitor;
