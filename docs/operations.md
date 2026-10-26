# Operations: local data, backups, and deployment

## Local database behavior

`docker compose up --build` starts MongoDB 8 as a single-node replica set and stores files in the named `pgconnect_mongo` volume. The `mongo-init` one-shot service initializes the replica set and waits for a writable primary. The API depends on that initialization because multi-document transactions require a replica set. Stopping containers preserves the volume. Do not run `docker compose down -v` unless you deliberately want to remove the local database; it deletes the named volume.

Spring Data creates declared indexes automatically. This is convenient for local use. Production index creation and changes should be planned as a database migration and monitored. This repository has no destructive schema reset or seed process.

## Local backup

The Compose MongoDB is unauthenticated and bound to loopback for local development. For a local logical backup, make a dated archive outside the project tree:

```sh
mkdir -p backups
docker compose exec -T mongo mongodump --db pgconnect --archive > "backups/pgconnect-$(date +%Y%m%d-%H%M%S).archive"
```

Keep backups outside version control, restrict file permissions, and store production backups encrypted in a separate account/location. Never put a production URI or backup credentials in a command committed to the repository.

## Restore rehearsal

Rehearse on a separate disposable MongoDB replica set, never against the source database. Restore into a fresh database name (for example `pgconnect_restore_test`) with namespace mapping. Avoid `--drop` for production restores. Then start the application against the restored test database and verify representative read paths before considering a recovery complete.

Example command when connected to a disposable test MongoDB:

```sh
mongorestore --uri="$RESTORE_TEST_MONGODB_URI" --archive="backups/pgconnect-YYYYMMDD-HHMMSS.archive" --nsFrom='pgconnect.*' --nsTo='pgconnect_restore_test.*'
```

`RESTORE_TEST_MONGODB_URI` must point to an isolated test database/replica set. This command is documentation only; it has not been run against user data.

## Production deployment checklist

1. Use an authenticated MongoDB replica set with TLS, least-privilege application credentials, network restrictions, encrypted backups, and a documented retention policy.
2. Deploy the API and frontend behind HTTPS. Set the exact `WEB_ORIGIN`, `EMAIL_WEB_ORIGIN`, and `SESSION_COOKIE_SECURE=true`; do not use the local Compose database for production.
3. Use a secret manager for database, email, object-storage, and payment credentials. Do not commit `.env` files or real credentials.
4. Configure a private S3-compatible bucket and narrowly scoped service credentials. Restrict bucket access; the application provides short-lived signed reads for property photos.
5. Configure Resend with a verified sender for email verification and password recovery.
6. Configure Razorpay test/live keys. Set subscription webhook URL to `https://<api-host>/api/webhooks/razorpay` and rent payment webhook URL to `https://<api-host>/api/webhooks/razorpay/rent`. Subscribe to subscription lifecycle events and captured rent events, use the matching webhook secret, and confirm delivery in the provider dashboard.
7. Configure Google geocoding quotas/restrictions for `GOOGLE_MAPS_API_KEY` if typed place/radius search is required.
8. Provision the first administrator through `PGCONNECT_ADMIN_EMAIL` and a unique password of at least 16 bytes. On subsequent boots, the bootstrap does not overwrite an existing account; rotate credentials through controlled operations.
9. Set resource limits and health checks, forward structured logs to a protected sink, monitor API/database/provider failures, and alert on backup failure and webhook errors.
10. Before launch, complete provider sandbox flows, security review, transaction/concurrency tests, browser accessibility checks, load measurements, backup restore rehearsal, and incident runbooks.

The Compose stack is for local development, not production. The API has process-local IP limits on sensitive routes; multi-instance deployments need a shared limiter. Full monitoring, production index migration tooling, and operational restore automation still need implementation.
