import { ArrowUpRight, Menu, X } from 'lucide-react';
import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';

export default function Header() {
  const [open, setOpen] = useState(false);
  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" to="/" aria-label="PGConnect home" onClick={() => setOpen(false)}>
          <span className="brand-mark">pg<span>.</span></span>
          <span className="brand-name">PGConnect</span>
        </Link>
        <button className="mobile-menu icon-button" onClick={() => setOpen(!open)} aria-label={open ? 'Close menu' : 'Open menu'}>
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
        <nav className={`header-nav ${open ? 'nav-open' : ''}`}>
          <NavLink to="/" onClick={() => setOpen(false)}>Find a PG</NavLink>
          <a href="#how-it-works" onClick={() => setOpen(false)}>How it works</a>
          <Link className="nav-owner" to="/register?role=owner" onClick={() => setOpen(false)}>List your PG <ArrowUpRight size={15} /></Link>
          <Link className="nav-login" to="/login" onClick={() => setOpen(false)}>Log in</Link>
        </nav>
      </div>
    </header>
  );
}
