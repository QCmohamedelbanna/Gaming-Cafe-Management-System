# Gaming-Cafe-Management-System

## Local development

The backend runs against MySQL. Start it with `docker compose up mysql`, then run the backend from `backend` with `mvn spring-boot:run` (uses the `dev` profile by default — see `application-dev.properties`), and the frontend from `frontend` with `npm run dev`. Copy `frontend/.env.example` to `frontend/.env` if you need to point the UI at a non-default API URL.

Phase 6 seeds an administrator on a new database:

- Username: `admin`
- Password: `admin123`

In production (the `prod` profile), the admin username/password, database connection, and CORS allowed origins are all required environment variables (`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`) with no built-in default — see `application-prod.properties`. Administrators can create manager and cashier accounts from **Users**. Cashiers must open a shift before taking payments; closing a shift records expected cash, actual cash, and the difference.

## Running the full stack with Docker

`docker compose up --build` builds and starts MySQL, the backend, and the frontend together (backend on `:8080`, frontend on `:5173`, MySQL on `:3306`). See `docker-compose.yml` for the default (dev-only) credentials baked in there — override them for anything beyond local use.

## Database backup and restore

`scripts/db-backup.sh` (`.ps1` on Windows) dumps the database to a timestamped, gzip-compressed file under `backups/`; `scripts/db-restore.sh` / `.ps1` restores from one. Both read `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` from the environment, defaulting to the `docker-compose.yml` credentials.

## Database migrations

Schema changes go through Flyway (`backend/src/main/resources/db/migration`) — `spring.jpa.hibernate.ddl-auto` is `validate`, so the app will refuse to start against a schema that doesn't match its entities. Add a new `V<n>__description.sql` file rather than editing an already-applied one.

## Tests and CI

Backend integration tests use Testcontainers to run against a real MySQL container (Docker required — see `backend/src/test/java/com/cafe/ps/AbstractMySQLIntegrationTest.java`); run them with `mvn test`. Frontend tests run with `npm test` (Vitest). Both run in CI on every push/PR — see `.github/workflows/ci.yml`.
