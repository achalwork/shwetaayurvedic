package com.shweta.consultation.service;

import com.shweta.consultation.config.AppProperties;
import com.shweta.consultation.dto.BookingRequest;
import com.shweta.consultation.dto.BookingResponse;
import com.shweta.consultation.dto.SlotDto;
import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.entity.AppointmentStatus;
import com.shweta.consultation.exception.BadRequestException;
import com.shweta.consultation.exception.SlotUnavailableException;
import com.shweta.consultation.repository.AppointmentRepository;
import com.shweta.consultation.util.TimeUtil;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookingService {

    private static final ZoneId IST = TimeUtil.IST;
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final AppointmentRepository repo;
    private final AppProperties props;
    private final MeetService meetService;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;

    public BookingService(AppointmentRepository repo, AppProperties props, MeetService meetService,
                          EmailService emailService, WhatsAppService whatsAppService) {
        this.repo = repo;
        this.props = props;
        this.meetService = meetService;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
    }

    private List<LocalTime> slotTimes() {
        return Arrays.stream(props.getConsultation().getSlots().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(LocalTime::parse)
                .toList();
    }

    private Instant toInstant(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(IST).toInstant();
    }

    public List<SlotDto> getSlots(LocalDate date) {
        boolean sunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;
        Instant dayStart = date.atStartOfDay(IST).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(IST).toInstant();
        Set<Instant> booked = new HashSet<>(repo.findBookedStarts(AppointmentStatus.CONFIRMED, dayStart, dayEnd));
        Instant now = Instant.now();

        return slotTimes().stream().map(t -> {
            Instant start = toInstant(date, t);
            boolean available = !sunday && start.isAfter(now) && !booked.contains(start);
            return new SlotDto(t.toString(), t.format(LABEL), available);
        }).toList();
    }

    public BookingResponse book(BookingRequest r) {
        validateSlot(r.date(), r.time());

        Instant start = toInstant(r.date(), r.time());
        Instant end = start.plus(props.getConsultation().getDurationMinutes(), ChronoUnit.MINUTES);
        String slotKey = start.toString();

        if (repo.existsBySlotKey(slotKey)) {
            throw new SlotUnavailableException();
        }

        Appointment a = new Appointment();
        a.setName(r.name().trim());
        a.setMobile(r.mobile());
        a.setEmail(r.email().trim());
        a.setLanguage(r.language());
        a.setReason(r.reason().trim());
        a.setNotes(r.notes() == null ? null : r.notes().trim());
        a.setService(r.service() == null || r.service().isBlank() ? null : r.service().trim());
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus(AppointmentStatus.CONFIRMED);
        a.setSlotKey(slotKey);

        try {
            a = repo.save(a);
        } catch (DataIntegrityViolationException e) {
            // Dusrya user ne same vel la book kela
            throw new SlotUnavailableException();
        }

        a.setReference(String.format("SHETE-%d-%05d", r.date().getYear(), a.getId()));

        MeetService.MeetResult meet = meetService.createMeet(a);
        if (meet != null) {
            a.setMeetLink(meet.link());
            a.setGoogleEventId(meet.eventId());
        }
        a = repo.save(a);

        // Async: booking response la delay hot nahi
        emailService.sendConfirmation(a);
        whatsAppService.sendConfirmation(a);

        log.info("Booking confirmed: {} at {}", a.getReference(), start);
        return BookingResponse.from(a);
    }

    private void validateSlot(LocalDate date, LocalTime time) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Sundays are unavailable.");
        }
        if (!slotTimes().contains(time)) {
            throw new BadRequestException("Invalid time slot.");
        }
        LocalDate today = LocalDate.now(IST);
        if (date.isAfter(today.plusDays(props.getConsultation().getMaxDaysAhead()))) {
            throw new BadRequestException("Date is too far in the future.");
        }
        if (!toInstant(date, time).isAfter(Instant.now())) {
            throw new BadRequestException("This time has already passed.");
        }
    }
}
