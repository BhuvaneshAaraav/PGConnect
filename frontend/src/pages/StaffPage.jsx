import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, Bell, Building2, Check, LogOut, Megaphone, MessageSquare, ShieldCheck, Utensils, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../lib/api.js';
import '../staff.css';

const nav = [['COMPLAINTS', 'Maintenance', MessageSquare], ['ANNOUNCEMENTS', 'Announcements', Bell], ['FOOD_MENU', 'Food menu', Utensils]];
const label = value => String(value || '').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());

export default function StaffPage() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null); const [properties, setProperties] = useState([]); const [propertyId, setPropertyId] = useState('');
  const [requestedSection, setRequestedSection] = useState('COMPLAINTS'); const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [notice, setNotice] = useState('');
  const property = properties.find(item => item.propertyId === propertyId);
  const tabs = useMemo(() => nav.filter(([key]) => property?.permissions?.includes(key)), [property]);
  const section = tabs.some(([key]) => key === requestedSection) ? requestedSection : tabs[0]?.[0];

  async function refresh() {
    setLoading(true); setError('');
    try {
      const account = await api('/api/auth/me');
      if (account.role !== 'STAFF') { navigate(account.role === 'OWNER' ? '/owner' : account.role === 'ADMIN' ? '/admin' : '/resident'); return; }
      setUser(account);
      const list = await api('/api/staff/properties'); setProperties(list);
      setPropertyId(current => list.some(item => item.propertyId === current) ? current : list[0]?.propertyId || '');
    } catch (e) { if ([401, 403].includes(e.status)) navigate('/login'); else setError(e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, []);

  async function loadSection() {
    if (!propertyId || !section) { setData([]); return; }
    setError('');
    const route = section === 'COMPLAINTS' ? 'complaints' : section === 'ANNOUNCEMENTS' ? 'announcements' : 'food-menu';
    try { setData(await api(`/api/staff/properties/${propertyId}/${route}`)); }
    catch (e) { setError(e.message); setData([]); }
  }
  useEffect(() => { loadSection(); }, [propertyId, section]);

  function flash(message) { setNotice(message); setTimeout(() => setNotice(''), 4000); }
  async function act(action, success) {
    setBusy(true); setError('');
    try { await action(); await loadSection(); if (success) flash(success); return true; }
    catch (e) { setError(e.message); return false; }
    finally { setBusy(false); }
  }
  async function logout() { try { await api('/api/auth/logout', { method: 'POST' }); } finally { navigate('/'); } }

  return <main className="workspace-page">
    <aside className="workspace-sidebar">
      <Link to="/" className="brand"><span className="brand-mark">pg<span>.</span></span><span className="brand-name">PGConnect</span></Link>
      <div className="workspace-switcher"><span>ASSIGNED PROPERTY</span><select aria-label="Select assigned property" value={propertyId} onChange={e => setPropertyId(e.target.value)}><option value="">Select a property</option>{properties.map(item => <option key={item.propertyId} value={item.propertyId}>{item.propertyName}</option>)}</select></div>
      <nav className="workspace-nav"><span className="workspace-nav-label">RESIDENT SERVICES</span>{tabs.map(([key, title, Icon]) => <button type="button" key={key} onClick={() => setRequestedSection(key)} className={section === key ? 'workspace-link active' : 'workspace-link'}><Icon size={16}/>{title}</button>)}</nav>
      <button className="workspace-logout" onClick={logout}><LogOut size={16}/>Log out</button>
    </aside>
    <section className="workspace-content">
      <div className="workspace-topbar"><div><span className="workspace-eyebrow">STAFF WORKSPACE</span><span className="workspace-account"><span>{user?.name?.slice(0, 1)?.toUpperCase() || 'S'}</span>{user?.name || 'Staff'}</span></div><span className="admin-secure"><ShieldCheck size={15}/> Owner granted access</span></div>
      {notice && <div className="notice-success"><Check size={16}/>{notice}<button onClick={() => setNotice('')} aria-label="Dismiss"><X size={14}/></button></div>}
      {error && <div className="notice-error"><AlertCircle size={16}/>{error}<button onClick={() => setError('')} aria-label="Dismiss"><X size={14}/></button></div>}
      {loading ? <div className="owner-loading"/> : !property ? <section className="workspace-empty"><Building2 size={28}/><h1>No property access</h1><p>Ask a PG owner to grant access to a property after you verify your staff account.</p></section> : <>
        <div className="workspace-heading"><div><span className="section-kicker">{property.locality}{property.city ? ` · ${property.city}` : ''}</span><h1>{nav.find(([key]) => key === section)?.[1] || 'Staff workspace'}</h1><p>{property.propertyName} <span>·</span> Your access is limited to owner-granted resident services.</p></div></div>
        {section === 'COMPLAINTS' && <Complaints propertyId={propertyId} complaints={data} act={act} busy={busy}/>}
        {section === 'ANNOUNCEMENTS' && <Announcements propertyId={propertyId} announcements={data} act={act} busy={busy}/>}
        {section === 'FOOD_MENU' && <Menu propertyId={propertyId} items={data} act={act} busy={busy}/>}
      </>}
      <footer className="workspace-footer">PGConnect staff workspace <span>·</span> Property services assigned by the owner</footer>
    </section>
  </main>;
}

function Complaints({propertyId, complaints, act, busy}) {
  return <div className="workspace-panel"><div className="panel-heading"><div><h2>Resident maintenance requests</h2><p>Update the status and leave a reply for the resident.</p></div></div>{complaints.length ? <div className="complaint-list">{complaints.map(item => <article key={item.id}><div className="complaint-meta"><b>{item.title}</b><span className={`status-badge status-${String(item.status).toLowerCase()}`}>{label(item.status)}</span></div><p>{item.description}</p><small>{item.category} · {new Date(item.createdAt).toLocaleString()}</small>{item.ownerResponse && <blockquote>{item.ownerResponse}</blockquote>}<ComplaintActions propertyId={propertyId} complaint={item} act={act} busy={busy}/></article>)}</div> : <Empty title="No maintenance requests" text="Resident reports for this property will appear here."/>}</div>;
}
function ComplaintActions({propertyId, complaint, act, busy}) {
  const [response, setResponse] = useState(complaint.ownerResponse || ''); const [status, setStatus] = useState(complaint.status);
  return <form className="complaint-actions" onSubmit={e => { e.preventDefault(); act(() => api(`/api/staff/properties/${propertyId}/complaints/${complaint.id}`, { method: 'PATCH', body: JSON.stringify({status, response}) }), 'Request updated and resident notified.'); }}><select value={status} onChange={e => setStatus(e.target.value)}>{['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'].map(value => <option key={value}>{value}</option>)}</select><input value={response} onChange={e => setResponse(e.target.value)} maxLength="1000" placeholder="Response to resident"/><button className="secondary-action" disabled={busy}>Update</button></form>;
}
function Announcements({propertyId, announcements, act, busy}) {
  const [title, setTitle] = useState(''); const [body, setBody] = useState(''); const [expiresOn, setExpiresOn] = useState('');
  return <><form className="workspace-form announcement-form" onSubmit={async e => {e.preventDefault(); const expiresAt = expiresOn ? new Date(`${expiresOn}T23:59:59`).toISOString() : null; const saved=await act(() => api(`/api/staff/properties/${propertyId}/announcements`, {method:'POST', body:JSON.stringify({title,body,expiresAt})}), 'Announcement published to residents.'); if(saved){setTitle('');setBody('');setExpiresOn('');}}}><h2>Post a property announcement</h2><p className="field-hint">This notice will go to residents at this property.</p><label>Title<input required maxLength="140" value={title} onChange={e=>setTitle(e.target.value)}/></label><label>Message<textarea required maxLength="2000" rows="3" value={body} onChange={e=>setBody(e.target.value)}/></label><label>Optional expiry date<input type="date" value={expiresOn} onChange={e=>setExpiresOn(e.target.value)}/></label><button className="primary-action" disabled={busy}><Megaphone size={15}/>Publish announcement</button></form><div className="workspace-panel"><div className="announcement-list">{announcements.map(item=><article key={item.id}><span>{new Date(item.publishedAt).toLocaleDateString()} {item.expiresAt?`· Expires ${new Date(item.expiresAt).toLocaleDateString()}`:''}</span><h3>{item.title}</h3><p>{item.body}</p><button className="danger-action" disabled={busy} onClick={()=>{if(window.confirm('Remove this resident announcement?'))act(()=>api(`/api/staff/properties/${propertyId}/announcements/${item.id}`,{method:'DELETE'}),'Announcement removed.');}}>Remove</button></article>)}{!announcements.length&&<Empty title="No announcements" text="There are no resident notices for this property yet."/>}</div></div></>;
}
function Menu({propertyId,items,act,busy}) {
  const [meal,setMeal]=useState({dayOfWeek:1,meal:'BREAKFAST',description:'',vegetarianOption:'',nonVegetarianOption:'',mealTime:'',published:true});
  return <><form className="workspace-form menu-form" onSubmit={e=>{e.preventDefault();act(()=>api(`/api/staff/properties/${propertyId}/food-menu`,{method:'PUT',body:JSON.stringify(meal)}),'Weekly menu item saved.');}}><h2>Weekly meal item</h2><div className="workspace-form-grid"><label>Day<select value={meal.dayOfWeek} onChange={e=>setMeal({...meal,dayOfWeek:Number(e.target.value)})}>{['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'].map((day,index)=><option value={index+1} key={day}>{day}</option>)}</select></label><label>Meal<select value={meal.meal} onChange={e=>setMeal({...meal,meal:e.target.value})}>{['BREAKFAST','LUNCH','EVENING_SNACKS','DINNER'].map(value=><option key={value}>{value}</option>)}</select></label></div><label>Description<input required maxLength="500" value={meal.description} onChange={e=>setMeal({...meal,description:e.target.value})}/></label><div className="workspace-form-grid"><label>Vegetarian option<input maxLength="30" value={meal.vegetarianOption} onChange={e=>setMeal({...meal,vegetarianOption:e.target.value})}/></label><label>Non-vegetarian option<input maxLength="30" value={meal.nonVegetarianOption} onChange={e=>setMeal({...meal,nonVegetarianOption:e.target.value})}/></label></div><label>Meal time<input maxLength="30" value={meal.mealTime} onChange={e=>setMeal({...meal,mealTime:e.target.value})}/></label><label className="checkbox-label"><input type="checkbox" checked={meal.published} onChange={e=>setMeal({...meal,published:e.target.checked})}/> Visible to residents</label><button className="primary-action" disabled={busy}><Utensils size={15}/>Save weekly menu</button></form><div className="workspace-panel"><div className="workspace-table-wrap"><table><thead><tr><th>Day</th><th>Meal</th><th>Menu</th><th>Vegetarian</th><th>Non-vegetarian</th><th>Visible</th></tr></thead><tbody>{items.map(item=><tr key={item.id}><td>{['','Mon','Tue','Wed','Thu','Fri','Sat','Sun'][item.dayOfWeek]}</td><td>{label(item.meal)}</td><td>{item.description}</td><td>{item.vegetarianOption||'—'}</td><td>{item.nonVegetarianOption||'—'}</td><td>{item.published?'Yes':'Draft'}</td></tr>)}</tbody></table>{!items.length&&<Empty title="No menu items configured" text="Weekly menu entries will appear here once saved."/>}</div></div></>;
}
function Empty({title,text}) { return <div className="workspace-empty-inline"><h3>{title}</h3><p>{text}</p></div>; }
