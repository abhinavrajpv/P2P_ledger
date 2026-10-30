import { useState, useEffect } from 'react';
import axios from 'axios';

const SettlementMonitor = () => {
    const [settlements, setSettlements] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [processing, setProcessing] = useState(false);
    const [processingId, setProcessingId] = useState(null);

    const fetchSettlements = async (page = 0) => {
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/admin/settlements', {
                headers: { Authorization: `Bearer ${token}` },
                params: { page, size: 10 }
            });
            setSettlements(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
        } catch (error) {
            console.error('Failed to fetch settlements:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSettlements();
    }, []);

    const processSettlement = async (transferId) => {
        setProcessingId(transferId);
        try {
            const token = localStorage.getItem('token');
            await axios.post('/api/admin/settlements/process', { transferId }, {
                headers: { Authorization: `Bearer ${token}` }
            });
            fetchSettlements(page);
        } catch (error) {
            console.error('Failed to process settlement:', error.response?.data?.message || error.message);
        } finally {
            setProcessingId(null);
        }
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchSettlements(newPage);
        }
    };

    return (
        <div className="settlement-monitor">
            <h3>Settlement Monitor</h3>
            {loading && <div className="loading">Loading settlements...</div>}
            {!loading && settlements.length === 0 && <p>No pending settlements</p>}
            {!loading && settlements.length > 0 && (
                <>
                    <div className="settlements-list">
                        {settlements.map(s => (
                            <div key={s.id} className="settlement-item">
                                <div className="settlement-info">
                                    <span>ID: {s.id}</span>
                                    <span>Amount: ₹{s.amount.toFixed(2)}</span>
                                    <span>Sender: {s.senderId}</span>
                                    <span>Receiver: {s.receiverId}</span>
                                </div>
                                <button
                                    onClick={() => processSettlement(s.id)}
                                    disabled={processingId === s.id}
                                >
                                    {processingId === s.id ? 'Processing...' : 'Process Now'}
                                </button>
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

export default SettlementMonitor;
