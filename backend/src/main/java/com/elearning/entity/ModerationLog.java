package com.elearning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping the MODERATION_LOG table.
 * Immutable audit trail tracking lesson approval and rejection actions by moderators.
 * Foreign keys to Lesson and Account enforce ON DELETE RESTRICT to preserve audit integrity.
 */
@Entity
@Table(name = "moderation_log")
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moderator_id", nullable = false)
    private Account moderator;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "flagged_fields", columnDefinition = "TEXT")
    private String flaggedFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ModerationLog() {
    }

    public ModerationLog(Lesson lesson, Account moderator, String action, String rejectionReason, String flaggedFields) {
        this.lesson = lesson;
        this.moderator = moderator;
        this.action = action;
        this.rejectionReason = rejectionReason;
        this.flaggedFields = flaggedFields;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Account getModerator() {
        return moderator;
    }

    public void setModerator(Account moderator) {
        this.moderator = moderator;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationLog that)) return false;
        return logId != null && logId.equals(that.logId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
