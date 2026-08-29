package com.elearning.dto.response;

import com.elearning.entity.ModerationLog;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Public response DTO representing an immutable moderation action audit log (GET /api/v1/moderator/history).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModerationLogResponse {

    private Long logId;
    private Long lessonId;
    private String lessonTitle;
    private Long moderatorId;
    private String moderatorEmail;
    private String action;
    private String rejectionReason;
    private String flaggedFields;
    private LocalDateTime createdAt;

    public ModerationLogResponse() {
    }

    public ModerationLogResponse(Long logId, Long lessonId, String lessonTitle, Long moderatorId, String moderatorEmail, String action, String rejectionReason, String flaggedFields, LocalDateTime createdAt) {
        this.logId = logId;
        this.lessonId = lessonId;
        this.lessonTitle = lessonTitle;
        this.moderatorId = moderatorId;
        this.moderatorEmail = moderatorEmail;
        this.action = action;
        this.rejectionReason = rejectionReason;
        this.flaggedFields = flaggedFields;
        this.createdAt = createdAt;
    }

    public static ModerationLogResponse fromEntity(ModerationLog log) {
        if (log == null) {
            return null;
        }
        Long lessonId = (log.getLesson() != null) ? log.getLesson().getLessonId() : null;
        String lessonTitle = (log.getLesson() != null) ? log.getLesson().getTitle() : null;
        Long moderatorId = (log.getModerator() != null) ? log.getModerator().getAccountId() : null;
        String moderatorEmail = (log.getModerator() != null) ? log.getModerator().getEmailOrPhone() : null;

        return new ModerationLogResponse(
                log.getLogId(),
                lessonId,
                lessonTitle,
                moderatorId,
                moderatorEmail,
                log.getAction(),
                log.getRejectionReason(),
                log.getFlaggedFields(),
                log.getCreatedAt()
        );
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public Long getModeratorId() {
        return moderatorId;
    }

    public void setModeratorId(Long moderatorId) {
        this.moderatorId = moderatorId;
    }

    public String getModeratorEmail() {
        return moderatorEmail;
    }

    public void setModeratorEmail(String moderatorEmail) {
        this.moderatorEmail = moderatorEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getFlaggedFields() {
        return flaggedFields;
    }

    public void setFlaggedFields(String flaggedFields) {
        this.flaggedFields = flaggedFields;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
