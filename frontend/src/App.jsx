import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import './index.css'

// Authentication
import AuthContainer from './components/auth/AuthContainer'
import PrivateRoute from './components/auth/PrivateRoute'

// Wallet
import WalletBalance from './components/wallet/WalletBalance'
import DepositForm from './components/wallet/DepositForm'
import WalletStatusToggle from './components/wallet/WalletStatusToggle'

// Transfer
import UserSearch from './components/transfer/UserSearch'
import TransferForm from './components/transfer/TransferForm'
import TransactionHistory from './components/transfer/TransactionHistory'
import TransferSummary from './components/transfer/TransferSummary'
import TransferDetail from './components/transfer/TransferDetail'

// Masking
import MaskingToggle from './components/masking/MaskingToggle'

// Notifications
import UnreadCountBadge from './components/notification/UnreadCountBadge'
import NotificationList from './components/notification/NotificationList'

// Admin
import DashboardStats from './components/admin/DashboardStats'
import TransactionMonitor from './components/admin/TransactionMonitor'
import SettlementMonitor from './components/admin/SettlementMonitor'
import FraudReview from './components/admin/FraudReview'
import JobMonitor from './components/admin/JobMonitor'
import ReconciliationReport from './components/admin/ReconciliationReport'

function App() {
    return (
        <div className="app">
            <Router>
                <header className="header">
                    <h1>P2P Payment Ledger</h1>
                    <UnreadCountBadge />
                </header>
                <main className="main">
                    <Routes>
                        {/* Public Routes */}
                        <Route path="/login" element={<AuthContainer />} />
                        <Route path="/register" element={<AuthContainer />} />
                        
                        {/* User Dashboard Routes - Protected */}
                        <Route path="/wallet" element={
                            <PrivateRoute>
                                <div className="dashboard">
                                    <WalletBalance />
                                    <DepositForm />
                                    <WalletStatusToggle />
                                </div>
                            </PrivateRoute>
                        } />
                        
                        <Route path="/transfers" element={
                            <PrivateRoute>
                                <div className="dashboard">
                                    <TransferForm />
                                    <UserSearch />
                                    <TransactionHistory />
                                    <TransferSummary />
                                    <MaskingToggle />
                                </div>
                            </PrivateRoute>
                        } />
                        
                        <Route path="/transfers/:id" element={
                            <PrivateRoute>
                                <div className="dashboard">
                                    <TransferDetail />
                                </div>
                            </PrivateRoute>
                        } />
                        
                        <Route path="/notifications" element={
                            <PrivateRoute>
                                <div className="dashboard">
                                    <NotificationList />
                                </div>
                            </PrivateRoute>
                        } />
                        
                        {/* Admin Routes - Protected & Admin Only */}
                        <Route path="/admin" element={
                            <PrivateRoute requireAdmin>
                                <div className="dashboard">
                                    <DashboardStats />
                                    <TransactionMonitor />
                                    <SettlementMonitor />
                                    <FraudReview />
                                    <JobMonitor />
                                    <ReconciliationReport />
                                </div>
                            </PrivateRoute>
                        } />
                        
                        {/* Default */}
                        <Route path="/" element={<Navigate to="/login" replace />} />
                    </Routes>
                </main>
            </Router>
        </div>
    )
}

export default App
