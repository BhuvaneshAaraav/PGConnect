package com.pgconnect.support;

import com.pgconnect.auth.Role;
import com.pgconnect.auth.UserAccount;
import com.pgconnect.auth.UserAccountRepository;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.concurrent.atomic.AtomicReference;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportTicketServiceTest {
    @Test
    void creatingTicketUsesAuthenticatedAccountAndRole() {
        UserAccount requester = new UserAccount("Resident", "resident@example.test", "hash", Role.RESIDENT);
        requester.setId("resident-1");
        AtomicReference<SupportTicket> saved = new AtomicReference<>();
        UserAccountRepository users = proxy(UserAccountRepository.class, (object, method, args) ->
                method.getName().equals("findById") ? Optional.of(requester) : null);
        SupportTicketRepository tickets = proxy(SupportTicketRepository.class, (object, method, args) -> {
            if (method.getName().equals("save")) { saved.set((SupportTicket) args[0]); return args[0]; }
            return null;
        });
        SupportTicketService service = new SupportTicketService(tickets, users, null);

        service.create("resident-1", new SupportTicketService.CreateTicketRequest("ACCOUNT", "Login help", "Cannot sign in"));

        assertEquals("resident-1", saved.get().getRequesterId());
        assertEquals(Role.RESIDENT, saved.get().getRequesterRole());
        assertEquals(SupportTicketStatus.OPEN, saved.get().getStatus());
    }

    @Test
    void accountInboxQueriesOnlyThatRequestersTickets() {
        AtomicReference<String> requesterId = new AtomicReference<>();
        SupportTicketRepository tickets = proxy(SupportTicketRepository.class, (object, method, args) -> {
            if (method.getName().equals("findTop100ByRequesterIdOrderByCreatedAtDesc")) { requesterId.set((String) args[0]); return List.of(); }
            return null;
        });
        SupportTicketService service = new SupportTicketService(tickets, null, null);

        service.myTickets("resident-1");

        assertEquals("resident-1", requesterId.get());
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
