-- =============================================================================
-- Migration: V1__init_schema.sql
-- Description: Khởi tạo 14 bảng quan hệ cơ sở dữ liệu học Hán tự & Từ vựng Tiếng Trung
-- Database: MySQL 8.4+
-- Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- =============================================================================

-- 1. Bảng ACCOUNT
CREATE TABLE `ACCOUNT` (
    `account_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `email_or_phone` VARCHAR(191) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'Active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_account_email_or_phone` UNIQUE (`email_or_phone`),
    CONSTRAINT `chk_account_status` CHECK (`status` IN ('Active', 'Inactive', 'Banned'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng ROLE
CREATE TABLE `ROLE` (
    `role_id` INT UNSIGNED NOT NULL PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL,
    CONSTRAINT `uk_role_name` UNIQUE (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng USER_PROFILE
CREATE TABLE `USER_PROFILE` (
    `user_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `account_id` BIGINT UNSIGNED NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `avatar_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_user_profile_account` UNIQUE (`account_id`),
    CONSTRAINT `fk_user_profile_account` FOREIGN KEY (`account_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng ACCOUNT_ROLE
CREATE TABLE `ACCOUNT_ROLE` (
    `account_id` BIGINT UNSIGNED NOT NULL,
    `role_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `role_id`),
    CONSTRAINT `fk_account_role_account` FOREIGN KEY (`account_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_account_role_role` FOREIGN KEY (`role_id`) 
        REFERENCES `ROLE` (`role_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng RADICAL
CREATE TABLE `RADICAL` (
    `radical_id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `character` VARCHAR(10) NOT NULL,
    `pinyin` VARCHAR(50) NOT NULL,
    `meaning_han_viet` VARCHAR(100) NOT NULL,
    `meaning_vi` VARCHAR(255) NOT NULL,
    `audio_url` VARCHAR(500) NULL,
    `video_writing_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_radical_character` UNIQUE (`character`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng VOCABULARY
CREATE TABLE `VOCABULARY` (
    `vocab_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `hanzi` VARCHAR(50) NOT NULL,
    `pinyin` VARCHAR(100) NOT NULL,
    `pinyin_raw` VARCHAR(100) NOT NULL,
    `meaning_han_viet` VARCHAR(100) NOT NULL,
    `meaning_vi` VARCHAR(255) NOT NULL,
    `audio_url` VARCHAR(500) NULL,
    `video_writing_url` VARCHAR(500) NULL,
    `example_sentence` VARCHAR(500) NULL,
    `example_translation` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `uk_vocab_hanzi_pinyin_raw` UNIQUE (`hanzi`, `pinyin_raw`),
    INDEX `idx_vocab_pinyin_raw` (`pinyin_raw`),
    INDEX `idx_vocab_hanzi` (`hanzi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bảng VOCAB_RADICAL
CREATE TABLE `VOCAB_RADICAL` (
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `radical_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`vocab_id`, `radical_id`),
    CONSTRAINT `fk_vocab_radical_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_vocab_radical_radical` FOREIGN KEY (`radical_id`) 
        REFERENCES `RADICAL` (`radical_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bảng LESSON
CREATE TABLE `LESSON` (
    `lesson_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `excel_file_url` VARCHAR(500) NULL,
    `created_by` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'Draft',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lesson_created_by` FOREIGN KEY (`created_by`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_lesson_status` CHECK (`status` IN ('Draft', 'Pending', 'Approved', 'Rejected')),
    INDEX `idx_lesson_status` (`status`),
    INDEX `idx_lesson_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng LESSON_VOCABULARY
CREATE TABLE `LESSON_VOCABULARY` (
    `lesson_id` BIGINT UNSIGNED NOT NULL,
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `order_index` INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (`lesson_id`, `vocab_id`),
    CONSTRAINT `fk_lesson_vocab_lesson` FOREIGN KEY (`lesson_id`) 
        REFERENCES `LESSON` (`lesson_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_lesson_vocab_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `uk_lesson_order_index` UNIQUE (`lesson_id`, `order_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bảng USER_SRS_SETTING
CREATE TABLE `USER_SRS_SETTING` (
    `setting_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `new_cards_per_day` INT UNSIGNED NOT NULL DEFAULT 20,
    `max_review_per_day` INT UNSIGNED NOT NULL DEFAULT 100,
    CONSTRAINT `uk_user_srs_setting_user` UNIQUE (`user_id`),
    CONSTRAINT `fk_user_srs_setting_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Bảng CARD_PROGRESS
CREATE TABLE `CARD_PROGRESS` (
    `progress_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `item_type` VARCHAR(20) NOT NULL,
    `item_id` BIGINT UNSIGNED NOT NULL,
    `ease_factor` DECIMAL(4,2) NOT NULL DEFAULT 2.50,
    `interval_days` INT UNSIGNED NOT NULL DEFAULT 0,
    `repetitions` INT UNSIGNED NOT NULL DEFAULT 0,
    `next_review_at` DATETIME NULL,
    CONSTRAINT `fk_card_progress_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_card_progress_item_type` CHECK (`item_type` IN ('VOCABULARY', 'RADICAL')),
    CONSTRAINT `uk_card_progress_user_item` UNIQUE (`user_id`, `item_type`, `item_id`),
    INDEX `idx_card_progress_due` (`user_id`, `next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng REVIEW_LOG
CREATE TABLE `REVIEW_LOG` (
    `log_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `item_type` VARCHAR(20) NOT NULL,
    `item_id` BIGINT UNSIGNED NOT NULL,
    `rating` TINYINT UNSIGNED NOT NULL,
    `interval_before` INT UNSIGNED NOT NULL,
    `interval_after` INT UNSIGNED NOT NULL,
    `review_time_seconds` INT UNSIGNED NOT NULL DEFAULT 0,
    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_review_log_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `chk_review_log_item_type` CHECK (`item_type` IN ('VOCABULARY', 'RADICAL')),
    CONSTRAINT `chk_review_log_rating` CHECK (`rating` IN (1, 2, 3, 4)),
    INDEX `idx_review_log_user_date` (`user_id`, `reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Bảng PERSONAL_NOTE
CREATE TABLE `PERSONAL_NOTE` (
    `note_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `vocab_id` BIGINT UNSIGNED NOT NULL,
    `content` VARCHAR(500) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_personal_note_user` FOREIGN KEY (`user_id`) 
        REFERENCES `USER_PROFILE` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_personal_note_vocab` FOREIGN KEY (`vocab_id`) 
        REFERENCES `VOCABULARY` (`vocab_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_personal_note_user_vocab` (`user_id`, `vocab_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Bảng MODERATION_LOG
CREATE TABLE `MODERATION_LOG` (
    `log_id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `lesson_id` BIGINT UNSIGNED NOT NULL,
    `moderator_id` BIGINT UNSIGNED NOT NULL,
    `action` VARCHAR(20) NOT NULL,
    `rejection_reason` VARCHAR(500) NULL,
    `flagged_fields` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_moderation_log_lesson` FOREIGN KEY (`lesson_id`) 
        REFERENCES `LESSON` (`lesson_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_moderation_log_moderator` FOREIGN KEY (`moderator_id`) 
        REFERENCES `ACCOUNT` (`account_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `chk_moderation_action` CHECK (`action` IN ('Approve', 'Reject')),
    INDEX `idx_moderation_lesson` (`lesson_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
