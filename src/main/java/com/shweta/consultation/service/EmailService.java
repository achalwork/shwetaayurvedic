package com.shweta.consultation.service;

import com.shweta.consultation.config.AppProperties;
import com.shweta.consultation.dto.ContactRequest;
import com.shweta.consultation.entity.Appointment;
import com.shweta.consultation.util.TimeUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties props;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, AppProperties props) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
    }

    @Async
    public void sendConfirmation(Appointment a) {
        send(a, "Your consultation is confirmed (" + a.getReference() + ")", "confirmation-email");
    }

    @Async
    public void sendReminder(Appointment a) {
        sendReminderNow(a);
    }

    /** Synchronous variant used by ReminderJob so it knows whether the email really went out. */
    public SendResult sendReminderNow(Appointment a) {
        return send(a, "Reminder: your consultation starts in " + props.getReminder().getMinutesBefore() + " minutes",
                "reminder-email");
    }

    /** Contact form: message doctor chya email var jato, reply kelyavar patient la jail. */
    @Async
    public void sendContactMessage(ContactRequest r) {
        AppProperties.Mail m = props.getMail();
        if (!m.isEnabled()) {
            log.info("Email disabled. Contact message from {} not emailed.", r.email());
            return;
        }
        String to = (m.getContactTo() != null && !m.getContactTo().isBlank()) ? m.getContactTo() : m.getFrom();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(m.getFrom());
            msg.setTo(to);
            msg.setReplyTo(r.email());
            msg.setSubject("Website contact: " + r.name());
            msg.setText("Name: " + r.name() + "\nEmail: " + r.email() + "\n\n" + r.message());
            mailSender.send(msg);
            log.info("Contact message from {} emailed", r.email());
        } catch (Exception e) {
            log.error("Contact email failed", e);
        }
    }

    private SendResult send(Appointment a, String subject, String template) {
        if (!props.getMail().isEnabled()) {
            log.info("Email disabled. Skipping '{}' for appointment {}", subject, a.getId());
            return SendResult.SKIPPED;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("name", a.getName());
            ctx.setVariable("reference", a.getReference());
            ctx.setVariable("date", TimeUtil.date(a.getStartTime()));
            ctx.setVariable("time", TimeUtil.time(a.getStartTime()));
            ctx.setVariable("meetLink", a.getMeetLink());
            ctx.setVariable("doctorName", props.getConsultation().getDoctorName());
            ctx.setVariable("minutes", props.getReminder().getMinutesBefore());

            String html = templateEngine.process(template, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
            helper.setTo(a.getEmail());
            if (props.getMail().getReplyTo() != null && !props.getMail().getReplyTo().isBlank()) {
                helper.setReplyTo(props.getMail().getReplyTo().trim());
            }
            String bcc = props.getMail().getDoctorBcc();
            if (bcc != null && !bcc.isBlank()) {
                helper.setBcc(bcc.trim());
            }
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email '{}' sent for appointment {}", subject, a.getId());
            return SendResult.SENT;
        } catch (Exception e) {
            log.error("Email failed for appointment {}", a.getId(), e);
            return SendResult.FAILED;
        }
    }
}