package com.ca.centranalytics.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates password strength requirements without ReDoS risk.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final String SPECIAL_CHARS = "!@#$%^&+=_~(){}[]|,;.:?";

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasDigit = false;
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (Character.isDigit(ch)) hasDigit = true;
            else if (Character.isLowerCase(ch)) hasLower = true;
            else if (Character.isUpperCase(ch)) hasUpper = true;
            else if (SPECIAL_CHARS.indexOf(ch) >= 0) hasSpecial = true;
        }

        return hasDigit && hasLower && hasUpper && hasSpecial;
    }
}
