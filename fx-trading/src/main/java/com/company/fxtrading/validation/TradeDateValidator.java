package com.company.fxtrading.validation;

import com.company.fxtrading.config.ValidationProperties;
import com.company.fxtrading.domain.Trade;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Validator for trade date and value date business rules.
 */
@Component
public class TradeDateValidator implements ConstraintValidator<ValidTradeDates, Trade> {

    @Autowired
    private ValidationProperties validationProperties;

    @Override
    public boolean isValid(Trade trade, ConstraintValidatorContext context) {
        if (trade == null || trade.getTradeDate() == null || trade.getValueDate() == null) {
            return true; // @NotNull handles null checks
        }

        LocalDate tradeDate = trade.getTradeDate();
        LocalDate valueDate = trade.getValueDate();
        LocalDate today = LocalDate.now();

        context.disableDefaultConstraintViolation();

        // Rule 1: Trade date cannot be in the future
        if (tradeDate.isAfter(today.plusDays(validationProperties.getMaxFutureDays()))) {
            context.buildConstraintViolationWithTemplate(
                "Trade date cannot be more than " + validationProperties.getMaxFutureDays() + " days in the future"
            ).addPropertyNode("tradeDate").addConstraintViolation();
            return false;
        }

        // Rule 2: Trade date cannot be too far in the past
        if (tradeDate.isBefore(today.minusDays(validationProperties.getMaxPastDays()))) {
            context.buildConstraintViolationWithTemplate(
                "Trade date cannot be more than " + validationProperties.getMaxPastDays() + " days in the past"
            ).addPropertyNode("tradeDate").addConstraintViolation();
            return false;
        }

        // Rule 3: Value date must be >= trade date
        if (valueDate.isBefore(tradeDate)) {
            context.buildConstraintViolationWithTemplate(
                "Value date must be on or after trade date"
            ).addPropertyNode("valueDate").addConstraintViolation();
            return false;
        }

        // Rule 4: Value date must be within acceptable offset
        long daysBetween = ChronoUnit.DAYS.between(tradeDate, valueDate);
        if (daysBetween > validationProperties.getMaxValueDateOffset()) {
            context.buildConstraintViolationWithTemplate(
                "Value date cannot be more than " + validationProperties.getMaxValueDateOffset() +
                " days after trade date"
            ).addPropertyNode("valueDate").addConstraintViolation();
            return false;
        }

        return true;
    }
}
