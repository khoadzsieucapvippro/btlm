-- Migration V4: Add version column to CARD_PROGRESS for optimistic concurrency control (BE-CONC-001)

ALTER TABLE `CARD_PROGRESS`
    ADD COLUMN `version` BIGINT UNSIGNED NOT NULL DEFAULT 0;
