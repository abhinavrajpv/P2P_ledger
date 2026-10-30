import { useState, useEffect } from 'react';
import axios from 'axios';

const UserSearch = ({ onUserSelect }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (query.length < 2) {
            setResults([]);
            setError('');
            return;
        }
        const timeoutId = setTimeout(() => {
            fetchUsers();
        }, 400);
        return () => clearTimeout(timeoutId);
    }, [query]);

    const fetchUsers = async () => {
        setLoading(true);
        setError('');
        try {
            const response = await axios.get('/api/users/search', {
                params: { query }
            });
            setResults(response.data || []);
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Search failed');
            setResults([]);
        } finally {
            setLoading(false);
        }
    };

    const handleSelect = (user) => {
        onUserSelect?.(user);
        setQuery('');
        setResults([]);
    };

    return (
        <div className="user-search">
            <h3>Search User</h3>
            <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>
                Search by email or phone to find a receiver's ID
            </p>
            <div className="search-input">
                <input
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Email or phone number..."
                />
            </div>
            {loading && <div className="loading">Searching...</div>}
            {error && <div className="error">{error}</div>}
            {!loading && results.length > 0 && (
                <div className="search-results">
                    {results.map(user => (
                        <div
                            key={user.id}
                            className="search-result"
                            onClick={() => handleSelect(user)}
                            style={{ cursor: 'pointer' }}
                        >
                            <strong>{user.name}</strong>
                            <span style={{ marginLeft: '0.5rem', color: '#555' }}>ID: {user.id}</span>
                            <div style={{ fontSize: '0.8rem', color: '#777' }}>{user.email} · {user.phone}</div>
                            <div style={{ fontSize: '0.75rem', color: '#1976d2', marginTop: '0.25rem' }}>
                                Click to use ID {user.id} in the transfer form
                            </div>
                        </div>
                    ))}
                </div>
            )}
            {!loading && query.length >= 2 && results.length === 0 && !error && (
                <div className="no-results">No users found</div>
            )}
        </div>
    );
};

export default UserSearch;
