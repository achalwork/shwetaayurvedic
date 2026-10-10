package com.shweta.consultation.dto;

import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.util.TimeUtil;

public record BookingResponse(
        String reference,
        String date,
        String time,
        String name,
        String mobile,
        String email,
        String language,
        String reason,
        String meetLink,
        String status
) {
    public static BookingResponse from(Appointment a) {
        return new BookingResponse(
                a.getReference(),
                TimeUtil.date(a.getStartTime()),
                TimeUtil.time(a.getStartTime()) + " IST",
                a.getName(),
                "+91" + a.getMobile(),
                a.getEmail(),
                a.getLanguage(),
                a.getReason(),
                // The Meet link is deliberately NOT returned at booking time. It is sent by
                // ReminderJob (email + WhatsApp) shortly before the consultation starts.
                null,
                a.getStatus().name());
    }
}
