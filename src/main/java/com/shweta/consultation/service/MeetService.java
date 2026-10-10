package com.shweta.consultation.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.shweta.consultation.config.AppProperties;
import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.util.TimeUtil;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Doctor chya Google Calendar madhe event banavto, tyatun Google Meet link auto-generate hoto.
 */
@Service
@Slf4j
public class MeetService {

    public record MeetResult(String link, String eventId) {}

    private final AppProperties props;
    private Calendar calendar;

    public MeetService(AppProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        AppProperties.Google g = props.getGoogle();
        if (!g.isEnabled()) {
            log.warn("Google Meet integration is DISABLED (app.google.enabled=false). Meet links will not be created.");
            return;
        }
        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(g.getClientId())
                    .setClientSecret(g.getClientSecret())
                    .setRefreshToken(g.getRefreshToken())
                    .build();
            calendar = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("Consultation Backend")
                    .build();
            log.info("Google Calendar client initialised.");
        } catch (Exception e) {
            log.error("Failed to initialise Google Calendar client", e);
        }
    }

    /** True only when the Google Calendar client was initialised, i.e. Meet links can be created. */
    public boolean isEnabled() {
        return calendar != null;
    }

    /** Fail zala tar null return karto, booking fail hot nahi. */
    public MeetResult createMeet(Appointment a) {
        if (calendar == null) {
            return null;
        }
        AppProperties.Google g = props.getGoogle();
        try {
            String tz = TimeUtil.IST.getId();
            Event event = new Event()
                    .setSummary("Online consultation: " + a.getName())
                    .setDescription((a.getService() == null ? "" : "Service: " + a.getService() + "\n") + "Reason: " + a.getReason())
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(a.getStartTime().toEpochMilli()))
                            .setTimeZone(tz))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(a.getEndTime().toEpochMilli()))
                            .setTimeZone(tz))
                    .setConferenceData(new ConferenceData().setCreateRequest(
                            new CreateConferenceRequest()
                                    .setRequestId(UUID.randomUUID().toString())
                                    .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"))));

            List<EventAttendee> attendees = new ArrayList<>();
            if (g.isSendInvites()) {
                attendees.add(new EventAttendee().setEmail(a.getEmail()));
            }
            if (g.getDoctorEmail() != null && !g.getDoctorEmail().isBlank()) {
                attendees.add(new EventAttendee().setEmail(g.getDoctorEmail().trim()));
            }
            if (!attendees.isEmpty()) {
                event.setAttendees(attendees);
            }

            Event created = calendar.events()
                    .insert(g.getCalendarId(), event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates(attendees.isEmpty() ? "none" : "all")
                    .execute();

            String link = created.getHangoutLink();
            if (link == null && created.getConferenceData() != null
                    && created.getConferenceData().getEntryPoints() != null) {
                for (EntryPoint ep : created.getConferenceData().getEntryPoints()) {
                    if ("video".equals(ep.getEntryPointType())) {
                        link = ep.getUri();
                        break;
                    }
                }
            }
            return new MeetResult(link, created.getId());
        } catch (Exception e) {
            log.error("Could not create Meet link for appointment {}", a.getId(), e);
            return null;
        }
    }
}
