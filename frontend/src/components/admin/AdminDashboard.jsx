import { useState } from 'react';
import DashboardStats from './DashboardStats';
import UserList from './UserList';
import TransactionMonitor from './TransactionMonitor';
import SettlementMonitor from './SettlementMonitor';
import FraudReview from './FraudReview';
import JobMonitor from './JobMonitor';
import ReconciliationReport from './ReconciliationReport';
import LoginAttemptsMonitor from './LoginAttemptsMonitor';

const AdminDashboard = () => {
    const [activeSection, setActiveSection] = useState('dashboard');

    const menuItems = [
        { id: 'dashboard', label: 'Dashboard', icon: '📊' },
        { id: 'users', label: 'User Management', icon: '👥' },
        { id: 'transactions', label: 'Transaction Monitoring', icon: '💸' },
        { id: 'settlements', label: 'Settlement Monitor', icon: '⏰' },
        { id: 'fraud', label: 'Fraud Review', icon: '🚨' },
        { id: 'login-attempts', label: 'Login Attempts', icon: '🔐' },
        { id: 'jobs', label: 'Scheduled Job Monitor', icon: '⚙️' },
        { id: 'reconciliation', label: 'Reconciliation Report', icon: '📑' }
    ];

    const renderContent = () => {
        switch (activeSection) {
            case 'dashboard':
                return <DashboardStats />;
            case 'users':
                return <UserList />;
            case 'transactions':
                return <TransactionMonitor />;
            case 'settlements':
                return <SettlementMonitor />;
            case 'fraud':
                return <FraudReview />;
            case 'login-attempts':
                return <LoginAttemptsMonitor />;
            case 'jobs':
                return <JobMonitor />;
            case 'reconciliation':
                return <ReconciliationReport />;
            default:
                return <DashboardStats />;
        }
    };

    return (
        <div className="admin-container">
            <div className="admin-sidebar">
                <h2 className="admin-title">Admin Panel</h2>
                <nav className="admin-nav">
                    {menuItems.map(item => (
                        <button
                            key={item.id}
                            className={`admin-nav-item ${activeSection === item.id ? 'active' : ''}`}
                            onClick={() => setActiveSection(item.id)}
                        >
                            <span className="nav-icon">{item.icon}</span>
                            <span className="nav-label">{item.label}</span>
                        </button>
                    ))}
                </nav>
            </div>
            <div className="admin-content">
                <div className="admin-content-header">
                    <h2>{menuItems.find(item => item.id === activeSection)?.label}</h2>
                </div>
                <div className="admin-content-body">
                    {renderContent()}
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;
