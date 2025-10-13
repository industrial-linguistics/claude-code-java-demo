package com.company.fxtrading.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Converts LocalDate to String for SQLite storage.
 * SQLite doesn't have a native DATE type, so we store dates as TEXT in ISO-8601 format.
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String convertToDatabaseColumn(LocalDate localDate) {
        return localDate == null ? null : localDate.format(FORMATTER);
    }

    @Override
    public LocalDate convertToEntityAttribute(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        // Handle both epoch milliseconds (old format) and ISO date strings (new format)
        try {
            // Try parsing as ISO date first
            return LocalDate.parse(dateString, FORMATTER);
        } catch (Exception e) {
            // If that fails, try parsing as epoch milliseconds
            try {
                long epochMilli = Long.parseLong(dateString);
                return java.time.Instant.ofEpochMilli(epochMilli)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse date: " + dateString, ex);
            }
        }
    }
}
