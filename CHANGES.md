# Changes in this version (Google Meet link ~10 minutes before the consultation)

1. **Confirmation email no longer contains the Meet link.** Before, the "Join Google Meet" button was in the
   confirmation email, so the patient got the link immediately and the "10 minutes before" promise was false.
2. **Google no longer emails the link at booking time.** `GOOGLE_SEND_INVITES` now defaults to `false`
   (with `true`, Google sends the patient a calendar invite containing the Meet link right away). The doctor is
   still invited (`GOOGLE_DOCTOR_EMAIL`).
3. **`POST /api/bookings` no longer returns `meetLink`.** The link is only delivered by the reminder.
4. **ReminderJob is more reliable.** Runs every 30 s (link arrives 10:00 - 9:30 min before the start). If the
   Meet link is missing (Google was unavailable at booking time) it is created at reminder time and the
   reminder is retried every run until it exists, instead of sending a reminder without a link.
   A late-created link is saved with a targeted UPDATE so it can never reset `reminderSent`.
5. **Optional `service` field** on `POST /api/bookings` (stored on the appointment, shown in the Google Calendar
   event description). New nullable column; `ddl-auto=update` adds it automatically.
6. **Email templates and WhatsApp template texts are now English** (they were Roman-script Marathi).
   Re-submit the two WhatsApp templates (README section 5) for Meta approval with `WHATSAPP_LANG=en`.
7. Removed `.idea/` and the H2 lock file from the zip (IDE/local files).

NOT compiled or run by the author of these changes (no Maven Central access in that environment); the changed
files were only syntax-checked. Run `mvn clean package` first.

## How to test the 10-minute reminder quickly
Pick a start time ~12 minutes from now (IST) and run the backend with, e.g.:
`CONSULT_SLOTS=14:42` `REMINDER_MINUTES=10` (+ your Google/Mail/WhatsApp variables). Book that slot on the site.
About 2 minutes later the log shows `Sending reminder for appointment SHETE-...` and the email/WhatsApp arrives
with the Meet link. No message with the link should arrive earlier than that.
