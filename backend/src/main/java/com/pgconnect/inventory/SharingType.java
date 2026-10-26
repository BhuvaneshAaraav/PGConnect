package com.pgconnect.inventory;

public enum SharingType {
    SINGLE(1), DOUBLE(2), TRIPLE(3), FOUR_SHARING(4);
    private final int capacity;
    SharingType(int capacity) { this.capacity = capacity; }
    public int capacity() { return capacity; }
}
