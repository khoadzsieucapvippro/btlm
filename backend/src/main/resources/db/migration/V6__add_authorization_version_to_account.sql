-- Migration V6: Add authorization_version column to ACCOUNT for immediate JWT role revocation (BE-AUTH-004)

ALTER TABLE `ACCOUNT`
    ADD COLUMN `authorization_version` BIGINT UNSIGNED NOT NULL DEFAULT 1;
