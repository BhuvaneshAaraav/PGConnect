package com.pgconnect.booking;

import com.pgconnect.tenancy.Tenancy;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookingController {
    private final BookingService bookings;
    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping("/api/public/properties/{propertyId}/available-beds")
    public List<BookingService.AvailableRoom> available(@PathVariable String propertyId) { return bookings.availableBeds(propertyId); }

    @PostMapping("/api/resident/properties/{propertyId}/beds/{bedId}/bookings")
    @PreAuthorize("hasRole('RESIDENT')")
    public Booking request(@PathVariable String propertyId, @PathVariable String bedId,
                           @Valid @RequestBody BookingService.BookingRequest request, Authentication auth) {
        return bookings.requestBooking(auth.getName(), propertyId, bedId, request);
    }

    @GetMapping("/api/resident/bookings")
    @PreAuthorize("hasRole('RESIDENT')")
    public List<BookingService.ResidentBookingView> residentBookings(Authentication auth) { return bookings.residentBookings(auth.getName()); }

    @PostMapping("/api/resident/bookings/{bookingId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public Booking cancel(@PathVariable String bookingId, Authentication auth) { return bookings.cancel(auth.getName(), bookingId); }

    @GetMapping("/api/owner/properties/{propertyId}/bookings")
    @PreAuthorize("hasRole('OWNER')")
    public List<BookingService.OwnerBookingView> ownerBookings(@PathVariable String propertyId, Authentication auth) {
        return bookings.ownerBookings(auth.getName(), propertyId);
    }

    @PostMapping("/api/owner/properties/{propertyId}/bookings/{bookingId}/decision")
    @PreAuthorize("hasRole('OWNER')")
    public Booking decide(@PathVariable String propertyId, @PathVariable String bookingId,
                          @Valid @RequestBody BookingService.BookingDecision request, Authentication auth) {
        return bookings.decide(auth.getName(), propertyId, bookingId, request);
    }

    @PostMapping("/api/owner/properties/{propertyId}/bookings/{bookingId}/move-in")
    @PreAuthorize("hasRole('OWNER')")
    public Tenancy moveIn(@PathVariable String propertyId, @PathVariable String bookingId,
                          @Valid @RequestBody BookingService.MoveInRequest request, Authentication auth) {
        return bookings.moveIn(auth.getName(), propertyId, bookingId, request);
    }
}
