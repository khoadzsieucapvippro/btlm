package com.elearning;

import com.elearning.common.ErrorCode;
import com.elearning.dto.request.ApproveLessonRequest;
import com.elearning.dto.request.RejectLessonRequest;
import com.elearning.dto.response.LessonDetailResponse;
import com.elearning.dto.response.ModerationQueueResponse;
import com.elearning.entity.Account;
import com.elearning.entity.Lesson;
import com.elearning.entity.LessonVocabulary;
import com.elearning.entity.ModerationLog;
import com.elearning.entity.Vocabulary;
import com.elearning.exception.BusinessException;
import com.elearning.repository.AccountRepository;
import com.elearning.repository.LessonRepository;
import com.elearning.repository.LessonVocabularyRepository;
import com.elearning.repository.ModerationLogRepository;
import com.elearning.service.ModerationService;
import com.elearning.service.impl.ModerationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task 6A.2: ModerationService & Audit Trail Unit Tests")
class ModerationServiceTests {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonVocabularyRepository lessonVocabularyRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ModerationLogRepository moderationLogRepository;

    private ModerationService moderationService;

    private Account moderatorAccount;
    private Account creatorAccount;
    private Lesson pendingLesson;
    private Lesson draftLesson;
    private Lesson approvedLesson;
    private Lesson rejectedLesson;
    private Vocabulary vocabularyXue;
    private LessonVocabulary lessonVocab;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationServiceImpl(lessonRepository, lessonVocabularyRepository, accountRepository, moderationLogRepository);

        moderatorAccount = new Account();
        moderatorAccount.setAccountId(1L);
        moderatorAccount.setEmailOrPhone("moderator@example.com");

        creatorAccount = new Account();
        creatorAccount.setAccountId(2L);
        creatorAccount.setEmailOrPhone("creator@example.com");

        pendingLesson = new Lesson("Bài Học Tiếng Trung 1", creatorAccount);
        pendingLesson.setLessonId(100L);
        pendingLesson.setStatus("Pending");

        draftLesson = new Lesson("Bài Học Nháp", creatorAccount);
        draftLesson.setLessonId(101L);
        draftLesson.setStatus("Draft");

        approvedLesson = new Lesson("Bài Học Đã Duyệt", creatorAccount);
        approvedLesson.setLessonId(102L);
        approvedLesson.setStatus("Approved");

        rejectedLesson = new Lesson("Bài Học Bị Từ Chối", creatorAccount);
        rejectedLesson.setLessonId(103L);
        rejectedLesson.setStatus("Rejected");

        vocabularyXue = new Vocabulary("学", "xué", "xue", "Học", "Học tập");
        vocabularyXue.setVocabId(10L);

        lessonVocab = new LessonVocabulary(pendingLesson, vocabularyXue, 1);

        // Setup security context with moderator
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "moderator@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MODERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getPendingLessons Tests")
    class GetPendingLessonsTests {

        @Test
        @DisplayName("Returns empty page when no pending lessons exist")
        void testGetPendingLessons_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(lessonRepository.findPendingLessonSummaries(pageable)).thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            Page<ModerationQueueResponse> result = moderationService.getPendingLessons(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(lessonRepository).findPendingLessonSummaries(pageable);
        }

        @Test
        @DisplayName("Returns populated page of ModerationQueueResponse for pending lessons")
        void testGetPendingLessons_populated() {
            Pageable pageable = PageRequest.of(0, 10);
            ModerationQueueResponse item = new ModerationQueueResponse(
                    100L, "Bài Học 1", "Pending", 2L, "creator@example.com", 5, LocalDateTime.now(), LocalDateTime.now()
            );
            when(lessonRepository.findPendingLessonSummaries(pageable)).thenReturn(new PageImpl<>(List.of(item), pageable, 1));

            Page<ModerationQueueResponse> result = moderationService.getPendingLessons(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getLessonId()).isEqualTo(100L);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Bài Học 1");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("Pending");
            assertThat(result.getContent().get(0).getCreatorId()).isEqualTo(2L);
            assertThat(result.getContent().get(0).getVocabularyCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getPendingLessonById Tests")
    class GetPendingLessonByIdTests {

        @Test
        @DisplayName("Returns LessonDetailResponse with constituent vocabulary when lesson exists")
        void testGetPendingLessonById_success() {
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(pendingLesson));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lessonVocab));

            LessonDetailResponse response = moderationService.getPendingLessonById(100L);

            assertThat(response).isNotNull();
            assertThat(response.getLessonId()).isEqualTo(100L);
            assertThat(response.getTitle()).isEqualTo("Bài Học Tiếng Trung 1");
            assertThat(response.getStatus()).isEqualTo("Pending");
            assertThat(response.getVocabularyCount()).isEqualTo(1);
            assertThat(response.getVocabularies()).hasSize(1);
            assertThat(response.getVocabularies().get(0).getHanzi()).isEqualTo("学");
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson does not exist")
        void testGetPendingLessonById_notFound() {
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> moderationService.getPendingLessonById(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson status is Draft (BE-AUTHZ-001)")
        void testGetPendingLessonById_draftStatus_throwsNotFound() {
            when(lessonRepository.findById(101L)).thenReturn(Optional.of(draftLesson));

            assertThatThrownBy(() -> moderationService.getPendingLessonById(101L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(lessonVocabularyRepository, never()).findByLessonIdWithVocabularyOrderAsc(any());
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson status is Approved (BE-AUTHZ-001)")
        void testGetPendingLessonById_approvedStatus_throwsNotFound() {
            when(lessonRepository.findById(102L)).thenReturn(Optional.of(approvedLesson));

            assertThatThrownBy(() -> moderationService.getPendingLessonById(102L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(lessonVocabularyRepository, never()).findByLessonIdWithVocabularyOrderAsc(any());
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson status is Rejected (BE-AUTHZ-001)")
        void testGetPendingLessonById_rejectedStatus_throwsNotFound() {
            when(lessonRepository.findById(103L)).thenReturn(Optional.of(rejectedLesson));

            assertThatThrownBy(() -> moderationService.getPendingLessonById(103L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(lessonVocabularyRepository, never()).findByLessonIdWithVocabularyOrderAsc(any());
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lessonId is null (BE-AUTHZ-001)")
        void testGetPendingLessonById_nullId_throwsNotFound() {
            assertThatThrownBy(() -> moderationService.getPendingLessonById(null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(lessonRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("approveLesson Tests")
    class ApproveLessonTests {

        @Test
        @DisplayName("Successfully approves a Pending lesson and transitions status to Approved and saves audit log")
        void testApproveLesson_success() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(pendingLesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lessonVocab));

            ApproveLessonRequest request = new ApproveLessonRequest("Duyệt nội dung chuẩn");
            LessonDetailResponse response = moderationService.approveLesson(100L, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Approved");
            assertThat(pendingLesson.getStatus()).isEqualTo("Approved");
            verify(lessonRepository).save(pendingLesson);

            ArgumentCaptor<ModerationLog> logCaptor = ArgumentCaptor.forClass(ModerationLog.class);
            verify(moderationLogRepository).save(logCaptor.capture());
            ModerationLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getAction()).isEqualTo("Approve");
            assertThat(savedLog.getModerator()).isEqualTo(moderatorAccount);
            assertThat(savedLog.getLesson()).isEqualTo(pendingLesson);
            assertThat(savedLog.getRejectionReason()).isNull();
            assertThat(savedLog.getFlaggedFields()).isNull();
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to approve a Draft lesson and saves no audit log")
        void testApproveDraftLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(101L)).thenReturn(Optional.of(draftLesson));

            assertThatThrownBy(() -> moderationService.approveLesson(101L, new ApproveLessonRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to approve an already Approved lesson and saves no audit log")
        void testApproveApprovedLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(102L)).thenReturn(Optional.of(approvedLesson));

            assertThatThrownBy(() -> moderationService.approveLesson(102L, new ApproveLessonRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to approve a Rejected lesson and saves no audit log")
        void testApproveRejectedLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(103L)).thenReturn(Optional.of(rejectedLesson));

            assertThatThrownBy(() -> moderationService.approveLesson(103L, new ApproveLessonRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson ID does not exist and saves no audit log")
        void testApproveLesson_notFound() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> moderationService.approveLesson(999L, new ApproveLessonRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws UNAUTHORIZED (401) when unauthenticated user calls approve")
        void testApproveLesson_unauthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> moderationService.approveLesson(100L, new ApproveLessonRequest()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    });

            verify(moderationLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("rejectLesson Tests")
    class RejectLessonTests {

        @Test
        @DisplayName("Successfully rejects a Pending lesson, transitions status to Rejected and saves audit log")
        void testRejectLesson_success() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(100L)).thenReturn(Optional.of(pendingLesson));
            when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(lessonVocabularyRepository.findByLessonIdWithVocabularyOrderAsc(100L)).thenReturn(List.of(lessonVocab));

            RejectLessonRequest request = new RejectLessonRequest("Pinyin từ vựng số 1 sai dấu", "[\"vocabularies[0].pinyin\"]");
            LessonDetailResponse response = moderationService.rejectLesson(100L, request);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("Rejected");
            assertThat(pendingLesson.getStatus()).isEqualTo("Rejected");
            verify(lessonRepository).save(pendingLesson);

            ArgumentCaptor<ModerationLog> logCaptor = ArgumentCaptor.forClass(ModerationLog.class);
            verify(moderationLogRepository).save(logCaptor.capture());
            ModerationLog savedLog = logCaptor.getValue();
            assertThat(savedLog.getAction()).isEqualTo("Reject");
            assertThat(savedLog.getModerator()).isEqualTo(moderatorAccount);
            assertThat(savedLog.getLesson()).isEqualTo(pendingLesson);
            assertThat(savedLog.getRejectionReason()).isEqualTo("Pinyin từ vựng số 1 sai dấu");
            assertThat(savedLog.getFlaggedFields()).isEqualTo("[\"vocabularies[0].pinyin\"]");
        }

        @Test
        @DisplayName("Throws BAD_REQUEST (400) when rejectionReason is null and saves no audit log")
        void testRejectLesson_nullReason_throwsBadRequest() {
            RejectLessonRequest request = new RejectLessonRequest(null, null);

            assertThatThrownBy(() -> moderationService.rejectLesson(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws BAD_REQUEST (400) when rejectionReason is blank and saves no audit log")
        void testRejectLesson_blankReason_throwsBadRequest() {
            RejectLessonRequest request = new RejectLessonRequest("   ", null);

            assertThatThrownBy(() -> moderationService.rejectLesson(100L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to reject a Draft lesson and saves no audit log")
        void testRejectDraftLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(101L)).thenReturn(Optional.of(draftLesson));

            RejectLessonRequest request = new RejectLessonRequest("Không hợp lệ");
            assertThatThrownBy(() -> moderationService.rejectLesson(101L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to reject an already Approved lesson and saves no audit log")
        void testRejectApprovedLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(102L)).thenReturn(Optional.of(approvedLesson));

            RejectLessonRequest request = new RejectLessonRequest("Không hợp lệ");
            assertThatThrownBy(() -> moderationService.rejectLesson(102L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws CONFLICT (409) when attempting to reject an already Rejected lesson and saves no audit log")
        void testRejectRejectedLesson_throwsConflict() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(103L)).thenReturn(Optional.of(rejectedLesson));

            RejectLessonRequest request = new RejectLessonRequest("Không hợp lệ");
            assertThatThrownBy(() -> moderationService.rejectLesson(103L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    });

            verify(lessonRepository, never()).save(any());
            verify(moderationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws NOT_FOUND (404) when lesson ID does not exist and saves no audit log")
        void testRejectLesson_notFound() {
            when(accountRepository.findByEmailOrPhone("moderator@example.com")).thenReturn(Optional.of(moderatorAccount));
            when(lessonRepository.findById(999L)).thenReturn(Optional.empty());

            RejectLessonRequest request = new RejectLessonRequest("Lý do hợp lệ");
            assertThatThrownBy(() -> moderationService.rejectLesson(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    });

            verify(moderationLogRepository, never()).save(any());
        }
    }
}
