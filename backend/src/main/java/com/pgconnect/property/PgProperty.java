package com.pgconnect.property;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("properties")
@CompoundIndexes({
        @CompoundIndex(name = "public_discovery", def = "{'status': 1, 'listingEnabled': 1, 'suspended': 1, 'city': 1}"),
        @CompoundIndex(name = "owner_properties", def = "{'ownerId': 1, 'createdAt': -1}")
})
public class PgProperty {
    @Id
    private String id;
    @Indexed
    private String ownerId;
    private String name;
    private String streetAddress;
    @Indexed
    private String locality;
    @Indexed
    private String city;
    private String state;
    private String postalCode;
    private AccommodationType accommodationType;
    private long startingRent;
    private boolean foodAvailable;
    private boolean airConditioned;
    private List<String> facilities = new ArrayList<>();
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
    private List<PropertyPhoto> photos = new ArrayList<>();
    private PropertyStatus status = PropertyStatus.DRAFT;
    private boolean approved;
    private boolean listingEnabled;
    private boolean suspended;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public PgProperty() {}
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getStreetAddress() { return streetAddress; }
    public String getLocality() { return locality; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public AccommodationType getAccommodationType() { return accommodationType; }
    public long getStartingRent() { return startingRent; }
    public boolean isFoodAvailable() { return foodAvailable; }
    public boolean isAirConditioned() { return airConditioned; }
    public List<String> getFacilities() { return facilities; }
    public GeoJsonPoint getLocation() { return location; }
    public List<PropertyPhoto> getPhotos() { return photos; }
    public PropertyStatus getStatus() { return status; }
    public boolean isApproved() { return approved; }
    public boolean isListingEnabled() { return listingEnabled; }
    public boolean isSuspended() { return suspended; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setId(String id) { this.id = id; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setName(String name) { this.name = name; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public void setLocality(String locality) { this.locality = locality; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setAccommodationType(AccommodationType accommodationType) { this.accommodationType = accommodationType; }
    public void setStartingRent(long startingRent) { this.startingRent = startingRent; }
    public void setFoodAvailable(boolean foodAvailable) { this.foodAvailable = foodAvailable; }
    public void setAirConditioned(boolean airConditioned) { this.airConditioned = airConditioned; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities == null ? new ArrayList<>() : new ArrayList<>(facilities); }
    public void setLocation(GeoJsonPoint location) { this.location = location; }
    public void setPhotos(List<PropertyPhoto> photos) { this.photos = photos == null ? new ArrayList<>() : new ArrayList<>(photos); }
    public void setStatus(PropertyStatus status) { this.status = status; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public void setListingEnabled(boolean listingEnabled) { this.listingEnabled = listingEnabled; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
