import { ArrowUpRight, BedDouble, MapPin, Wifi } from 'lucide-react';
import { Link } from 'react-router-dom';

function formatRupees(amount) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(amount);
}

export default function PropertyCard({ property }) {
  const photo = property.photos?.[0];
  return (
    <Link to={`/properties/${property.id}`} className="property-card">
      <div className="property-photo">
        {photo?.url ? <img src={photo.url} alt={photo.altText || `${property.name} property`} /> : (
          <div className="photo-unavailable"><span className="photo-sun" /><span>Photos unavailable</span></div>
        )}
        <span className="property-type">{property.accommodationType?.replaceAll('_', ' ') || 'Accommodation'}</span>
      </div>
      <div className="property-card-body">
        <div className="property-heading">
          <div><h3>{property.name}</h3><p><MapPin size={14} />{[property.locality, property.city].filter(Boolean).join(', ')}</p></div>
          <span className="property-link" aria-hidden="true"><ArrowUpRight size={17} /></span>
        </div>
        <div className="property-card-bottom">
          <div className="property-amenities">
            {property.facilities?.slice(0, 2).map((facility) => <span key={facility}><Wifi size={13} />{facility}</span>)}
            {property.availableBeds > 0 && <span><BedDouble size={13} />{property.availableBeds} beds</span>}
          </div>
          <div className="property-price"><strong>{formatRupees(property.startingRent)}</strong><span>/ month onwards</span></div>
        </div>
        <p className="review-empty">{property.ratingCount > 0 ? `${property.averageRating.toFixed(1)} · ${property.ratingCount} reviews` : 'No reviews yet'}</p>
        {(property.foodAvailable || property.airConditioned || property.distanceKm != null) && <div className="property-extra">{property.foodAvailable&&<span>Food available</span>}{property.airConditioned&&<span>AC rooms</span>}{property.distanceKm!=null&&Number.isFinite(property.distanceKm)&&<span>{property.distanceKm.toFixed(1)} km away</span>}</div>}
      </div>
    </Link>
  );
}
