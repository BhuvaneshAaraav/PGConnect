package com.pgconnect.storage;

public interface PhotoStorage {
    void upload(String key, byte[] bytes, String contentType);
    void delete(String key);
    String privateReadUrl(String key);
}
