# Dr. Shweta Shete website - frontend + backend integration

## Status
| Item | Status |
|---|---|
| Frontend <-> backend connection | **Done in this package** (`frontend/src/data/api.ts`). The original frontend only used `mockApi.ts`. |
| Slots, booking, 409 double-booking, contact form | Connected + tested end-to-end (against a stand-in server that mirrors the backend's API) |
| Google Meet link ~10 min before | Backend logic fixed (see `backend/CHANGES.md`); **not compiled/run here** |
| Real Google / WhatsApp / email sending | Needs your credentials (backend README sections 3-5) |

## Run locally
1. Backend: `cd backend && mvn spring-boot:run` (dev profile: H2 DB, Google/WhatsApp/email disabled).
2. Frontend: `cd frontend && npm install`, create `.env.local` with `VITE_API_URL=http://localhost:8080`, `npm run dev`.
3. Production: set `VITE_API_URL=https://<backend>` when building the frontend, and `CORS_ORIGINS=https://<frontend>` on the backend.

## Before going live - checklist
- [ ] `mvn clean package` succeeds
- [ ] Google OAuth consent screen is "In production" (refresh token otherwise dies after 7 days); `GOOGLE_ENABLED=true`
- [ ] `GOOGLE_SEND_INVITES=false` (otherwise Google emails the Meet link at booking time)
- [ ] WhatsApp templates re-submitted in English and approved by Meta
- [ ] Email SPF/DKIM/DMARC set (or messages go to spam)
- [ ] Backend runs on an always-on instance (the reminder scheduler stops when a free host sleeps)
- [ ] Doctor reviews the live Privacy Policy text (retention period is "to be confirmed")
- [ ] Decide on spam protection for `/api/bookings` and `/api/contact` (none exists yet)

## Known limitations
- Backend has one slot length (30 min) and one schedule for all services, so every service shows 30 min in live mode.
- No cancel/reschedule, no admin screen (the doctor sees bookings in Google Calendar).
- If a reminder is claimed but WhatsApp/email then fails, it is logged but not retried.
- Patients are not Google Calendar guests, so the doctor must admit them from the Meet waiting screen.
