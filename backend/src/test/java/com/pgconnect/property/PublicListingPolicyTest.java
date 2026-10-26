package com.pgconnect.property;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.SubscriptionStatus;
import com.pgconnect.auth.UserAccount;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicListingPolicyTest {
    @Test
    void requiresApprovalPublicationPhotosAndActiveOwnerSubscription() {
        UserAccount owner = new UserAccount("Owner", "owner@example.test", "hash", Role.OWNER);
        owner.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        PgProperty property = new PgProperty();
        property.setStatus(PropertyStatus.PUBLISHED);
        property.setApproved(true);
        property.setListingEnabled(true);
        property.setPhotos(List.of(new PropertyPhoto("https://images.example.test/pg.jpg", "key", "Exterior", "owner-id")));

        assertTrue(PublicListingPolicy.isEligible(property, owner));

        property.setPhotos(List.of());
        assertFalse(PublicListingPolicy.isEligible(property, owner));
        property.setPhotos(List.of(new PropertyPhoto("https://images.example.test/pg.jpg", "key", "Exterior", "owner-id")));
        owner.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        assertFalse(PublicListingPolicy.isEligible(property, owner));
        owner.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        owner.setEnabled(false);
        assertFalse(PublicListingPolicy.isEligible(property, owner));
    }
}
