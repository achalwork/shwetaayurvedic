package com.shweta.consultation.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequest(
        @NotNull(message = "Date is required") LocalDate date,
        @NotNull(message = "Time is required") LocalTime time,
        @NotBlank(message = "Name is required") @Size(max = 150) String name,
        @NotBlank(message = "Mobile is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number") String mobile,
        @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 200) String email,
        @NotBlank(message = "Language is required") @Size(max = 50) String language,
        @NotBlank(message = "Reason is required") @Size(max = 2000) String reason,
        @Size(max = 2000) String notes,
        @Size(max = 150) String service,
        @NotNull @AssertTrue(message = "Consent is required") Boolean consent
) {}
