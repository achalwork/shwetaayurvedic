# Consultation Backend (Spring Boot 3 / Java 17)

Dr. Shweta Shete online consultation booking sathi backend.

**Kay kartay:**
- Slots dakhavto (`GET /api/slots`), double booking rokhto
- Booking save karto (`POST /api/bookings`)
- Google Calendar API ne **Google Meet link** banavto
- Confirmation **Email + WhatsApp** pathavto
- Consultation chya **10 min aadhi** Meet link **WhatsApp + Email** var pathavto (scheduler)

---

## 1. Local run (credentials shivay)

Java 17 + Maven lagte.

```bash
mvn spring-boot:run
```

Default profile `dev` ahe: H2 file DB (`./data/`), Google/WhatsApp/Email **disabled**.
Booking chalte, fakt Meet link/messages jat nahit (logs madhe "disabled" disel).

Test:
```bash
curl "http://localhost:8080/api/slots?date=2026-10-12"

curl -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" -d '{
  "date":"2026-10-12","time":"10:00","name":"Test User","mobile":"9876543210",
  "email":"test@example.com","language":"English","reason":"Skin problem",
  "notes":"","consent":true}'
```

## 2. API

### GET `/api/slots?date=YYYY-MM-DD`
```json
[
  {"time":"10:00","label":"10:00 AM","available":true},
  {"time":"10:30","label":"10:30 AM","available":false}
]
```
Sunday, bhutkal ani already book zalele slots `available:false` yetat.

### POST `/api/bookings`
Request body: `date` (YYYY-MM-DD), `time` (HH:mm, IST), `name`, `mobile` (10 digit), `email`, `language`, `reason`, `notes` (optional), `consent` (true).

Response `201`: `reference, date, time, name, mobile, email, language, reason, meetLink, status`.
Errors: `400` (validation, `fieldErrors` map), `409` (slot already booked).

### POST `/api/contact`
Body: `name`, `email`, `message` (10+ chars). Contact page chya form cha message `CONTACT_TO_EMAIL` var email hoto (reply-to = patient cha email). Response `202`.

### Frontend madhe use (React) - zip madhe already integrated ahe (`src/data/api.ts`)
```js
const API = import.meta.env.VITE_API_URL; // e.g. https://your-backend.onrender.com

export const getSlots = (date) =>
  fetch(`${API}/api/slots?date=${date}`).then(r => r.json());

export const createBooking = async (payload) => {
  const res = await fetch(`${API}/api/bookings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await res.json();
  if (!res.ok) throw data; // data.message, data.fieldErrors
  return data;
};
```
`CORS_ORIGINS` madhe frontend cha URL add kara.

---

## 3. Google Meet setup

1. https://console.cloud.google.com → project banva → **Google Calendar API** enable kara.
2. **OAuth consent screen** configure kara. **Important:** Publishing status **"In production"** kara. "Testing" madhe refresh token **7 diwsat expire** hoto ani Meet link banne band hoil.
3. **Credentials → Create OAuth client ID → Web application**. Redirect URI: `https://developers.google.com/oauthplayground`
4. https://developers.google.com/oauthplayground ughda:
   - Settings (gear) → **Use your own OAuth credentials** → Client ID/Secret taka
   - Scope: `https://www.googleapis.com/auth/calendar.events` → Authorize (**doctor cha Google account vapra**)
   - **Exchange authorization code for tokens** → **refresh_token** copy kara
5. Env variables set kara: `GOOGLE_ENABLED=true`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REFRESH_TOKEN`

Patient cha email attendee mhanun add hoto, mhanje Google swatah calendar invite pathavto (`GOOGLE_SEND_INVITES=false` karun band karta yeil).

## 4. Email setup

**Gmail (testing):** 2-Step Verification on → Google Account → Security → **App passwords** → 16 char password.
```
MAIL_ENABLED=true
MAIL_USERNAME=youremail@gmail.com
MAIL_PASSWORD=<app password>
```
**Production:** Brevo / Amazon SES / Resend SMTP vapra (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`). Aaplya domain var **SPF, DKIM, DMARC** set kara, nahi tar spam madhe jail.

`MAIL_DOCTOR_BCC=doctor@email.com` set kelat tar pratyek confirmation ani reminder email chi BCC copy doctor la pan jate (patient la BCC address dist nahi).

Templates: `src/main/resources/templates/*.html`

## 5. WhatsApp Cloud API setup

1. https://developers.facebook.com → App banva (Business type) → **WhatsApp** product add kara.
2. Business number add kara (to number normal WhatsApp app madhe vaparlela nasava).
3. **Phone number ID** copy kara. **System User** banvun **permanent access token** generate kara (`whatsapp_business_messaging`, `whatsapp_business_management`).
4. **Message Templates** (category: **Utility**) banva ani approval chi vaat bagha:

**`consultation_confirmation`** (3 variables)
```
Hello {{1}}, your online consultation with Dr. Shweta Shete is confirmed for {{2}} at {{3}}. We will send you the Google Meet link about 10 minutes before it starts.
```

**`consultation_reminder`** (3 variables)
```
Hello {{1}}, your online consultation starts at {{2}}. Join using this link: {{3}}
```

5. Template cha language jo select kelat toch `WHATSAPP_LANG` madhe taka (`en`, `mr`, `en_US`...).
6. Env: `WHATSAPP_ENABLED=true`, `WHATSAPP_PHONE_ID`, `WHATSAPP_TOKEN`.

Notes:
- Mobile number automatically `91` + 10 digits format madhe jato.
- Frontend form madhe consent text madhe **WhatsApp/email var messages** cha mention add kara (opt-in).
- Template parameters madhe newline/tab nasave, ani 4+ spaces nasave (Meta reject karto).

---

## 6. Production deploy

Cloudflare Pages fakt frontend sathi ahe. Backend Render / Railway / Fly.io / AWS / VPS var chalva.

```bash
docker build -t consultation-backend .
docker run -p 8080:8080 --env-file .env consultation-backend
```

Production env: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (MySQL), plus section 3-5 chya vars. `.env.example` bagha.

**Free tier var sleep hotoy ka?** Render free tier idle zalyavar sleep hoto, mhanje scheduler (reminder) chalnar nahi. Reminders sathi always-on instance lagto.

### MySQL setup
```sql
CREATE DATABASE consultation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'consult'@'%' IDENTIFIED BY 'strong-password';
GRANT ALL PRIVILEGES ON consultation.* TO 'consult'@'%';
FLUSH PRIVILEGES;
```
`DB_URL=jdbc:mysql://HOST:3306/consultation?useSSL=true&serverTimezone=UTC&characterEncoding=UTF-8`
Tables (`appointments`) pahilya run la Hibernate swatah banvto. Marathi text sathi database `utf8mb4` ch asava.

## 7. Project structure
```
src/main/java/com/shweta/consultation/
  ConsultationApplication.java
  config/       AppProperties, CorsConfig
  controller/   BookingController
  dto/          BookingRequest, BookingResponse, SlotDto, ErrorResponse
  entity/       Appointment, AppointmentStatus
  exception/    GlobalExceptionHandler, BadRequest..., SlotUnavailable...
  repository/   AppointmentRepository
  service/      BookingService, MeetService, EmailService, WhatsAppService, ReminderJob
  util/         TimeUtil
```

## 8. Design notes
- Time **UTC (Instant)** madhe store, IST madhe dakhvto.
- Double booking: `slotKey` unique constraint + exists check.
- Reminder duplicate rokhnyasathi `UPDATE ... WHERE reminder_sent=false` atomic claim (multiple instances safe).
- Meet/Email/WhatsApp fail zala tari booking fail hot nahi; error logs madhe disto.
- `ddl-auto=update` ahe. Production madhe pudhe Flyway/Liquibase vapra.
- Slot timings: `CONSULT_SLOTS` env var (IST, 24h, comma separated).

## 9. Doctor cha account vaparta yet nasel tar (tumcha swatacha Gmail)
- Email: `MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM` madhe **tumcha** Gmail + app password. `MAIL_REPLY_TO` ani `MAIL_DOCTOR_BCC` madhe doctor cha email.
- Meet: Google Cloud + OAuth Playground **tumchya** Google account ne kara. `GOOGLE_DOCTOR_EMAIL=khushishete9075@gmail.com` set kara, mhanje doctor la pratyek booking cha calendar invite yeto ani to Meet madhe direct join karto (host nasla tari).
