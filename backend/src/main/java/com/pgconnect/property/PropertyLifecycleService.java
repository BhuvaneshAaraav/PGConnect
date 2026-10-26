package com.pgconnect.property;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.common.AuditLog;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PropertyLifecycleService {
    private final PgPropertyRepository properties;
    private final UserAccountRepository users;
    private final MongoTemplate mongoTemplate;

    public PropertyLifecycleService(PgPropertyRepository properties, UserAccountRepository users, MongoTemplate mongoTemplate) {
        this.properties = properties; this.users = users; this.mongoTemplate = mongoTemplate;
    }

    public PgProperty updateDraft(String ownerId, String propertyId, PropertyService.CreatePropertyRequest request) {
        PgProperty property = owned(ownerId, propertyId);
        if (property.getStatus() != PropertyStatus.DRAFT && property.getStatus() != PropertyStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a draft or rejected property can be edited");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide both coordinates or leave both empty");
        if (request.latitude() != null && (request.latitude() < -90 || request.latitude() > 90 || request.longitude() < -180 || request.longitude() > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are out of range");
        }
        property.setName(request.name().trim()); property.setStreetAddress(request.streetAddress().trim());
        property.setLocality(request.locality().trim()); property.setCity(request.city().trim());
        property.setState(request.state().trim()); property.setPostalCode(request.postalCode());
        property.setAccommodationType(request.accommodationType()); property.setStartingRent(request.startingRent());
        property.setFacilities(request.facilities());
        property.setFoodAvailable(request.foodAvailable()); property.setAirConditioned(request.airConditioned());
        property.setLocation(request.latitude() == null ? null : new org.springframework.data.mongodb.core.geo.GeoJsonPoint(request.longitude(), request.latitude()));
        property.setUpdatedAt(Instant.now());
        return properties.save(property);
    }

    public PgProperty submitForReview(String ownerId, String propertyId) {
        PgProperty property = owned(ownerId, propertyId);
        UserAccount owner = users.findById(ownerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (owner.getRole() != Role.OWNER || owner.getSubscriptionStatus() != SubscriptionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "An active, verified owner subscription is required before review");
        }
        if (!owner.isEmailVerified()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Verify the owner email before submitting a property for review");
        if (property.getPhotos().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload at least one genuine property photo before review");
        if (property.getStatus() != PropertyStatus.DRAFT && property.getStatus() != PropertyStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Property is not in a submittable state");
        }
        property.setStatus(PropertyStatus.PENDING_REVIEW); property.setApproved(false); property.setListingEnabled(false);
        property.setUpdatedAt(Instant.now());
        mongoTemplate.save(new AuditLog(ownerId, "PROPERTY_SUBMITTED_FOR_REVIEW", "Property", propertyId, Map.of()));
        return properties.save(property);
    }

    public PgProperty setListingEnabled(String ownerId, String propertyId, boolean enabled) {
        PgProperty property = owned(ownerId, propertyId);
        UserAccount owner = users.findById(ownerId).orElseThrow();
        if (enabled && (owner.getSubscriptionStatus() != SubscriptionStatus.ACTIVE || property.getStatus() != PropertyStatus.PUBLISHED || !property.isApproved() || property.getPhotos().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Property must be approved, have photos, and have an active owner subscription before publishing");
        }
        property.setListingEnabled(enabled); property.setUpdatedAt(Instant.now());
        return properties.save(property);
    }

    public List<PgProperty> adminQueue() {
        return properties.findAll().stream().filter(property -> property.getStatus() == PropertyStatus.PENDING_REVIEW).toList();
    }

    @org.springframework.transaction.annotation.Transactional
    public PgProperty adminDecision(String adminId, String propertyId, AdminPropertyDecision decision) {
        PgProperty property = properties.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        if (property.getStatus() != PropertyStatus.PENDING_REVIEW) throw new ResponseStatusException(HttpStatus.CONFLICT, "Property is not awaiting review");
        String action;
        if (decision.approve()) {
            UserAccount owner = users.findById(property.getOwnerId()).orElse(null);
            if (!PublicListingPolicy.isEligible(newCandidate(property), owner)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner subscription, property photos, or verification requirements are not satisfied");
            }
            property.setStatus(PropertyStatus.PUBLISHED); property.setApproved(true); property.setListingEnabled(true); action = "PROPERTY_APPROVED";
        } else {
            property.setStatus(PropertyStatus.REJECTED); property.setApproved(false); property.setListingEnabled(false); action = "PROPERTY_REJECTED";
        }
        property.setUpdatedAt(Instant.now());
        mongoTemplate.save(new AuditLog(adminId, action, "Property", propertyId, Map.of("reason", decision.reason() == null ? "" : decision.reason())));
        return properties.save(property);
    }

    @org.springframework.transaction.annotation.Transactional
    public PgProperty suspend(String adminId, String propertyId, String reason) {
        PgProperty property = properties.findById(propertyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
        property.setStatus(PropertyStatus.SUSPENDED); property.setSuspended(true); property.setListingEnabled(false); property.setUpdatedAt(Instant.now());
        mongoTemplate.save(new AuditLog(adminId, "PROPERTY_SUSPENDED", "Property", propertyId, Map.of("reason", reason == null ? "" : reason)));
        return properties.save(property);
    }

    private PgProperty newCandidate(PgProperty source) {
        PgProperty candidate = new PgProperty();
        candidate.setOwnerId(source.getOwnerId()); candidate.setPhotos(source.getPhotos());
        candidate.setStatus(PropertyStatus.PUBLISHED); candidate.setApproved(true); candidate.setListingEnabled(true);
        return candidate;
    }

    private PgProperty owned(String ownerId, String propertyId) {
        return properties.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    public record AdminPropertyDecision(boolean approve, String reason) {}
}
