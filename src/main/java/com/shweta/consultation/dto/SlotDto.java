package com.shweta.consultation.dto;

/** time = "10:00" (24h IST), label = "10:00 AM" */
public record SlotDto(String time, String label, boolean available) {}
