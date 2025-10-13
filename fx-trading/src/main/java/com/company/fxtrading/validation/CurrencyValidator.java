package com.company.fxtrading.validation;

import com.company.fxtrading.config.ValidationProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Validator that checks if currency code is in the allowed list.
 */
@Component
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Autowired
    private ValidationProperties validationProperties;

    @Override
    public boolean isValid(String currency, ConstraintValidatorContext context) {
        if (currency == null) {
            return true; // @NotNull handles null check
        }

        boolean isValid = validationProperties.getAllowedCurrencies().contains(currency.toUpperCase());

        if (!isValid) {
            // Customize error message with actual allowed currencies
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Invalid currency code '" + currency + "'. Must be one of: " +
                validationProperties.getAllowedCurrencies()
            ).addConstraintViolation();
        }

        return isValid;
    }
}
