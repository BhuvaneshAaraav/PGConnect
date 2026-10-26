import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight, Eye, EyeOff, LockKeyhole, Mail, ShieldCheck, UserRound } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import './auth-google.css';

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

export default function AuthPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const isRegister = location.pathname === '/register';
  const requestedRole = new URLSearchParams(location.search).get('role')?.toUpperCase();
  const initialRole = ['OWNER', 'STAFF'].includes(requestedRole) ? requestedRole : 'RESIDENT';
  const [role, setRole] = useState(initialRole);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function finishSignIn() {
    const me = await api('/api/auth/me');
    const next = new URLSearchParams(location.search).get('next');
    if (next?.startsWith('/') && !next.startsWith('//')) navigate(next);
    else navigate(me.role === 'OWNER' ? '/owner' : me.role === 'ADMIN' ? '/admin' : me.role === 'STAFF' ? '/staff' : '/resident');
  }

  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    try {
      if (isRegister) await api('/api/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password, role }) });
      else await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
      await finishSignIn();
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  async function signInWithGoogle(credential) {
    setBusy(true); setError('');
    try {
      await api('/api/auth/google', { method: 'POST', body: JSON.stringify({ credential, role: isRegister ? role : null }) });
      await finishSignIn();
    } catch (err) { setError(err.message); }
    finally { setBusy(false); }
  }

  return (
    <main className="auth-page"><div className="auth-aside"><Link to="/" className="back-link"><ArrowLeft size={15} /> Back to discovery</Link><div className="auth-aside-content"><div className="auth-aside-icon"><ShieldCheck size={22} /></div><div className="section-kicker">PGCONNECT</div><h1>{isRegister ? 'A place to begin.' : 'Good to see you.'}</h1><p>{isRegister ? 'Make your move with a little more confidence. Your next place is out there.' : 'Your place, your people, and the details that make it home.'}</p><div className="auth-aside-line" /></div><span className="auth-aside-footer">Find your place. Feel at home.</span></div>
      <div className="auth-main"><div className="auth-form-wrap"><div className="section-kicker">{isRegister ? 'GET STARTED' : 'WELCOME BACK'}</div><h2>{isRegister ? 'Create your account' : 'Log in to PGConnect'}</h2><p className="auth-subtitle">{isRegister ? 'A few details and you’re on your way.' : 'Enter your details to continue.'}</p>
        {isRegister && <div className="role-select"><button type="button" className={role === 'RESIDENT' ? 'selected' : ''} onClick={() => setRole('RESIDENT')}><UserRound size={16} /> I’m looking for a PG</button><button type="button" className={role === 'OWNER' ? 'selected' : ''} onClick={() => setRole('OWNER')}><ShieldCheck size={16} /> I own a PG</button><button type="button" className={role === 'STAFF' ? 'selected' : ''} onClick={() => setRole('STAFF')}><ShieldCheck size={16} /> I’m PG staff</button></div>}
        <form className="auth-form" onSubmit={submit}>
          {isRegister && <label>Your name<input required minLength="2" maxLength="80" autoComplete="name" value={name} onChange={(event) => setName(event.target.value)} placeholder="Full name" /></label>}
          <label>Email address<span className="field-icon"><Mail size={16} /></span><input required type="email" autoComplete="email" maxLength="254" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
          <label>Password<span className="field-icon"><LockKeyhole size={16} /></span><input required type={showPassword ? 'text' : 'password'} autoComplete={isRegister ? 'new-password' : 'current-password'} minLength={isRegister ? 10 : 1} maxLength="72" value={password} onChange={(event) => setPassword(event.target.value)} placeholder={isRegister ? 'At least 10 characters' : 'Your password'} /><button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'}>{showPassword ? <EyeOff size={16} /> : <Eye size={16} />}</button></label>
          {error && <div className="form-error">{error}</div>}
          <button className="form-submit" disabled={busy}>{busy ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'} <ArrowRight size={17} /></button>
        </form>
        <div className="auth-google-section">
          <div className="auth-google-divider"><span>or continue with</span></div>
          {googleClientId ? <GoogleSignInButton clientId={googleClientId} onCredential={signInWithGoogle} /> : <p className="auth-google-note">Google sign-in will be available after the OAuth client ID is configured.</p>}
        </div>
        {!isRegister && <p className="auth-forgot"><Link to="/password-reset">Forgot your password?</Link></p>}
        {isRegister && <div className="auth-verification-note"><ShieldCheck size={15}/><span>{role==='STAFF'?'Verify your email, then ask the property owner to grant your staff account access.': 'Verify your email to request a bed or submit a property for review. Email delivery must be configured by the platform.'}</span></div>}
        <p className="auth-switch">{isRegister ? 'Already have an account?' : 'New to PGConnect?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Log in' : 'Create an account'}</Link></p>
      </div></div></main>
  );
}

function GoogleSignInButton({ clientId, onCredential }) {
  const mount = useRef(null);
  const callback = useRef(onCredential);
  const [error, setError] = useState('');
  callback.current = onCredential;

  useEffect(() => {
    let active = true;
    loadGoogleIdentity().then(() => {
      if (!active || !mount.current) return;
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: response => response?.credential && callback.current(response.credential),
      });
      window.google.accounts.id.renderButton(mount.current, {
        type: 'standard', theme: 'outline', size: 'large', shape: 'rectangular', text: 'continue_with', width: 320,
      });
    }).catch(() => { if (active) setError('Google sign-in could not load. You can still use email and password.'); });
    return () => { active = false; mount.current?.replaceChildren(); };
  }, [clientId]);

  return <div className="google-signin-wrap"><div ref={mount} className="google-signin-button"/>{error && <p className="auth-google-error">{error}</p>}</div>;
}

function loadGoogleIdentity() {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (!window.pgconnectGoogleIdentityPromise) {
    window.pgconnectGoogleIdentityPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => window.google?.accounts?.id ? resolve() : reject(new Error('Google Identity Services did not initialize'));
      script.onerror = () => reject(new Error('Google Identity Services could not load'));
      document.head.appendChild(script);
    }).catch(error => {
      delete window.pgconnectGoogleIdentityPromise;
      throw error;
    });
  }
  return window.pgconnectGoogleIdentityPromise;
}
