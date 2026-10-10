package com.shweta.consultation.controller;

import com.shweta.consultation.dto.ContactRequest;
import com.shweta.consultation.service.EmailService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContactController {

    private final EmailService emailService;

    public ContactController(EmailService emailService) {
        this.emailService = emailService;
    }

    /** POST /api/contact : contact form message doctor la email var jato. */
    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> contact(@Valid @RequestBody ContactRequest request) {
        emailService.sendContactMessage(request);
        return Map.of("status", "received");
    }
}
