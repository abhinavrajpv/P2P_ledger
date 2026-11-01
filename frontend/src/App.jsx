import { BrowserRouter as Router, Routes, Route, Navigate, NavLink, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import './index.css'
import { getUser, clearAuth, isLoggedIn } from './auth'

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
import MiniStatement from './components/transfer/MiniStatement'

// Masking
import MaskingToggle from './components/masking/MaskingToggle'

// Notifications
import UnreadCountBadge from './components/notification/UnreadCountBadge'
import NotificationList from './components/notification/NotificationList'

// Admin
import DashboardStats from './components/admin/DashboardStats'
import UserList from './components/admin/UserList'
import TransactionMonitor from './components/admin/TransactionMonitor'
import SettlementMonitor from './components/admin/SettlementMonitor'
import FraudReview from './components/admin/FraudReview'
import JobMonitor from './components/admin/JobMonitor'
import ReconciliationReport from './components/admin/ReconciliationReport'

function NavBar() {
    const navigate = useNavigate()
    const user = getUser()
    const loggedIn = isLoggedIn()

    const handleLogout = () => {
        clearAuth()
        navigate('/login')
    }

    if (!loggedIn) return null

    return (
        <nav className="navbar">
            <div className="nav-links">
                <NavLink to="/wallet" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    💳 Wallet
                </NavLink>
                <NavLink to="/transfers" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    💸 Send Money
                </NavLink>
                <NavLink to="/notifications" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    🔔 Notifications <UnreadCountBadge inline />
                </NavLink>
                {user?.role === 'ADMIN' && (
                    <NavLink to="/admin" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                        🛠 Admin
                    </NavLink>
                )}
            </div>
            <div className="nav-user">
                <span className="nav-username">👤 {user?.name || 'User'}</span>
                <button className="logout-btn" onClick={handleLogout}>Logout</button>
            </div>
        </nav>
    )
}

// Wallet page with auto-refresh after deposit
function WalletPage() {
    const [balanceKey, setBalanceKey] = useState(0)
    return (
        <div className="dashboard">
            <WalletBalance key={balanceKey} />
            <DepositForm onSuccess={() => setBalanceKey(k => k + 1)} />
            <WalletStatusToggle />
        </div>
    )
}

// Send Money page — UserSearch auto-fills receiver ID in TransferForm
function TransfersPage() {
    const [selectedReceiverId, setSelectedReceiverId] = useState(null)
    const [selectedTransferId, setSelectedTransferId] = useState(null)
    
    if (selectedTransferId) {
        return (
            <div className="dashboard">
                <TransferDetail 
                    transferId={selectedTransferId} 
                    onBack={() => setSelectedTransferId(null)} 
                />
            </div>
        )
    }
    
    return (
        <div className="dashboard">
            <TransferForm prefilledReceiverId={selectedReceiverId} onReceiverUsed={() => setSelectedReceiverId(null)} />
            <UserSearch onUserSelect={(user) => setSelectedReceiverId(user.id)} />
            <MiniStatement />
            <TransactionHistory onTransferSelect={(transfer) => setSelectedTransferId(transfer.id)} />
            <TransferSummary />
            <MaskingToggle />
        </div>
    )
}

function App() {
    return (
        <div className="app">
            <Router>
                <header className="header">
                    <h1>P2P Payment Ledger</h1>
                </header>
                <NavBar />
                <main className="main">
                    <Routes>
                        {/* Public */}
                        <Route path="/login" element={<AuthContainer />} />
                        <Route path="/register" element={<AuthContainer />} />

                        {/* Wallet */}
                        <Route path="/wallet" element={
                            <PrivateRoute>
                                <WalletPage />
                            </PrivateRoute>
                        } />

                        {/* Send Money / Transfers */}
                        <Route path="/transfers" element={
                            <PrivateRoute>
                                <TransfersPage />
                            </PrivateRoute>
                        } />

                        <Route path="/transfers/:id" element={
                            <PrivateRoute>
                                <div className="dashboard">
                                    <TransferDetail />
                                </div>
                            </PrivateRoute>
                        } />

                        {/* Notifications */}
                        <Route path="/notifications" element={
                            <PrivateRoute>
                                <NotificationList />
                            </PrivateRoute>
                        } />

                        {/* Admin */}
                        <Route path="/admin" element={
                            <PrivateRoute requireAdmin>
                                <div className="dashboard">
                                    <DashboardStats />
                                    <UserList />
                                    <TransactionMonitor />
                                    <SettlementMonitor />
                                    <FraudReview />
                                    <JobMonitor />
                                    <ReconciliationReport />
                                </div>
                            </PrivateRoute>
                        } />

                        {/* Default redirect */}
                        <Route path="/" element={<Navigate to="/login" replace />} />
                    </Routes>
                </main>
            </Router>
        </div>
    )
}

export default App
