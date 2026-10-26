package com.pgconnect.staff;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import com.pgconnect.property.PgPropertyRepository;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StaffAccessServiceTest {
    @Test
    void staffCannotUseAServicePermissionTheOwnerDidNotGrant() {
        UserAccount staff = new UserAccount("Staff", "staff@example.test", "hash", Role.STAFF);
        staff.setId("staff-1");
        StaffMembership membership = new StaffMembership();
        membership.setStaffUserId("staff-1");
        membership.setPropertyId("property-1");
        membership.setPermissions(Set.of(StaffPermission.COMPLAINTS));
        UserAccountRepository users = proxy(UserAccountRepository.class, (object, method, args) ->
                method.getName().equals("findById") ? Optional.of(staff) : null);
        StaffMembershipRepository memberships = proxy(StaffMembershipRepository.class, (object, method, args) ->
                method.getName().equals("findByStaffUserIdAndPropertyId") ? Optional.of(membership) : null);
        PgPropertyRepository properties = proxy(PgPropertyRepository.class, (object, method, args) -> { throw new AssertionError("A denied permission must not query the property"); });
        StaffAccessService service = new StaffAccessService(memberships, users, properties, null);

        ResponseStatusException denied = assertThrows(ResponseStatusException.class,
                () -> service.requireStaffOwner("staff-1", "property-1", StaffPermission.FOOD_MENU));

        assertEquals(HttpStatus.NOT_FOUND, denied.getStatusCode());
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> type.getSimpleName() + " test proxy";
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> null;
                };
            }
            return handler.invoke(object, method, args);
        }));
    }
}
