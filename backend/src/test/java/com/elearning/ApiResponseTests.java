package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.response.ApiResponse;
import com.elearning.dto.response.PageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse, PageResponse & ErrorCode Contract Tests")
class ApiResponseTests {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    record SamplePayload(Long id, String name) {}

    @Nested
    @DisplayName("ApiResponse Serialization Tests")
    class ApiResponseSerializationTests {

        @Test
        @DisplayName("GIVEN success response with payload WHEN serialized THEN contains code, message, empty errors, and data")
        void testSuccessWithDataSerialization() throws Exception {
            // GIVEN
            SamplePayload payload = new SamplePayload(1L, "HSK 1 Vocabulary");
            ApiResponse<SamplePayload> response = ApiResponse.success(payload);

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.has("code")).isTrue();
            assertThat(root.has("message")).isTrue();
            assertThat(root.has("errors")).isTrue();
            assertThat(root.has("data")).isTrue();

            assertThat(root.get("code").asText()).isEqualTo("SUCCESS");
            assertThat(root.get("message").asText()).isEqualTo(ErrorCode.SUCCESS.getDefaultMessage());
            assertThat(root.get("errors").isArray()).isTrue();
            assertThat(root.get("errors").isEmpty()).isTrue();

            assertThat(root.get("data").get("id").asLong()).isEqualTo(1L);
            assertThat(root.get("data").get("name").asText()).isEqualTo("HSK 1 Vocabulary");
        }

        @Test
        @DisplayName("GIVEN success response with custom message WHEN serialized THEN custom message is preserved")
        void testSuccessWithCustomMessage() throws Exception {
            // GIVEN
            ApiResponse<String> response = ApiResponse.success("Custom success operation", "payload");

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("code").asText()).isEqualTo("SUCCESS");
            assertThat(root.get("message").asText()).isEqualTo("Custom success operation");
            assertThat(root.get("data").asText()).isEqualTo("payload");
            assertThat(root.get("errors").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("GIVEN error response without error list WHEN serialized THEN errors array is empty and data is null")
        void testErrorWithoutErrorsList() throws Exception {
            // GIVEN
            ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Resource not found");

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("code").asText()).isEqualTo("NOT_FOUND");
            assertThat(root.get("message").asText()).isEqualTo("Resource not found");
            assertThat(root.get("errors").isArray()).isTrue();
            assertThat(root.get("errors").isEmpty()).isTrue();
            assertThat(root.get("data").isNull()).isTrue();
        }

        @Test
        @DisplayName("GIVEN error response with errors list WHEN serialized THEN errors array contains all items")
        void testErrorWithErrorsList() throws Exception {
            // GIVEN
            List<String> errors = List.of("email is required", "password must be at least 6 characters");
            ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Validation failed", errors);

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("code").asText()).isEqualTo("VALIDATION_ERROR");
            assertThat(root.get("message").asText()).isEqualTo("Validation failed");
            assertThat(root.get("errors").isArray()).isTrue();
            assertThat(root.get("errors").size()).isEqualTo(2);
            assertThat(root.get("errors").get(0).asText()).isEqualTo("email is required");
            assertThat(root.get("errors").get(1).asText()).isEqualTo("password must be at least 6 characters");
            assertThat(root.get("data").isNull()).isTrue();
        }

        @Test
        @DisplayName("GIVEN ApiResponse constructed with ErrorCode enum WHEN serialized THEN code and message match enum")
        void testErrorWithErrorCodeEnum() throws Exception {
            // GIVEN
            ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED);

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("code").asText()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
            assertThat(root.get("message").asText()).isEqualTo(ErrorCode.UNAUTHORIZED.getDefaultMessage());
            assertThat(root.get("errors").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("GIVEN ApiResponse with collection data WHEN serialized THEN generic list is properly serialized")
        void testGenericCollectionSerialization() throws Exception {
            // GIVEN
            List<String> items = List.of("一", "二", "三");
            ApiResponse<List<String>> response = ApiResponse.success(items);

            // WHEN
            String json = objectMapper.writeValueAsString(response);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("data").isArray()).isTrue();
            assertThat(root.get("data").size()).isEqualTo(3);
            assertThat(root.get("data").get(0).asText()).isEqualTo("一");
            assertThat(root.get("data").get(1).asText()).isEqualTo("二");
            assertThat(root.get("data").get(2).asText()).isEqualTo("三");
        }
    }

    @Nested
    @DisplayName("PageResponse Tests")
    class PageResponseTests {

        @Test
        @DisplayName("GIVEN Spring Data Page with content WHEN converted via from() THEN maps all pagination metadata accurately")
        void testPageResponseMappingFromSpringDataPage() throws Exception {
            // GIVEN
            List<String> content = List.of("Radical 1", "Radical 2", "Radical 3");
            Page<String> springPage = new PageImpl<>(content, PageRequest.of(1, 3), 10);

            // WHEN
            PageResponse<String> pageResponse = PageResponse.from(springPage);

            // THEN
            assertThat(pageResponse.getPage()).isEqualTo(1);
            assertThat(pageResponse.getSize()).isEqualTo(3);
            assertThat(pageResponse.getTotalElements()).isEqualTo(10L);
            assertThat(pageResponse.getTotalPages()).isEqualTo(4);
            assertThat(pageResponse.getItems()).containsExactly("Radical 1", "Radical 2", "Radical 3");

            // Verify JSON serialization matching API.md section 1.3
            String json = objectMapper.writeValueAsString(pageResponse);
            JsonNode root = objectMapper.readTree(json);

            assertThat(root.has("page")).isTrue();
            assertThat(root.has("size")).isTrue();
            assertThat(root.has("totalElements")).isTrue();
            assertThat(root.has("totalPages")).isTrue();
            assertThat(root.has("items")).isTrue();

            assertThat(root.get("page").asInt()).isEqualTo(1);
            assertThat(root.get("size").asInt()).isEqualTo(3);
            assertThat(root.get("totalElements").asLong()).isEqualTo(10L);
            assertThat(root.get("totalPages").asInt()).isEqualTo(4);
            assertThat(root.get("items").size()).isEqualTo(3);
        }

        @Test
        @DisplayName("GIVEN empty Spring Data Page WHEN converted via from() THEN returns empty items array and 0 totals")
        void testEmptyPageResponse() throws Exception {
            // GIVEN
            Page<String> emptySpringPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

            // WHEN
            PageResponse<String> pageResponse = PageResponse.from(emptySpringPage);

            // THEN
            assertThat(pageResponse.getPage()).isEqualTo(0);
            assertThat(pageResponse.getSize()).isEqualTo(20);
            assertThat(pageResponse.getTotalElements()).isEqualTo(0L);
            assertThat(pageResponse.getTotalPages()).isEqualTo(0);
            assertThat(pageResponse.getItems()).isEmpty();

            String json = objectMapper.writeValueAsString(pageResponse);
            JsonNode root = objectMapper.readTree(json);
            assertThat(root.get("items").isArray()).isTrue();
            assertThat(root.get("items").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("GIVEN null Spring Data Page WHEN converted via from() THEN returns safe default empty page")
        void testNullSpringPageHandling() {
            // WHEN
            PageResponse<String> pageResponse = PageResponse.from(null);

            // THEN
            assertThat(pageResponse.getPage()).isEqualTo(0);
            assertThat(pageResponse.getSize()).isEqualTo(0);
            assertThat(pageResponse.getTotalElements()).isEqualTo(0L);
            assertThat(pageResponse.getTotalPages()).isEqualTo(0);
            assertThat(pageResponse.getItems()).isEmpty();
        }

        @Test
        @DisplayName("GIVEN ApiResponse wrapping PageResponse WHEN serialized THEN produces exact envelope from API.md 1.3")
        void testFullEnvelopedPageResponseSerialization() throws Exception {
            // GIVEN
            Page<String> springPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            PageResponse<String> pageResponse = PageResponse.from(springPage);
            ApiResponse<PageResponse<String>> fullResponse = ApiResponse.success("Tải danh sách thành công", pageResponse);

            // WHEN
            String json = objectMapper.writeValueAsString(fullResponse);
            JsonNode root = objectMapper.readTree(json);

            // THEN
            assertThat(root.get("code").asText()).isEqualTo("SUCCESS");
            assertThat(root.get("message").asText()).isEqualTo("Tải danh sách thành công");
            assertThat(root.get("errors").isArray()).isTrue();
            assertThat(root.get("errors").isEmpty()).isTrue();

            JsonNode data = root.get("data");
            assertThat(data.get("page").asInt()).isEqualTo(0);
            assertThat(data.get("size").asInt()).isEqualTo(20);
            assertThat(data.get("totalElements").asLong()).isEqualTo(0L);
            assertThat(data.get("totalPages").asInt()).isEqualTo(0);
            assertThat(data.get("items").isArray()).isTrue();
            assertThat(data.get("items").isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("ErrorCode Enum Contract Tests")
    class ErrorCodeTests {

        @Test
        @DisplayName("GIVEN ErrorCode enum WHEN checked THEN all standard project codes match API.md specifications")
        void testStandardErrorCodesContract() {
            assertThat(ErrorCode.SUCCESS.getCode()).isEqualTo("SUCCESS");
            assertThat(ErrorCode.SUCCESS.getHttpStatus()).isEqualTo(HttpStatus.OK);

            assertThat(ErrorCode.CREATED.getCode()).isEqualTo("CREATED");
            assertThat(ErrorCode.CREATED.getHttpStatus()).isEqualTo(HttpStatus.CREATED);

            assertThat(ErrorCode.VALIDATION_ERROR.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(ErrorCode.VALIDATION_ERROR.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(ErrorCode.BAD_REQUEST.getCode()).isEqualTo("BAD_REQUEST");
            assertThat(ErrorCode.BAD_REQUEST.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

            assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo("UNAUTHORIZED");
            assertThat(ErrorCode.UNAUTHORIZED.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);

            assertThat(ErrorCode.FORBIDDEN.getCode()).isEqualTo("FORBIDDEN");
            assertThat(ErrorCode.FORBIDDEN.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(ErrorCode.NOT_FOUND.getCode()).isEqualTo("NOT_FOUND");
            assertThat(ErrorCode.NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);

            assertThat(ErrorCode.CONFLICT.getCode()).isEqualTo("CONFLICT");
            assertThat(ErrorCode.CONFLICT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);

            assertThat(ErrorCode.UNPROCESSABLE_ENTITY.getCode()).isEqualTo("UNPROCESSABLE_ENTITY");
            assertThat(ErrorCode.UNPROCESSABLE_ENTITY.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

            assertThat(ErrorCode.FILE_TOO_LARGE.getCode()).isEqualTo("FILE_TOO_LARGE");
            assertThat(ErrorCode.FILE_TOO_LARGE.getHttpStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

            assertThat(ErrorCode.FILE_TYPE_INVALID.getCode()).isEqualTo("FILE_TYPE_INVALID");
            assertThat(ErrorCode.FILE_TYPE_INVALID.getHttpStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

            assertThat(ErrorCode.INTERNAL_ERROR.getCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("GIVEN ErrorCode constants WHEN verified THEN all have non-blank defaultMessage")
        void testErrorCodeDefaultMessages() {
            for (ErrorCode errorCode : ErrorCode.values()) {
                assertThat(errorCode.getCode()).isNotBlank();
                assertThat(errorCode.getDefaultMessage()).isNotBlank();
                assertThat(errorCode.getHttpStatus()).isNotNull();
            }
        }
    }
}
