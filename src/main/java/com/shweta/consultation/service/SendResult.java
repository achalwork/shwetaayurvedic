package com.shweta.consultation.service;

/** Outcome of one notification channel (email / WhatsApp). */
public enum SendResult {
    /** Message was handed to the provider successfully. */
    SENT,
    /** Channel is disabled by configuration; nothing to send, nothing to retry. */
    SKIPPED,
    /** Channel is enabled but sending failed; worth retrying. */
    FAILED
}