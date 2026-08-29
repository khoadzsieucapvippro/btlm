-- Migration V5: Add version column to LESSON for optimistic concurrency control (BE-CONC-003)

ALTER TABLE `LESSON`
    ADD COLUMN `version` BIGINT UNSIGNED NOT NULL DEFAULT 0;