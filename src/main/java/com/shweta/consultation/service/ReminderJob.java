package com.shweta.consultation.service;

import com.shweta.consultation.config.AppProperties;
import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.entity.AppointmentStatus;
import com.shweta.consultation.repository.AppointmentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs every 30 seconds. For every CONFIRMED appointment that starts within the next N minutes
 * (app.reminder.minutes-before, default 10) and has not been reminded yet, it sends the
 * Google Meet link by WhatsApp + email. The patient therefore receives the link between
 * N minutes and N minutes minus 30 seconds before the consultation.
 *
 * If every enabled channel fails the claim is released and the reminder is retried on the next run
 * until the appointment starts.
 *
 * If the Meet link is missing (Google was down at booking time) it is created now and the
 * reminder is retried on the next run, instead of sending a reminder without a link.
 */
@Component
@Slf4j
public class ReminderJob {

    private final AppointmentRepository repo;
    private final AppProperties props;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final MeetService meetService;

    public ReminderJob(AppointmentRepository repo, AppProperties props,
                       WhatsAppService whatsAppService, EmailService emailService,
                       MeetService meetService) {
        this.repo = repo;
        this.props = props;
        this.whatsAppService = whatsAppService;
        this.emailService = emailService;
        this.meetService = meetService;
    }

    @Scheduled(fixedRate = 30_000)
    public void sendReminders() {
        Instant now = Instant.now();
        Instant limit = now.plus(props.getReminder().getMinutesBefore(), ChronoUnit.MINUTES);
        List<Appointment> due = repo.findDue(AppointmentStatus.CONFIRMED, now, limit);

        for (Appointment a : due) {
            if (!ensureMeetLink(a)) {
                // Google is enabled but we still have no link: do NOT claim, retry on the next run.
                log.error("No Meet link yet for appointment {}; reminder will be retried", a.getReference());
                continue;
            }
            // Atomic claim: multiple instances astil tari fakt ek reminder jato
            if (repo.claimReminder(a.getId()) == 1) {
                log.info("Sending reminder for appointment {} (meet link present: {})",
                        a.getReference(), a.getMeetLink() != null);
                SendResult wa = whatsAppService.sendReminderNow(a);
                SendResult mail = emailService.sendReminderNow(a);
                boolean anySent = wa == SendResult.SENT || mail == SendResult.SENT;
                boolean anyFailed = wa == SendResult.FAILED || mail == SendResult.FAILED;
                if (!anySent && anyFailed) {
                    // Nothing reached the patient: release the claim, retry on the next run
                    // (the query only returns appointments that have not started yet).
                    repo.releaseReminder(a.getId());
                    log.error("Reminder for {} failed on all enabled channels (whatsapp={}, email={}); will retry",
                            a.getReference(), wa, mail);
                }
            }
        }
    }

    /**
     * @return true when the appointment can be reminded now: it has a link, or Google integration
     *         is disabled altogether (nothing to wait for, a link will never exist).
     */
    private boolean ensureMeetLink(Appointment a) {
        if (a.getMeetLink() != null && !a.getMeetLink().isBlank()) {
            return true;
        }
        if (!meetService.isEnabled()) {
            return true;
        }
        MeetService.MeetResult meet = meetService.createMeet(a);
        if (meet == null || meet.link() == null) {
            return false;
        }
        repo.updateMeet(a.getId(), meet.link(), meet.eventId());
        a.setMeetLink(meet.link());
        a.setGoogleEventId(meet.eventId());
        return true;
    }
}