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
    const [selectedTransaction, setSelectedTransaction] = useState(null);
    const [ledgerEntries, setLedgerEntries] = useState([]);
    const [loadingLedger, setLoadingLedger] = useState(false);

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

    const fetchLedgerEntries = async (transferId) => {
        setLoadingLedger(true);
        try {
            const response = await axios.get(`/api/transfers/${transferId}/ledger`);
            setLedgerEntries(response.data);
        } catch (error) {
            console.error('Failed to fetch ledger entries:', error.response?.data?.message || error.message);
            setLedgerEntries([]);
        } finally {
            setLoadingLedger(false);
        }
    };

    const handleTransactionClick = (transaction) => {
        setSelectedTransaction(transaction);
        fetchLedgerEntries(transaction.id);
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

    if (selectedTransaction) {
        return (
            <div className="transaction-detail-view">
                <button className="back-btn" onClick={() => setSelectedTransaction(null)}>
                    ← Back to Transactions
                </button>
                
                <div className="transaction-detail-card">
                    <h3>Transaction Details</h3>
                    <div className="detail-grid">
                        <div className="detail-item">
                            <label>Transaction ID:</label>
                            <span>{selectedTransaction.id}</span>
                        </div>
                        <div className="detail-item">
                            <label>Status:</label>
                            <span className={`status-badge status-${selectedTransaction.status?.toLowerCase()}`}>
                                {selectedTransaction.status}
                            </span>
                        </div>
                        <div className="detail-item">
                            <label>Amount:</label>
                            <span className="amount-large">₹{selectedTransaction.amount?.toFixed(2)}</span>
                        </div>
                        <div className="detail-item">
                            <label>From:</label>
                            <span>User {selectedTransaction.senderId}</span>
                        </div>
                        <div className="detail-item">
                            <label>To:</label>
                            <span>User {selectedTransaction.receiverId}</span>
                        </div>
                        <div className="detail-item">
                            <label>Created:</label>
                            <span>{new Date(selectedTransaction.createdAt).toLocaleString('en-IN')}</span>
                        </div>
                        <div className="detail-item">
                            <label>Settlement Date:</label>
                            <span>{new Date(selectedTransaction.settlementDate).toLocaleString('en-IN')}</span>
                        </div>
                        <div className="detail-item">
                            <label>Request ID:</label>
                            <span className="request-id">{selectedTransaction.requestId}</span>
                        </div>
                        {selectedTransaction.fraudFlagged && (
                            <div className="detail-item fraud-warning">
                                <label>⚠️ Fraud Status:</label>
                                <span>This transaction has been flagged for review</span>
                            </div>
                        )}
                    </div>

                    <div className="ledger-section">
                        <h4>Ledger Entries</h4>
                        {loadingLedger && <div className="loading">Loading ledger entries...</div>}
                        {!loadingLedger && ledgerEntries.length === 0 && (
                            <p className="no-entries">No ledger entries found</p>
                        )}
                        {!loadingLedger && ledgerEntries.length > 0 && (
                            <div className="ledger-entries-list">
                                {ledgerEntries.map((entry, index) => (
                                    <div key={index} className={`ledger-entry ${entry.type?.toLowerCase()}`}>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Entry ID:</span>
                                            <span className="ledger-value">{entry.id}</span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">User ID:</span>
                                            <span className="ledger-value">{entry.userId}</span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Type:</span>
                                            <span className={`ledger-type ${entry.type?.toLowerCase()}`}>
                                                {entry.type}
                                            </span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Amount:</span>
                                            <span className="ledger-amount">₹{entry.amount?.toFixed(2)}</span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Balance Before:</span>
                                            <span className="ledger-value">₹{entry.balanceBefore?.toFixed(2)}</span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Balance After:</span>
                                            <span className="ledger-value">₹{entry.balanceAfter?.toFixed(2)}</span>
                                        </div>
                                        <div className="ledger-row">
                                            <span className="ledger-label">Created:</span>
                                            <span className="ledger-value">
                                                {new Date(entry.createdAt).toLocaleString('en-IN')}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="transaction-monitor">
            <div className="filters-container">
                <div className="filter-grid">
                    <select 
                        value={status} 
                        onChange={(e) => setStatus(e.target.value)}
                        className="filter-input"
                    >
                        <option value="">All Status</option>
                        <option value="PENDING">PENDING</option>
                        <option value="SUCCESS">SUCCESS</option>
                        <option value="FAILED">FAILED</option>
                        <option value="DECLINED">DECLINED</option>
                    </select>
                    <input 
                        type="number" 
                        placeholder="Sender ID" 
                        value={senderId} 
                        onChange={(e) => setSenderId(e.target.value)}
                        className="filter-input"
                    />
                    <input 
                        type="number" 
                        placeholder="Receiver ID" 
                        value={receiverId} 
                        onChange={(e) => setReceiverId(e.target.value)}
                        className="filter-input"
                    />
                    <input 
                        type="date" 
                        value={startDate} 
                        onChange={(e) => setStartDate(e.target.value)}
                        placeholder="Start Date"
                        className="filter-input"
                    />
                    <input 
                        type="date" 
                        value={endDate} 
                        onChange={(e) => setEndDate(e.target.value)}
                        placeholder="End Date"
                        className="filter-input"
                    />
                    <input 
                        type="number" 
                        placeholder="Min Amount (₹)" 
                        value={minAmount} 
                        onChange={(e) => setMinAmount(e.target.value)}
                        step="0.01"
                        className="filter-input"
                    />
                    <input 
                        type="number" 
                        placeholder="Max Amount (₹)" 
                        value={maxAmount} 
                        onChange={(e) => setMaxAmount(e.target.value)}
                        step="0.01"
                        className="filter-input"
                    />
                </div>
                <div className="filter-actions">
                    <button onClick={() => fetchTransactions(0)} className="btn-search">Search</button>
                    <button onClick={clearFilters} className="btn-clear">Clear Filters</button>
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
                                className={`transaction-item clickable ${tx.status?.toLowerCase()} ${tx.status === 'PENDING' ? 'highlight-pending' : ''}`}
                                onClick={() => handleTransactionClick(tx)}
                            >
                                <div className="tx-header">
                                    <span className="tx-id">ID: {tx.id}</span>
                                    <span className={`status-badge status-${tx.status?.toLowerCase()}`}>
                                        {tx.status}
                                    </span>
                                </div>
                                <div className="tx-details">
                                    <span className="amount">Amount: ₹{tx.amount?.toFixed(2)}</span>
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
