package com.pgconnect.storage;

import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/owner/properties/{propertyId}/photos")
@PreAuthorize("hasRole('OWNER')")
public class PropertyPhotoController {
    private final PropertyPhotoService photos;
    public PropertyPhotoController(PropertyPhotoService photos) { this.photos = photos; }

    @GetMapping
    public List<PropertyPhotoService.PhotoResponse> list(@PathVariable String propertyId, Authentication auth) {
        return photos.list(auth.getName(), propertyId);
    }

    @PostMapping(consumes = "multipart/form-data")
    public PropertyPhotoService.PhotoResponse upload(@PathVariable String propertyId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) @Size(max = 160) String altText,
                                                      Authentication auth) {
        return photos.upload(auth.getName(), propertyId, file, altText);
    }

    @DeleteMapping("/{photoId}")
    public void remove(@PathVariable String propertyId, @PathVariable String photoId, Authentication auth) {
        photos.remove(auth.getName(), propertyId, photoId);
    }
}
