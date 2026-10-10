package com.shweta.consultation.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeUtil {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private TimeUtil() {}

    public static String date(Instant i) {
        return DATE.format(i.atZone(IST));
    }

    public static String time(Instant i) {
        return TIME.format(i.atZone(IST));
    }
}
