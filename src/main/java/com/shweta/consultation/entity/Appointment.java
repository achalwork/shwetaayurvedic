package com.shweta.consultation.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appt_start", columnList = "start_time"),
        @Index(name = "idx_appt_status_start", columnList = "status,start_time")
})
@Getter
@Setter
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 40)
    private String reference;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(length = 2000)
    private String notes;

    /** Service chosen on the website (e.g. "General Ayurvedic Consultation"). Optional. */
    @Column(length = 150)
    private String service;

    /** UTC madhe store hota. IST madhe dakhvaycha. */
    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(length = 500)
    private String meetLink;

    @Column(length = 200)
    private String googleEventId;

    @Column(nullable = false)
    private boolean reminderSent = false;

    /** Double booking rokhnyasathi unique. Cancel kelyavar null kara. */
    @Column(unique = true, length = 40)
    private String slotKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
