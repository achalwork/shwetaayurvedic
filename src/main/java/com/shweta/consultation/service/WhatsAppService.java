package com.shweta.consultation.service;

import com.shweta.consultation.config.AppProperties;
import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.util.TimeUtil;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * WhatsApp Business Cloud API (Meta). Template messages vaparto, karan business-initiated
 * message sathi approved template lagte.
 */
@Service
@Slf4j
public class WhatsAppService {

    private final AppProperties props;
    private final RestClient rest;

    public WhatsAppService(AppProperties props) {
        this.props = props;
        // Timeouts so a hung Meta API call can never block the reminder scheduler thread.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(10_000);
        this.rest = RestClient.builder().requestFactory(factory).build();
    }

    /** Template params: {{1}} name, {{2}} date, {{3}} time */
    @Async
    public void sendConfirmation(Appointment a) {
        sendTemplate(a, props.getWhatsapp().getConfirmationTemplate(),
                List.of(a.getName(), TimeUtil.date(a.getStartTime()), TimeUtil.time(a.getStartTime()) + " IST"));
    }

    /** Template params: {{1}} name, {{2}} time, {{3}} meet link */
    @Async
    public void sendReminder(Appointment a) {
        sendReminderNow(a);
    }

    /** Synchronous variant used by ReminderJob so it knows whether the message really went out. */
    public SendResult sendReminderNow(Appointment a) {
        String link = a.getMeetLink() != null ? a.getMeetLink() : "the doctor will share the joining link with you shortly";
        return sendTemplate(a, props.getWhatsapp().getReminderTemplate(),
                List.of(a.getName(), TimeUtil.time(a.getStartTime()) + " IST", link));
    }

    private SendResult sendTemplate(Appointment a, String templateName, List<String> params) {
        AppProperties.Whatsapp w = props.getWhatsapp();
        if (!w.isEnabled()) {
            log.info("WhatsApp disabled. Skipping template '{}' for appointment {}", templateName, a.getId());
            return SendResult.SKIPPED;
        }
        try {
            List<Map<String, String>> parameters = params.stream()
                    .map(p -> Map.of("type", "text", "text", p))
                    .toList();

            Map<String, Object> body = Map.of(
                    "messaging_product", "whatsapp",
                    "to", "91" + a.getMobile(),
                    "type", "template",
                    "template", Map.of(
                            "name", templateName,
                            "language", Map.of("code", w.getLanguageCode()),
                            "components", List.of(Map.of("type", "body", "parameters", parameters))));

            rest.post()
                    .uri("https://graph.facebook.com/{v}/{id}/messages", w.getApiVersion(), w.getPhoneId())
                    .header("Authorization", "Bearer " + w.getToken())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("WhatsApp template '{}' sent for appointment {}", templateName, a.getId());
            return SendResult.SENT;
        } catch (RestClientResponseException e) {
            log.error("WhatsApp API error for appointment {}: {} {}", a.getId(), e.getStatusCode(),
                    e.getResponseBodyAsString());
            return SendResult.FAILED;
        } catch (Exception e) {
            log.error("WhatsApp send failed for appointment {}", a.getId(), e);
            return SendResult.FAILED;
        }
    }
}