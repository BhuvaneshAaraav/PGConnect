package com.pgconnect.property;

public class PropertyPhoto {
    private String id;
    private String url;
    private String storageKey;
    private String altText;
    private String uploadedBy;
    private String contentType;
    private long sizeBytes;

    public PropertyPhoto() {}
    public PropertyPhoto(String url, String storageKey, String altText, String uploadedBy) {
        this(java.util.UUID.randomUUID().toString(), url, storageKey, altText, uploadedBy, "image/jpeg", 0);
    }
    public PropertyPhoto(String id, String url, String storageKey, String altText, String uploadedBy, String contentType, long sizeBytes) {
        this.id = id; this.url = url; this.storageKey = storageKey; this.altText = altText;
        this.uploadedBy = uploadedBy; this.contentType = contentType; this.sizeBytes = sizeBytes;
    }
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getStorageKey() { return storageKey; }
    public String getAltText() { return altText; }
    public String getUploadedBy() { return uploadedBy; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setId(String id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public void setAltText(String altText) { this.altText = altText; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
