import { useState, useEffect } from 'react';
import axios from 'axios';

const TransactionMonitor = () => {
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [status, setStatus] = useState('');
    const [senderId, setSenderId] = useState('');
    const [receiverId, setReceiverId] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchTransactions = async (page = 0) => {
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            const params = {
                page,
                size: 10,
                status: status || undefined,
                senderId: senderId || undefined,
                receiverId: receiverId || undefined,
                startDate: startDate || undefined,
                endDate: endDate || undefined
            };
            const response = await axios.get('/api/admin/transactions', {
                headers: { Authorization: `Bearer ${token}` },
                params
            });
            setTransactions(response.data.content);
            setTotalPages(response.data.totalPages);
            setPage(page);
        } catch (error) {
            console.error('Failed to fetch transactions:', error.response?.data?.message || error.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTransactions();
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < totalPages) {
            fetchTransactions(newPage);
        }
    };

    return (
        <div className="transaction-monitor">
            <h3>Transaction Monitor</h3>
            <div className="filters">
                <input type="text" placeholder="Status" value={status} onChange={(e) => setStatus(e.target.value)} />
                <input type="number" placeholder="Sender ID" value={senderId} onChange={(e) => setSenderId(e.target.value)} />
                <input type="number" placeholder="Receiver ID" value={receiverId} onChange={(e) => setReceiverId(e.target.value)} />
                <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                <button onClick={() => fetchTransactions(0)}>Search</button>
            </div>
            {loading && <div className="loading">Loading...</div>}
            {!loading && transactions.length === 0 && <p>No transactions found</p>}
            {!loading && transactions.length > 0 && (
                <>
                    <div className="transactions-list">
                        {transactions.map(tx => (
                            <div key={tx.id} className="transaction-item">
                                <span>ID: {tx.id}</span>
                                <span>Amount: ₹{tx.amount.toFixed(2)}</span>
                                <span>Sender: {tx.senderId}</span>
                                <span>Receiver: {tx.receiverId}</span>
                                <span className={`status-${tx.status?.toLowerCase()}`}>{tx.status}</span>
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

export default TransactionMonitor;
