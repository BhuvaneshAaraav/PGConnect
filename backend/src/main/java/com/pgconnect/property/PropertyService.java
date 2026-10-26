package com.pgconnect.property;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.pgconnect.storage.PhotoStorage;
import com.pgconnect.inventory.BedRepository;
import com.pgconnect.inventory.BedStatus;
import com.pgconnect.inventory.RoomRepository;
import com.pgconnect.inventory.SharingType;
import com.pgconnect.inventory.Room;
import com.pgconnect.management.ReviewRepository;
import com.pgconnect.management.PropertyReview;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class PropertyService {
    private final PgPropertyRepository properties;
    private final UserAccountRepository users;
    private final MongoTemplate mongoTemplate;
    private final ObjectProvider<PhotoStorage> photoStorage;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final ReviewRepository reviews;

    public PropertyService(PgPropertyRepository properties, UserAccountRepository users, MongoTemplate mongoTemplate,
                           ObjectProvider<PhotoStorage> photoStorage, RoomRepository rooms, BedRepository beds, ReviewRepository reviews) {
        this.properties = properties; this.users = users; this.mongoTemplate = mongoTemplate; this.photoStorage = photoStorage;
        this.rooms = rooms; this.beds = beds; this.reviews = reviews;
    }

    public List<PgProperty> ownerProperties(String ownerId) {
        return properties.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public PropertyCardResponse publicProperty(String propertyId) {
        PgProperty property = properties.findById(propertyId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        UserAccount owner = users.findById(property.getOwnerId()).orElse(null);
        if (!PublicListingPolicy.isEligible(property, owner)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        List<PropertyReview> propertyReviews=reviews.findByPropertyIdAndStatusOrderByCreatedAtDesc(property.getId(),"PUBLISHED");
        return PropertyCardResponse.from(property,photoStorage.getIfAvailable(),beds.countByPropertyIdAndStatus(property.getId(),BedStatus.AVAILABLE),propertyReviews.size(),
                Math.round(propertyReviews.stream().mapToInt(PropertyReview::getRating).average().orElse(0)*10d)/10d,null);
    }

    public PgProperty create(String ownerId, @Valid CreatePropertyRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide both latitude and longitude, or leave both empty");
        }
        if (request.latitude() != null && (request.latitude() < -90 || request.latitude() > 90
                || request.longitude() < -180 || request.longitude() > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are out of range");
        }
        PgProperty property = new PgProperty();
        property.setOwnerId(ownerId);
        property.setName(request.name().trim());
        property.setStreetAddress(request.streetAddress().trim());
        property.setLocality(request.locality().trim());
        property.setCity(request.city().trim());
        property.setState(request.state().trim());
        property.setPostalCode(request.postalCode());
        property.setAccommodationType(request.accommodationType());
        property.setStartingRent(request.startingRent());
        property.setFoodAvailable(request.foodAvailable());
        property.setAirConditioned(request.airConditioned());
        property.setFacilities(request.facilities());
        if (request.latitude() != null) property.setLocation(new GeoJsonPoint(request.longitude(), request.latitude()));
        property.setStatus(PropertyStatus.DRAFT);
        property.setApproved(false);
        property.setListingEnabled(false);
        property.setSuspended(false);
        property.setCreatedAt(Instant.now());
        property.setUpdatedAt(Instant.now());
        return properties.save(property);
    }

    public DiscoveryPage discover(DiscoveryRequest request) {
        if ((request.maxRent() != null && request.maxRent() < 0) || (request.minRent() != null && request.minRent() < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rent filters cannot be negative");
        }
        if (request.minRent() != null && request.maxRent() != null && request.minRent() > request.maxRent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum rent cannot exceed maximum rent");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together");
        }
        if (request.latitude() != null && (request.latitude() < -90 || request.latitude() > 90
                || request.longitude() < -180 || request.longitude() > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are out of range");
        }
        if (request.latitude() != null && (request.radiusKm() == null || request.radiusKm() <= 0 || request.radiusKm() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A radius between 0 and 100 km is required for location search");
        }
        if (request.latitude() == null && request.radiusKm() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a search location when using a radius");
        }
        if ("DISTANCE".equalsIgnoreCase(request.sort()) && request.latitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Distance sorting requires a search location");
        }

        List<UserAccount> owners = users.findByRoleAndSubscriptionStatus(Role.OWNER, SubscriptionStatus.ACTIVE);
        if (owners.isEmpty()) return new DiscoveryPage(List.of(), request.page(), request.size(), false);
        Map<String, UserAccount> ownersById = owners.stream().collect(Collectors.toMap(UserAccount::getId, owner -> owner));
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerId").in(ownersById.keySet()));
        query.addCriteria(Criteria.where("status").is(PropertyStatus.PUBLISHED));
        query.addCriteria(Criteria.where("approved").is(true));
        query.addCriteria(Criteria.where("listingEnabled").is(true));
        query.addCriteria(Criteria.where("suspended").is(false));
        query.addCriteria(Criteria.where("photos.0").exists(true));
        if (request.accommodationType() != null) query.addCriteria(Criteria.where("accommodationType").is(request.accommodationType()));
        if (request.maxRent() != null) query.addCriteria(Criteria.where("startingRent").lte(request.maxRent()));
        if (request.minRent() != null) query.addCriteria(Criteria.where("startingRent").gte(request.minRent()));
        if (request.foodAvailable() != null) query.addCriteria(Criteria.where("foodAvailable").is(request.foodAvailable()));
        if (request.airConditioned() != null) query.addCriteria(Criteria.where("airConditioned").is(request.airConditioned()));
        if (request.facility() != null && !request.facility().isBlank()) query.addCriteria(Criteria.where("facilities").in(java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(request.facility().trim()), java.util.regex.Pattern.CASE_INSENSITIVE)));
        if (request.query() != null && !request.query().isBlank()) {
            String expression = java.util.regex.Pattern.quote(request.query().trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(expression, "i"),
                    Criteria.where("streetAddress").regex(expression, "i"),
                    Criteria.where("locality").regex(expression, "i"),
                    Criteria.where("city").regex(expression, "i")));
        }
        if (request.latitude() != null) {
            query.addCriteria(Criteria.where("location").nearSphere(new GeoJsonPoint(request.longitude(), request.latitude()))
                    .maxDistance(request.radiusKm() * 1000));
        }
        if (request.sharingType() != null) {
            Set<String> availablePropertyIds = new java.util.HashSet<>();
            for (Room room : rooms.findByOwnerIdInAndSharingType(ownersById.keySet(), request.sharingType())) {
                if (beds.findByRoomIdOrderByBedCodeAsc(room.getId()).stream().anyMatch(b -> b.getStatus() == BedStatus.AVAILABLE)) availablePropertyIds.add(room.getPropertyId());
            }
            if (availablePropertyIds.isEmpty()) return new DiscoveryPage(List.of(), request.page(), request.size(), false);
            query.addCriteria(Criteria.where("_id").in(availablePropertyIds));
        }
        if (Boolean.TRUE.equals(request.availableBedsOnly())) {
            Set<String> availablePropertyIds = beds.findByOwnerIdInAndStatus(ownersById.keySet(), BedStatus.AVAILABLE).stream().map(com.pgconnect.inventory.Bed::getPropertyId).collect(Collectors.toSet());
            if (availablePropertyIds.isEmpty()) return new DiscoveryPage(List.of(), request.page(), request.size(), false);
            query.addCriteria(Criteria.where("_id").in(availablePropertyIds));
        }
        if (request.latitude()==null) query.with(Sort.by(Sort.Direction.DESC,"createdAt"));
        query.limit(5001);
        List<PgProperty> matched = mongoTemplate.find(query, PgProperty.class);
        List<PropertyCardResponse> cards = new ArrayList<>(matched.stream()
                .filter(property -> PublicListingPolicy.isEligible(property, ownersById.get(property.getOwnerId())))
                .map(property -> {
                    List<PropertyReview> propertyReviews=reviews.findByPropertyIdAndStatusOrderByCreatedAtDesc(property.getId(),"PUBLISHED");
                    long available=beds.countByPropertyIdAndStatus(property.getId(),BedStatus.AVAILABLE);
                    double rating=propertyReviews.stream().mapToInt(PropertyReview::getRating).average().orElse(0);
                    Double distance=request.latitude()==null?null:distanceKm(request.latitude(),request.longitude(),property);
                    return PropertyCardResponse.from(property, photoStorage.getIfAvailable(),available,propertyReviews.size(),Math.round(rating*10d)/10d,distance);
                }).toList());
        cards.removeIf(card -> Boolean.TRUE.equals(request.availableBedsOnly()) && card.availableBeds()==0);
        if (request.minimumRating() != null) cards.removeIf(card -> card.ratingCount()==0 || card.averageRating()<request.minimumRating());
        Comparator<PropertyCardResponse> sorter = request.sort()==null||request.sort().isBlank()?null:switch (request.sort().toUpperCase()) {
            case "RENT_ASC" -> Comparator.comparingLong(PropertyCardResponse::startingRent);
            case "RENT_DESC" -> Comparator.comparingLong(PropertyCardResponse::startingRent).reversed();
            case "RATING" -> Comparator.comparingDouble(PropertyCardResponse::averageRating).reversed();
            case "DISTANCE" -> Comparator.comparing(card -> card.distanceKm()==null?Double.MAX_VALUE:card.distanceKm());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sort must be DISTANCE, RENT_ASC, RENT_DESC or RATING");
        };
        if (sorter!=null) cards.sort(sorter);
        long start=(long)request.page()*request.size();int from=(int)Math.min(cards.size(),start);int to=Math.min(cards.size(),from+request.size());boolean hasNext=to<cards.size();
        cards=cards.subList(from,to);
        return new DiscoveryPage(cards, request.page(), request.size(), hasNext);
    }

    private double distanceKm(double latitude,double longitude,PgProperty property){
        if(property.getLocation()==null)return Double.MAX_VALUE;
        double lat2=property.getLocation().getY(),lon2=property.getLocation().getX();double dLat=Math.toRadians(lat2-latitude),dLon=Math.toRadians(lon2-longitude);
        double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(latitude))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 6371.0088*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
    }

    public record CreatePropertyRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 220) String streetAddress,
            @NotBlank @Size(max = 100) String locality,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Pattern(regexp = "[0-9]{6}") String postalCode,
            @NotNull AccommodationType accommodationType,
            @Min(1) @Max(10000000) long startingRent,
            @Size(max = 20) List<@NotBlank @Size(max = 40) String> facilities,
            Double latitude,
            Double longitude,
            boolean foodAvailable,
            boolean airConditioned) {}

    public record DiscoveryRequest(
            @Size(max = 120) String query,
            Double latitude,
            Double longitude,
            Double radiusKm,
            AccommodationType accommodationType,
            @Min(0) Long maxRent,
            @Min(0) Long minRent,
            Boolean foodAvailable,
            Boolean airConditioned,
            @Size(max=40) String facility,
            SharingType sharingType,
            Boolean availableBedsOnly,
            @Min(0) @Max(5) Double minimumRating,
            @Size(max=20) String sort,
            @Min(0) int page,
            @Min(1) @Max(50) int size) {
        public DiscoveryRequest {
            if (page < 0) page = 0;
            if (size <= 0) size = 12;
            if (size > 50) size = 50;
        }
    }

    public record DiscoveryPage(List<PropertyCardResponse> content, int page, int size, boolean hasNext) {}

    public record PublicPhotoResponse(String id, String url, String altText) {
        static PublicPhotoResponse from(PropertyPhoto photo, PhotoStorage storage) {
            String url = photo.getStorageKey() == null ? photo.getUrl() : storage == null ? null : storage.privateReadUrl(photo.getStorageKey());
            return new PublicPhotoResponse(photo.getId(), url, photo.getAltText());
        }
    }

    public record PropertyCardResponse(String id, String name, String streetAddress,
                                       String locality, String city, String state,
                                       AccommodationType accommodationType, long startingRent,
                                       List<String> facilities, List<PublicPhotoResponse> photos,
                                       long availableBeds,long ratingCount,double averageRating,Double distanceKm,
                                       boolean foodAvailable,boolean airConditioned,Double latitude,Double longitude) {
        static PropertyCardResponse from(PgProperty property, PhotoStorage storage,long availableBeds,long ratingCount,double averageRating,Double distanceKm) {
            GeoJsonPoint location=property.getLocation();
            return new PropertyCardResponse(property.getId(), property.getName(), property.getStreetAddress(),
                    property.getLocality(), property.getCity(), property.getState(),
                    property.getAccommodationType(), property.getStartingRent(), property.getFacilities(),
                    property.getPhotos().stream().map(photo -> PublicPhotoResponse.from(photo, storage)).toList(),availableBeds,ratingCount,averageRating,distanceKm,property.isFoodAvailable(),property.isAirConditioned(),location==null?null:location.getY(),location==null?null:location.getX());
        }
    }
}
