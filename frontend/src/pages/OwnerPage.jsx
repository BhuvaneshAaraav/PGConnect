import { useEffect, useMemo, useState } from 'react';
import { AlertCircle, ArrowRight, Banknote, BedDouble, Bell, Building2, CalendarDays, Camera, Check, ChevronDown, CircleHelp, ClipboardList, CreditCard, DoorOpen, FileBarChart2, House, LogOut, Megaphone, MessageSquare, Plus, ShieldCheck, Trash2, Utensils, Users, UserCog, LifeBuoy, X } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api, apiUpload, queryString } from '../lib/api.js';
import SupportTickets from '../components/SupportTickets.jsx';
import '../subscription.css';

const nav = [
  ['overview', 'Overview', House], ['notifications', 'Inbox', Bell], ['properties', 'My properties', Building2], ['inventory', 'Buildings, rooms & beds', DoorOpen],
  ['bookings', 'Booking requests', ClipboardList], ['residents', 'Residents', Users], ['rent', 'Rent & payments', Banknote],
  ['photos', 'Property photos', Camera], ['services', 'Food, complaints & notices', Utensils], ['deposits', 'Deposits', CreditCard],
  ['reports', 'Reports', FileBarChart2], ['staff', 'Staff access', UserCog], ['subscription', 'Subscription & invoices', ShieldCheck], ['support', 'Help & support', LifeBuoy], ['settings', 'Profile & settings', CircleHelp],
];
const fmt = (paise) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(paise || 0) / 100);
const rupees = (amount) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(Number(amount || 0));
const periodNow = () => new Date().toISOString().slice(0, 7);
const statusLabel = (value) => String(value || 'DRAFT').replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());

export default function OwnerPage() {
  const navigate = useNavigate(); const location = useLocation();
  const section = location.pathname.split('/')[2] || 'overview';
  const [user, setUser] = useState(null); const [properties, setProperties] = useState([]); const [propertyId, setPropertyId] = useState('');
  const [data, setData] = useState({}); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false);
  const [error, setError] = useState(''); const [notice, setNotice] = useState(''); const [progress, setProgress] = useState(0);
  const [showPropertyForm, setShowPropertyForm] = useState(false); const [period, setPeriod] = useState(periodNow());
  const [editingProperty, setEditingProperty] = useState(null);
  const property = properties.find((item) => item.id === propertyId) || null;

  async function refresh() {
    setLoading(true); setError('');
    try {
      const account = await api('/api/auth/me');
      if (account.role !== 'OWNER') { navigate(account.role === 'ADMIN' ? '/admin' : account.role === 'STAFF' ? '/staff' : '/resident'); return; }
      setUser(account);
      const list = await api('/api/owner/properties'); setProperties(list);
      setPropertyId((selected) => list.some((item) => item.id === selected) ? selected : list[0]?.id || '');
    } catch (e) { if ([401, 403].includes(e.status)) navigate('/login'); else setError(e.message); }
    finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, []);

  async function loadSection() {
  // Subscription and notifications belong to the owner account.
  // They must work even when the owner has not created a PG yet.

  const accountLevelSections = [
    "notifications",
    "subscription",
  ];

  if (
    !propertyId &&
    !accountLevelSections.includes(section)
  ) {
    setData({});
    return;
  }

  setError("");

  try {
    const paths = {
      overview: [
        `/api/owner/properties/${propertyId}/inventory`,
        `/api/owner/properties/${propertyId}/residents`,
        `/api/owner/properties/${propertyId}/rent/bed-status?billingPeriod=${period}`,
        "/api/owner/subscription",
      ],

      notifications: [
        "/api/owner/notifications",
      ],

      inventory: [
        `/api/owner/properties/${propertyId}/inventory`,
      ],

      bookings: [
        `/api/owner/properties/${propertyId}/bookings`,
      ],

      residents: [
        `/api/owner/properties/${propertyId}/residents`,
      ],

      rent: [
        `/api/owner/properties/${propertyId}/rent/charges?billingPeriod=${period}`,
        `/api/owner/properties/${propertyId}/rent/bed-status?billingPeriod=${period}`,
        `/api/owner/rent/outstanding?${queryString({ propertyId })}`,
      ],

      photos: [
        `/api/owner/properties/${propertyId}/photos`,
      ],

      services: [
        `/api/owner/properties/${propertyId}/complaints`,
        `/api/owner/properties/${propertyId}/announcements`,
        `/api/owner/properties/${propertyId}/food-menu`,
      ],

      deposits: [
        `/api/owner/properties/${propertyId}/deposits`,
        `/api/owner/properties/${propertyId}/residents`,
      ],

      reports: [
        `/api/owner/rent/outstanding?${queryString({ propertyId })}`,
      ],

      staff: [
        `/api/owner/properties/${propertyId}/staff`,
      ],

      subscription: [
        "/api/owner/subscription",
        "/api/owner/subscription/invoices",
      ],
    }[section] || [];

    const results = await Promise.all(
      paths.map((path) => api(path))
    );

    const nextData = Object.fromEntries(
      results.map((value, index) => [
        `value${index}`,
        value,
      ])
    );

    setData(nextData);
  } catch (error) {
    setError(
      error.message || "Unable to load account information."
    );
  }
}
  useEffect(() => { loadSection(); }, [section, propertyId, period]);

  function flash(message) { setNotice(message); setTimeout(() => setNotice(''), 5000); }
  async function act(action, success) {
    setBusy(true); setError('');
    try { await action(); await Promise.all([refresh(), loadSection()]); if (success) flash(success); return true; }
    catch (e) { setError(e.message); return false; }
    finally { setBusy(false); }
  }
  async function logout() { try { await api('/api/auth/logout', { method: 'POST' }); } finally { navigate('/'); } }

  const metrics = useMemo(() => {
    const inventory = data.value0?.buildings ? data.value0 : (section === 'overview' ? data.value0 : null);
    const residents = section === 'overview' ? data.value1 : null;
    const statuses = section === 'overview' ? data.value2 : null;
    const booked = (statuses || []).reduce((sum, b) => sum + (b.paidAmount || 0), 0);
    const due = (statuses || []).reduce((sum, b) => sum + b.outstandingBalance, 0);
    return { inventory, residentCount: residents?.length ?? 0, billed: (statuses || []).reduce((sum, b) => sum + (b.rentAmount || 0), 0), collected: booked, due };
  }, [data, section]);

  return <main className="workspace-page">
    <aside className="workspace-sidebar">
      <Link to="/" className="brand"><span className="brand-mark">pg<span>.</span></span><span className="brand-name">PGConnect</span></Link>
      <div className="workspace-switcher"><span>PROPERTY WORKSPACE</span><select aria-label="Select property" value={propertyId} onChange={(e) => setPropertyId(e.target.value)}><option value="">Select a property</option>{properties.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}</select><ChevronDown size={14} /></div>
      <nav className="workspace-nav"><span className="workspace-nav-label">MANAGE</span>{nav.map(([id, title, Icon]) => <Link key={id} to={`/owner/${id}`} className={section === id ? 'workspace-link active' : 'workspace-link'}><Icon size={16} />{title}</Link>)}</nav>
      <button className="workspace-logout" onClick={logout}><LogOut size={16} />Log out</button>
    </aside>
    <section className="workspace-content">
      <div className="workspace-topbar"><div><span className="workspace-eyebrow">OWNER WORKSPACE</span><span className="workspace-account"><span>{user?.name?.slice(0, 1)?.toUpperCase() || 'O'}</span>{user?.name || 'Owner'}</span></div><div className={`workspace-subscription state-${(user?.subscriptionStatus || 'pending').toLowerCase()}`}><i /> Subscription · {statusLabel(user?.subscriptionStatus || 'PENDING')}</div></div>
      {notice && <div className="notice-success"><Check size={16} />{notice}<button onClick={() => setNotice('')} aria-label="Dismiss"><X size={14} /></button></div>}
      {error && <div className="notice-error"><AlertCircle size={16} />{error}<button onClick={() => setError('')} aria-label="Dismiss"><X size={14} /></button></div>}
      {/* {loading ? <div className="owner-loading" /> : !propertyId && section !== 'support' ? <section className="workspace-empty"><Building2 size={28} /><h1>Start your property workspace</h1><p>Add a property to set up its buildings, rooms, beds, rent and resident services.</p><button className="primary-action" onClick={() => setShowPropertyForm(true)}><Plus size={16} /> Add a property</button></section> : <>
        <div className="workspace-heading"><div><span className="section-kicker">{property ? `${property.locality}${property.city ? ` · ${property.city}` : ''}` : user?.email}</span><h1>{nav.find(([id]) => id === section)?.[1] || 'Owner workspace'}</h1><p>{property ? `${property.name} · ${statusLabel(property.status)}${!property.approved ? ' · Private until approved' : ''}` : 'Account-level support requests'}</p></div>{section === 'properties' && <button className="primary-action" onClick={() => setShowPropertyForm(true)}><Plus size={16} />Add a property</button>}</div>
        {showPropertyForm && <PropertyForm key={editingProperty?.id || 'new'} property={editingProperty} close={() => { setShowPropertyForm(false); setEditingProperty(null); }} saved={async (saved) => { setShowPropertyForm(false); setEditingProperty(null); await refresh(); setPropertyId(saved.id); flash('Draft saved. Add owner photos, configure inventory, activate the subscription, then submit for review.'); }} />}
        {section === 'overview' && <Overview property={property} properties={properties} inventory={metrics.inventory} residents={metrics.residentCount} billed={metrics.billed} collected={metrics.collected} due={metrics.due} period={period} setPeriod={setPeriod} bedStatuses={data.value2 || []} subscription={data.value3} />}
        {section === 'notifications' && <OwnerInbox notifications={data.value0 || []} act={act} busy={busy} />}
        {section === 'properties' && <Properties properties={properties} selected={propertyId} select={setPropertyId} openForm={() => {setEditingProperty(null);setShowPropertyForm(true);}} edit={(p)=>{setEditingProperty(p);setShowPropertyForm(true);}} onAction={act} busy={busy} />}
        {section === 'inventory' && <Inventory propertyId={propertyId} inventory={data.value0} onRefresh={loadSection} act={act} busy={busy} />}
        {section === 'bookings' && <Bookings propertyId={propertyId} bookings={data.value0 || []} act={act} busy={busy} />}
        {section === 'residents' && <Residents residents={data.value0 || []} />}
        {section === 'rent' && <Rent propertyId={propertyId} period={period} setPeriod={setPeriod} charges={data.value0 || []} statuses={data.value1 || []} act={act} busy={busy} />}
        {section === 'photos' && <Photos propertyId={propertyId} photos={data.value0 || []} reload={loadSection} act={act} progress={progress} setProgress={setProgress} />}
        {section === 'services' && <Services propertyId={propertyId} complaints={data.value0 || []} announcements={data.value1 || []} menu={data.value2 || []} act={act} busy={busy} />}
        {section === 'deposits' && <Deposits propertyId={propertyId} deposits={data.value0 || []} residents={data.value1 || []} act={act} busy={busy} />}
        {section === 'reports' && <Reports propertyId={propertyId} rows={data.value0 || []} act={act} busy={busy} />}
        {section === 'staff' && <StaffAccess propertyId={propertyId} members={data.value0 || []} act={act} busy={busy} />}
        {section === 'support' && <SupportTickets />}
        {section === 'subscription' && <Subscription subscription={data.value0} invoices={data.value1 || []} status={user?.subscriptionStatus} act={act} busy={busy} />}
        {section === 'settings' && <Settings user={user} update={(account) => setUser(account)} act={act} busy={busy} />}
      </>} */}

      {loading ? (
  <div className="owner-loading" />
) : (
  <>
    {showPropertyForm && (
      <PropertyForm
        key={editingProperty?.id || "new"}
        property={editingProperty}
        close={() => {
          setShowPropertyForm(false);
          setEditingProperty(null);
        }}
        saved={async (saved) => {
          setShowPropertyForm(false);
          setEditingProperty(null);

          await refresh();

          setPropertyId(saved.id);

          flash(
            "Property draft saved. Upload genuine photos, configure rooms and beds, activate the subscription, and submit for review."
          );
        }}
      />
    )}

    {!propertyId &&
    ![
      "properties",
      "notifications",
      "subscription",
      "support",
      "settings",
    ].includes(section) ? (
      <section className="workspace-empty">
        <Building2 size={28} />

        <h1>Start your property workspace</h1>

        <p>
          Add your real PG property to configure buildings,
          rooms, beds, rent, and resident services.
          Property information will be saved through
          the Java backend to MongoDB.
        </p>

        <button
          className="primary-action"
          onClick={() => {
            setEditingProperty(null);
            setShowPropertyForm(true);
          }}
        >
          <Plus size={16} />
          Add a property
        </button>
      </section>
    ) : (
      <>
        <div className="workspace-heading">
          <div>
            <span className="section-kicker">
              {property
                ? `${property.locality}${
                    property.city
                      ? ` · ${property.city}`
                      : ""
                  }`
                : user?.email}
            </span>

            <h1>
              {nav.find(([id]) => id === section)?.[1] ||
                "Owner workspace"}
            </h1>

            <p>
              {property
                ? `${property.name} · ${statusLabel(
                    property.status
                  )}${
                    !property.approved
                      ? " · Private until approved"
                      : ""
                  }`
                : section === "subscription"
                ? "Account-level subscription and invoices"
                : section === "settings"
                ? "Account-level settings"
                : section === "notifications"
                ? "Account-level notifications"
                : "Account-level support requests"}
            </p>
          </div>

          {section === "properties" && (
            <button
              className="primary-action"
              onClick={() => {
                setEditingProperty(null);
                setShowPropertyForm(true);
              }}
            >
              <Plus size={16} />
              Add a property
            </button>
          )}
        </div>

        {section === "overview" && (
          <Overview
            property={property}
            properties={properties}
            inventory={metrics.inventory}
            residents={metrics.residentCount}
            billed={metrics.billed}
            collected={metrics.collected}
            due={metrics.due}
            period={period}
            setPeriod={setPeriod}
            bedStatuses={data.value2 || []}
            subscription={data.value3}
          />
        )}

        {section === "notifications" && (
          <OwnerInbox
            notifications={data.value0 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "properties" && (
          <Properties
            properties={properties}
            selected={propertyId}
            select={setPropertyId}
            openForm={() => {
              setEditingProperty(null);
              setShowPropertyForm(true);
            }}
            edit={(property) => {
              setEditingProperty(property);
              setShowPropertyForm(true);
            }}
            onAction={act}
            busy={busy}
          />
        )}

        {section === "inventory" && (
          <Inventory
            propertyId={propertyId}
            inventory={data.value0}
            onRefresh={loadSection}
            act={act}
            busy={busy}
          />
        )}

        {section === "bookings" && (
          <Bookings
            propertyId={propertyId}
            bookings={data.value0 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "residents" && (
          <Residents residents={data.value0 || []} />
        )}

        {section === "rent" && (
          <Rent
            propertyId={propertyId}
            period={period}
            setPeriod={setPeriod}
            charges={data.value0 || []}
            statuses={data.value1 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "photos" && (
          <Photos
            propertyId={propertyId}
            photos={data.value0 || []}
            reload={loadSection}
            act={act}
            progress={progress}
            setProgress={setProgress}
          />
        )}

        {section === "services" && (
          <Services
            propertyId={propertyId}
            complaints={data.value0 || []}
            announcements={data.value1 || []}
            menu={data.value2 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "deposits" && (
          <Deposits
            propertyId={propertyId}
            deposits={data.value0 || []}
            residents={data.value1 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "reports" && (
          <Reports
            propertyId={propertyId}
            rows={data.value0 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "staff" && (
          <StaffAccess
            propertyId={propertyId}
            members={data.value0 || []}
            act={act}
            busy={busy}
          />
        )}

        {section === "support" && <SupportTickets />}

        {section === "subscription" && (
          <Subscription
            subscription={data.value0}
            invoices={data.value1 || []}
            status={user?.subscriptionStatus}
            act={act}
            busy={busy}
          />
        )}

        {section === "settings" && (
          <Settings
            user={user}
            update={(account) => setUser(account)}
            act={act}
            busy={busy}
          />
        )}
      </>
    )}
  </>
)}
      <footer className="workspace-footer">PGConnect owner workspace <span>·</span> Records shown from your account</footer>
    </section>
  </main>;
}

function PropertyForm({ close, saved, property }) {
  const [form, setForm] = useState(property ? { name:property.name||'',streetAddress:property.streetAddress||'',locality:property.locality||'',city:property.city||'',state:property.state||'',postalCode:property.postalCode||'',accommodationType:property.accommodationType||'CO_LIVING',startingRent:property.startingRent||'',latitude:property.location?.y||'',longitude:property.location?.x||'',facilities:(property.facilities||[]).join(', '),foodAvailable:!!property.foodAvailable,airConditioned:!!property.airConditioned } : { name:'',streetAddress:'',locality:'',city:'',state:'',postalCode:'',accommodationType:'CO_LIVING',startingRent:'',latitude:'',longitude:'',facilities:'',foodAvailable:false,airConditioned:false });
  const [error,setError]=useState(''); const [busy,setBusy]=useState(false);
  async function submit(e){e.preventDefault();setBusy(true);setError('');try{const p=await api(property?`/api/owner/properties/${property.id}`:'/api/owner/properties',{method:property?'PUT':'POST',body:JSON.stringify({...form,startingRent:Number(form.startingRent),latitude:form.latitude?Number(form.latitude):null,longitude:form.longitude?Number(form.longitude):null,facilities:form.facilities.split(',').map(x=>x.trim()).filter(Boolean)})});await saved(p);}catch(err){setError(err.message);}finally{setBusy(false);}}
  return <form className="workspace-form property-create-form" onSubmit={submit}><div className="workspace-form-heading"><div><h2>{property?'Edit property draft':'New property draft'}</h2><p>Use real property details. Drafts remain private during setup.</p></div><button type="button" className="icon-button" onClick={close} aria-label="Close"><X size={18}/></button></div>{error&&<div className="notice-error">{error}</div>}
    <label>Property name<input required maxLength="120" value={form.name} onChange={e=>setForm({...form,name:e.target.value})}/></label><label>Street address<input required maxLength="220" value={form.streetAddress} onChange={e=>setForm({...form,streetAddress:e.target.value})}/></label>
    <div className="workspace-form-grid"><label>Locality<input required value={form.locality} onChange={e=>setForm({...form,locality:e.target.value})}/></label><label>City<input required value={form.city} onChange={e=>setForm({...form,city:e.target.value})}/></label><label>State<input required value={form.state} onChange={e=>setForm({...form,state:e.target.value})}/></label><label>PIN code<input required pattern="[0-9]{6}" value={form.postalCode} onChange={e=>setForm({...form,postalCode:e.target.value})}/></label></div>
    <div className="workspace-form-grid"><label>PG type<select value={form.accommodationType} onChange={e=>setForm({...form,accommodationType:e.target.value})}><option value="BOYS">Boys’ PG</option><option value="GIRLS">Girls’ PG</option><option value="CO_LIVING">Co-living</option></select></label><label>Starting rent (₹)<input required type="number" min="1" value={form.startingRent} onChange={e=>setForm({...form,startingRent:e.target.value})}/></label></div>
    <label>Facilities <span className="field-hint">Comma separated</span><input value={form.facilities} onChange={e=>setForm({...form,facilities:e.target.value})} placeholder="Wi-Fi, laundry, hot water"/></label><div className="workspace-form-grid"><label>Latitude<input type="number" step="any" min="-90" max="90" value={form.latitude} onChange={e=>setForm({...form,latitude:e.target.value})}/></label><label>Longitude<input type="number" step="any" min="-180" max="180" value={form.longitude} onChange={e=>setForm({...form,longitude:e.target.value})}/></label></div><div className="workspace-check-grid"><label className="checkbox-label"><input type="checkbox" checked={form.foodAvailable} onChange={e=>setForm({...form,foodAvailable:e.target.checked})}/> Food available</label><label className="checkbox-label"><input type="checkbox" checked={form.airConditioned} onChange={e=>setForm({...form,airConditioned:e.target.checked})}/> AC available</label></div>
    <div className="form-actions"><button type="button" className="secondary-action" onClick={close}>Cancel</button><button disabled={busy} className="primary-action">{busy?'Saving…':'Save draft'}<ArrowRight size={15}/></button></div>
  </form>;
}

function Overview({ property, properties, inventory, residents, billed, collected, due, period, setPeriod, bedStatuses, subscription }) {
  return <>
    <div className="workspace-notice"><ShieldCheck size={18}/><span><b>Public visibility is checked by the server.</b> A listing needs approved photos, active subscription and platform approval. Current subscription: {statusLabel(subscription?.status || 'PENDING')}.</span></div>
    <div className="workspace-metrics"><Metric label="PROPERTIES" value={properties.length} note="Across your account"/><Metric label="BUILDINGS" value={inventory?.buildings?.length ?? '—'} note="Selected property"/><Metric label="ROOMS & BEDS" value={`${inventory?.buildings?.reduce((sum,b)=>sum+b.rooms.length,0)??'—'} / ${inventory?.totalBeds??'—'}`} note="Actual configured inventory"/><Metric label="RESIDENTS" value={residents} note="Current property tenancies"/><Metric label="RENT BILLED" value={rupees(billed)} note={period}/><Metric label="RENT COLLECTED" value={rupees(collected)} note="Recorded payments"/><Metric label="OUTSTANDING" value={rupees(due)} note="Current selected period"/><Metric label="AVAILABLE BEDS" value={inventory?.availableBeds ?? '—'} note="Live bed records"/></div>
    <div className="workspace-panel"><div className="panel-heading"><div><h2>Monthly room and bed status</h2><p>Color and text use each actual bed charge for the selected billing period.</p></div><label className="period-label"><CalendarDays size={16}/><input type="month" value={period} onChange={e=>setPeriod(e.target.value)}/></label></div><BedGrid statuses={bedStatuses}/></div>
    <div className="workspace-panel"><div className="panel-heading"><div><h2>Property publication</h2><p>{property?.name} · {statusLabel(property?.status)}</p></div><StatusBadge value={property?.status}/></div><p className="subtle-copy">{property?.photos?.length ? `${property.photos.length} owner-uploaded photo(s) recorded.` : 'Upload genuine photos before submitting this property for review.'} Add buildings, rooms and beds before accepting booking requests.</p></div>
  </>;
}
function Metric({label,value,note}){return <article className="workspace-metric"><span>{label}</span><strong>{value}</strong><small>{note}</small></article>}
function StatusBadge({value}){return <span className={`status-badge status-${String(value||'draft').toLowerCase()}`}>{statusLabel(value)}</span>}
function BedGrid({statuses=[]}){
  if(!statuses.length)return <Empty title="No bed records yet" text="Once you add buildings, rooms and beds, the monthly room status will appear here."/>;
  const rooms=Object.values(statuses.reduce((all,bed)=>{const key=`${bed.buildingId}-${bed.roomId}`;(all[key] ||= {building:bed.buildingName||'Building',room:bed.roomNumber||'Room',beds:[]}).beds.push(bed);return all;},{}));
  return <div className="bed-grid">{rooms.map((room,i)=><div className="bed-room" key={`${room.room}-${i}`}><div><b>{room.building}</b><span>Room {room.room}</span></div><div className="bed-pills">{room.beds.map(b=><span key={b.bedId} className={`bed-indicator indicator-${b.indicator?.toLowerCase()}`} title={`${b.residentName||'Vacant'} · ${statusLabel(b.indicator)}`}><b>{b.bedCode}</b><small>{statusLabel(b.indicator)}</small></span>)}</div></div>)}</div>;
}
function Empty({title,text}){return <div className="workspace-empty-inline"><Building2 size={20}/><b>{title}</b><span>{text}</span></div>}

function Properties({properties,selected,select,openForm,edit,onAction,busy}){
  return <><div className="workspace-list-toolbar"><p>Only these actual owner-created records are in your account.</p><button className="primary-action" onClick={openForm}><Plus size={15}/>Add property</button></div>{properties.length?<div className="workspace-property-list">{properties.map(p=><article className={`workspace-property-card ${p.id===selected?'selected':''}`} key={p.id}><div className="property-card-main" onClick={()=>select(p.id)}><div className="workspace-property-symbol"><Building2 size={20}/></div><div><h3>{p.name}</h3><p>{[p.locality,p.city,p.state].filter(Boolean).join(', ')}</p><span>{p.accommodationType?.replaceAll('_',' ')} · from {rupees(p.startingRent)}/month</span></div></div><StatusBadge value={p.status}/><div className="property-card-actions">{['DRAFT','REJECTED'].includes(p.status)&&<><button className="secondary-action" onClick={()=>edit(p)}>Edit details</button><button className="secondary-action" disabled={busy} onClick={()=>onAction(()=>api(`/api/owner/properties/${p.id}/submit`,{method:'POST'}),'Property submitted for administrator review.')}>Submit for review</button></>}{p.status==='PUBLISHED'&&<button className="secondary-action" onClick={()=>onAction(()=>api(`/api/owner/properties/${p.id}/listing?enabled=${!p.listingEnabled}`,{method:'PATCH'}),'Listing visibility updated.')}>{p.listingEnabled?'Pause listing':'Publish listing'}</button>}</div></article>)}</div>:<Empty title="No properties on your account" text="Create a draft to get started; no example listings are added."/>}</>;
}

function Inventory({propertyId,inventory,act,busy}){
  const [building,setBuilding]=useState({name:'',code:'',floorCount:1});const [room,setRoom]=useState({buildingId:'',floorNumber:0,roomNumber:'',sharingType:'DOUBLE',monthlyRent:'',bedCodes:'',facilities:''});
  async function addBuilding(e){e.preventDefault();await act(()=>api(`/api/owner/properties/${propertyId}/buildings`,{method:'POST',body:JSON.stringify({...building,floorCount:Number(building.floorCount)})}),'Building created.');setBuilding({name:'',code:'',floorCount:1});}
  async function addRoom(e){e.preventDefault();await act(()=>api(`/api/owner/properties/${propertyId}/buildings/${room.buildingId}/rooms`,{method:'POST',body:JSON.stringify({...room,floorNumber:Number(room.floorNumber),monthlyRent:Number(room.monthlyRent),bedCodes:room.bedCodes.split(',').map(x=>x.trim()).filter(Boolean),facilities:room.facilities.split(',').map(x=>x.trim()).filter(Boolean)})}),'Room and bed records created.');setRoom({...room,roomNumber:'',monthlyRent:'',bedCodes:''});}
  return <><div className="workspace-metrics inventory-metrics"><Metric label="TOTAL BEDS" value={inventory?.totalBeds??0} note="Explicit bed records"/><Metric label="OCCUPIED" value={inventory?.occupiedBeds??0} note="Active tenancies"/><Metric label="AVAILABLE" value={inventory?.availableBeds??0} note="Can be requested"/><Metric label="RESERVED" value={inventory?.reservedBeds??0} note="Temporary bookings"/><Metric label="MAINTENANCE" value={inventory?.maintenanceBeds??0} note="Not bookable"/></div>
    <div className="inventory-layout"><section className="workspace-panel"><div className="panel-heading"><div><h2>Buildings, floors and rooms</h2><p>Capacity must match individual bed records.</p></div></div>{inventory?.buildings?.length?inventory.buildings.map(b=><div className="building-block" key={b.id}><h3>{b.name} <span>Building {b.code} · {b.floorCount} floors</span></h3>{b.rooms.map(r=><div className="room-block" key={r.id}><div><b>Room {r.roomNumber}</b><small>Floor {r.floorNumber} · {r.sharingType.replaceAll('_',' ')} · {rupees(r.monthlyRent)}/month</small></div><div className="bed-pills">{r.beds.map(bed=><div className={`bed-indicator indicator-${bed.status.toLowerCase()}`} key={bed.id}><b>{bed.bedCode}</b><small>{statusLabel(bed.status)}</small>{['AVAILABLE','MAINTENANCE','UNAVAILABLE'].includes(bed.status)&&<select aria-label={`Update ${bed.bedCode}`} value={bed.status} onChange={e=>act(()=>api(`/api/owner/properties/${propertyId}/beds/${bed.id}/status?status=${e.target.value}`,{method:'PATCH'}),'Bed status updated.')}><option value="AVAILABLE">Available</option><option value="MAINTENANCE">Maintenance</option><option value="UNAVAILABLE">Unavailable</option></select>}</div>)}</div></div>)}</div>):<Empty title="No buildings configured" text="Add your actual buildings, rooms and individual beds below."/>}</section>
    <div className="inventory-forms"><form className="workspace-form" onSubmit={addBuilding}><h2>Add a building</h2><label>Building name<input required value={building.name} onChange={e=>setBuilding({...building,name:e.target.value})}/></label><div className="workspace-form-grid"><label>Building code<input required value={building.code} onChange={e=>setBuilding({...building,code:e.target.value})}/></label><label>Floor count<input required type="number" min="1" max="100" value={building.floorCount} onChange={e=>setBuilding({...building,floorCount:e.target.value})}/></label></div><button className="primary-action" disabled={busy}>Add building</button></form><form className="workspace-form" onSubmit={addRoom}><h2>Add room and beds</h2><label>Building<select required value={room.buildingId} onChange={e=>setRoom({...room,buildingId:e.target.value})}><option value="">Select building</option>{(inventory?.buildings||[]).map(b=><option key={b.id} value={b.id}>{b.name} · {b.code}</option>)}</select></label><div className="workspace-form-grid"><label>Floor<input required type="number" min="0" value={room.floorNumber} onChange={e=>setRoom({...room,floorNumber:e.target.value})}/></label><label>Room number<input required value={room.roomNumber} onChange={e=>setRoom({...room,roomNumber:e.target.value})}/></label><label>Sharing<select value={room.sharingType} onChange={e=>setRoom({...room,sharingType:e.target.value})}><option value="SINGLE">Single</option><option value="DOUBLE">Double</option><option value="TRIPLE">Triple</option><option value="FOUR_SHARING">Four sharing</option></select></label><label>Monthly rent (₹)<input required type="number" min="1" value={room.monthlyRent} onChange={e=>setRoom({...room,monthlyRent:e.target.value})}/></label></div><label>Bed IDs <span className="field-hint">Comma separated, exactly as many as sharing capacity</span><input required value={room.bedCodes} onChange={e=>setRoom({...room,bedCodes:e.target.value})} placeholder="A1, A2"/></label><label>Room facilities<input value={room.facilities} onChange={e=>setRoom({...room,facilities:e.target.value})} placeholder="AC, study desk"/></label><button className="primary-action" disabled={busy||!room.buildingId}>Create room and beds</button></form></div></div></>;
}

function Bookings({propertyId,bookings,act,busy}){
  return <div className="workspace-panel"><div className="panel-heading"><div><h2>Booking requests</h2><p>Requests temporarily reserve a bed and expire automatically if no move-in is completed.</p></div></div>{bookings.length?<div className="workspace-table-wrap"><table><thead><tr><th>Resident</th><th>Building / room / bed</th><th>Message</th><th>Status</th><th>Expires</th><th>Actions</th></tr></thead><tbody>{bookings.map(b=><tr key={b.id}><td><b>{b.residentName||'Resident'}</b><small>{b.residentEmail}</small></td><td>{b.buildingName||'Building'} · {b.roomNumber||'Room'} / {b.bedCode||'Bed'}</td><td>{b.residentMessage||'—'}</td><td><StatusBadge value={b.status}/></td><td>{b.expiresAt?new Date(b.expiresAt).toLocaleString():''}</td><td>{b.status==='REQUESTED'&&<div className="inline-actions"><button className="secondary-action" disabled={busy} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/bookings/${b.id}/decision`,{method:'POST',body:JSON.stringify({approve:true})}),'Booking approved.')}>Approve</button><button className="danger-action" disabled={busy} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/bookings/${b.id}/decision`,{method:'POST',body:JSON.stringify({approve:false})}),'Booking rejected.')}>Reject</button></div>}{b.status==='APPROVED'&&<button className="primary-action" disabled={busy} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/bookings/${b.id}/move-in`,{method:'POST',body:JSON.stringify({startDate:new Date().toISOString().slice(0,10)})}),'Move-in completed and tenancy created.')}>Move in</button>}</td></tr>)}</tbody></table></div>:<Empty title="No booking requests" text="Eligible listings with available beds can receive resident booking requests."/>}</div>;
}

function Residents({residents}){return <div className="workspace-panel"><div className="panel-heading"><div><h2>Current and move-out residents</h2><p>Resident details are visible only within your own property.</p></div></div>{residents.length?<div className="workspace-table-wrap"><table><thead><tr><th>Resident</th><th>Building</th><th>Room / bed</th><th>Monthly rent</th><th>Move-in date</th><th>Status</th></tr></thead><tbody>{residents.map(r=><tr key={r.tenancyId}><td><b>{r.residentName}</b><small>{r.residentEmail}</small></td><td>{r.buildingName}</td><td>{r.roomNumber} / {r.bedCode}</td><td>{rupees(r.monthlyRent)}</td><td>{r.startDate}</td><td><StatusBadge value={r.status}/></td></tr>)}</tbody></table></div>:<Empty title="No current residents" text="A resident appears here after an approved booking is moved in."/>}</div>}

function Rent({propertyId,period,setPeriod,charges,statuses,act,busy}){
  const [chargeId,setChargeId]=useState('');const [amount,setAmount]=useState('');const [method,setMethod]=useState('CASH');const [reference,setReference]=useState('');
  const unpaid=charges.filter(c=>c.outstandingBalance>0);
  return <><div className="workspace-panel"><div className="panel-heading"><div><h2>Rent charges · {period}</h2><p>Monthly charge snapshots keep historical rent amounts unchanged.</p></div><label className="period-label"><CalendarDays size={16}/><input type="month" value={period} onChange={e=>setPeriod(e.target.value)}/></label></div><div className="form-actions"><button className="primary-action" disabled={busy} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/rent/charges/generate?billingPeriod=${period}`,{method:'POST'}),'Rent charge generation finished. Existing charges were not duplicated.')}>Generate monthly charges</button></div>{charges.length?<div className="workspace-table-wrap"><table><thead><tr><th>Resident</th><th>Room / bed</th><th>Charge</th><th>Paid</th><th>Outstanding</th><th>Due</th><th>Status</th></tr></thead><tbody>{charges.map(c=><tr key={c.id}><td><b>{c.residentName}</b><small>{c.propertyName}</small></td><td>{c.roomNumber} / {c.bedCode}</td><td>{rupees(c.rentAmount)}</td><td>{rupees(c.paidAmount)}</td><td>{rupees(c.outstandingBalance)}</td><td>{c.dueDate}</td><td><StatusBadge value={c.indicator}/></td></tr>)}</tbody></table></div>:<Empty title="No charges for this month" text="Generate monthly charges from active tenancies to begin collecting and recording rent."/>}</div>
    <div className="workspace-panel"><div className="panel-heading"><div><h2>Record an offline payment</h2><p>Cash, bank transfer and offline UPI entries are clearly marked as owner recorded.</p></div></div><form className="payment-entry" onSubmit={e=>{e.preventDefault();const key=crypto.randomUUID();act(()=>api(`/api/owner/properties/${propertyId}/rent/charges/${chargeId}/offline-payment`,{method:'POST',body:JSON.stringify({amount:Number(amount),idempotencyKey:key,method,reference})}),'Offline payment recorded and audited.');setAmount('');setReference('');}}><label>Unpaid charge<select required value={chargeId} onChange={e=>setChargeId(e.target.value)}><option value="">Select resident charge</option>{unpaid.map(c=><option key={c.id} value={c.id}>{c.residentName} · Room {c.roomNumber} bed {c.bedCode} · {rupees(c.outstandingBalance)} due</option>)}</select></label><div className="workspace-form-grid"><label>Amount in rupees<input required type="number" min="1" step="1" value={amount} onChange={e=>setAmount(e.target.value)}/></label><label>Method<select value={method} onChange={e=>setMethod(e.target.value)}><option value="CASH">Cash</option><option value="BANK_TRANSFER">Bank transfer</option><option value="UPI_OFFLINE">UPI · recorded offline</option></select></label></div><label>Reference / note<input value={reference} onChange={e=>setReference(e.target.value)} maxLength="160"/></label><button className="primary-action" disabled={busy||!chargeId}>Record payment</button></form></div>
    <div className="workspace-panel"><div className="panel-heading"><div><h2>Bed-level status</h2><p>Paid is green; overdue is red only after the due date passes.</p></div></div><BedGrid statuses={statuses}/></div></>;
}

function Photos({propertyId,photos,reload,act,progress,setProgress}){
  const [file,setFile]=useState(null);const [alt,setAlt]=useState('');const [error,setError]=useState('');const [busy,setBusy]=useState(false);
  async function upload(e){e.preventDefault();if(!file)return;setBusy(true);setError('');try{await apiUpload(`/api/owner/properties/${propertyId}/photos`,{file,altText:alt},setProgress);setFile(null);setAlt('');setProgress(0);await reload();}catch(e){setError(e.message);}finally{setBusy(false);}}
  return <><div className="workspace-panel"><div className="panel-heading"><div><h2>Owner uploaded property photos</h2><p>JPEG or PNG, up to 10 MB each. Uploads are validated and stored privately.</p></div><span>{photos.length} / 20</span></div>{error&&<div className="notice-error">{error}</div>}<form className="photo-upload-form" onSubmit={upload}><label className="file-pick">Choose photo<input type="file" accept="image/jpeg,image/png" onChange={e=>setFile(e.target.files?.[0]||null)}/></label><label>Photo description<input value={alt} maxLength="160" onChange={e=>setAlt(e.target.value)} placeholder="Building exterior, shared kitchen…"/></label><button className="primary-action" disabled={!file||busy}>{busy?`Uploading ${progress}%…`:'Upload photo'}</button>{busy&&<progress max="100" value={progress}/>}</form></div><div className="photo-manager-grid">{photos.map(photo=><article className="photo-manager-card" key={photo.id}>{photo.url?<img src={photo.url} alt={photo.altText||'Owner uploaded property'}/>:<div className="photo-unavailable">Photos unavailable</div>}<div><b>{photo.altText||'Property photo'}</b><small>{Math.round(photo.sizeBytes/1024)} KB · {photo.contentType}</small></div><button className="danger-action" onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/photos/${photo.id}`,{method:'DELETE'}),'Photo removed.')} aria-label="Remove photo"><Trash2 size={15}/></button></article>)}{!photos.length&&<Empty title="No photos uploaded" text="The listing cannot be submitted until you upload genuine photos of this property."/>}</div></>;
}

function Services({propertyId,complaints,announcements,menu,act,busy}){
  const [tab,setTab]=useState('complaints');const [notice,setNotice]=useState({title:'',body:'',audience:'PROPERTY',buildingId:'',residentId:'',expiresOn:''});const [meal,setMeal]=useState({dayOfWeek:1,meal:'BREAKFAST',description:'',vegetarianOption:'',nonVegetarianOption:'',mealTime:'',published:true});const [inventory,setInventory]=useState(null);const [residents,setResidents]=useState([]);const [loadError,setLoadError]=useState('');
  useEffect(()=>{Promise.all([api(`/api/owner/properties/${propertyId}/inventory`),api(`/api/owner/properties/${propertyId}/residents`)]).then(([i,r])=>{setInventory(i);setResidents(r);}).catch(e=>setLoadError(e.message));},[propertyId]);
  return <><div className="workspace-tabs">{[['complaints','Maintenance'],['announcements','Announcements'],['menu','Food menu']].map(([id,label])=><button key={id} className={tab===id?'selected':''} onClick={()=>setTab(id)}>{label}</button>)}</div>
    {tab==='complaints'&&<div className="workspace-panel"><div className="panel-heading"><div><h2>Maintenance requests</h2><p>Respond to complaints from your actual residents.</p></div></div>{complaints.length?<div className="complaint-list">{complaints.map(c=><article key={c.id}><div className="complaint-meta"><b>{c.title}</b><StatusBadge value={c.status}/></div><p>{c.description}</p><small>{c.category} · {new Date(c.createdAt).toLocaleString()}</small>{c.ownerResponse&&<blockquote>{c.ownerResponse}</blockquote>}<ComplaintActions complaint={c} propertyId={propertyId} act={act} busy={busy}/></article>)}</div>:<Empty title="No maintenance requests" text="Resident reports will show here."/>}</div>}
    {tab==='announcements'&&<><form className="workspace-form announcement-form" onSubmit={e=>{e.preventDefault();const target=notice.audience==='BUILDING'?{buildingId:notice.buildingId}:notice.audience==='RESIDENT'?{residentId:notice.residentId}:{};const expiresAt=notice.expiresOn?new Date(`${notice.expiresOn}T23:59:59`).toISOString():null;act(()=>api(`/api/owner/properties/${propertyId}/announcements`,{method:'POST',body:JSON.stringify({title:notice.title,body:notice.body,...target,expiresAt})}),'Announcement published to its selected audience.');setNotice({title:'',body:'',audience:'PROPERTY',buildingId:'',residentId:'',expiresOn:''});}}><h2>Post an announcement</h2>{loadError&&<div className="notice-error">{loadError}</div>}<div className="workspace-form-grid"><label>Audience<select value={notice.audience} onChange={e=>setNotice({...notice,audience:e.target.value})}><option value="PROPERTY">All residents at this property</option><option value="BUILDING">One building</option><option value="RESIDENT">One resident</option></select></label>{notice.audience==='BUILDING'&&<label>Building<select required value={notice.buildingId} onChange={e=>setNotice({...notice,buildingId:e.target.value})}><option value="">Choose a building</option>{(inventory?.buildings||[]).map(b=><option key={b.id} value={b.id}>{b.name} · {b.code}</option>)}</select></label>}{notice.audience==='RESIDENT'&&<label>Resident<select required value={notice.residentId} onChange={e=>setNotice({...notice,residentId:e.target.value})}><option value="">Choose a resident</option>{residents.map(r=><option key={r.tenancyId} value={r.residentId}>{r.residentName} · Room {r.roomNumber} / {r.bedCode}</option>)}</select></label>}</div><label>Title<input required maxLength="140" value={notice.title} onChange={e=>setNotice({...notice,title:e.target.value})}/></label><label>Message<textarea required maxLength="2000" rows="3" value={notice.body} onChange={e=>setNotice({...notice,body:e.target.value})}/></label><label>Optional expiry date<input type="date" value={notice.expiresOn} onChange={e=>setNotice({...notice,expiresOn:e.target.value})}/></label><button className="primary-action" disabled={busy||(notice.audience==='BUILDING'&&!notice.buildingId)||(notice.audience==='RESIDENT'&&!notice.residentId)}><Megaphone size={15}/>Publish announcement</button></form><div className="workspace-panel"><div className="announcement-list">{announcements.map(a=><article key={a.id}><span>{new Date(a.publishedAt).toLocaleDateString()} {a.expiresAt?`· Expires ${new Date(a.expiresAt).toLocaleDateString()}`:''}</span><h3>{a.title}</h3><p>{a.body}</p><small>{a.residentId?'Specific resident':a.buildingId?'Specific building':'All property residents'}</small><button className="danger-action" onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/announcements/${a.id}`,{method:'DELETE'}),'Announcement removed.')}>Remove</button></article>)}{!announcements.length&&<Empty title="No announcements" text="Nothing has been posted to residents yet."/>}</div></div></>}
    {tab==='menu'&&<><form className="workspace-form menu-form" onSubmit={e=>{e.preventDefault();act(()=>api(`/api/owner/properties/${propertyId}/food-menu`,{method:'PUT',body:JSON.stringify(meal)}),'Weekly menu item saved.');}}><h2>Weekly meal item</h2><div className="workspace-form-grid"><label>Day<select value={meal.dayOfWeek} onChange={e=>setMeal({...meal,dayOfWeek:Number(e.target.value)})}>{['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'].map((d,i)=><option value={i+1} key={d}>{d}</option>)}</select></label><label>Meal<select value={meal.meal} onChange={e=>setMeal({...meal,meal:e.target.value})}>{['BREAKFAST','LUNCH','EVENING_SNACKS','DINNER'].map(x=><option key={x}>{x}</option>)}</select></label></div><label>Description<input required value={meal.description} onChange={e=>setMeal({...meal,description:e.target.value})}/></label><div className="workspace-form-grid"><label>Vegetarian option<input value={meal.vegetarianOption} onChange={e=>setMeal({...meal,vegetarianOption:e.target.value})}/></label><label>Non-vegetarian option<input value={meal.nonVegetarianOption} onChange={e=>setMeal({...meal,nonVegetarianOption:e.target.value})}/></label></div><label>Meal time<input value={meal.mealTime} onChange={e=>setMeal({...meal,mealTime:e.target.value})} placeholder="8:00–9:00 AM"/></label><label className="checkbox-label"><input type="checkbox" checked={meal.published} onChange={e=>setMeal({...meal,published:e.target.checked})}/> Visible to residents</label><button className="primary-action" disabled={busy}><Utensils size={15}/>Save weekly menu</button></form><div className="workspace-panel"><div className="workspace-table-wrap"><table><thead><tr><th>Day</th><th>Meal</th><th>Menu</th><th>Vegetarian</th><th>Non-vegetarian</th><th>Visible</th></tr></thead><tbody>{menu.map(m=><tr key={m.id}><td>{['','Mon','Tue','Wed','Thu','Fri','Sat','Sun'][m.dayOfWeek]}</td><td>{statusLabel(m.meal)}</td><td>{m.description}</td><td>{m.vegetarianOption||'—'}</td><td>{m.nonVegetarianOption||'—'}</td><td>{m.published?'Yes':'Draft'}</td></tr>)}</tbody></table>{!menu.length&&<Empty title="No menu items configured" text="Only food details entered and published by the property owner appear to residents."/>}</div></div></>}
  </>;
}
function ComplaintActions({complaint,propertyId,act,busy}){const [response,setResponse]=useState(complaint.ownerResponse||'');const [status,setStatus]=useState(complaint.status);return <form className="complaint-actions" onSubmit={e=>{e.preventDefault();act(()=>api(`/api/owner/properties/${propertyId}/complaints/${complaint.id}`,{method:'PATCH',body:JSON.stringify({status,response})}),'Complaint updated and resident notified.');}}><select value={status} onChange={e=>setStatus(e.target.value)}>{['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'].map(s=><option key={s}>{s}</option>)}</select><input value={response} onChange={e=>setResponse(e.target.value)} maxLength="1000" placeholder="Response to resident"/><button className="secondary-action" disabled={busy}>Update</button></form>}

function Reports({propertyId,rows,act,busy}){const exportCsv=()=>{const header=['Resident','Property','Building','Room','Bed','Billing period','Due date','Rent','Paid','Outstanding','Status'];const values=rows.map(r=>{const c=r.charge;return [r.residentName,r.propertyName,r.buildingName,c.roomNumber,c.bedCode,c.billingPeriod,c.dueDate,c.rentAmount,c.paidAmount,c.outstandingBalance,c.indicator]});const csv=[header,...values].map(row=>row.map(v=>`"${String(v??'').replaceAll('"','""')}"`).join(',')).join('\n');const link=document.createElement('a');link.href=URL.createObjectURL(new Blob([csv],{type:'text/csv'}));link.download=`pgconnect-outstanding-rent-${propertyId}.csv`;link.click();URL.revokeObjectURL(link.href);};return <div className="workspace-panel"><div className="panel-heading"><div><h2>Outstanding rent report</h2><p>Actual charges with balances due. Use Rent & payments to record an authorized offline receipt.</p></div><button className="secondary-action" disabled={!rows.length} onClick={exportCsv}><FileBarChart2 size={15}/>Export CSV</button></div>{rows.length?<div className="workspace-table-wrap"><table><thead><tr><th>Resident</th><th>Building / room / bed</th><th>Period</th><th>Due</th><th>Amount due</th><th>Status</th><th>Reminder</th></tr></thead><tbody>{rows.map(r=><tr key={r.charge.id}><td>{r.residentName}</td><td>{r.buildingName} · {r.roomNumber} / {r.bedCode}</td><td>{r.charge.billingPeriod}</td><td>{r.charge.dueDate}</td><td>{rupees(r.charge.outstandingBalance)}</td><td><StatusBadge value={r.charge.indicator}/></td><td><button className="secondary-action" disabled={busy} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/rent/charges/${r.charge.id}/reminder`,{method:'POST'}),'Rent reminder added to the resident inbox and audited.')}>Send reminder</button></td></tr>)}</tbody></table></div>:<Empty title="No outstanding charges" text="This property has no unpaid rent records at the moment."/>}</div>}

function Deposits({propertyId,deposits,residents,act,busy}){const [form,setForm]=useState({tenancyId:'',amount:'',reference:''});return <><form className="workspace-form" onSubmit={e=>{e.preventDefault();act(()=>api(`/api/owner/properties/${propertyId}/deposits`,{method:'POST',body:JSON.stringify({...form,amount:Number(form.amount)})}),'Deposit recorded with an audit entry.');setForm({tenancyId:'',amount:'',reference:''});}}><h2>Record a tenancy deposit</h2><label>Resident tenancy<select required value={form.tenancyId} onChange={e=>setForm({...form,tenancyId:e.target.value})}><option value="">Select a resident</option>{residents.map(r=><option key={r.tenancyId} value={r.tenancyId}>{r.residentName} · Room {r.roomNumber} bed {r.bedCode}</option>)}</select></label><div className="workspace-form-grid"><label>Amount (₹)<input required type="number" min="1" value={form.amount} onChange={e=>setForm({...form,amount:e.target.value})}/></label><label>Reference<input value={form.reference} onChange={e=>setForm({...form,reference:e.target.value})}/></label></div><button className="primary-action" disabled={busy||!form.tenancyId}>Record deposit</button></form><div className="workspace-panel"><div className="workspace-table-wrap"><table><thead><tr><th>Resident</th><th>Received</th><th>Refunded</th><th>Status</th><th>Action</th></tr></thead><tbody>{deposits.map(d=><tr key={d.id}><td>{residents.find(r=>r.tenancyId===d.tenancyId)?.residentName||d.residentId}</td><td>{rupees(d.amount)}</td><td>{rupees(d.refundedAmount)}</td><td>{statusLabel(d.status)}</td><td>{d.amount>d.refundedAmount&&<button className="secondary-action" onClick={()=>{const amount=prompt(`Remaining deposit ${rupees(d.amount-d.refundedAmount)}. Refund amount in rupees:`);if(amount&&Number(amount)>0)act(()=>api(`/api/owner/properties/${propertyId}/deposits/${d.id}/refund`,{method:'POST',body:JSON.stringify({amount:Number(amount),reason:'Owner recorded deposit refund'})}),'Deposit refund recorded and audited.');}}>Record refund</button>}</td></tr>)}</tbody></table>{!deposits.length&&<Empty title="No deposits recorded" text="Deposit balances remain separate from rent charges."/>}</div></div></>}

function OwnerInbox({notifications,act,busy}){return <div className="workspace-panel"><div className="panel-heading"><div><h2>Account notifications</h2><p>Subscription and property workflow updates for your owner account.</p></div></div>{notifications.length?<div className="announcement-list">{notifications.map(item=><article key={item.id}><span>{new Date(item.createdAt).toLocaleString()} · {statusLabel(item.type)}</span><h3>{item.title}</h3><p>{item.message}</p>{!item.readAt&&<button className="secondary-action" disabled={busy} onClick={()=>act(()=>api(`/api/notifications/${item.id}/read`,{method:'POST'}),'Notification marked as read.')}>Mark as read</button>}</article>)}</div>:<Empty title="Your inbox is clear" text="Subscription reminders and owner workflow updates will appear here."/>}</div>}

function Subscription({subscription,invoices,status,act,busy}){
  const [error,setError]=useState('');
  async function start(){setError('');try{const checkout=await api('/api/owner/subscription/checkout',{method:'POST',body:'{}'});if(!window.Razorpay){await new Promise((resolve,reject)=>{const s=document.createElement('script');s.src='https://checkout.razorpay.com/v1/checkout.js';s.onload=resolve;s.onerror=reject;document.body.appendChild(s);});}
    const modal=new window.Razorpay({key:checkout.keyId,subscription_id:checkout.subscriptionId,name:'PGConnect',description:'₹1,000 monthly owner platform subscription',handler:async response=>{try{const verified=await api('/api/owner/subscription/verify',{method:'POST',body:JSON.stringify({subscriptionId:response.razorpay_subscription_id,paymentId:response.razorpay_payment_id,signature:response.razorpay_signature})});setError(verified.message);await act(async()=>{await api('/api/owner/subscription');},'Payment signature verified. Subscription eligibility updates after Razorpay webhook confirmation.');}catch(e){setError(e.message);}},modal:{ondismiss:()=>{}}});modal.open();
  }catch(e){setError(e.message);}}
  const cancelRenewal=()=>{if(window.confirm('Stop automatic renewal at the end of the current billing period? Your subscription and eligible listings remain active until then.'))act(()=>api('/api/owner/subscription/cancel',{method:'POST',body:'{}'}),'Auto-renewal will stop at the current period end.');};
  return <><div className="subscription-plan"><div><span className="section-kicker">OWNER PLATFORM PLAN</span><h2>₹1,000 <small>/ month</small></h2><p>Recurring subscription for property management and eligible listing publication. Resident rent is billed separately.</p></div><StatusBadge value={status||'PENDING'}/><button className="primary-action" disabled={busy||status==='ACTIVE'} onClick={start}><CreditCard size={16}/>{status==='ACTIVE'?(subscription?.cancelAtCycleEnd?'Renewal cancelled':'Subscription active'):'Start monthly subscription'}</button></div>{error&&<div className="notice-error">{error}</div>}<div className="workspace-panel"><div className="panel-heading"><div><h2>Subscription record</h2><p>Eligibility is set only by verified Razorpay webhook events.</p></div></div>{subscription?<><div className="subscription-details"><span>Provider subscription <b>{subscription.providerSubscriptionId}</b></span><span>Started <b>{subscription.startedAt?new Date(subscription.startedAt).toLocaleDateString():'Pending'}</b></span><span>Current period ends <b>{subscription.currentPeriodEnd?new Date(subscription.currentPeriodEnd).toLocaleDateString():'—'}</b></span><span>Renewal date <b>{subscription.renewalAt?new Date(subscription.renewalAt).toLocaleDateString():'—'}</b></span></div>{subscription.cancelAtCycleEnd?<div className="workspace-callout"><ShieldCheck size={17}/> Auto-renewal is scheduled to stop on {subscription.currentPeriodEnd?new Date(subscription.currentPeriodEnd).toLocaleDateString():'the current period end'}. Access stays active until that date.</div>:status==='ACTIVE'&&<div className="subscription-cancel-action"><button className="danger-action" disabled={busy} onClick={cancelRenewal}>Turn off auto-renewal</button><span>Cancellation takes effect at the end of this billing period.</span></div>}</>:<Empty title="No subscription record" text="Configure Razorpay keys on the backend to start checkout. Subscription status will remain pending until a verified provider event arrives."/>}</div><div className="workspace-panel"><div className="panel-heading"><div><h2>Subscription invoices</h2><p>Platform subscription invoices are kept separate from accommodation rent.</p></div></div>{invoices.length?<div className="workspace-table-wrap"><table><thead><tr><th>Invoice</th><th>Provider payment</th><th>Amount</th><th>Currency</th><th>Status</th><th>Date</th></tr></thead><tbody>{invoices.map(i=><tr key={i.id}><td>{i.providerInvoiceId}</td><td>{i.providerPaymentId||'—'}</td><td>{fmt(i.amountPaise)}</td><td>{i.currency}</td><td>{statusLabel(i.status)}</td><td>{new Date(i.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div>:<Empty title="No invoices yet" text="Verified subscription billing invoices will appear here."/>}</div></>;
}

function StaffAccess({propertyId,members,act,busy}){
  const [email,setEmail]=useState('');const [permissions,setPermissions]=useState(['COMPLAINTS']);
  const options=[['COMPLAINTS','Maintenance requests'],['ANNOUNCEMENTS','Resident announcements'],['FOOD_MENU','Weekly food menu']];
  const toggle=(value)=>setPermissions(current=>current.includes(value)?current.filter(x=>x!==value):[...current,value]);
  return <><div className="workspace-notice"><ShieldCheck size={18}/><span><b>Access is limited to this property.</b> Staff can only use the service areas you select. Ask them to create a staff account and verify their email before adding them here.</span></div>
    <form className="workspace-form" onSubmit={async e=>{e.preventDefault();const saved=await act(()=>api(`/api/owner/properties/${propertyId}/staff`,{method:'POST',body:JSON.stringify({email,permissions})}),'Staff access granted for this property.');if(saved)setEmail('');}}><h2>Add staff access</h2><label>Staff account email<input required type="email" maxLength="254" value={email} onChange={e=>setEmail(e.target.value)} placeholder="staff@example.com"/></label><div className="staff-permissions"><span>Permissions</span>{options.map(([key,label])=><label key={key} className="checkbox-label"><input type="checkbox" checked={permissions.includes(key)} onChange={()=>toggle(key)}/>{label}</label>)}</div><button className="primary-action" disabled={busy||!permissions.length}><UserCog size={15}/>Grant selected access</button><p className="field-hint">Staff accounts register at <Link to="/register?role=STAFF">/register?role=STAFF</Link> and must verify their email first.</p></form>
    <div className="workspace-panel"><div className="panel-heading"><div><h2>Property staff</h2><p>Change permissions or revoke access at any time.</p></div></div>{members.length?<div className="staff-member-list">{members.map(member=><StaffMember key={member.id} member={member} propertyId={propertyId} options={options} act={act} busy={busy}/>)}</div>:<Empty title="No staff access" text="Only accounts you add here can handle resident services for this property."/>}</div></>;
}
function StaffMember({member,propertyId,options,act,busy}){
  const [permissions,setPermissions]=useState(member.permissions||[]);const [active,setActive]=useState(member.active);
  useEffect(()=>{setPermissions(member.permissions||[]);setActive(member.active);},[member]);
  const toggle=(value)=>setPermissions(current=>current.includes(value)?current.filter(x=>x!==value):[...current,value]);
  return <article className="staff-member"><div><b>{member.name}</b><small>{member.email||'Account unavailable'} · Added {member.createdAt?new Date(member.createdAt).toLocaleDateString():'—'}</small></div><StatusBadge value={member.active?'ACTIVE':'REVOKED'}/><div className="staff-permissions">{options.map(([key,label])=><label key={key} className="checkbox-label"><input type="checkbox" checked={permissions.includes(key)} onChange={()=>toggle(key)} disabled={!active}/>{label}</label>)}</div><label className="checkbox-label"><input type="checkbox" checked={active} onChange={e=>setActive(e.target.checked)}/> Access active</label><div className="inline-actions"><button className="secondary-action" disabled={busy||active&&!permissions.length} onClick={()=>act(()=>api(`/api/owner/properties/${propertyId}/staff/${member.id}`,{method:'PATCH',body:JSON.stringify({active,permissions})}),'Staff permissions updated.')}>Save access</button>{member.active&&<button className="danger-action" disabled={busy} onClick={()=>{if(window.confirm(`Revoke ${member.name}'s access to this property?`))act(()=>api(`/api/owner/properties/${propertyId}/staff/${member.id}`,{method:'DELETE'}),'Staff access revoked.');}}>Revoke</button>}</div></article>;
}
function Settings({user,update}){const [name,setName]=useState(user?.name||'');const [phone,setPhone]=useState(user?.phone||'');const [error,setError]=useState('');const [notice,setNotice]=useState('');const [busy,setBusy]=useState(false);async function save(e){e.preventDefault();setError('');try{const account=await api('/api/auth/profile',{method:'PATCH',body:JSON.stringify({name,phone})});update(account);setNotice('Profile saved.');}catch(e){setError(e.message);}}async function resend(){setBusy(true);setError('');setNotice('');try{const result=await api('/api/auth/email-verification/request',{method:'POST',body:JSON.stringify({email:user.email})});setNotice(result.message);}catch(e){setError(e.message);}finally{setBusy(false);}}return <div className="workspace-panel"><h2>Profile and account settings</h2><form className="workspace-form" onSubmit={save}>{error&&<div className="notice-error">{error}</div>}{notice&&<div className="notice-success">{notice}</div>}<label>Name<input required minLength="2" maxLength="80" value={name} onChange={e=>setName(e.target.value)}/></label><label>Phone number<input maxLength="20" value={phone} onChange={e=>setPhone(e.target.value)} placeholder="Optional contact number"/></label><label>Email<input disabled value={user?.email||''}/><small>{user?.emailVerified?'Email verified.':'Email is not verified yet.'}</small></label><div className="form-actions"><button className="primary-action">Save profile</button>{!user?.emailVerified&&<button type="button" className="secondary-action" disabled={busy} onClick={resend}>Resend verification email</button>}</div></form><div className="workspace-callout"><Bell size={17}/> Rent reminders and workflow updates appear in the in-app inbox. Email or SMS notification delivery is not enabled.</div><div className="workspace-callout"><Users size={17}/> Property staff access and permissions are managed in the Staff access section.</div></div>}
