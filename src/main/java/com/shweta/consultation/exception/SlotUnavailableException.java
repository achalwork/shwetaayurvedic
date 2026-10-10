package com.shweta.consultation.exception;

public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException() {
        super("This time slot is no longer available. Please choose another time.");
    }
}
