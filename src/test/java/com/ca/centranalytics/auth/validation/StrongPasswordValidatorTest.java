package com.ca.centranalytics.auth.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsValidPassword() {
        assertThat(validator.isValid("Test1234!", null)).isTrue();
    }

    @Test
    void acceptsExactlyEightCharacters() {
        assertThat(validator.isValid("Abc1234!", null)).isTrue();
    }

    @Test
    void rejectsNullPassword() {
        assertThat(validator.isValid(null, null)).isFalse();
    }

    @Test
    void rejectsBlankPassword() {
        assertThat(validator.isValid("", null)).isFalse();
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Sh1A!",             // Too short (5 chars)
            "abcdefgh",          // No uppercase, digit, special
            "ABCDEFGH",          // No lowercase, digit, special
            "12345678",          // No letters, special
            "Abcdefgh",          // No digit, special
            "Abcdefg1",          // No special char
            "ABCDEFG1",          // No lowercase, special
            "abcd!@#$",          // No uppercase, digit
    })
    void rejectsInvalidPassword(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Test1234!",
            "P@ssw0rd",
            "MyStr0ng!Pass",
            "A1b2c3d4!",
            "Complex#Pass99"
    })
    void acceptsStrongPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }
}
