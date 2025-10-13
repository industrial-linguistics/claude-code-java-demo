package com.company.fxtrading.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converts LocalDateTime to String for SQLite storage.
 * SQLite doesn't have a native TIMESTAMP type, so we store timestamps as TEXT in ISO-8601 format.
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    // SQLite datetime format: "YYYY-MM-DD HH:MM:SS"
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.format(FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        // Handle both epoch milliseconds (old format) and ISO datetime strings (new format)
        try {
            // Try parsing as SQLite datetime format first
            return LocalDateTime.parse(dateTimeString, FORMATTER);
        } catch (Exception e) {
            // Try ISO format with T separator
            try {
                return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                // If that fails, try parsing as epoch milliseconds
                try {
                    long epochMilli = Long.parseLong(dateTimeString);
                    return java.time.Instant.ofEpochMilli(epochMilli)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Unable to parse datetime: " + dateTimeString, ex);
                }
            }
        }
    }
}
