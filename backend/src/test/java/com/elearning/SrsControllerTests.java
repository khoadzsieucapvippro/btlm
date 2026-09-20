package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.controller.SrsController;
import com.elearning.dto.request.ReviewCardRequest;
import com.elearning.dto.response.DueCardResponse;
import com.elearning.dto.response.StudyStatsResponse;
import com.elearning.exception.BusinessException;
import com.elearning.exception.GlobalExceptionHandler;
import com.elearning.service.SrsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 7B.2: SrsController Unit Contract Tests")
class SrsControllerTests {

    @Mock
    private SrsService srsService;

    @InjectMocks
    private SrsController srsController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(srsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("GET /api/v1/srs/due Tests")
    class GetDueCardsEndpointTests {

        @Test
        @DisplayName("GET /api/v1/srs/due with parameters delegates to SrsService and returns ApiResponse")
        void testGetDueCards_withParams_success() throws Exception {
            DueCardResponse card1 = new DueCardResponse();
            card1.setItemType("VOCABULARY");
            card1.setItemId(101L);
            card1.setHanzi("水");
            card1.setPinyin("shuǐ");
            card1.setMeaningVi("Nước");
            card1.setEaseFactor(new BigDecimal("2.50"));
            card1.setIntervalDays(1);
            card1.setRepetitions(1);

            when(srsService.getDueCards(eq("VOCABULARY"), eq(10)))
                    .thenReturn(List.of(card1));

            mockMvc.perform(get("/api/v1/srs/due")
                            .param("itemType", "VOCABULARY")
                            .param("limit", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.message", is("Thao tác thành công")))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].itemType", is("VOCABULARY")))
                    .andExpect(jsonPath("$.data[0].itemId", is(101)))
                    .andExpect(jsonPath("$.data[0].hanzi", is("水")));

            verify(srsService).getDueCards(eq("VOCABULARY"), eq(10));
        }

        @Test
        @DisplayName("GET /api/v1/srs/due with no parameters delegates with nulls")
        void testGetDueCards_noParams_success() throws Exception {
            when(srsService.getDueCards(null, null)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/srs/due")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(srsService).getDueCards(null, null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/srs/review Tests")
    class ReviewCardEndpointTests {

        @Test
        @DisplayName("POST /api/v1/srs/review with valid body delegates to SrsService and returns ApiResponse")
        void testReviewCard_validRequest_success() throws Exception {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 200L, 3, 4);

            DueCardResponse updatedResponse = new DueCardResponse();
            updatedResponse.setItemType("VOCABULARY");
            updatedResponse.setItemId(200L);
            updatedResponse.setHanzi("火");
            updatedResponse.setPinyin("huǒ");
            updatedResponse.setMeaningVi("Lửa");
            updatedResponse.setEaseFactor(new BigDecimal("2.50"));
            updatedResponse.setIntervalDays(1);
            updatedResponse.setRepetitions(1);

            when(srsService.reviewCard(any(ReviewCardRequest.class))).thenReturn(updatedResponse);

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.itemType", is("VOCABULARY")))
                    .andExpect(jsonPath("$.data.itemId", is(200)))
                    .andExpect(jsonPath("$.data.intervalDays", is(1)))
                    .andExpect(jsonPath("$.data.repetitions", is(1)));

            verify(srsService).reviewCard(any(ReviewCardRequest.class));
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with invalid rating returns 400 Bad Request and does not call service")
        void testReviewCard_invalidRating_returns400() throws Exception {
            ReviewCardRequest invalidRequest = new ReviewCardRequest("VOCABULARY", 200L, 5, 4); // rating 5 is out of bounds

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(srsService, never()).reviewCard(any());
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with invalid itemType returns 400 Bad Request")
        void testReviewCard_invalidItemType_returns400() throws Exception {
            ReviewCardRequest invalidRequest = new ReviewCardRequest("LESSON", 200L, 3, 4);

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(srsService, never()).reviewCard(any());
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with negative reviewTimeSeconds returns 400 Bad Request")
        void testReviewCard_negativeTime_returns400() throws Exception {
            ReviewCardRequest invalidRequest = new ReviewCardRequest("VOCABULARY", 200L, 3, -1);

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

            verify(srsService, never()).reviewCard(any());
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with missing item in service propagates NOT_FOUND 404")
        void testReviewCard_serviceThrowsNotFound_propagates404() throws Exception {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 999999L, 3, 2);

            when(srsService.reviewCard(any(ReviewCardRequest.class)))
                    .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy từ vựng với ID: 999999"));

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Không tìm thấy từ vựng với ID: 999999")));
        }

        @Test
        @DisplayName("POST /api/v1/srs/review with concurrent conflict returns 409 CONFLICT (BE-CONC-001)")
        void testReviewCard_concurrentConflict_returns409() throws Exception {
            ReviewCardRequest request = new ReviewCardRequest("VOCABULARY", 200L, 3, 2);

            when(srsService.reviewCard(any(ReviewCardRequest.class)))
                    .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Row was updated or deleted by another transaction"));

            mockMvc.perform(post("/api/v1/srs/review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code", is("CONFLICT")))
                    .andExpect(jsonPath("$.message", is("Dữ liệu đã bị thay đổi bởi một phiên làm việc khác, vui lòng thử lại")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/srs/stats Tests")
    class GetStudyStatsEndpointTests {

        @Test
        @DisplayName("GET /api/v1/srs/stats delegates to SrsService and returns ApiResponse")
        void testGetStudyStats_success() throws Exception {
            StudyStatsResponse stats = new StudyStatsResponse(12L, 28L, 20, 100);

            when(srsService.getStudyStats()).thenReturn(stats);

            mockMvc.perform(get("/api/v1/srs/stats")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data.cardsDue", is(12)))
                    .andExpect(jsonPath("$.data.reviewsToday", is(28)))
                    .andExpect(jsonPath("$.data.newCardsLimit", is(20)))
                    .andExpect(jsonPath("$.data.maxReviewLimit", is(100)));

            verify(srsService).getStudyStats();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/srs/new-cards and /api/v1/srs/lessons/{id}/new-cards Tests")
    class GetNewCardCandidatesEndpointTests {

        @Test
        @DisplayName("GET /api/v1/srs/new-cards with lessonId delegates to SrsService and returns candidates")
        void testGetNewCardCandidates_queryParam_success() throws Exception {
            DueCardResponse card1 = new DueCardResponse();
            card1.setItemType("VOCABULARY");
            card1.setItemId(10L);
            card1.setHanzi("学");
            card1.setIsNew(true);

            when(srsService.getNewCardCandidates(1L, 10)).thenReturn(List.of(card1));

            mockMvc.perform(get("/api/v1/srs/new-cards")
                            .param("lessonId", "1")
                            .param("limit", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].itemId", is(10)))
                    .andExpect(jsonPath("$.data[0].hanzi", is("学")))
                    .andExpect(jsonPath("$.data[0].isNew", is(true)));

            verify(srsService).getNewCardCandidates(1L, 10);
        }

        @Test
        @DisplayName("GET /api/v1/srs/lessons/{lessonId}/new-cards path variable delegates to SrsService")
        void testGetNewCardCandidates_pathVariable_success() throws Exception {
            DueCardResponse card1 = new DueCardResponse();
            card1.setItemType("VOCABULARY");
            card1.setItemId(20L);
            card1.setHanzi("习");
            card1.setIsNew(true);

            when(srsService.getNewCardCandidates(2L, null)).thenReturn(List.of(card1));

            mockMvc.perform(get("/api/v1/srs/lessons/2/new-cards")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code", is("SUCCESS")))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].itemId", is(20)))
                    .andExpect(jsonPath("$.data[0].hanzi", is("习")));

            verify(srsService).getNewCardCandidates(2L, null);
        }
    }
}
