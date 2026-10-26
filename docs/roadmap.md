# Delivery status and limitations

PGConnect now implements real MongoDB-backed discovery, booking, tenancy, rent, subscription, resident services, and administration workflows. The application intentionally contains no preloaded example owners, properties, residents, photos, reviews, or payment history. A build passing does not mean production integrations are configured.

## Implemented

- Responsive React/Vite public search, property detail, owner, resident, and admin workspaces.
- Spring Boot REST API with MongoDB, BCrypt password hashes, cookie sessions, CSRF, role checks, owner/resident resource scoping, validation, and consistent errors.
- Resident, owner, staff, and admin backend roles with protected booking, tenancy, rent, inventory, subscription, and property-management APIs.
- Google Identity Services login and registration with backend JWT signature, issuer, audience, expiry, and verified-email checks; existing verified email accounts are linked without changing their role.
- Email verification and one-time password reset flows, sent through Resend when configured.
- Owner property drafts, edit, photo upload validation and private S3-compatible storage support, review submission and publish lifecycle.
- Public visibility enforcement for approved/published/enabled properties with an active owner subscription, real photos, and no suspension.
- Locality search, optional Google geocoding candidate resolution, coordinate radius query, and Google Maps list/map view; rent, property type, food, AC, facilities, sharing, availability, and rating filters.
- Owner buildings, rooms, beds, availability statuses, atomic temporary reservations, booking decisions, expiries, move-in and move-out, current accommodation, and history.
- Monthly rent charge generation with a tenancy-period uniqueness key, rent history, partial/offline payment records, gateway order/attempt/verification/webhook code, allocations, receipts, outstanding/overdue report, in-app reminders, and monthly bed status.
- ₹1,000 default monthly owner subscription plan, Razorpay checkout and signed lifecycle webhook handling, invoices, end-of-period auto-renewal cancellation, and expiry eligibility logic. The amount/grace period are environment-configurable.
- Hourly subscription expiry processing with a deduplicated in-app renewal reminder and notices when scheduled cancellation or expiry takes effect; owners can read and dismiss these from the inbox.
- Owner food menus, complaints and responses, announcements, deposit/refund records, resident notifications, verified-stay reviews, moderation/reporting, and audit entries.
- Property-scoped staff accounts with owner-granted permissions for complaints, announcements, and food menus. Staff accounts self-register and verify their email before the owner grants access; other owner operations remain owner-only.
- Account-scoped support requests with an administrator reply/status queue and audited updates.
- Administrator provisioning, property review/suspension, owner enablement, listing/review reports, complaint review, subscription and financial report, and audit-log screens.
- Docker Compose local stack with MongoDB replica-set initialization; API, data-model, environment, backup, and deployment docs.

## Requires provider configuration

- Email verification/password reset: Resend key and verified sender address.
- Google sign-in: set the same web OAuth client ID as backend `GOOGLE_OAUTH_CLIENT_ID` and frontend `VITE_GOOGLE_CLIENT_ID`; register `http://localhost:5174` and the production site as authorized JavaScript origins in Google Cloud.
- Owner photos: S3 or compatible private object storage and credentials.
- Owner subscription and resident online rent: Razorpay account keys plus two configured webhook paths. No gateway call is made without keys.
- Place geocoding and radius search from a typed college/locality: backend Google Maps key. The map view requires a separate browser Maps JavaScript key, restricted to the deployed website origin.

## Incomplete or limited

- Phone verification, SMS, MFA, and broader security abuse controls are not implemented. A process-local IP rate limit protects login, registration, account-link, verification-token, and booking endpoints; deployments with multiple API instances need a shared limiter.
- Email delivery is limited to verification and password reset. Business notifications are in-app only; there is no scheduled email/SMS queue or resident push delivery.
- A dedicated college/location reference collection is not included; typed place resolution depends on the configured geocoder.
- Gateway rent refunds/reconciliation UI, suspicious-activity tooling, and configurable platform settings are not implemented. Subscription expiry notices are in-app only; they do not send email or SMS.
- Property-wide versus room-specific amenities are not modeled separately. Facilities are property-level text; sharing/rent/capacity are room-level.
- Mongo schema validation, production index migration tooling, load testing, observability dashboards/alerts, and automated restore rehearsal are not implemented.
- Search currently filters a bounded result set in application code for some availability/rating/facility/sort combinations. It is suitable for the initial product slice, not a measured high-volume search service.
- Automated coverage is limited. The current unit suite does not prove Mongo transaction behavior under concurrency, payment provider reconciliation, or security under production configuration.

## Test record

Checks executed for this delivery:

- `frontend`: `npm run build` — passed with Vite production output.
- `backend`: `mvn -B -o -Dmaven.repo.local=/private/tmp/pgconnect-m2 clean package` — all backend sources compiled and 10 JUnit tests passed with 0 failures/errors, including Google account provisioning/linking and rate-limit behavior.
- Root: `docker compose config --quiet` — passed. Docker Desktop itself was not running, so the Compose containers were not launched.
- A temporary, isolated MongoDB replica set and the packaged API were used for a persisted-data smoke run: registration, private property draft, owner inventory, eligible bed discovery, resident booking, owner approval, move-in, monthly charge generation, and a partial offline payment allocation all passed. The smoke database and test records were removed afterward. A temporary test-only photo record and direct verification/subscription flags were used only in that disposable database to reach the booking path; cloud photo upload, real email, and gateway webhooks were not tested.
- Staff permission denial and support account scoping have unit coverage. Those new features were not exercised in a database-backed workflow.
- Google login unit tests cover verified-account creation, linking without changing an existing role, rejection of unverified email, and missing-client configuration. A live Google sign-in was not run because no OAuth web client ID has been configured.
- Both webhook routes were called without a session against a disposable API/Mongo instance. They reached their handlers and returned the expected `503` because no provider webhook secret was configured; signed provider events were not tested.
- The rebuilt API connected to the configured MongoDB Atlas database. `/actuator/health` returned `UP`; the public listing query returned HTTP 200 with an empty result page. This was read-only and no records were inserted or changed.

These checks do not prove concurrent booking/payment behavior, browser accessibility, provider sandbox flows, production security, load capacity, or backup restoration. Re-run `npm run build` in `frontend` and `mvn test` in `backend` for the automated checks.
