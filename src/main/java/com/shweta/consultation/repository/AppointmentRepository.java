package com.shweta.consultation.repository;

import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsBySlotKey(String slotKey);

    @Query("select a.startTime from Appointment a where a.status = :status " +
            "and a.startTime >= :from and a.startTime < :to")
    List<Instant> findBookedStarts(@Param("status") AppointmentStatus status,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("select a from Appointment a where a.status = :status and a.reminderSent = false " +
            "and a.startTime > :now and a.startTime <= :limit order by a.startTime")
    List<Appointment> findDue(@Param("status") AppointmentStatus status,
                              @Param("now") Instant now,
                              @Param("limit") Instant limit);

    /** Saves a late-created Meet link without touching any other column (e.g. reminderSent). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Appointment a set a.meetLink = :link, a.googleEventId = :eventId where a.id = :id")
    int updateMeet(@Param("id") Long id, @Param("link") String link, @Param("eventId") String eventId);

    /**
     * Atomic claim: fakt ek instance la 1 miltoy, mhanje double reminder jat nahi
     * (multiple backend instances astil tari).
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Appointment a set a.reminderSent = true where a.id = :id and a.reminderSent = false")
    int claimReminder(@Param("id") Long id);

    /** Undo a claim when every enabled channel failed, so the next run retries (until the start time). */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Appointment a set a.reminderSent = false where a.id = :id and a.reminderSent = true")
    int releaseReminder(@Param("id") Long id);
}