-- =============================================================================
-- Migration: V2__seed_roles.sql
-- Description: Seed 4 fixed system roles (1=Learner, 2=Creator, 3=Moderator, 4=Admin)
-- Database: MySQL 8.4+
-- Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- =============================================================================

INSERT INTO `role` (`role_id`, `role_name`) VALUES
    (1, 'Learner'),
    (2, 'Creator'),
    (3, 'Moderator'),
    (4, 'Admin');
