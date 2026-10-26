# PGConnect
# complete project idea is start from line 166
PGConnect is a React/Vite and Spring Boot application for discovering owner-registered PGs and managing property operations. MongoDB is the source of truth. The application starts with no seeded listings, users, rooms, bookings, payments, or reviews.

The repository contains working public discovery, owner, resident, staff, and administrator workflows. Owners can grant staff property-scoped access to resident complaints, announcements, and weekly menus. Integrations that require provider accounts (email, S3-compatible photo storage, Razorpay, and Google geocoding) are controlled by environment variables. A listing stays private until its owner uploads real photos, has an active subscription, and passes administrator review.

## Run locally with Docker

Requirements: Docker Desktop. Copy the sample environment file, fill any integrations you want to exercise, then start the stack:

```sh
cp .env.example .env
docker compose up --build
```

Open <http://localhost:3000>. The API health endpoint is <http://localhost:8080/actuator/health>. MongoDB data remains in the named `pgconnect_mongo` volume. The Compose stack initializes a single-node MongoDB replica set because booking and financial workflows use transactions. It does not delete or reset an existing database.

Administrator creation is optional. Set `PGCONNECT_ADMIN_EMAIL` and a password of at least 16 bytes before first startup to provision the first administrator. Bootstrap never overwrites an existing account. If no administrator is configured, owners can create drafts but no one can approve them.

## Run Vite and Spring Boot separately

Start the Compose MongoDB and replica-set initializer:

```sh
docker compose up -d mongo mongo-init
```

In one terminal, run the API from `backend`:

```sh
cd backend
MONGODB_URI='mongodb://localhost:27017/pgconnect?replicaSet=rs0' WEB_ORIGIN=http://localhost:5173 mvn spring-boot:run
```

In another terminal, run the Vite app:

```sh
cd frontend
npm install
npm run dev
```

To use MongoDB Atlas instead of the local Compose database, put the URI in the ignored root `.env` file, then start the API from `backend` with:

```sh
set -a
. ../.env
set +a
mvn spring-boot:run
```

Never commit `.env`; use a secret manager for deployed environments.

For email links in this mode, set `EMAIL_WEB_ORIGIN=http://localhost:5173` on the API. See [.env.example](.env.example) for the supported configuration.

## Integrations

- **Email:** Set `EMAIL_RESEND_API_KEY` and a verified `EMAIL_FROM` address. Verification and password reset links use Resend. Account-link requests return a generic response so they do not confirm whether an email is registered.
- **Google sign-in:** Set the same web OAuth client ID in backend `GOOGLE_OAUTH_CLIENT_ID` and `frontend/.env.local` as `VITE_GOOGLE_CLIENT_ID`. Create a Web application OAuth client and allow the frontend origin (the current dev server is `http://localhost:5174`). The frontend button appears when configured; the backend verifies the Google-signed ID token before starting a PGConnect session.
- **Property photos:** Set `OBJECT_STORAGE_ENABLED=true`, bucket, region, and AWS credentials or an S3-compatible endpoint. Uploads are validated and image bytes are re-encoded before private object storage. Without storage, upload is unavailable and properties cannot pass the photo publication gate.
- **Payments:** Set Razorpay test keys and webhook secret. Configure `/api/webhooks/razorpay` for subscription events and `/api/webhooks/razorpay/rent` for captured rent payments. Browser callbacks do not activate subscriptions or mark rent paid without server verification.
- **Location search:** Set `GOOGLE_MAPS_API_KEY` for place resolution and radius search. Without it, locality text search and browser geolocation coordinates remain available. To enable the Google Maps list/map switch, create `frontend/.env.local` with `VITE_GOOGLE_MAPS_API_KEY=...`; restrict that browser key by HTTP referrer and Maps JavaScript API.

See [docs/api.md](docs/api.md), [docs/data-model.md](docs/data-model.md), [docs/operations.md](docs/operations.md), and [docs/roadmap.md](docs/roadmap.md) for API contracts, persistence, backup/deployment guidance, and current limitations.

Staff members create a staff account at `/register?role=STAFF`, verify their email, then ask a PG owner to grant access from the property's **Staff access** section.

## Build and tests

```sh
cd frontend && npm run build
cd ../backend && mvn test
```

These checks cover compilation and the current unit test suite. They are not a production-readiness or load-test claim. This application still needs provider configuration, expanded integration/concurrency/security tests, a shared rate limiter for multi-instance deployments, and operational monitoring before production use.


# PGConnect

**An open-source PG discovery and hostel management platform.**

PGConnect aims to connect residents with real paying guest accommodations while giving property owners tools to manage buildings, rooms, beds, residents, bookings, and monthly rent.

## Features

### Resident experience
- Discover registered PGs near a college, locality, or current location.
- Filter properties by budget, accommodation type, facilities, and availability.
- View property details and owner-uploaded photographs.
- Request or book an available bed.
- View accommodation information and monthly rent records.
- Access payment history, food menus, complaints, and announcements.

### PG owner experience
- Manage multiple PG properties.
- Configure buildings, rooms, and individual beds.
- Manage residents and booking requests.
- Track monthly rent and outstanding balances.
- View paid and overdue rent indicators by building, room, and bed.
- Upload property photographs.
- Manage facilities, food menus, complaints, and announcements.
- Manage the platform subscription.

### Platform administration
- Review and approve properties.
- Manage owner subscriptions.
- Moderate reported listings.
- Manage platform support and administrative operations.

## Technology stack

- React.js and JavaScript
- Java and Spring Boot
- Spring Security
- MongoDB and Spring Data MongoDB
- Docker for local development

## Project status

**Work in progress — not production-ready.**

Some modules and integrations may be incomplete or require external service configuration. Consult the issue tracker and roadmap before assuming a feature is fully implemented.

PGConnect must not display fabricated PG listings or pretend that test payment data represents real transactions.

## Getting started

Before running the application, consult the project setup documentation.

You will need compatible Java and Maven versions, Node.js and npm, and a working MongoDB instance. Some features require additional service configuration.

1. Clone the repository.
2. Configure your local environment using `.env.example` and the backend configuration instructions.
3. Start MongoDB.
4. Start the Spring Boot backend.
5. Install frontend dependencies and start the React development server.

Never commit your real `.env` file, database credentials, payment-provider secrets, or private resident information.

## Roadmap

- [ ] Complete owner registration and property creation.
- [ ] Complete building, room, and bed editing.
- [ ] Complete resident registration and direct bed assignment.
- [ ] Strengthen concurrent booking protection.
- [ ] Implement reliable automatic monthly rent generation.
- [ ] Complete payment reconciliation and refund workflows.
- [ ] Complete email and SMS notifications.
- [ ] Finish nearby geographical search and filtering.
- [ ] Expand automated tests and deployment documentation.

Check GitHub Issues for current tasks and known defects.

## Contributing

Contributions are welcome! Read `CONTRIBUTING.md` before opening an issue or submitting a pull request.

Please discuss larger changes before investing substantial work.

## License

See the `LICENSE` file. Confirm the project license before reusing or redistributing this code.



PGConnect — Real-World PG and Hostel Management Platform

1. Project Overview

Build a complete, production-oriented web application called PGConnect, a platform that connects people looking for paying guest accommodation (PGs) with real PG owners.

The platform must serve students, working professionals, and other residents looking for accommodation. It must also provide PG owners with a complete property management system.

This is a real-world application, not a static website, UI demonstration, college project prototype, or application populated with fake information.

The system must use a real MongoDB database. All properties, owners, residents, rooms, beds, bookings, subscriptions, rent charges, payment statuses, reviews, and other application records must come from the database.

Never create fake PG listings, fictional residents, fabricated payment histories, or sample properties that appear to be real.

If the database contains no eligible properties, display an informative empty state. Never manufacture listings to make the homepage appear populated.

The application must have a polished, modern, responsive interface and a secure, maintainable backend architecture.

⸻

2. Mandatory Technology Stack

Use the following technologies.

Frontend

* React.js
* JavaScript
* Vite
* React Router
* A consistent CSS design system
* Responsive layouts for desktop, tablets, and mobile devices
* Reusable components and properly organized frontend modules

Backend

* Java
* Spring Boot
* Spring Security
* Spring Data MongoDB
* REST APIs
* Jakarta Bean Validation
* Appropriate transaction and concurrency controls

Database

* MongoDB
* MongoDB indexes for frequently queried fields
* MongoDB geospatial indexes for nearby PG searches
* Database schema validation where appropriate
* References between related documents designed carefully for consistency

Supporting technologies

* Cloud image storage for property photographs
* A suitable Indian payment gateway, such as Razorpay, for subscription and rent payments
* Email or SMS notification providers
* Docker for local development
* JUnit and Mockito for backend tests
* Appropriate frontend testing tools
* Logging, monitoring, and error handling

Use a modular monolithic architecture initially. Do not introduce microservices unless actual requirements justify them.

MongoDB must be the authoritative data source for application records.

⸻

3. User Roles

The application must support three major roles.

1. Public visitors and prospective residents.
2. Registered residents.
3. PG owners.

A separate platform administrator role must manage property verification, owner subscriptions, reported listings, and platform operations.

Enforce role-based and resource-level authorization in the Java backend.

Hiding a button in React must never be considered sufficient security.

A resident must not access another resident’s private information. One owner must not access another owner’s properties or financial records. Administrative operations must require explicit administrator authorization.

⸻

4. Public Homepage — PG Discovery Before Login

When someone opens PGConnect, the first page must be a public PG discovery homepage.

Do not force visitors to log in before searching for accommodation.

The homepage must support searching by:

* College name.
* City.
* Locality.
* Area or address.
* Current device location, when the user grants permission.

For example, a student studying at AMC College should be able to search for PGs around the college and choose a preferred locality such as JP Nagar.

The user must be able to select a search radius:

* Within 1 km.
* Within 2 km.
* Within 5 km.
* Within 10 km.
* A custom radius where supported.

The search should support both radius-based searches and locality-based searches.

Clearly explain which search mode is active. Do not silently exclude a locality’s properties just because they fall outside a selected radius.

Provide an option such as “Find PGs Near Me.” Request browser geolocation permission only when necessary.

If the user denies location permission, continue to support college and locality searches.

Do not store precise user location unnecessarily.

Strict property visibility rule

Only actual properties registered by PG owners through PGConnect may appear in public discovery.

A property must satisfy all applicable requirements before appearing publicly:

1. The property was created by an authenticated owner.
2. The property has been approved by the platform.
3. Its listing is enabled and published.
4. The owner’s subscription is active.
5. The property is not suspended or otherwise restricted.
6. The property has actual photos uploaded by the owner.
7. Any required verification and publication conditions have been completed.

Enforce these rules in the backend.

Never rely exclusively on frontend filtering.

Never automatically display all PGs found on Google Maps or another external platform. External map services may help identify geographical locations, but they must not create fictional PG listings in our database.

If no eligible property exists, display:

“No registered PGs found nearby yet.”

Provide options to change the location, widen the search radius, or try another locality.

If the backend or database is unavailable, display a genuine error state. Do not disguise infrastructure failures as successful searches with zero results.

Property search filters

Allow residents to filter properties by:

* Monthly rent.
* Boys’ PG, girls’ PG, or co-living accommodation.
* Single, double, triple, or four-sharing rooms.
* Available beds.
* Food availability.
* AC or non-AC.
* Wi-Fi.
* Laundry.
* Parking.
* Hot water.
* Distance from a selected college or location.
* Property ratings and reviews.
* Other facilities configured by the owner.

Provide sorting by distance, rent, and rating where reliable data exists.

Support both map and list views when map integration is configured.

Property cards must display real database information, including the actual property name, locality, rent, facilities, approved photos, rating data where available, and current availability.

Never display fabricated ratings or reviews.

Only use an honest empty state when no review exists.

⸻

5. Real PG Photographs

Property images must be uploaded by the actual PG owner or an authorized property manager.

Do not use stock photographs from Unsplash, random internet images, generated hostel photographs, or unrelated images as real property photographs.

Do not show invented property photos in sample cards.

Every property must support multiple owner-uploaded images, such as:

* Building exterior.
* Bedrooms.
* Bathrooms.
* Kitchen.
* Common areas.
* Study spaces.
* Other relevant facilities.

Provide image previews, upload validation, progress indicators, removal controls, and clear upload errors.

Store the actual image files in suitable cloud object storage. Store their URLs, metadata, and ownership relationships in MongoDB.

Avoid storing large binary images directly in ordinary MongoDB documents.

Only authorized owners or property managers should upload or modify images belonging to their properties.

Property images must not become publicly visible merely because someone uploads a photograph. Apply appropriate property publication and moderation rules.

If a registered owner has not uploaded a photograph, show a neutral placeholder clearly indicating that photos are unavailable. Never substitute an unrelated image.

⸻

6. Resident Registration and Login

Residents include college students, working professionals, and other people who need PG accommodation.

Support secure account registration and login.

Provide the necessary authentication, validation, session management, password reset, and account security features.

Email or phone verification should be supported.

A newly registered account must not automatically become an occupant of a property.

A resident must become associated with a specific property and bed through a valid booking, an authorized owner assignment, or another approved accommodation workflow.

Resident dashboard

After login, the resident should see their actual account and accommodation information.

The dashboard must include:

* Resident profile.
* Current property.
* Building.
* Room number.
* Bed number.
* Monthly rent.
* Rent due date.
* Current payment status.
* Outstanding balance.
* Payment history.
* Receipts.
* Food menu.
* Facilities.
* Announcements.
* Complaints.
* Booking requests.
* Move-in and move-out information.

Do not display fictitious accommodation information if the resident has not booked a bed or been assigned to a PG.

For residents who have no current accommodation, display a useful message and a link to search available properties.

Resident navigation

Create pages for:

1. Dashboard.
2. Find PGs.
3. Property details.
4. Room and bed selection.
5. Booking requests.
6. My accommodation.
7. Rent and payments.
8. Payment history.
9. Receipts.
10. Food menu.
11. Complaints and maintenance.
12. Announcements.
13. Profile and settings.
14. Move-out requests.

⸻

7. Room and Bed Selection

A resident must be able to view a property, select a room, and select an individual available bed.

The owner must configure the property’s buildings, floors, rooms, sharing arrangements, beds, and rent.

For example, the database might contain:

Property: Sunrise Residency
Building: A
Room: 115
Room type: Four sharing

Beds:

* A1: Occupied.
* A2: Occupied.
* B1: Occupied.
* B2: Available.

These are illustrative examples only and must not be inserted automatically as real property records.

The actual interface must use records entered by registered owners.

Each bed must have a unique identifier and an appropriate relationship to its room and property.

Display only genuinely available beds as bookable.

Prevent two users from successfully booking the same bed at the same time. Implement server-side atomic booking operations, suitable locking or conditional updates, and appropriate database constraints or uniqueness rules.

Temporary reservations must expire when applicable.

A property owner should be able to approve or reject booking requests when their business rules require approval.

Do not mark a resident as occupying a bed until the booking and move-in workflow reaches the appropriate confirmed state.

⸻

8. PG Owner Registration

Create a dedicated owner registration process.

The workflow should be:

1. Owner opens PGConnect.
2. Selects “List Your PG.”
3. Creates an owner account.
4. Verifies their email or phone.
5. Completes the owner profile.
6. Reviews the subscription plan.
7. Activates their subscription through a verified payment.
8. Adds their property details.
9. Uploads real property photographs.
10. Configures rooms, beds, facilities, and rent.
11. Submits the property for verification.
12. The administrator reviews the property.
13. Once all publication conditions are satisfied, the property becomes publicly visible.

Allow owners to prepare property information before paying if appropriate, but do not publish their properties until subscription and verification requirements are met.

⸻

9. PG Owner Dashboard

The owner dashboard is the central property management interface.

An owner must be able to manage multiple PG properties under one account.

For example, an owner may manage PG 1, PG 2, and PG 3.

These names are examples of the interface structure, not pre-populated real property data.

The owner should be able to select a property and access only the corresponding property’s information.

Owner dashboard overview

Display actual database-derived metrics, including:

* Total properties.
* Total buildings.
* Total rooms.
* Total beds.
* Occupied beds.
* Available beds.
* Temporarily reserved beds.
* Beds unavailable for maintenance.
* Resident count.
* Monthly rent billed.
* Monthly rent collected.
* Outstanding rent.
* Subscription status.

Calculate metrics from actual records.

Do not hardcode sample numbers into the dashboard.

Do not confuse room capacity, active residents, and available beds.

Owner navigation

Include:

1. Overview.
2. My Properties.
3. Building Management.
4. Rooms and Beds.
5. Residents.
6. Booking Requests.
7. Rent and Payments.
8. Deposit Management.
9. Property Photos.
10. Facilities.
11. Food Menu.
12. Complaints.
13. Announcements.
14. Reports.
15. Subscription and Invoices.
16. Staff Management.
17. Profile and Settings.

⸻

10. Building, Room, and Bed Management

Owners must be able to manage their actual accommodation inventory.

Support multiple buildings per owner and multiple rooms per building.

For every room, support appropriate fields such as:

* Property ID.
* Building ID.
* Floor number.
* Room number.
* Room type.
* Maximum capacity.
* Actual bed records.
* Monthly rent.
* Available facilities.
* Room status.

Every bed should have its own record and status.

Support the following bed states where appropriate:

* AVAILABLE.
* RESERVED.
* OCCUPIED.
* MAINTENANCE.
* UNAVAILABLE.

Availability must be determined from the current authoritative records.

Do not let the owner create inconsistent room capacity and bed records.

If a room is already occupied, prevent unsafe capacity changes.

Implement validation to prevent the deletion or reassignment of a bed that is associated with an active resident without following the appropriate transfer or move-out process.

⸻

11. Critical Feature: Green and Red Monthly Rent Indicators

This is one of the most important features of PGConnect.

The owner must know exactly which building, room, and bed belongs to every resident and whether that resident has paid the rent for a selected month.

Create a visual room-and-bed dashboard.

For example:

Building A.

Room 101:

* Bed A1: Resident — Green — Paid.
* Bed A2: Resident — Red — Overdue.
* Bed B1: Resident — Green — Paid.
* Bed B2: Vacant — Gray.

Room 102:

* Bed A1: Resident — Red — Overdue.
* Bed A2: Resident — Green — Paid.

This is an example of the intended interface. Actual values must come from MongoDB.

Color meanings

* Green: The selected month’s rent has been fully paid and the payment has been confirmed.
* Red: The selected month’s rent is overdue and remains outstanding.
* Orange: Rent is due soon or due today.
* Yellow: A payment is processing or awaiting confirmation.
* Gray: Vacant bed.
* Blue: Reserved bed.

Always display text labels and accessible status indicators alongside colors.

An unpaid charge that has not passed its due date must not automatically be labeled overdue.

Clicking a bed

When the owner clicks an occupied bed, open a detailed panel showing:

* Property name.
* Building.
* Floor.
* Room number.
* Bed number.
* Resident name.
* Resident contact details, subject to permissions.
* Monthly rent.
* Current billing month.
* Due date.
* Confirmed payment status.
* Outstanding amount.
* Previous successful payments.
* Deposit details where relevant.

Provide appropriate actions:

* View resident details.
* View rent history.
* Send payment reminder.
* Record authorized offline payment.
* Open a receipt.
* Initiate an authorized correction or refund workflow where applicable.

Keep an audit trail of financial changes.

Unpaid rent report

Create a separate report for residents with outstanding rent.

It must support filtering by:

* Property.
* Building.
* Room.
* Bed.
* Resident.
* Billing month.
* Payment status.

Show the actual outstanding amount and the associated rent charge.

Provide authorized export and reminder actions.

Allow the owner to identify all unpaid and overdue residents across multiple properties without opening every room individually.

⸻

12. Monthly Rent and Payment Management

Rent must be represented using separate billing records for each resident and billing period.

Never store the entire financial history in a single mutable paid field.

A rent charge must contain appropriate information such as:

* Resident ID.
* Tenancy ID.
* Property ID.
* Building, room, and bed references where needed.
* Billing period.
* Rent amount.
* Due date.
* Payments allocated.
* Outstanding balance.
* Charge status.
* Creation and adjustment history.

Use an appropriate date and currency representation. For India, display monetary values in INR.

Preserve historical rent amounts when the owner changes future rent.

For example, a resident’s rent might be ₹8,000 in one month and ₹8,500 in a later month. The previous month’s charge must remain unchanged.

Support monthly billing, outstanding balances, partial payments where allowed, and historical records.

Design billing generation to be idempotent so repeated scheduled executions cannot create duplicate rent charges for the same tenancy and billing period.

⸻

13. Online Rent Payments

Integrate a suitable payment gateway after the core booking and billing workflows are implemented.

The payment workflow should be:

1. Resident selects an outstanding rent charge.
2. Backend validates the resident’s authorization and charge.
3. Backend creates a payment order.
4. Payment gateway processes the transaction.
5. Gateway sends a signed confirmation webhook.
6. Backend verifies the signature and transaction details.
7. Backend records the payment.
8. Backend updates the relevant payment allocation and rent balance.
9. The resident receives a receipt.
10. The owner’s dashboard reflects the confirmed payment.

Never trust a frontend success message as proof of payment.

Protect against duplicate payment callbacks, repeated requests, invalid amounts, delayed notifications, and failed payments.

Keep separate payment attempts, confirmed transactions, allocations, and refunds where needed.

Never store raw payment-card numbers or CVVs.

Offline cash, bank, or UPI payments may be recorded by authorized owners or staff, but those records must be clearly distinguished from gateway-verified transactions.

⸻

14. Owner Subscription — ₹1,000 Per Month

PGConnect must charge owners a recurring subscription to use the owner management platform and publish eligible properties.

The initial proposed subscription price is ₹1,000 per month.

The owner subscription is separate from accommodation rent.

The resident pays the accommodation rent to the PG owner. The owner pays the platform subscription to PGConnect.

Keep these financial flows separate in the database, API design, reporting, and payment records.

Subscription management

Support fields such as:

* Owner ID.
* Plan ID.
* Subscription status.
* Billing period.
* Subscription start date.
* Current period start.
* Current period end.
* Renewal date.
* Subscription invoice references.
* Payment transaction references.
* Cancellation information.
* Applicable grace-period rules.

Support suitable lifecycle statuses, including:

* PENDING.
* ACTIVE.
* PAST_DUE.
* EXPIRED.
* CANCELLED.
* SUSPENDED.

Only a verified subscription payment may activate or renew the subscription.

The backend must determine subscription eligibility. A frontend success page must never activate a subscription on its own.

Subscription expiry

If an owner’s subscription expires:

1. Prevent their ineligible properties from appearing in public search results.
2. Prevent new public bookings for those properties where required.
3. Notify the owner.
4. Allow the owner to renew the subscription.
5. Restore eligible public listings after successful renewal, subject to property approval and publication status.

Do not delete properties, residents, rent charges, payment records, or historical financial information when a subscription expires.

Provide a clear renewal workflow.

The proposed initial pricing rule is ₹1,000 per owner account per month. Keep plan configuration separate so pricing and property limits can change without rewriting the entire subscription system.

⸻

15. Property Facilities and Food Management

Owners must be able to configure actual property amenities.

Examples include:

* Wi-Fi.
* Air conditioning.
* Laundry.
* Parking.
* Hot water.
* Housekeeping.
* Study areas.
* Security arrangements.
* Other facilities.

Distinguish between property-wide amenities and room-specific amenities.

For food management, support:

* Breakfast.
* Lunch.
* Evening snacks.
* Dinner.
* Vegetarian and non-vegetarian options.
* Weekly menus.
* Meal timings.
* Temporary menu changes.

Residents should see only the food information published by their property owner.

Do not display generic fabricated menus as if they were real property information.

⸻

16. Complaints, Maintenance, and Announcements

Residents must be able to report issues such as:

* Water supply.
* Electricity.
* Wi-Fi.
* Cleaning.
* Repairs.
* Food quality.
* Other accommodation-related problems.

Store complaints in MongoDB and associate them with the appropriate resident and property.

Support complaint statuses such as:

* OPEN.
* ASSIGNED.
* IN_PROGRESS.
* RESOLVED.
* CLOSED.

Owners or authorized staff should be able to update status and provide responses.

Provide announcements for specific properties, buildings, or residents.

Support notifications for rent reminders, booking updates, maintenance updates, and subscription renewal.

⸻

17. Ratings and Reviews

Allow residents to review properties according to a defined policy.

Where appropriate, require evidence of a verified stay before accepting a review.

Store actual reviews and calculate ratings from eligible records.

Prevent owners from arbitrarily creating or editing resident reviews.

Support moderation and abuse reporting.

If no reviews exist, show “No reviews yet” rather than inventing ratings.

⸻

18. Administrator Dashboard

Build a separate platform administrator interface.

Administrators must be able to:

* Review property registration submissions.
* Verify properties.
* Approve or reject public listings.
* Review reported listings.
* Manage owner account status.
* Inspect subscription status.
* View platform-level financial reports.
* Monitor suspicious activity.
* Investigate complaints and disputes.
* Manage support requests.
* Access relevant audit logs.
* Configure platform settings.

Platform administrators must not be ordinary owners with unrestricted privileges.

Require authorization for every administrative endpoint and audit sensitive actions.

⸻

19. MongoDB Data Model

Design MongoDB collections carefully before implementing all business workflows.

The data model should include at least the following logical entities:

1. Users.
2. Owner profiles.
3. Resident profiles.
4. Staff memberships and permissions.
5. PG properties.
6. Buildings.
7. Rooms.
8. Beds.
9. Bookings.
10. Tenancies.
11. Rent charges.
12. Payment attempts.
13. Confirmed payments.
14. Payment allocations.
15. Refunds.
16. Deposits.
17. Owner subscriptions.
18. Subscription invoices.
19. Subscription payments.
20. Property photos.
21. Facilities.
22. Food menus.
23. Complaints.
24. Announcements.
25. Notifications.
26. Reviews.
27. Audit logs.
28. College and location reference records, where needed.

Choose embedded documents or references according to access patterns, consistency needs, document size, and growth.

Do not put unlimited arrays of residents, rooms, payments, or images inside a single property document.

Important entities should have stable identifiers.

Define indexes and uniqueness rules to prevent duplicates and support common lookups.

Examples of important uniqueness rules include:

* Unique normalized account email when present.
* Unique normalized phone number when present.
* Unique bed identifiers within the correct scope.
* No duplicate rent charge for the same tenancy and billing period.
* Idempotency keys for payment operations.

Use MongoDB transactions when a workflow needs multiple document updates to succeed or fail together, provided the deployment supports them.

Where transactions are unsuitable, define recovery and reconciliation mechanisms.

Maintain explicit relationships between owners, properties, buildings, rooms, beds, bookings, tenancies, rent charges, and payments.

Document the complete data model and API contracts.

Do not assume MongoDB has created every collection merely because Java model classes exist. Collections may appear only when records are inserted or explicitly created.

⸻

20. Nearby Geographical Search

Implement actual geographical search using MongoDB geospatial capabilities.

Property location records must contain validated coordinates.

Use an appropriate GeoJSON Point representation and create a 2dsphere index.

Support searches around:

* A selected college.
* A locality or address resolved through a geocoding provider.
* The user’s current location, when permission is granted.

Do not treat an arbitrary text string as geographical coordinates.

Use a legitimate geocoding or place-search provider to resolve real college and locality locations.

Validate provider results and handle ambiguous names.

The search must query MongoDB using the chosen location and radius and return only eligible, published properties.

Calculate distance using the database’s geographical search results or another appropriate geographical method.

Do not pretend a radius filter works when it merely changes text in the frontend.

The API must enforce visibility conditions, radius, and applicable accommodation filters.

⸻

21. UI/UX and Design Requirements

Create a polished, professional accommodation platform, not a plain HTML document with browser-default styling.

The visual design should feel modern, trustworthy, clean, and suitable for a commercial property management service.

Use:

* A consistent color palette.
* Professional typography.
* Appropriate spacing.
* Well-designed property cards.
* Responsive navigation.
* Accessible form controls.
* Clear loading and error states.
* Empty states.
* Confirmation dialogs for destructive actions.
* Proper field validation.
* Responsive tables and dashboards.
* Consistent icons.
* Clear success and failure notifications.

The public homepage should emphasize property discovery and real property photographs.

The resident dashboard should emphasize accommodation, rent, booking, and useful resident services.

The owner dashboard should emphasize operational management, room and bed availability, outstanding rent, and monthly reporting.

The room-and-bed payment interface should clearly communicate payment status using colors, readable text, and accessible visual indicators.

Ensure CSS is correctly imported and loaded throughout the application.

Do not deliver an unstyled page that uses default browser typography and button styles.

Do not replace a polished working interface with a stripped-down version merely because the backend is being changed.

Separate reusable UI components from business logic.

Keep the design responsive on desktop, tablet, and mobile browsers.

⸻

22. Backend APIs and Frontend Integration

Define REST APIs for all important workflows.

Expected API areas include:

* Authentication.
* Owner accounts.
* Resident accounts.
* Public property search.
* Property management.
* Photo uploads.
* Buildings.
* Rooms.
* Beds.
* Bookings.
* Tenancies.
* Rent billing.
* Payments.
* Refunds.
* Subscriptions.
* Facilities.
* Food menus.
* Complaints.
* Announcements.
* Notifications.
* Reviews.
* Administration.
* Reports.

Use suitable HTTP methods and status codes.

Validate every request and return consistent error responses.

The frontend must consume actual API responses.

Do not hardcode property data, resident information, payment statuses, owner metrics, room availability, or subscription statuses into production-facing components.

Use server-side pagination for large result sets.

Keep database IDs and relationships consistent across the application.

Where a feature has not yet been implemented, show an honest unavailable or coming-soon state. Do not simulate a successful real-world operation.

⸻

23. Security Requirements

The application will store personal information, accommodation details, and financial records.

Implement security throughout the project.

Requirements include:

* Strong password hashing.
* Secure authentication.
* Appropriate session or token management.
* Authorization on every protected endpoint.
* Resource-level access checks.
* Input validation.
* Rate limiting on sensitive endpoints.
* Protection against common web vulnerabilities.
* Secure file upload validation.
* File-size and upload-count limits.
* Safe secret management.
* HTTPS in production.
* Secure error handling.
* Audit logging for sensitive operations.
* Appropriate data retention and deletion policies.
* Database backups.
* Least-privilege access.

Do not commit real credentials, payment secrets, or database passwords to source control.

Provide environment-variable examples without embedding actual secrets.

For authentication, use established Spring Security mechanisms and choose a session or token strategy appropriate for the deployed frontend and backend architecture.

Do not roll your own cryptography.

Use short-lived credentials and safe session handling where applicable.

⸻

24. Scalability and Performance

Design the application for increasing numbers of owners, properties, rooms, residents, and concurrent requests.

Focus on correctness before adding infrastructure unnecessarily.

Implement:

* Appropriate MongoDB indexes.
* Pagination.
* Efficient queries.
* Database connection management.
* Safe concurrent bed booking.
* Idempotent billing and payment operations.
* Efficient image storage.
* Background processing for reminders and reports.
* Structured logs.
* Health checks.
* Monitoring.
* Backups.
* Load testing.

Do not claim that the application supports a particular number of concurrent users unless testing supports that claim.

Protect room availability and payment processing against concurrency bugs.

A database record is the authority for whether a bed is available; a stale frontend view must never be enough to reserve a bed.

Avoid introducing microservices, Redis, Kafka, or Kubernetes unless measured requirements justify them.

⸻

25. Development Process

Build the application incrementally and ensure every milestone works.

Do not attempt to implement every feature in one untested code dump.

Phase 1: Project foundation

* Review the existing repository.
* Preserve useful working code.
* Establish the frontend and backend structure.
* Confirm that React styles render correctly.
* Configure MongoDB.
* Document environment variables.
* Create the initial data model.
* Implement health checks.
* Add basic automated tests.

Phase 2: Authentication

* Owner registration.
* Resident registration.
* Login and logout.
* Administrator provisioning.
* Password reset and verification.
* Role-based authorization.
* Protected routes and APIs.

Phase 3: Real owner property management

* Owner creates property.
* Owner enters actual address and coordinates.
* Owner uploads genuine photographs.
* Owner adds buildings.
* Owner adds rooms.
* Owner adds beds.
* Data persists in MongoDB.
* Owner can view and edit their own records.

Phase 4: Public PG discovery

* Search by college.
* Search by locality.
* Current-location search.
* Actual geographical filtering.
* Real property cards.
* Real property photos.
* Real availability.
* Eligibility and publication rules.
* Empty and error states.

Phase 5: Booking and resident management

* Booking requests.
* Bed reservation.
* Duplicate-booking prevention.
* Owner approval.
* Resident allocation.
* Tenancy management.
* Move-in and move-out workflows.

Phase 6: Rent and owner dashboard

* Monthly rent charges.
* Payment history.
* Outstanding balances.
* Green and red bed indicators.
* Unpaid-rent report.
* Offline payment recording.
* Receipts and reminders.

Phase 7: Subscription and gateway integration

* ₹1,000 monthly owner subscription.
* Subscription billing and invoices.
* Verified payment processing.
* Subscription renewal.
* Subscription expiry.
* Listing eligibility rules.
* Payment reconciliation.

Phase 8: Additional management features

* Facilities.
* Food menus.
* Complaints.
* Announcements.
* Reviews.
* Notifications.
* Deposits.
* Reports.
* Audit logs.

Phase 9: Production readiness

* Automated testing.
* Security review.
* Concurrent booking tests.
* Payment workflow tests.
* Load testing.
* Database backups.
* Deployment configuration.
* Monitoring and operational documentation.

⸻

26. Non-Negotiable Rules

The following requirements must be followed throughout development:

1. Never populate public listings with fake PGs.
2. Never use stock photographs to represent actual properties.
3. Never fabricate resident information.
4. Never fabricate payment records or reviews.
5. Never hardcode successful payment statuses.
6. Never display unauthorized private information.
7. Never trust frontend validation as the sole security control.
8. Never publish an unapproved or ineligible property.
9. Never allow two residents to successfully reserve the same bed.
10. Never erase historical rent and payment records when a subscription expires.
11. Never treat a browser payment-success message as verified gateway confirmation.
12. Never claim that a feature works when it is only a visual mockup.
13. Never overwrite the existing MongoDB database or delete its data without explicit authorization.
14. Never silently ignore failed database operations.
15. Never deliver an unstyled interface.
16. Never replace actual database records with fabricated fallback data.
17. Clearly distinguish implemented functionality from incomplete functionality.
18. Test important workflows using real persisted records before declaring them complete.

⸻

27. Expected Deliverables

Provide:

1. A complete, organized React frontend.
2. A Java Spring Boot backend.
3. A documented MongoDB data model.
4. Required indexes and initialization procedures.
5. REST API documentation.
6. Authentication and authorization.
7. Environment configuration examples.
8. Docker Compose configuration for local development.
9. Automated tests.
10. Instructions for running the project on macOS.
11. Database and backup documentation.
12. Deployment instructions.
13. A list of completed features.
14. A list of incomplete features and limitations.
15. A record of tests actually executed and their results.

Do not report a successful build or passing tests unless they were actually executed.

If a dependency cannot be installed or a test cannot run, state the limitation clearly and provide the next steps.

Final objective

Deliver a secure, professional PG management and discovery platform in which:

* Residents can discover real, registered PGs near their colleges or preferred localities before logging in.
* Only eligible properties created by actual PG owners appear in public search.
* Owners pay the configured monthly platform subscription.
* Owners manage multiple properties, buildings, rooms, and individual beds.
* Residents book specific available beds and manage their accommodation.
* Owners can identify the exact building, room, and bed associated with each resident.
* Monthly rent status appears clearly as green for paid and red for overdue.
* Every payment, booking, subscription, and property record is backed by MongoDB.
* Real PG photographs come from authorized owner uploads.
* Data remains consistent under concurrent requests.
* The entire application is maintainable, testable, secure, and ready for progressive production deployment.

Start by examining the existing codebase, identifying defects and incomplete features, and establishing a working baseline. Then implement the application phase by phase, preserving real data and validating every important workflow.