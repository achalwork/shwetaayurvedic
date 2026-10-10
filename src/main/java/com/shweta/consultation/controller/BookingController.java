package com.shweta.consultation.controller;

import com.shweta.consultation.dto.BookingRequest;
import com.shweta.consultation.dto.BookingResponse;
import com.shweta.consultation.dto.SlotDto;
import com.shweta.consultation.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** GET /api/slots?date=2026-10-10 */
    @GetMapping("/slots")
    public List<SlotDto> slots(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getSlots(date);
    }

    /** POST /api/bookings */
    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request) {
        return bookingService.book(request);
    }
}
