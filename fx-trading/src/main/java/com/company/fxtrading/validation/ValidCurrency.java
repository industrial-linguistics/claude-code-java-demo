package com.company.fxtrading.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a currency code is in the allowed list.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
@Documented
public @interface ValidCurrency {
    String message() default "Invalid currency code. Must be one of: {allowedCurrencies}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
