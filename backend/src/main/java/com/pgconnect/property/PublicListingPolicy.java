package com.pgconnect.property;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;

public final class PublicListingPolicy {
    private PublicListingPolicy() {}

    public static boolean isEligible(PgProperty property, UserAccount owner) {
        return owner != null
                && owner.getRole() == Role.OWNER
                && owner.isEnabled()
                && owner.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                && property.getStatus() == PropertyStatus.PUBLISHED
                && property.isApproved()
                && property.isListingEnabled()
                && !property.isSuspended()
                && property.getPhotos() != null
                && !property.getPhotos().isEmpty();
    }
}
