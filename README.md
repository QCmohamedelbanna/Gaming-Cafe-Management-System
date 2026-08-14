# Gaming-Cafe-Management-System

## Local development

Start the backend from `backend` with `mvn spring-boot:run`, then start the frontend from `frontend` with `npm run dev`.

Phase 6 seeds an administrator on a new database:

- Username: `admin`
- Password: `admin123`

Override `app.default-admin-username` and `app.default-admin-password` in the backend configuration before deploying. Administrators can create manager and cashier accounts from **Users**. Cashiers must open a shift before taking payments; closing a shift records expected cash, actual cash, and the difference.
