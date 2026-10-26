package com.pgconnect.storage;

import com.pgconnect.property.PgProperty;
import com.pgconnect.property.PgPropertyRepository;
import com.pgconnect.property.PropertyPhoto;
import jakarta.validation.constraints.Size;
import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
public class PropertyPhotoService {
    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PHOTOS = 20;
    private final PgPropertyRepository properties;
    private final MongoTemplate mongoTemplate;
    private final PhotoStorage storage;

    public PropertyPhotoService(PgPropertyRepository properties, MongoTemplate mongoTemplate,
                                ObjectProvider<PhotoStorage> storageProvider) {
        this.properties = properties; this.mongoTemplate = mongoTemplate; this.storage = storageProvider.getIfAvailable();
    }

    public List<PhotoResponse> list(String ownerId, String propertyId) {
        PgProperty property = ownedProperty(ownerId, propertyId);
        return property.getPhotos().stream().map(this::toResponse).toList();
    }

    public PhotoResponse upload(String ownerId, String propertyId, MultipartFile file, String altText) {
        if (storage == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud photo storage is not configured");
        PgProperty property = ownedProperty(ownerId, propertyId);
        if (property.getPhotos().size() >= MAX_PHOTOS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A property can have at most 20 photos");
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image under 10 MB");
        }
        String key = null;
        boolean metadataSaved = false;
        try {
            ProcessedImage image = sanitize(file.getBytes(), file.getContentType());
            String extension = image.contentType().equals("image/png") ? "png" : "jpg";
            key = ownerId + "/" + propertyId + "/" + UUID.randomUUID() + "." + extension;
            storage.upload(key, image.bytes(), image.contentType());
            PropertyPhoto photo = new PropertyPhoto(UUID.randomUUID().toString(), null, key,
                    altText == null || altText.isBlank() ? "Property photo" : altText.trim(), ownerId, image.contentType(), image.bytes().length);
            Query ownedAndHasRoom = Query.query(Criteria.where("_id").is(propertyId).and("ownerId").is(ownerId)
                    .and("photos.19").exists(false));
            PgProperty updated = mongoTemplate.findAndModify(ownedAndHasRoom,
                    new Update().push("photos", photo).set("updatedAt", Instant.now()), PgProperty.class);
            if (updated == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Property changed or reached its 20 photo limit");
            }
            metadataSaved = true;
            return toResponse(photo);
        } catch (ResponseStatusException exception) {
            if (!metadataSaved && key != null) deleteQuietly(key);
            throw exception;
        } catch (IOException exception) {
            if (!metadataSaved && key != null) deleteQuietly(key);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected image could not be read");
        } catch (RuntimeException exception) {
            if (!metadataSaved && key != null) deleteQuietly(key);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Photo storage failed. Please try again.");
        }
    }

    public void remove(String ownerId, String propertyId, String photoId) {
        if (storage == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloud photo storage is not configured");
        PgProperty property = ownedProperty(ownerId, propertyId);
        PropertyPhoto photo = property.getPhotos().stream().filter(item -> photoId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property photo not found"));
        try {
            Query query = Query.query(Criteria.where("_id").is(propertyId).and("ownerId").is(ownerId));
            var result = mongoTemplate.updateFirst(query, new Update().pull("photos", new Document("id", photoId)).set("updatedAt", Instant.now()), PgProperty.class);
            if (result.getModifiedCount() == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Property photos changed. Refresh and retry.");
            storage.delete(photo.getStorageKey());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Photo removal failed. Please try again.");
        }
    }

    public String publicUrl(PropertyPhoto photo) {
        if (photo.getStorageKey() == null) return photo.getUrl();
        return storage == null ? null : storage.privateReadUrl(photo.getStorageKey());
    }

    private PhotoResponse toResponse(PropertyPhoto photo) {
        return new PhotoResponse(photo.getId(), publicUrl(photo), photo.getAltText(), photo.getContentType(), photo.getSizeBytes());
    }

    private PgProperty ownedProperty(String ownerId, String propertyId) {
        return properties.findByIdAndOwnerId(propertyId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
    }

    private void deleteQuietly(String key) {
        try { storage.delete(key); } catch (RuntimeException ignored) { }
    }

    private ProcessedImage sanitize(byte[] original, String declaredType) throws IOException {
        if (declaredType == null || (!declaredType.equalsIgnoreCase("image/jpeg") && !declaredType.equalsIgnoreCase("image/png"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG and PNG photos are accepted");
        }
        String expectedType = declaredType.equalsIgnoreCase("image/png") ? "image/png" : "image/jpeg";
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is not a supported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase();
                boolean png = format.equals("png");
                if (!(png || format.equals("jpeg") || format.equals("jpg")) || (png != expectedType.equals("image/png"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image content does not match its file type");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 8000 || height > 8000 || (long) width * height > 40_000_000L) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image dimensions exceed the upload limit");
                }
                BufferedImage source = reader.read(0);
                BufferedImage output = source;
                if (!png && source.getType() != BufferedImage.TYPE_INT_RGB) {
                    output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = output.createGraphics(); graphics.drawImage(source, 0, 0, null); graphics.dispose();
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(output, png ? "png" : "jpeg", bytes);
                if (bytes.size() > MAX_UPLOAD_BYTES) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Processed image is over 10 MB");
                return new ProcessedImage(bytes.toByteArray(), png ? "image/png" : "image/jpeg");
            } finally { reader.dispose(); }
        }
    }

    private record ProcessedImage(byte[] bytes, String contentType) {}
    public record PhotoResponse(String id, String url, String altText, String contentType, long sizeBytes) {}
}
