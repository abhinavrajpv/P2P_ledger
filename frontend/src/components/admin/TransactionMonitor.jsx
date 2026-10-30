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
    const [minAmount, setMinAmount] = useState('');
    const [maxAmount, setMaxAmount] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchTransactions = async (page = 0) => {
        setLoading(true);
        try {
            const params = {
                page,
                size: 10,
                status: status || undefined,
                senderId: senderId || undefined,
                receiverId: receiverId || undefined,
                startDate: startDate || undefined,
                endDate: endDate || undefined,
                minAmount: minAmount || undefined,
                maxAmount: maxAmount || undefined
            };
            const response = await axios.get('/api/admin/transactions', {
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

    const clearFilters = () => {
        setStatus('');
        setSenderId('');
        setReceiverId('');
        setStartDate('');
        setEndDate('');
        setMinAmount('');
        setMaxAmount('');
        fetchTransactions(0);
    };

    return (
        <div className="transaction-monitor">
            <h3>Transaction Monitor</h3>
            <div className="filters">
                <div className="filter-row">
                    <input 
                        type="text" 
                        placeholder="Status (PENDING/SUCCESS/FAILED)" 
                        value={status} 
                        onChange={(e) => setStatus(e.target.value)} 
                    />
                    <input 
                        type="number" 
                        placeholder="Sender ID" 
                        value={senderId} 
                        onChange={(e) => setSenderId(e.target.value)} 
                    />
                    <input 
                        type="number" 
                        placeholder="Receiver ID" 
                        value={receiverId} 
                        onChange={(e) => setReceiverId(e.target.value)} 
                    />
                </div>
                <div className="filter-row">
                    <input 
                        type="date" 
                        value={startDate} 
                        onChange={(e) => setStartDate(e.target.value)} 
                        placeholder="Start Date"
                    />
                    <input 
                        type="date" 
                        value={endDate} 
                        onChange={(e) => setEndDate(e.target.value)} 
                        placeholder="End Date"
                    />
                </div>
                <div className="filter-row">
                    <input 
                        type="number" 
                        placeholder="Min Amount (₹)" 
                        value={minAmount} 
                        onChange={(e) => setMinAmount(e.target.value)}
                        step="0.01"
                    />
                    <input 
                        type="number" 
                        placeholder="Max Amount (₹)" 
                        value={maxAmount} 
                        onChange={(e) => setMaxAmount(e.target.value)}
                        step="0.01"
                    />
                </div>
                <div className="filter-actions">
                    <button onClick={() => fetchTransactions(0)}>Search</button>
                    <button onClick={clearFilters} className="clear-btn">Clear Filters</button>
                </div>
            </div>
            {loading && <div className="loading">Loading...</div>}
            {!loading && transactions.length === 0 && <p>No transactions found</p>}
            {!loading && transactions.length > 0 && (
                <>
                    <div className="transactions-list">
                        {transactions.map(tx => (
                            <div 
                                key={tx.id} 
                                className={`transaction-item ${tx.status?.toLowerCase()} ${tx.status === 'PENDING' ? 'highlight-pending' : ''}`}
                            >
                                <div className="tx-header">
                                    <span className="tx-id">ID: {tx.id}</span>
                                    <span className={`status-badge status-${tx.status?.toLowerCase()}`}>
                                        {tx.status}
                                    </span>
                                </div>
                                <div className="tx-details">
                                    <span className="amount">Amount: ₹{tx.amount.toFixed(2)}</span>
                                    <span>From: User {tx.senderId}</span>
                                    <span>To: User {tx.receiverId}</span>
                                    <span className="date">
                                        {new Date(tx.createdAt).toLocaleString('en-IN')}
                                    </span>
                                    {tx.fraudFlagged && (
                                        <span className="fraud-flag">⚠️ Fraud Flagged</span>
                                    )}
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

export default TransactionMonitor;
