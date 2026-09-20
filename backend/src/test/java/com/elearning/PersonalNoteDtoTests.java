package com.elearning;

import com.elearning.dto.request.PersonalNoteRequest;
import com.elearning.dto.response.PersonalNoteResponse;
import com.elearning.entity.PersonalNote;
import com.elearning.entity.UserProfile;
import com.elearning.entity.Vocabulary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 8A.1: PersonalNote DTOs Validation & Mapping Tests")
class PersonalNoteDtoTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("PersonalNoteRequest: valid content within 500 chars passes validation")
    void testValidRequest() {
        PersonalNoteRequest request = new PersonalNoteRequest("Đây là ghi chú học tập từ vựng: 学习汉语。");
        Set<ConstraintViolation<PersonalNoteRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PersonalNoteRequest: exactly 500 chars passes validation")
    void testExact500Chars() {
        String content500 = "A".repeat(500);
        PersonalNoteRequest request = new PersonalNoteRequest(content500);
        Set<ConstraintViolation<PersonalNoteRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("PersonalNoteRequest: 501 chars fails validation with Size constraint")
    void test501Chars_failsValidation() {
        String content501 = "A".repeat(501);
        PersonalNoteRequest request = new PersonalNoteRequest(content501);
        Set<ConstraintViolation<PersonalNoteRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("tối đa 500 ký tự");
    }

    @Test
    @DisplayName("PersonalNoteRequest: null content fails validation with NotNull constraint")
    void testNullContent_failsValidation() {
        PersonalNoteRequest request = new PersonalNoteRequest(null);
        Set<ConstraintViolation<PersonalNoteRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("không được để null");
    }

    @Test
    @DisplayName("PersonalNoteResponse: fromEntity mapping maps all entity fields accurately")
    void testResponseFromEntity() {
        UserProfile user = new UserProfile();
        user.setUserId(10L);

        Vocabulary vocab = new Vocabulary();
        vocab.setVocabId(20L);

        PersonalNote note = new PersonalNote(user, vocab, "Ghi chú từ vựng");
        note.setNoteId(100L);
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);

        PersonalNoteResponse response = PersonalNoteResponse.fromEntity(note);

        assertThat(response).isNotNull();
        assertThat(response.getNoteId()).isEqualTo(100L);
        assertThat(response.getVocabId()).isEqualTo(20L);
        assertThat(response.getContent()).isEqualTo("Ghi chú từ vựng");
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }
}
