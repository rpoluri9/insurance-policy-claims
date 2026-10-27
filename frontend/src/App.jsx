import { useEffect, useState } from 'react'
import { api, login, logout, isLoggedIn } from './api.js'

const POLICY_TYPES = ['HEALTH', 'AUTO', 'HOME', 'LIFE', 'TRAVEL']
const POLICY_STATUSES = ['PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED', 'RENEWED']
const CLAIM_STATUSES = ['REGISTERED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'SETTLED']

function useError() {
  const [error, setError] = useState('')
  return [error, setError]
}

function LoginForm({ onLoggedIn }) {
  const [username, setUsername] = useState('demo')
  const [password, setPassword] = useState('demo123')
  const [error, setError] = useError()

  async function submit(e) {
    e.preventDefault()
    setError('')
    login(username, password)
    try {
      // Prove the credentials work before entering the app.
      await api('/api/customers')
      onLoggedIn()
    } catch (err) {
      logout()
      setError('Login failed: ' + err.message)
    }
  }

  return (
    <form onSubmit={submit}>
      <h2>Sign in</h2>
      <p>Demo only — the backend uses a single local demo user (see README).</p>
      <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" />
      <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" />
      <button type="submit">Sign in</button>
      {error && <div className="error">{error}</div>}
    </form>
  )
}

function CustomersPage() {
  const [customers, setCustomers] = useState([])
  const [error, setError] = useError()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', address: '' })

  async function load() {
    try {
      setCustomers(await api('/api/customers'))
    } catch (err) {
      setError(err.message)
    }
  }
  useEffect(() => { load() }, [])

  async function submit(e) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/customers', 'POST', { ...form, phone: form.phone || undefined, address: form.address || undefined })
      setForm({ firstName: '', lastName: '', email: '', phone: '', dateOfBirth: '', address: '' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h2>Customers</h2>
      {error && <div className="error">{error}</div>}
      <form onSubmit={submit}>
        <input required placeholder="First name" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
        <input required placeholder="Last name" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
        <input required type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input placeholder="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        <input type="date" value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
        <input placeholder="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
        <button type="submit">Create customer</button>
      </form>
      <table>
        <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Date of birth</th></tr></thead>
        <tbody>
          {customers.map((c) => (
            <tr key={c.id}><td>{c.id}</td><td>{c.firstName} {c.lastName}</td><td>{c.email}</td><td>{c.phone || ''}</td><td>{c.dateOfBirth || ''}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function PoliciesPage() {
  const [policies, setPolicies] = useState([])
  const [error, setError] = useError()
  const [notice, setNotice] = useState('')
  const [form, setForm] = useState({ customerId: '', policyType: 'HEALTH', coverageAmount: '', effectiveDate: '', renewalDate: '' })
  const [quote, setQuote] = useState({ customerId: '', policyType: 'HEALTH', coverageAmount: '' })
  const [quoteResult, setQuoteResult] = useState(null)

  async function load() {
    try {
      setPolicies(await api('/api/policies'))
    } catch (err) {
      setError(err.message)
    }
  }
  useEffect(() => { load() }, [])

  async function submit(e) {
    e.preventDefault()
    setError('')
    setNotice('')
    try {
      const created = await api('/api/policies', 'POST', { ...form, customerId: Number(form.customerId), coverageAmount: Number(form.coverageAmount) })
      setNotice(`Policy ${created.policyNumber} created — premium ${created.premiumAmount}`)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function submitQuote(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await api('/api/policies/quote', 'POST', { ...quote, customerId: Number(quote.customerId), coverageAmount: Number(quote.coverageAmount) })
      setQuoteResult(res.premiumAmount)
    } catch (err) {
      setError(err.message)
    }
  }

  async function changeStatus(policy, status) {
    setError('')
    try {
      await api(`/api/policies/${policy.id}/status`, 'PUT', { status })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h2>Policies</h2>
      {error && <div className="error">{error}</div>}
      {notice && <div className="notice">{notice}</div>}
      <form onSubmit={submit}>
        <input required type="number" placeholder="Customer ID" value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })} />
        <select value={form.policyType} onChange={(e) => setForm({ ...form, policyType: e.target.value })}>
          {POLICY_TYPES.map((t) => <option key={t}>{t}</option>)}
        </select>
        <input required type="number" step="0.01" placeholder="Coverage amount" value={form.coverageAmount} onChange={(e) => setForm({ ...form, coverageAmount: e.target.value })} />
        <input required type="date" value={form.effectiveDate} onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })} />
        <input required type="date" value={form.renewalDate} onChange={(e) => setForm({ ...form, renewalDate: e.target.value })} />
        <button type="submit">Create policy</button>
      </form>
      <form onSubmit={submitQuote}>
        <input required type="number" placeholder="Customer ID" value={quote.customerId} onChange={(e) => setQuote({ ...quote, customerId: e.target.value })} />
        <select value={quote.policyType} onChange={(e) => setQuote({ ...quote, policyType: e.target.value })}>
          {POLICY_TYPES.map((t) => <option key={t}>{t}</option>)}
        </select>
        <input required type="number" step="0.01" placeholder="Coverage amount" value={quote.coverageAmount} onChange={(e) => setQuote({ ...quote, coverageAmount: e.target.value })} />
        <button type="submit" className="secondary">Get premium quote</button>
        {quoteResult !== null && <span>Premium: {quoteResult}</span>}
      </form>
      <table>
        <thead><tr><th>Policy no.</th><th>Customer</th><th>Type</th><th>Coverage</th><th>Premium</th><th>Status</th><th>Change status</th></tr></thead>
        <tbody>
          {policies.map((p) => (
            <tr key={p.id}>
              <td>{p.policyNumber}</td>
              <td>{p.customer ? `${p.customer.firstName} ${p.customer.lastName}` : p.customerId}</td>
              <td>{p.policyType}</td><td>{p.coverageAmount}</td><td>{p.premiumAmount}</td><td>{p.status}</td>
              <td>
                <select defaultValue="" onChange={(e) => e.target.value && changeStatus(p, e.target.value)}>
                  <option value="" disabled>Move to…</option>
                  {POLICY_STATUSES.map((s) => <option key={s}>{s}</option>)}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ClaimsPage() {
  const [claims, setClaims] = useState([])
  const [error, setError] = useError()
  const [form, setForm] = useState({ policyId: '', description: '', claimAmount: '', incidentDate: '' })

  async function load() {
    try {
      setClaims(await api('/api/claims'))
    } catch (err) {
      setError(err.message)
    }
  }
  useEffect(() => { load() }, [])

  async function submit(e) {
    e.preventDefault()
    setError('')
    try {
      await api('/api/claims', 'POST', { ...form, policyId: Number(form.policyId), claimAmount: Number(form.claimAmount) })
      setForm({ policyId: '', description: '', claimAmount: '', incidentDate: '' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function changeStatus(claim, status) {
    setError('')
    try {
      await api(`/api/claims/${claim.id}/status`, 'PUT', { status, reason: `Moved to ${status} from the web UI` })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function settle(claim) {
    setError('')
    try {
      await api(`/api/claims/${claim.id}/settle`, 'POST')
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h2>Claims</h2>
      {error && <div className="error">{error}</div>}
      <form onSubmit={submit}>
        <input required type="number" placeholder="Policy ID" value={form.policyId} onChange={(e) => setForm({ ...form, policyId: e.target.value })} />
        <input required placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <input required type="number" step="0.01" placeholder="Claim amount" value={form.claimAmount} onChange={(e) => setForm({ ...form, claimAmount: e.target.value })} />
        <input required type="date" value={form.incidentDate} onChange={(e) => setForm({ ...form, incidentDate: e.target.value })} />
        <button type="submit">Register claim</button>
      </form>
      <table>
        <thead><tr><th>Claim no.</th><th>Policy</th><th>Amount</th><th>Status</th><th>Settled</th><th>Actions</th></tr></thead>
        <tbody>
          {claims.map((c) => (
            <tr key={c.id}>
              <td>{c.claimNumber}</td>
              <td>{c.policy ? c.policy.policyNumber : c.policyId}</td>
              <td>{c.claimAmount}</td><td>{c.status}</td><td>{c.settledAmount ?? ''}</td>
              <td>
                <select defaultValue="" onChange={(e) => e.target.value && changeStatus(c, e.target.value)}>
                  <option value="" disabled>Move to…</option>
                  {CLAIM_STATUSES.map((s) => <option key={s}>{s}</option>)}
                </select>
                {' '}
                <button className="secondary" disabled={c.status !== 'APPROVED'} onClick={() => settle(c)}>Settle</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function App() {
  const [loggedIn, setLoggedIn] = useState(isLoggedIn())
  const [tab, setTab] = useState('customers')

  if (!loggedIn) {
    return (
      <div className="container">
        <h1>Insurance Policy and Claims</h1>
        <LoginForm onLoggedIn={() => setLoggedIn(true)} />
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Insurance Policy and Claims</h1>
      <nav>
        <button className={tab === 'customers' ? 'active' : ''} onClick={() => setTab('customers')}>Customers</button>
        <button className={tab === 'policies' ? 'active' : ''} onClick={() => setTab('policies')}>Policies</button>
        <button className={tab === 'claims' ? 'active' : ''} onClick={() => setTab('claims')}>Claims</button>
        <button className="secondary" onClick={() => { logout(); setLoggedIn(false) }}>Sign out</button>
      </nav>
      {tab === 'customers' && <CustomersPage />}
      {tab === 'policies' && <PoliciesPage />}
      {tab === 'claims' && <ClaimsPage />}
    </div>
  )
}
