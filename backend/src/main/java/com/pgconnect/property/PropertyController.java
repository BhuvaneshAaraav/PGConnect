package com.pgconnect.property;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PropertyController {
    private final PropertyService service;
    public PropertyController(PropertyService service) { this.service = service; }

    @GetMapping("/api/public/properties")
    public PropertyService.DiscoveryPage discover(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) AccommodationType accommodationType,
            @RequestParam(required = false) Long maxRent,
            @RequestParam(required = false) Long minRent,
            @RequestParam(required = false) Boolean foodAvailable,
            @RequestParam(required = false) Boolean airConditioned,
            @RequestParam(required = false) String facility,
            @RequestParam(required = false) com.pgconnect.inventory.SharingType sharingType,
            @RequestParam(required = false) Boolean availableBedsOnly,
            @RequestParam(required = false) Double minimumRating,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var request = new PropertyService.DiscoveryRequest(query, latitude, longitude, radiusKm, accommodationType, maxRent,
                minRent,foodAvailable,airConditioned,facility,sharingType,availableBedsOnly,minimumRating,sort,page,size);
        return service.discover(request);
    }

    @GetMapping("/api/public/properties/{propertyId}")
    public PropertyService.PropertyCardResponse publicProperty(@org.springframework.web.bind.annotation.PathVariable String propertyId) {
        return service.publicProperty(propertyId);
    }

    @GetMapping("/api/owner/properties")
    public List<PgProperty> ownerProperties(Authentication authentication) {
        return service.ownerProperties(authentication.getName());
    }

    @PostMapping("/api/owner/properties")
    public PgProperty create(@Valid @RequestBody PropertyService.CreatePropertyRequest request,
                             Authentication authentication) {
        return service.create(authentication.getName(), request);
    }
}
