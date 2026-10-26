# REST API

API base path is `/api`. JSON is used except multipart photo uploads and actuator endpoints. Authenticated browser requests use a server-side session cookie and CSRF token. Call `GET /api/auth/csrf` and send the returned header/token on every state-changing request. Razorpay webhooks are HMAC-verified and exempt from browser CSRF. Errors are JSON with timestamp, status, error, message, path, and details.

Owner endpoints are scoped to the authenticated owner and the requested property. Resident endpoints scope data to the authenticated resident. Administrator APIs require the `ADMIN` role. A UI route or hidden button is not an authorization boundary.

## Authentication and account security

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/api/auth/csrf` | Public | Issues/returns a CSRF token. |
| POST | `/api/auth/register` | Public | Creates a `RESIDENT`, `OWNER`, or `STAFF` account and starts a session. `ADMIN` registration is rejected. Sends verification mail if configured. |
| POST | `/api/auth/login` | Public | Authenticates email/password and rotates the session ID. |
| POST | `/api/auth/google` | Public | Body `{ "credential": "<Google ID token>", "role": "RESIDENT" }` on account creation; validates signature, issuer, audience, expiry, and verified email, then creates or links a role-scoped account and starts a session. `role` is ignored for existing accounts; `ADMIN` cannot be self-registered. Requires matching backend and frontend OAuth client IDs. |
| POST | `/api/auth/logout` | Session | Invalidates the session. |
| GET | `/api/auth/me` | Session | Returns current profile and role, never password data. |
| PATCH | `/api/auth/profile` | Session | Updates name and phone. Email changes are not supported. |
| POST | `/api/auth/email-verification/request` | Public | Body `{ "email": "..." }`; response is generic. Requires email provider configuration. |
| POST | `/api/auth/email-verification/confirm` | Public | Body `{ "token": "..." }`; one-time link expires after 24 hours. |
| POST | `/api/auth/password-reset/request` | Public | Body `{ "email": "..." }`; response is generic. Requires email provider configuration. |
| POST | `/api/auth/password-reset/complete` | Public | Body `{ "token": "...", "password": "..." }`; one-time link expires after 30 minutes. |

Owner property submission and resident bed booking require verified email. Account-link delivery uses Resend when configured.

## Public discovery and property details

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/public/properties` | Paginated eligible listings. Query parameters: `query`, `latitude`, `longitude`, `radiusKm`, `accommodationType`, `minRent`, `maxRent`, `foodAvailable`, `airConditioned`, `facility`, `sharingType`, `availableBedsOnly`, `minimumRating`, `sort`, `page`, `size` (maximum 50). |
| GET | `/api/public/properties/{propertyId}` | Property details only when the property passes the full server-side publication policy. |
| GET | `/api/public/properties/{propertyId}/available-beds` | Available owner-configured rooms and beds for an eligible property. |
| GET | `/api/public/properties/{propertyId}/reviews` | Published reviews with only public fields, rating, date, and aggregate. |
| GET | `/api/public/locations/resolve?query=...` | Google geocoding candidates if configured; does not create property records. |

`sort` supports rent and rating; distance sorting is available when location coordinates are supplied. Text locality search does not apply a distance cutoff. A radius search requires both coordinates and a radius. Property cards include nullable latitude/longitude so the optional map view can place eligible listings. The response includes `content`, `page`, `size`, and `hasNext`.

## Owner properties, photos, and inventory

| Method | Path | Notes |
| --- | --- | --- |
| GET / POST | `/api/owner/properties` | List the authenticated owner's properties / create a private draft. |
| PUT | `/api/owner/properties/{propertyId}` | Edit a draft or rejected property. |
| POST | `/api/owner/properties/{propertyId}/submit` | Submit for review; needs verified email, active subscription, and actual owner photos. |
| PATCH | `/api/owner/properties/{propertyId}/listing?enabled=true` | Toggle listing after publication requirements pass. |
| GET / POST | `/api/owner/properties/{propertyId}/photos` | List photos / upload multipart field `file` and optional `altText`. Maximum 20 photos, 10 MB each, JPEG/PNG only. |
| DELETE | `/api/owner/properties/{propertyId}/photos/{photoId}` | Remove an owned photo. |
| GET / POST | `/api/owner/properties/{propertyId}/buildings` | List / create owner buildings. |
| GET | `/api/owner/properties/{propertyId}/inventory` | Buildings, rooms, beds, and totals. |
| POST | `/api/owner/properties/{propertyId}/buildings/{buildingId}/rooms` | Create a room and its configured bed records. |
| PATCH | `/api/owner/properties/{propertyId}/beds/{bedId}/status?status=...` | Set an eligible bed status (available, maintenance, unavailable, etc.). |

Property body fields: `name`, `streetAddress`, `locality`, `city`, `state`, six-digit `postalCode`, `accommodationType`, `startingRent` in INR, `facilities`, optional paired `latitude`/`longitude`, `foodAvailable`, and `airConditioned`. The server sets owner and draft/publication fields.

## Bookings and accommodation

| Method | Path | Access / Notes |
| --- | --- | --- |
| POST | `/api/resident/properties/{propertyId}/beds/{bedId}/bookings` | Resident request for a specific available bed. Atomically reserves the bed with an expiry. |
| GET | `/api/resident/bookings` | Resident's bookings. |
| POST | `/api/resident/bookings/{bookingId}/cancel` | Resident cancellation. |
| GET | `/api/owner/properties/{propertyId}/bookings` | Owner's property booking queue. |
| POST | `/api/owner/properties/{propertyId}/bookings/{bookingId}/decision` | Owner approves or rejects. |
| POST | `/api/owner/properties/{propertyId}/bookings/{bookingId}/move-in` | Owner completes move-in and creates tenancy. |
| GET | `/api/resident/accommodation` | Current tenancy. |
| GET | `/api/resident/accommodation/history` | Resident's stay history. |
| POST | `/api/resident/accommodation/{tenancyId}/move-out` | Resident requests move-out. |
| GET | `/api/owner/properties/{propertyId}/residents` | Current residents scoped to the property. |
| POST | `/api/owner/properties/{propertyId}/tenancies/{tenancyId}/move-out` | Owner accepts/completes a move-out. |

## Rent and payment operations

Owner endpoints:

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/owner/properties/{propertyId}/rent/charges/generate?billingPeriod=YYYY-MM` | Idempotently generate monthly tenancy charges. |
| GET | `/api/owner/properties/{propertyId}/rent/charges?billingPeriod=YYYY-MM` | Property charge details and balances. |
| POST | `/api/owner/properties/{propertyId}/rent/charges/{chargeId}/offline-payment` | Record cash, bank, or UPI payment with unique idempotency key; gateway method is rejected. |
| GET | `/api/owner/properties/{propertyId}/rent/bed-status?billingPeriod=YYYY-MM` | Accessible bed status, due, paid, and balance for the selected month. |
| GET | `/api/owner/rent/outstanding` | Filter by `propertyId`, `buildingId`, `roomId`, `bedId`, `residentId`, `billingPeriod`, and `overdueOnly`. |
| POST | `/api/owner/properties/{propertyId}/rent/charges/{chargeId}/reminder` | Adds an in-app resident reminder and audit record. |

Resident endpoints:

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/resident/rent/charges` | Resident's own rent charges. |
| GET | `/api/resident/rent/payments` | Resident's own confirmed payment records. |
| GET | `/api/resident/rent/payments/{paymentId}/receipt` | Resident-only receipt. |
| POST | `/api/resident/rent/charges/{chargeId}/checkout` | Creates/reuses a Razorpay order for the current outstanding balance. Body has a unique `idempotencyKey`. |
| POST | `/api/resident/rent/payments/verify` | Verifies callback signature and checks captured provider payment amount, currency, and order before allocation. |
| POST | `/api/webhooks/razorpay/rent` | HMAC-verified `payment.captured` / `order.paid` rent webhook. |

Financial amounts in management records are whole INR rupees; Razorpay API amounts are paise. The browser success callback is not authoritative. Subscription revenue is stored/reported separately from rent.

## Subscriptions and management services

### Property staff

Staff accounts self-register as `STAFF` and verify their email. An owner then grants property-specific permissions; there is no emailed invitation flow. Access can be revoked by the owner. The server rechecks the active membership and permission on every staff operation, and staff changes are written to audit logs.

| Method | Path | Access / Notes |
| --- | --- | --- |
| GET / POST | `/api/owner/properties/{propertyId}/staff` | Owner lists staff / grants an existing verified staff account selected service permissions. |
| PATCH / DELETE | `/api/owner/properties/{propertyId}/staff/{membershipId}` | Owner updates or revokes access for one staff member. |
| GET | `/api/staff/properties` | Staff's assigned properties and service permissions. |
| GET / PATCH | `/api/staff/properties/{propertyId}/complaints[/{complaintId}]` | Staff with `COMPLAINTS` may read and update complaints. |
| GET / POST / DELETE | `/api/staff/properties/{propertyId}/announcements[/{id}]` | Staff with `ANNOUNCEMENTS` may publish property-wide notices and remove notices. |
| GET / PUT | `/api/staff/properties/{propertyId}/food-menu` | Staff with `FOOD_MENU` may view and update the weekly menu. |

The supported permission values are `COMPLAINTS`, `ANNOUNCEMENTS`, and `FOOD_MENU`. Staff do not inherit owner access to inventory, residents, bookings, rent, finances, subscriptions, photos, or property settings.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/owner/subscription` | Current owner subscription, including whether end-of-cycle cancellation is scheduled. |
| GET | `/api/owner/subscription/invoices` | Owner's platform subscription invoices. |
| POST | `/api/owner/subscription/checkout` | Starts/reuses the configured monthly Razorpay subscription. |
| POST | `/api/owner/subscription/verify` | Verifies checkout signature; verified webhook remains the authority for activation. |
| POST | `/api/owner/subscription/cancel` | Idempotently schedules auto-renewal cancellation for the end of the active billing period; requires Razorpay credentials. |
| POST | `/api/webhooks/razorpay` | HMAC-verified subscription lifecycle events. |
| GET / PUT | `/api/owner/properties/{propertyId}/food-menu` | List / save a weekly meal. |
| GET / POST | `/api/owner/properties/{propertyId}/announcements` | List / publish property, building, or resident notice. |
| DELETE | `/api/owner/properties/{propertyId}/announcements/{id}` | Delete an owned announcement. |
| GET / PATCH | `/api/owner/properties/{propertyId}/complaints` | List / update resident complaint status and response. |
| GET / POST | `/api/resident/complaints` | List / submit complaint tied to current tenancy. |
| GET | `/api/resident/announcements` | Notices for resident's active tenancy. |
| GET | `/api/resident/food-menu` | Published menus for resident's active tenancy. |
| GET | `/api/resident/notifications` | Resident inbox. |
| GET | `/api/owner/notifications` | Owner inbox. |
| POST | `/api/notifications/{id}/read` | Mark caller-owned notification read. |
| GET / POST | `/api/support/tickets` | List the caller's support requests / open a support request as any authenticated account. |
| POST | `/api/resident/properties/{propertyId}/reviews` | Verified-stay review. |
| POST | `/api/resident/reviews/{id}/report` | Report another resident's review. |
| POST | `/api/resident/properties/{propertyId}/report` | Report an eligible public listing. |
| GET / POST | `/api/owner/properties/{propertyId}/deposits` | List / record tenancy deposits. |
| POST | `/api/owner/properties/{propertyId}/deposits/{id}/refund` | Record an audited deposit refund (manual record, not gateway refund). |

## Administration

All `/api/admin/**` paths require an administrator account provisioned from environment variables or already in MongoDB.

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/api/admin/overview` | Account, review, report, subscription, and complaint counts. |
| GET | `/api/admin/properties/review-queue` | Pending real property submissions. |
| POST | `/api/admin/properties/{propertyId}/decision` | Approve or reject and audit. |
| POST | `/api/admin/properties/{propertyId}/suspend?reason=...` | Suspend and audit. |
| GET | `/api/admin/owners` | Owner status, subscription status, property count. |
| PATCH | `/api/admin/owners/{id}/enabled?enabled=true&reason=...` | Enable/disable owner and audit. |
| GET | `/api/admin/reports/listings` | Open listing reports. |
| POST | `/api/admin/reports/listings/{id}/decision` | Suspend or dismiss a report. |
| GET | `/api/admin/reviews/reported` | Reported reviews. |
| POST | `/api/admin/reviews/{id}/decision` | Keep or remove review. |
| GET | `/api/admin/complaints` | Platform complaint review. |
| GET | `/api/admin/support/tickets` | Support request queue with requester contact details. |
| PATCH | `/api/admin/support/tickets/{id}` | Reply, set ticket status, and create an audit event. |
| GET | `/api/admin/subscriptions` | Subscription records. |
| GET | `/api/admin/subscription-invoices` | Platform invoices. |
| GET | `/api/admin/financial-report` | Subscription invoice revenue and rent outstanding, reported separately. |
| GET | `/api/admin/audit-logs?entityType=...` | Recent audit records. |

This API is the implemented surface, not a guarantee that every deployment is configured for external service traffic. SMS delivery, configurable platform settings, gateway refunds, and full fraud monitoring are not implemented.
