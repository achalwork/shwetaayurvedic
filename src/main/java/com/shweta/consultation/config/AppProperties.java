package com.shweta.consultation.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Consultation consultation = new Consultation();
    private Google google = new Google();
    private Whatsapp whatsapp = new Whatsapp();
    private Mail mail = new Mail();
    private Reminder reminder = new Reminder();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
    }

    @Data
    public static class Consultation {
        private String doctorName = "Dr. Shweta Sanjay Shete";
        private int durationMinutes = 30;
        /** Comma separated, IST, 24h format */
        private String slots = "10:00,10:30,11:00,11:30,12:00,12:30,16:00,16:30,17:00,17:30,18:00";
        private int maxDaysAhead = 60;
    }

    @Data
    public static class Google {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String refreshToken = "";
        private String calendarId = "primary";
        /**
         * When true Google emails the calendar invite (which contains the Meet link) to the patient
         * IMMEDIATELY at booking time. Keep false so the link is only sent ~10 minutes before.
         */
        private boolean sendInvites = false;
        /** Doctor cha email: tyala pratyek event cha invite jato, to Meet madhe direct join karu shakto. */
        private String doctorEmail = "";
    }

    @Data
    public static class Whatsapp {
        private boolean enabled = false;
        private String phoneId = "";
        private String token = "";
        private String apiVersion = "v20.0";
        private String languageCode = "en";
        /** Params: {{1}} name, {{2}} date, {{3}} time */
        private String confirmationTemplate = "consultation_confirmation";
        /** Params: {{1}} name, {{2}} time, {{3}} meet link */
        private String reminderTemplate = "consultation_reminder";
    }

    @Data
    public static class Mail {
        private boolean enabled = false;
        private String from = "";
        private String fromName = "Dr. Shweta Shete";
        /** Optional: pratyek email chi BCC copy doctor la jate. Rikame thevla tar BCC jat nahi. */
        private String doctorBcc = "";
        /** Reply kelyavar jatat tya address la (doctor cha email). */
        private String replyTo = "";
        /** Contact form messages ithe jatat. Rikame asel tar app.mail.from vaparla jato. */
        private String contactTo = "";
    }

    @Data
    public static class Reminder {
        private int minutesBefore = 10;
    }
}
