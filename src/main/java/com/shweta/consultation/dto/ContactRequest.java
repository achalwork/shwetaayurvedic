package com.shweta.consultation.dto;

import jakarta.validation.constraints.*;

public record ContactRequest(
        @NotBlank(message = "Enter your name.") @Size(max = 150) String name,
        @NotBlank(message = "Enter your email.") @Email(message = "Enter a valid email address.") @Size(max = 200) String email,
        @NotBlank(message = "Write a short message.") @Size(min = 10, max = 3000, message = "Message needs 10 to 3000 characters.") String message
) {}
