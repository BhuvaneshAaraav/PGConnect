import { useEffect, useMemo, useRef, useState } from 'react';
import { ArrowRight, BedDouble, Check, Compass, LocateFixed, MapPin, Search, ShieldCheck, Sparkles, Utensils, WalletCards, List as ListIcon, Map as MapIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import PropertyCard from '../components/PropertyCard.jsx';
import { api, queryString } from '../lib/api.js';
import '../map.css';

const radii = [1, 2, 5, 10];
const mapsKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';

export default function HomePage() {
  const [place, setPlace] = useState('');
  const [radius, setRadius] = useState(5);
  const [kind, setKind] = useState('');
  const [maxRent, setMaxRent] = useState('');
  const [minRent, setMinRent] = useState('');
  const [searchMode, setSearchMode] = useState('locality');
  const [customRadius, setCustomRadius] = useState('');
  const [sharingType, setSharingType] = useState('');
  const [facility, setFacility] = useState('');
  const [foodAvailable, setFoodAvailable] = useState(false);
  const [airConditioned, setAirConditioned] = useState(false);
  const [availableBedsOnly, setAvailableBedsOnly] = useState(false);
  const [minimumRating, setMinimumRating] = useState('');
  const [sort, setSort] = useState('');
  const [locationOptions, setLocationOptions] = useState([]);
  const [near, setNear] = useState(null);
  const [results, setResults] = useState([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const [view, setView] = useState('list');

  const params = useMemo(() => ({
    query: place.trim(),
    latitude: near?.latitude,
    longitude: near?.longitude,
    radiusKm: near ? (customRadius ? Number(customRadius) : radius) : undefined,
    accommodationType: kind,
    maxRent: maxRent || undefined,
    minRent: minRent || undefined,
    facility: facility || undefined,
    foodAvailable: foodAvailable || undefined,
    airConditioned: airConditioned || undefined,
    availableBedsOnly: availableBedsOnly || undefined,
    sharingType: sharingType || undefined,
    minimumRating: minimumRating || undefined,
    sort: sort || undefined,
    page: 0,
    size: 12,
  }), [place, near, radius, customRadius, kind, maxRent, minRent, facility, foodAvailable, airConditioned, availableBedsOnly, sharingType, minimumRating, sort]);

  async function loadListings(searchParams = params, searched = false) {
    setLoading(true);
    setError('');
    try {
      const response = await api(`/api/public/properties?${queryString(searchParams)}`);
      setResults(response.content || []);
      setPage(response.page || 0);
      setHasNext(Boolean(response.hasNext));
      setHasSearched(searched);
    } catch (err) {
      setError(err.status >= 500 ? 'We can’t reach PGConnect right now. Please try again in a moment.' : err.message);
      setResults([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadListings(params, false); }, []);

  function search(event) {
    event.preventDefault();
    if (searchMode === 'radius' && place.trim()) {
      setLoading(true); setError(''); setLocationOptions([]);
      api(`/api/public/locations/resolve?${queryString({ query: place.trim() })}`).then((candidates) => {
        if (!candidates.length) { setLoading(false); setError('No location matched that name. Try a more specific college, city or locality.'); return; }
        if (candidates.length > 1) { setLocationOptions(candidates); setLoading(false); return; }
        const candidate = candidates[0]; const radiusValue = customRadius ? Number(customRadius) : radius;
        setNear({ latitude: candidate.latitude, longitude: candidate.longitude }); setPlace(candidate.formattedAddress);
        loadListings({ ...params, query: '', latitude: candidate.latitude, longitude: candidate.longitude, radiusKm: radiusValue }, true);
      }).catch((err) => { setLoading(false); setError(err.message); });
      return;
    }
    setNear(null);
    setLocationOptions([]);
    loadListings({ ...params, latitude: undefined, longitude: undefined, radiusKm: undefined }, true);
  }

  function chooseLocation(candidate) {
    const radiusValue = customRadius ? Number(customRadius) : radius;
    setLocationOptions([]); setNear({ latitude: candidate.latitude, longitude: candidate.longitude }); setPlace(candidate.formattedAddress);
    loadListings({ ...params, query: '', latitude: candidate.latitude, longitude: candidate.longitude, radiusKm: radiusValue }, true);
  }

  function findNearby() {
    if (!navigator.geolocation) {
      setError('Location search is not available in this browser. Search by a college or locality instead.');
      return;
    }
    setLoading(true);
    setError('');
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      const point = { latitude: coords.latitude, longitude: coords.longitude };
      setNear(point);
      setPlace('');
      loadListings({ ...params, query: '', ...point, radiusKm: customRadius ? Number(customRadius) : radius }, true);
    }, () => {
      setLoading(false);
      setError('Location permission was unavailable. You can still search by college or locality.');
    }, { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 });
  }

  const modeLabel = near ? `Within ${radius} km of your location` : place.trim() ? `Locality search for “${place.trim()}”` : 'All eligible locations';

  return (
    <main>
      <section className="hero-section">
        <div className="hero-orbit orbit-one" /><div className="hero-orbit orbit-two" />
        <div className="hero-content">
          <div className="eyebrow"><span className="eyebrow-dot" /> A better way to find your place</div>
          <h1>Find a place<br />to <em>belong.</em></h1>
          <p className="hero-copy">Real PGs, close to campus and the life you’re building.</p>
          <form className="search-panel" onSubmit={search}>
            <label className="search-place"><span className="search-label">WHERE ARE YOU HEADED?</span><span className="search-input-wrap"><MapPin size={19} /><input value={place} onChange={(event) => { setPlace(event.target.value); setNear(null); setLocationOptions([]); }} placeholder="College, city or locality" aria-label="College, city or locality" /><button type="button" className="near-button" onClick={findNearby} title="Use your current location"><LocateFixed size={18} /><span>Near me</span></button></span></label>
            <label className="search-distance"><span className="search-label">SEARCH MODE</span><select value={searchMode} onChange={(event) => setSearchMode(event.target.value)}><option value="locality">Match this area</option><option value="radius">Radius around place</option></select></label>
            <label className="search-distance"><span className="search-label">RADIUS</span><select value={radius} onChange={(event) => { const nextRadius = Number(event.target.value); setRadius(nextRadius); setCustomRadius(''); if (near) loadListings({ ...params, radiusKm: nextRadius }, hasSearched); }}>{radii.map((value) => <option value={value} key={value}>Within {value} km</option>)}</select></label>
            <button className="search-submit" type="submit"><Search size={18} /><span>Find a PG</span></button>
          </form>
          {searchMode === 'radius' && <label className="custom-radius">Custom radius (km, up to 100)<input type="number" min="0.1" max="100" step="0.1" value={customRadius} onChange={(event) => setCustomRadius(event.target.value)} placeholder="Use preset radius" /></label>}
          {locationOptions.length > 0 && <div className="location-choices"><b>Choose the matching place</b>{locationOptions.map((candidate) => <button type="button" key={candidate.placeId} onClick={() => chooseLocation(candidate)}><MapPin size={15}/>{candidate.formattedAddress}<ArrowRight size={14}/></button>)}</div>}
          <div className="hero-footnote"><ShieldCheck size={15} /> Only owner-registered and approved properties appear here.</div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="art-window"><div className="window-sky" /><div className="window-building"><span /><span /><span /><span /><span /><span /></div><div className="window-plant"><i /><i /><i /><b /></div></div>
          <div className="art-note note-top"><span className="note-icon"><Compass size={15} /></span><span>Find your corner<br /><b>near what matters</b></span></div>
          <div className="art-note note-bottom"><span className="note-icon note-green"><Check size={16} /></span><span>Thoughtfully listed<br /><b>by real PG owners</b></span></div>
          <span className="art-sparkle"><Sparkles size={21} /></span>
        </div>
      </section>

      <section className="discovery-section" id="listings">
        <div className="section-kicker">YOUR NEXT ADDRESS</div>
        <div className="section-heading-row"><div><h2>A good place to start.</h2><p>Browse approved owner listings and real property details.</p></div><div className="listing-header-actions">{mapsKey&&<div className="view-switch" aria-label="Property results view"><button type="button" className={view==='list'?'active':''} onClick={()=>setView('list')}><ListIcon size={14}/>List</button><button type="button" className={view==='map'?'active':''} onClick={()=>setView('map')}><MapIcon size={14}/>Map</button></div>}<div className="sort-note"><ShieldCheck size={15} /> Published after review</div></div></div>
        <div className="filter-row">
          <div className="filter-label"><span className="live-dot" /> {modeLabel}</div>
          <select aria-label="Accommodation type" value={kind} onChange={(event) => setKind(event.target.value)}>
            <option value="">All stays</option><option value="BOYS">Boys’ PG</option><option value="GIRLS">Girls’ PG</option><option value="CO_LIVING">Co-living</option>
          </select>
          <label className="rent-filter"><WalletCards size={15} /><span>Rent ₹</span><input inputMode="numeric" type="number" min="0" placeholder="Min" value={minRent} onChange={(event) => setMinRent(event.target.value)} /><input inputMode="numeric" type="number" min="0" placeholder="Max" value={maxRent} onChange={(event) => setMaxRent(event.target.value)} /></label>
          <select aria-label="Room sharing" value={sharingType} onChange={(event) => setSharingType(event.target.value)}><option value="">Any sharing</option><option value="SINGLE">Single</option><option value="DOUBLE">Double</option><option value="TRIPLE">Triple</option><option value="FOUR_SHARING">Four sharing</option></select>
          <select aria-label="Facility" value={facility} onChange={(event) => setFacility(event.target.value)}><option value="">Any facility</option>{['Wi-Fi','Laundry','Parking','Hot water','Housekeeping','Study area'].map(value=><option key={value}>{value}</option>)}</select>
          <select aria-label="Sort results" value={sort} onChange={(event) => setSort(event.target.value)}><option value="">Recommended</option><option value="RENT_ASC">Lowest rent</option><option value="RENT_DESC">Highest rent</option><option value="RATING">Highest rated</option><option value="DISTANCE" disabled={!near}>Nearest</option></select>
          <label className="filter-check"><input type="checkbox" checked={availableBedsOnly} onChange={e=>setAvailableBedsOnly(e.target.checked)}/><BedDouble size={14}/>Available beds</label>
          <label className="filter-check"><input type="checkbox" checked={foodAvailable} onChange={e=>setFoodAvailable(e.target.checked)}/><Utensils size={14}/>Food</label>
          <label className="filter-check"><input type="checkbox" checked={airConditioned} onChange={e=>setAirConditioned(e.target.checked)}/>AC</label>
          <label className="rating-filter">Rating <select value={minimumRating} onChange={e=>setMinimumRating(e.target.value)}><option value="">Any</option><option value="3">3+ stars</option><option value="4">4+ stars</option></select></label>
          <button className="apply-filter" onClick={() => loadListings(params, true)}>Apply filters <ArrowRight size={15} /></button>
        </div>
        {error && <div className="state-panel error-state"><span className="state-symbol">!</span><div><h3>Search is temporarily unavailable</h3><p>{error}</p><button className="text-action" onClick={() => loadListings(params, hasSearched)}>Try again <ArrowRight size={15} /></button></div></div>}
        {!error && loading && <div className="loading-grid" aria-label="Loading properties"><span /><span /><span /></div>}
        {!error && !loading && results.length > 0 && view==='list' && <div className="property-grid">{results.map((property) => <PropertyCard property={property} key={property.id} />)}</div>}
        {!error && !loading && results.length > 0 && view==='map' && <PropertyMap results={results} near={near} apiKey={mapsKey}/>}
        {!error && !loading && results.length > 0 && <Pagination page={page} hasNext={hasNext} previous={() => loadListings({ ...params, page: Math.max(0, page - 1) }, hasSearched)} next={() => loadListings({ ...params, page: page + 1 }, hasSearched)} />}
        {!error && !loading && results.length === 0 && <div className="state-panel empty-state"><div className="empty-illustration"><span className="empty-sun" /><span className="empty-hill hill-a" /><span className="empty-hill hill-b" /><MapPin size={22} /></div><div><div className="empty-label">A little room to explore</div><h3>No registered PGs found nearby yet.</h3><p>{hasSearched ? 'Try another college or locality, or widen your search area to see what’s available.' : 'New places will show up here as owners list and verify their properties.'}</p><button className="text-action" onClick={() => { setPlace(''); setNear(null); setRadius(10); loadListings({ page: 0, size: 12 }, true); }}>Explore all areas <ArrowRight size={15} /></button></div></div>}
        <div className="discovery-disclaimer"><ShieldCheck size={15} /> Listings are published only after owner registration, photo upload, an active plan, and platform approval.</div>
      </section>

      <section className="how-section" id="how-it-works"><div className="how-copy"><div className="section-kicker">A LITTLE MORE CERTAINTY</div><h2>Moving is a big step.<br /><em>Finding a place</em> can be simple.</h2><p>Search by college or locality, compare real owner-provided details and request a specific available bed.</p><Link className="dark-action" to="/register">Create an account <ArrowRight size={16} /></Link></div><div className="steps-list"><div className="step"><span>01</span><div><h3>Search by area</h3><p>Match a locality, resolve a college or use your current location.</p></div></div><div className="step"><span>02</span><div><h3>Review eligible PGs</h3><p>See owner-provided facilities, real photos and current availability.</p></div></div><div className="step"><span>03</span><div><h3>Request an available bed</h3><p>Owners review your request before move-in is recorded.</p></div></div></div></section>
      <footer className="site-footer"><Link className="brand" to="/"><span className="brand-mark">pg<span>.</span></span><span className="brand-name">PGConnect</span></Link><span>Find your place. Feel at home.</span><span>Owner-registered listings only</span></footer>
    </main>
  );
}

function Pagination({page,hasNext,previous,next}){return <div className="results-pagination"><button type="button" disabled={page===0} onClick={previous}>Previous</button><span>Page {page+1}</span><button type="button" disabled={!hasNext} onClick={next}>Next</button></div>}

function loadMapsApi(apiKey){
  if(window.google?.maps)return Promise.resolve(window.google.maps);
  if(!window.__pgconnectMapsPromise){window.__pgconnectMapsPromise=new Promise((resolve,reject)=>{const script=document.createElement('script');script.src=`https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&v=weekly`;script.async=true;script.onload=()=>window.google?.maps?resolve(window.google.maps):reject(new Error('Google Maps did not initialize.'));script.onerror=()=>reject(new Error('Google Maps could not load. Check the browser API key and allowed domains.'));document.head.appendChild(script);});}
  return window.__pgconnectMapsPromise;
}

function PropertyMap({results,near,apiKey}){
  const container=useRef(null);const map=useRef(null);const markers=useRef([]);const [error,setError]=useState('');
  useEffect(()=>{let active=true;const points=results.filter(item=>Number.isFinite(item.latitude)&&Number.isFinite(item.longitude));
    if(!points.length){setError('These listings do not have map coordinates. Try another search or use list view.');return()=>{active=false;};}
    setError('');loadMapsApi(apiKey).then(google=>{if(!active||!container.current)return;const center=near?{lat:near.latitude,lng:near.longitude}:{lat:points[0].latitude,lng:points[0].longitude};
      if(!map.current)map.current=new google.Map(container.current,{center,zoom:12,mapTypeControl:false,streetViewControl:false,fullscreenControl:true});
      markers.current.forEach(marker=>marker.setMap(null));markers.current=[];const bounds=new google.LatLngBounds();
      if(near)bounds.extend(center);
      points.forEach(property=>{const position={lat:property.latitude,lng:property.longitude};bounds.extend(position);const marker=new google.Marker({map:map.current,position,title:property.name});const content=document.createElement('a');content.href=`/properties/${encodeURIComponent(property.id)}`;content.textContent=`${property.name} · ${new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(property.startingRent)}/month`;content.style.cssText='color:#315b49;font:600 13px sans-serif;text-decoration:none';const info=new google.InfoWindow({content});marker.addListener('click',()=>info.open({anchor:marker,map:map.current}));markers.current.push(marker);});
      if(points.length>1||near)map.current.fitBounds(bounds);else map.current.setZoom(14);
    }).catch(err=>{if(active)setError(err.message);});
    return()=>{active=false;markers.current.forEach(marker=>marker.setMap(null));markers.current=[];map.current=null;};
  },[results,near,apiKey]);
  return error?<div className="map-empty"><MapIcon size={24}/><p>{error}</p></div>:<div className="property-map" ref={container} role="img" aria-label="Map of eligible PG properties"/>;
}
