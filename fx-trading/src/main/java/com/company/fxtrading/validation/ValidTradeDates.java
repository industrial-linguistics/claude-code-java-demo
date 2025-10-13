package com.company.fxtrading.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates trade date and value date relationship.
 * Rules:
 * - Trade date cannot be in the future
 * - Trade date cannot be too far in the past
 * - Value date must be >= trade date
 * - Value date must be within acceptable offset from trade date
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TradeDateValidator.class)
@Documented
public @interface ValidTradeDates {
    String message() default "Invalid trade dates";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
