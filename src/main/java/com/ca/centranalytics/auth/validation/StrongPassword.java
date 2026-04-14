package com.ca.centranalytics.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that password meets security requirements:
 * minimum 8 characters, at least one digit, one lowercase,
 * one uppercase, and one special character.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Пароль должен содержать минимум 8 символов, " +
            "хотя бы одну цифру, одну строчную букву, " +
            "одну заглавную букву и один специальный символ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
