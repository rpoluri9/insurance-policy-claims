# Insurance Claims Frontend (React + Vite)

A small React UI for the Insurance Policy and Claims backend. It is a demo
interface for the same local demo setup as the API: no production identity
provider, no real credentials.

## Run it

```bash
cd frontend
npm install
npm run dev        # serves on http://localhost:5173
```

The backend must be running (see the main README: `docker compose up
--build`, or `mvn spring-boot:run` with a local MySQL). The API base URL
defaults to `http://localhost:8080`; override it with `VITE_API_URL`:

```bash
VITE_API_URL=http://localhost:8080 npm run dev
```

Sign in with the backend's demo user (see the main README). Credentials are
kept only in the browser tab's `sessionStorage` and are sent as an HTTP
Basic `Authorization` header on every API call.

## Pages

- **Customers** — list customers, create a customer.
- **Policies** — list policies, create a policy (premium is calculated by
  the backend), get a no-commit premium quote, change policy status.
- **Claims** — list claims, register a claim, move it through the claim
  lifecycle, settle an APPROVED claim.

## Build for production

```bash
npm run build      # output in frontend/dist/
npm run preview    # serves the build on http://localhost:4173
```
