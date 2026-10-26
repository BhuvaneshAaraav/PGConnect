# MongoDB data model

MongoDB is authoritative. Spring Data creates indexes on application startup (`spring.data.mongodb.auto-index-creation=true`); a MongoDB replica set is required for multi-document transactional workflows. The Compose initializer creates a local single-node replica set. No seed runner or sample production records exist. No collection is created until a document is saved or an index is initialized.

## Collections in the current application

| Collection | Use and relationships |
| --- | --- |
| `users` | Owner, resident, and admin profiles; normalized email, BCrypt hash, role, enabled state, phone, email verification, optional unique Google `sub`, and owner subscription eligibility. Owner/resident profiles are currently fields on this document. |
| `staff_memberships` | Owner-granted, property-scoped staff membership, active state, and permission set (`COMPLAINTS`, `ANNOUNCEMENTS`, `FOOD_MENU`). Staff user identities remain in `users`; access grants and revocations are audited. |
| `account_tokens` | Hashed, single-use email verification and password reset tokens with expiry. Raw tokens are only sent to the provider. TTL expiry index. |
| `properties` | Owner ID, real address, optional GeoJSON point, facilities, food/AC flags, listing lifecycle, suspension, and bounded photo metadata. Photo bytes live in private object storage. |
| `buildings` | Owner/property reference and unique normalized building code. |
| `rooms` | Building/property/owner references, floor, room code, monthly rent, sharing type, room capacity/status, and embedded bounded bed IDs as convenience. |
| `beds` | Stable bed record with property/building/room references, normalized code, status, tenancy/reservation references, and `reservedUntil`. |
| `bookings` | Resident, owner, property, room, bed, lifecycle status, expiry, and messages. Conditional bed updates prevent duplicate holds. |
| `tenancies` | Resident/owner/property/building/room/bed relationship, historical rent, stay dates, move-out state. |
| `rent_charges` | One monthly charge per tenancy and billing period with immutable rent amount, due date, paid amount, outstanding balance, and status. |
| `rent_payments` | Confirmed offline or gateway payment with owner/resident/tenancy/charge references, method, receipt, provider ID, verification marker, and idempotency key. |
| `payment_attempts` | Razorpay rent order lifecycle, amount in paise, unique provider order and request idempotency key. |
| `payment_allocations` | Connects confirmed rent payments to rent charges. |
| `owner_subscriptions` | Owner's separate platform subscription, provider ID, lifecycle, billing period, dates, and scheduled end-of-period cancellation flag. |
| `subscription_plans` | Provider plan identifiers keyed by configured monthly amount in paise. |
| `subscription_invoices` | Provider subscription billing records, kept separate from resident rent. |
| `razorpay_events` | Deduplicates signed provider subscription webhook events. Rent payment confirmation also uses payment attempt state transitions. |
| `deposits` | Tenancy deposit received and cumulative manually recorded refunds. |
| `food_menus` | Property weekly day/meal entries, published to active residents. |
| `complaints` | Resident/tenancy/property association, status, owner assignment and response. |
| `announcements` | Owner/property with optional building or resident audience and optional expiry. |
| `notifications` | In-app inbox records by user; not a provider delivery queue. |
| `reviews` | Resident/property/tenancy relationship, rating, moderation state, and report metadata. Public API projects only review ID, rating, text, and date. |
| `property_reports` | Reports on public properties and administrator decision state. |
| `audit_logs` | Sensitive property, payment, deposit, complaint, moderation, support, staff membership, and owner actions. |
| `support_tickets` | Account-owned support request, requester role, category, subject, description, admin response, status, and timestamps. Requester and status/time indexes support private inbox and admin queue lookups. |

Configurable platform settings, standalone facility records, college reference places, and gateway rent refunds are not currently modeled.

## Important indexes and uniqueness

- Unique normalized user email; role/subscription lookup.
- `properties.location` `2dsphere` geospatial index; owner/time, public status, city, locality, and accommodation type indexes.
- Unique building code per property; room number per building; bed code per room.
- Rent charge unique `(tenancyId, billingPeriod)` to make charge generation idempotent.
- Unique rent payment idempotency key; unique payment attempt idempotency key and sparse unique provider order ID.
- Unique review per `(residentId, propertyId)`; unique weekly menu per `(propertyId, dayOfWeek, meal)`.
- Unique subscription plan amount, sparse unique provider subscription/invoice IDs, and unique subscription event IDs.
- Token hash unique and token expiry TTL; notification inbox, complaint queue, report queue, and audit query indexes.

Review all declared annotations in the Java model before changing production index definitions. Auto-index creation is convenient for local development; production index creation should be planned and monitored.

## Consistency and currency

MongoDB transactions cover workflows that update multiple records together, including booking decisions/move-in, rent payment allocation, complaints/notifications, deposits/audit, and provider subscription processing. A replica-set deployment is therefore required. Atomic conditional updates remain the final authority for bed reservation, payment allocation, and refund bounds. Frontend state can be stale and is never proof of availability or payment.

Rent and deposit amounts are stored in whole INR rupees. Razorpay subscription and rent amounts use paise at the provider boundary. Subscription revenue and resident rent are distinct flows. Each charge preserves the tenancy rent for its billing period when future room rent changes.

## References and lifecycle

Related documents use stable Mongo IDs. The server derives owner/resident scope from the authenticated session; clients do not submit trusted owner IDs. Expiring a subscription removes public listing eligibility but does not delete the property's residents, charges, payments, or history. A published listing also requires administrator approval, an active owner subscription, enabled listing status, no suspension, and at least one actual owner photo.

No destructive migration is included. Back up before planned production schema or index changes. See [operations](operations.md) for backup and restore rehearsal guidance.
