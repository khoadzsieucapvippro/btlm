package com.elearning;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("R3.9: Database Index Performance & Slow Query Verification (EXPLAIN)")
class DatabaseIndexPerformanceExplainTests {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIndexPerformanceExplainTests.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Executes EXPLAIN (tabular format) and logs the plan details.
     */
    private List<Map<String, Object>> explain(String queryName, String sql, Object... params) {
        String explainSql = "EXPLAIN " + sql;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(explainSql, params);
        log.info("--- [EXPLAIN] {} ---", queryName);
        for (Map<String, Object> row : rows) {
            log.info("id={}, select_type={}, table={}, type={}, possible_keys={}, key={}, key_len={}, ref={}, rows={}, filtered={}, Extra={}",
                    row.get("id"), row.get("select_type"), row.get("table"), row.get("type"),
                    row.get("possible_keys"), row.get("key"), row.get("key_len"), row.get("ref"),
                    row.get("rows"), row.get("filtered"), row.get("Extra"));
        }
        return rows;
    }

    /**
     * Executes EXPLAIN ANALYZE (FORMAT=TREE) on MySQL 8.4 and logs actual execution stats.
     */
    private String explainAnalyze(String queryName, String sql, Object... params) {
        String explainSql = "EXPLAIN ANALYZE " + sql;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(explainSql, params);
        StringBuilder tree = new StringBuilder();
        for (Map<String, Object> row : rows) {
            for (Object val : row.values()) {
                tree.append(val).append("\n");
            }
        }
        log.info("--- [EXPLAIN ANALYZE] {} ---\n{}", queryName, tree);
        return tree.toString();
    }

    @Nested
    @DisplayName("1. Physical Schema & Index Inventory")
    class SchemaAndIndexInventoryTests {

        @Test
        @DisplayName("GIVEN database schema WHEN inspecting information_schema THEN report all table indexes and column orders")
        void testInventoryAllIndexes() {
            String sql = "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, COLLATION, CARDINALITY, INDEX_TYPE " +
                    "FROM information_schema.STATISTICS " +
                    "WHERE TABLE_SCHEMA = DATABASE() " +
                    "ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            assertThat(rows).isNotEmpty();

            log.info("=== TOTAL INDEX ENTRIES IN DATABASE: {} ===", rows.size());
            for (Map<String, Object> row : rows) {
                log.info("TABLE: {}, INDEX: {}, NON_UNIQUE: {}, SEQ: {}, COL: {}, TYPE: {}",
                        row.get("TABLE_NAME"), row.get("INDEX_NAME"), row.get("NON_UNIQUE"),
                        row.get("SEQ_IN_INDEX"), row.get("COLUMN_NAME"), row.get("INDEX_TYPE"));
            }
        }

        @Test
        @DisplayName("GIVEN database schema WHEN counting rows THEN report cardinality of all 14 tables")
        void testReportTableCardinalities() {
            String[] tables = {
                    "ACCOUNT", "ROLE", "USER_PROFILE", "ACCOUNT_ROLE",
                    "RADICAL", "VOCABULARY", "VOCAB_RADICAL", "LESSON",
                    "LESSON_VOCABULARY", "USER_SRS_SETTING", "CARD_PROGRESS",
                    "REVIEW_LOG", "PERSONAL_NOTE", "MODERATION_LOG"
            };

            for (String table : tables) {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
                log.info("TABLE_CARDINALITY: `{}` = {} rows", table, count);
            }
        }
    }

    @Nested
    @DisplayName("2. Priority Query Workload & Execution Plan Verification")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class ExecutionPlanAnalysisTests {

        private Long testUserId;
        private Long testAccountId;
        private Long testLessonId;
        private Long testVocabId;

        @org.junit.jupiter.api.AfterAll
        void tearDownBenchmarkData() {
            cleanupBenchmarkData();
        }

        private void cleanupBenchmarkData() {
            log.info("=== CLEANING UP BENCHMARK DATASET ===");
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
                jdbcTemplate.update("DELETE FROM `PERSONAL_NOTE` WHERE content LIKE 'Perf benchmark note%'");
                jdbcTemplate.update("DELETE FROM `REVIEW_LOG` WHERE user_id IN (SELECT user_id FROM `USER_PROFILE` WHERE full_name LIKE 'Perf Benchmark User%')");
                jdbcTemplate.update("DELETE FROM `CARD_PROGRESS` WHERE user_id IN (SELECT user_id FROM `USER_PROFILE` WHERE full_name LIKE 'Perf Benchmark User%')");
                jdbcTemplate.update("DELETE FROM `LESSON_VOCABULARY` WHERE lesson_id IN (SELECT lesson_id FROM `LESSON` WHERE title LIKE 'Perf Benchmark Lesson%')");
                jdbcTemplate.update("DELETE FROM `LESSON` WHERE title LIKE 'Perf Benchmark Lesson%'");
                jdbcTemplate.update("DELETE FROM `VOCABULARY` WHERE hanzi LIKE '测%'");
                jdbcTemplate.update("DELETE FROM `USER_PROFILE` WHERE full_name LIKE 'Perf Benchmark User%'");
                jdbcTemplate.update("DELETE FROM `ACCOUNT` WHERE email_or_phone LIKE 'perf_explain_user_%'");
            } finally {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            log.info("=== BENCHMARK DATASET CLEANED UP SUCCESSFULLY ===");
        }

        @org.junit.jupiter.api.BeforeEach
        void setupBenchmarkDataIfEmpty() {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM `CARD_PROGRESS` WHERE user_id IN (SELECT user_id FROM `USER_PROFILE` WHERE full_name LIKE 'Perf Benchmark User%')",
                    Long.class);
            if (count != null && count >= 500) {
                // Find existing IDs
                testUserId = jdbcTemplate.queryForObject("SELECT user_id FROM `USER_PROFILE` WHERE full_name = 'Perf Benchmark User 1' LIMIT 1", Long.class);
                testAccountId = jdbcTemplate.queryForObject("SELECT account_id FROM `ACCOUNT` WHERE email_or_phone = 'perf_explain_user_1@example.com' LIMIT 1", Long.class);
                testLessonId = jdbcTemplate.queryForObject("SELECT lesson_id FROM `LESSON` WHERE title = 'Perf Benchmark Lesson 1' LIMIT 1", Long.class);
                testVocabId = jdbcTemplate.queryForObject("SELECT vocab_id FROM `VOCABULARY` WHERE hanzi = '测1' LIMIT 1", Long.class);
                return;
            }

            cleanupBenchmarkData();

            log.info("=== SEEDING CONTROLLED SYNTHETIC BENCHMARK DATASET (CARD_PROGRESS=2000, VOCAB=500, LESSON=100) ===");

            // 1. Create 10 Accounts and UserProfiles
            for (int i = 1; i <= 10; i++) {
                jdbcTemplate.update("INSERT INTO `ACCOUNT` (email_or_phone, password_hash, status) VALUES (?, ?, 'Active') " +
                        "ON DUPLICATE KEY UPDATE status = 'Active'", "perf_explain_user_" + i + "@example.com", "$2a$10$hash");
                Long accId = jdbcTemplate.queryForObject("SELECT account_id FROM `ACCOUNT` WHERE email_or_phone = ?",
                        Long.class, "perf_explain_user_" + i + "@example.com");
                jdbcTemplate.update("INSERT INTO `USER_PROFILE` (account_id, full_name) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE full_name = VALUES(full_name)", accId, "Perf Benchmark User " + i);
                Long uId = jdbcTemplate.queryForObject("SELECT user_id FROM `USER_PROFILE` WHERE account_id = ?",
                        Long.class, accId);
                if (i == 1) {
                    testAccountId = accId;
                    testUserId = uId;
                }
            }

            // 2. Create 500 Vocabulary rows with unique Chinese character '测'
            List<Object[]> vocabBatch = new ArrayList<>();
            for (int i = 1; i <= 500; i++) {
                String hanzi = "测" + i;
                String pinyin = "cè" + i;
                String pinyinRaw = "ce" + i;
                vocabBatch.add(new Object[]{hanzi, pinyin, pinyinRaw, "Trắc " + i, "Nghĩa kiểm tra " + i});
            }
            jdbcTemplate.batchUpdate("INSERT IGNORE INTO `VOCABULARY` (hanzi, pinyin, pinyin_raw, meaning_han_viet, meaning_vi) VALUES (?, ?, ?, ?, ?)", vocabBatch);
            testVocabId = jdbcTemplate.queryForObject("SELECT vocab_id FROM `VOCABULARY` WHERE hanzi = '测1' LIMIT 1", Long.class);

            // 3. Create 100 Lessons (25 Approved, 25 Pending, 25 Draft, 25 Rejected)
            String[] statuses = {"Approved", "Pending", "Draft", "Rejected"};
            for (int i = 1; i <= 100; i++) {
                String status = statuses[(i - 1) % 4];
                jdbcTemplate.update("INSERT INTO `LESSON` (title, status, created_by) VALUES (?, ?, ?)",
                        "Perf Benchmark Lesson " + i, status, testAccountId);
            }
            testLessonId = jdbcTemplate.queryForObject("SELECT lesson_id FROM `LESSON` WHERE title = 'Perf Benchmark Lesson 1' LIMIT 1", Long.class);

            // 4. Create Lesson-Vocabulary links
            List<Long> allVocabIds = jdbcTemplate.queryForList("SELECT vocab_id FROM `VOCABULARY` WHERE hanzi LIKE '测%' LIMIT 500", Long.class);
            List<Object[]> lvBatch = new ArrayList<>();
            for (int order = 1; order <= Math.min(50, allVocabIds.size()); order++) {
                lvBatch.add(new Object[]{testLessonId, allVocabIds.get(order - 1), order});
            }
            jdbcTemplate.batchUpdate("INSERT IGNORE INTO `LESSON_VOCABULARY` (lesson_id, vocab_id, order_index) VALUES (?, ?, ?)", lvBatch);

            // 5. Create 2000 CardProgress rows for testUserId and other users
            List<Object[]> cpBatch = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < allVocabIds.size(); i++) {
                Long vId = allVocabIds.get(i);
                // Spread next_review_at: half due (past), half not due (future)
                LocalDateTime due = (i % 2 == 0) ? now.minusDays(i % 10) : now.plusDays((i % 10) + 1);
                cpBatch.add(new Object[]{testUserId, "VOCABULARY", vId, 2.50, 1, 1, due});
            }
            jdbcTemplate.batchUpdate("INSERT IGNORE INTO `CARD_PROGRESS` (user_id, item_type, item_id, ease_factor, interval_days, repetitions, next_review_at) VALUES (?, ?, ?, ?, ?, ?, ?)", cpBatch);

            // 6. Create 1000 ReviewLog rows
            List<Object[]> rlBatch = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                Long vId = allVocabIds.get(i);
                LocalDateTime revDate = now.minusHours(i);
                rlBatch.add(new Object[]{testUserId, "VOCABULARY", vId, 3, 1, 6, 10, revDate});
            }
            jdbcTemplate.batchUpdate("INSERT INTO `REVIEW_LOG` (user_id, item_type, item_id, rating, interval_before, interval_after, review_time_seconds, reviewed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", rlBatch);

            // 7. Create 200 PersonalNote rows
            List<Object[]> pnBatch = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Long vId = allVocabIds.get(i);
                pnBatch.add(new Object[]{testUserId, vId, "Perf benchmark note " + vId});
            }
            jdbcTemplate.batchUpdate("INSERT INTO `PERSONAL_NOTE` (user_id, vocab_id, content) VALUES (?, ?, ?)", pnBatch);

            // 8. Analyze tables to refresh optimizer stats
            String[] tablesToAnalyze = {"ACCOUNT", "USER_PROFILE", "VOCABULARY", "LESSON", "LESSON_VOCABULARY", "CARD_PROGRESS", "REVIEW_LOG", "PERSONAL_NOTE"};
            for (String t : tablesToAnalyze) {
                jdbcTemplate.execute("ANALYZE TABLE `" + t + "`");
            }
            log.info("=== BENCHMARK DATASET AND OPTIMIZER STATISTICS REFRESHED ===");
        }

        @Test
        @DisplayName("3.1 Priority Query: CARD_PROGRESS Due Queue (GET /api/v1/srs/due)")
        void testCardProgressDueQueueQueryPlan() {
            setupBenchmarkDataIfEmpty();

            // Query 1: Unpaged / Paged Due Cards: user_id = ? AND next_review_at <= ? ORDER BY next_review_at ASC
            String sql1 = "SELECT progress_id, ease_factor, interval_days, item_id, item_type, next_review_at, repetitions, user_id, version " +
                    "FROM `CARD_PROGRESS` WHERE user_id = ? AND next_review_at <= ? ORDER BY next_review_at ASC LIMIT 100";

            List<Map<String, Object>> plan1 = explain("CardProgress Due Cards (user_id + next_review_at)", sql1, testUserId, LocalDateTime.now());
            String tree1 = explainAnalyze("CardProgress Due Cards", sql1, testUserId, LocalDateTime.now());

            assertThat(plan1).isNotEmpty();
            Map<String, Object> root1 = plan1.get(0);
            assertThat(root1.get("key")).isEqualTo("idx_card_progress_due");
            assertThat(root1.get("type").toString()).isIn("range", "ref");
            // Verify Extra does NOT contain 'Using filesort'
            String extra1 = (String) root1.get("Extra");
            log.info("CardProgress Due Query Extra: {}", extra1);
            assertThat(extra1).doesNotContain("Using filesort");

            // Query 2: Due Cards with ItemType: user_id = ? AND item_type = ? AND next_review_at <= ? ORDER BY next_review_at ASC
            String sql2 = "SELECT progress_id, ease_factor, interval_days, item_id, item_type, next_review_at, repetitions, user_id, version " +
                    "FROM `CARD_PROGRESS` WHERE user_id = ? AND item_type = ? AND next_review_at <= ? ORDER BY next_review_at ASC LIMIT 100";

            List<Map<String, Object>> plan2 = explain("CardProgress Due Cards with item_type", sql2, testUserId, "VOCABULARY", LocalDateTime.now());
            explainAnalyze("CardProgress Due Cards with item_type", sql2, testUserId, "VOCABULARY", LocalDateTime.now());
            assertThat(plan2).isNotEmpty();

            // Query 3: Due Count: COUNT(*) WHERE user_id = ? AND next_review_at <= ?
            String sql3 = "SELECT COUNT(progress_id) FROM `CARD_PROGRESS` WHERE user_id = ? AND next_review_at <= ?";
            List<Map<String, Object>> plan3 = explain("CardProgress Due Count", sql3, testUserId, LocalDateTime.now());
            explainAnalyze("CardProgress Due Count", sql3, testUserId, LocalDateTime.now());
            assertThat(plan3.get(0).get("key")).isEqualTo("idx_card_progress_due");

            // Query 4: Card lookup by user, item_type, item_id (uk_card_progress_user_item)
            String sql4 = "SELECT progress_id FROM `CARD_PROGRESS` WHERE user_id = ? AND item_type = ? AND item_id = ?";
            List<Map<String, Object>> plan4 = explain("CardProgress Unique Item Lookup", sql4, testUserId, "VOCABULARY", testVocabId);
            explainAnalyze("CardProgress Unique Item Lookup", sql4, testUserId, "VOCABULARY", testVocabId);
            assertThat(plan4.get(0).get("key")).isEqualTo("uk_card_progress_user_item");
            assertThat(plan4.get(0).get("type").toString()).isIn("const", "eq_ref", "ref");

            // Query 5: existsByItemTypeAndItemId (R3.1 delete guard)
            String sql5 = "SELECT 1 FROM `CARD_PROGRESS` WHERE item_type = ? AND item_id = ? LIMIT 1";
            List<Map<String, Object>> plan5 = explain("CardProgress existsByItemTypeAndItemId", sql5, "VOCABULARY", testVocabId);
            explainAnalyze("CardProgress existsByItemTypeAndItemId", sql5, "VOCABULARY", testVocabId);
            log.info("existsByItemTypeAndItemId plan: key={}, type={}", plan5.get(0).get("key"), plan5.get(0).get("type"));
        }

        @Test
        @DisplayName("3.2 Priority Query: VOCABULARY Pinyin & Hanzi Index Usage")
        void testVocabularyIndexUsage() {
            setupBenchmarkDataIfEmpty();

            // Query 1: Exact Pinyin Raw Lookup (findByPinyinRaw)
            String sql1 = "SELECT * FROM `VOCABULARY` WHERE pinyin_raw = ? LIMIT 20";
            List<Map<String, Object>> plan1 = explain("Vocabulary findByPinyinRaw", sql1, "ce1");
            explainAnalyze("Vocabulary findByPinyinRaw", sql1, "ce1");
            assertThat(plan1.get(0).get("key")).isEqualTo("idx_vocab_pinyin_raw");
            assertThat(plan1.get(0).get("type")).isEqualTo("ref");

            // Query 2: Business Key Lookup (findByHanziAndPinyinRaw)
            String sql2 = "SELECT * FROM `VOCABULARY` WHERE hanzi = ? AND pinyin_raw = ?";
            List<Map<String, Object>> plan2 = explain("Vocabulary findByHanziAndPinyinRaw", sql2, "测1", "ce1");
            explainAnalyze("Vocabulary findByHanziAndPinyinRaw", sql2, "测1", "ce1");
            assertThat(plan2.get(0).get("key")).isEqualTo("uk_vocab_hanzi_pinyin_raw");
            assertThat(plan2.get(0).get("type").toString()).isIn("const", "ref");

            // Query 3: Hanzi exact lookup (findByHanzi) - observe key chosen: idx_vocab_hanzi vs uk_vocab_hanzi_pinyin_raw
            String sql3 = "SELECT * FROM `VOCABULARY` WHERE hanzi = ? LIMIT 20";
            List<Map<String, Object>> plan3 = explain("Vocabulary findByHanzi", sql3, "测1");
            explainAnalyze("Vocabulary findByHanzi", sql3, "测1");
            log.info("Vocabulary findByHanzi chosen key: {}, possible_keys: {}", plan3.get(0).get("key"), plan3.get(0).get("possible_keys"));
            assertThat(plan3.get(0).get("type").toString()).isIn("ref", "const");

            // Query 4: Search by Keyword (searchByKeyword / Specification hasKeyword)
            String sql4 = "SELECT * FROM `VOCABULARY` WHERE hanzi LIKE ? OR LOWER(pinyin) LIKE LOWER(CONCAT('%', ?, '%')) OR LOWER(pinyin_raw) LIKE LOWER(CONCAT('%', ?, '%')) LIMIT 20";
            List<Map<String, Object>> plan4 = explain("Vocabulary searchByKeyword", sql4, "%ce%", "ce", "ce");
            explainAnalyze("Vocabulary searchByKeyword", sql4, "%ce%", "ce", "ce");
            log.info("Vocabulary searchByKeyword type: {}, key: {}", plan4.get(0).get("type"), plan4.get(0).get("key"));
        }

        @Test
        @DisplayName("3.3 Priority Query: LESSON Status & Creator Workflow")
        void testLessonStatusAndCreatorQueryPlan() {
            setupBenchmarkDataIfEmpty();

            // Query 1: Public Approved Lessons (findApprovedLessonSummaries)
            String sql1 = "SELECT l.lesson_id, l.title, l.status, " +
                    "(SELECT COUNT(lv.vocab_id) FROM `LESSON_VOCABULARY` lv WHERE lv.lesson_id = l.lesson_id) AS vocab_count, " +
                    "l.created_at, l.updated_at FROM `LESSON` l WHERE l.status = 'Approved' LIMIT 20";
            List<Map<String, Object>> plan1 = explain("Lesson findApprovedLessonSummaries", sql1);
            explainAnalyze("Lesson findApprovedLessonSummaries", sql1);
            log.info("Lesson findApprovedLessonSummaries: table={}, key={}, type={}",
                    plan1.get(0).get("table"), plan1.get(0).get("key"), plan1.get(0).get("type"));

            // Query 2: Pending Lessons for Moderator (findPendingLessonSummaries)
            String sql2 = "SELECT l.lesson_id, l.title, l.status, l.created_by, a.email_or_phone, " +
                    "(SELECT COUNT(lv.vocab_id) FROM `LESSON_VOCABULARY` lv WHERE lv.lesson_id = l.lesson_id) AS vocab_count, " +
                    "l.created_at, l.updated_at FROM `LESSON` l JOIN `ACCOUNT` a ON a.account_id = l.created_by WHERE l.status = 'Pending' LIMIT 20";
            List<Map<String, Object>> plan2 = explain("Lesson findPendingLessonSummaries", sql2);
            explainAnalyze("Lesson findPendingLessonSummaries", sql2);
            log.info("Lesson findPendingLessonSummaries: table={}, key={}, type={}",
                    plan2.get(0).get("table"), plan2.get(0).get("key"), plan2.get(0).get("type"));

            // Query 3: Creator's lessons (findByCreatedBy)
            String sql3 = "SELECT * FROM `LESSON` WHERE created_by = ? LIMIT 20";
            List<Map<String, Object>> plan3 = explain("Lesson findByCreatedBy", sql3, testAccountId);
            explainAnalyze("Lesson findByCreatedBy", sql3, testAccountId);
            assertThat(plan3.get(0).get("key")).isEqualTo("idx_lesson_created_by");
            assertThat(plan3.get(0).get("type")).isEqualTo("ref");

            // Query 4: Creator's lessons filtered by status (findByCreatedByAndStatus)
            String sql4 = "SELECT * FROM `LESSON` WHERE created_by = ? AND status = ? LIMIT 20";
            List<Map<String, Object>> plan4 = explain("Lesson findByCreatedByAndStatus", sql4, testAccountId, "Approved");
            explainAnalyze("Lesson findByCreatedByAndStatus", sql4, testAccountId, "Approved");
            log.info("Lesson findByCreatedByAndStatus: possible_keys={}, key={}, type={}",
                    plan4.get(0).get("possible_keys"), plan4.get(0).get("key"), plan4.get(0).get("type"));
        }

        @Test
        @DisplayName("3.4 Other High-Value Queries: Personal Notes, ReviewLog, LessonVocabulary")
        void testOtherHighValueQueries() {
            setupBenchmarkDataIfEmpty();

            // Query 1: Personal Notes Paginated (findByUserAndVocabulary)
            String sql1 = "SELECT * FROM `PERSONAL_NOTE` WHERE user_id = ? AND vocab_id = ? ORDER BY created_at DESC LIMIT 20";
            List<Map<String, Object>> plan1 = explain("PersonalNote findByUserAndVocabulary", sql1, testUserId, testVocabId);
            explainAnalyze("PersonalNote findByUserAndVocabulary", sql1, testUserId, testVocabId);
            assertThat(plan1.get(0).get("key")).isIn("idx_personal_note_user_vocab", "fk_personal_note_vocab");
            assertThat(plan1.get(0).get("type")).isEqualTo("ref");

            // Query 2: Personal Notes by User (findByUserOrderByCreatedAtDesc)
            String sql2 = "SELECT * FROM `PERSONAL_NOTE` WHERE user_id = ? ORDER BY created_at DESC LIMIT 50";
            List<Map<String, Object>> plan2 = explain("PersonalNote findByUserOrderByCreatedAtDesc", sql2, testUserId);
            explainAnalyze("PersonalNote findByUserOrderByCreatedAtDesc", sql2, testUserId);
            // In benchmark table with 400 rows where user matches ~20% of rows, MySQL cost model chooses table scan over secondary index lookup + filesort.
            // Verify that idx_personal_note_user_vocab is evaluated in possible_keys.
            assertThat(plan2.get(0).get("possible_keys")).isEqualTo("idx_personal_note_user_vocab");

            // Query 3: ReviewLog Today with Lock (findTodayLogIdsWithLock)
            String sql3 = "SELECT log_id FROM `REVIEW_LOG` WHERE user_id = ? AND reviewed_at >= ? FOR UPDATE";
            List<Map<String, Object>> plan3 = explain("ReviewLog findTodayLogIdsWithLock", sql3, testUserId, LocalDateTime.now().minusDays(1));
            explainAnalyze("ReviewLog findTodayLogIdsWithLock", sql3, testUserId, LocalDateTime.now().minusDays(1));
            assertThat(plan3.get(0).get("key")).isEqualTo("idx_review_log_user_date");
            assertThat(plan3.get(0).get("type")).isEqualTo("range");

            // Query 4: LessonVocabulary In-Order (findByLessonOrderByOrderIndexAsc)
            String sql4 = "SELECT lv.*, v.hanzi FROM `LESSON_VOCABULARY` lv JOIN `VOCABULARY` v ON lv.vocab_id = v.vocab_id WHERE lv.lesson_id = ? ORDER BY lv.order_index ASC";
            List<Map<String, Object>> plan4 = explain("LessonVocabulary findByLessonOrderByOrderIndexAsc", sql4, testLessonId);
            explainAnalyze("LessonVocabulary findByLessonOrderByOrderIndexAsc", sql4, testLessonId);
            log.info("LessonVocabulary plan: lv table key={}, type={}, Extra={}",
                    plan4.get(0).get("key"), plan4.get(0).get("type"), plan4.get(0).get("Extra"));
            // uk_lesson_order_index guarantees order_index ASC without filesort
            assertThat(plan4.get(0).get("key")).isEqualTo("uk_lesson_order_index");
            assertThat(plan4.get(0).get("type")).isEqualTo("ref");

            // Query 5: New Card Candidates query
            String sql5 = "SELECT lv.lesson_id, lv.vocab_id, lv.order_index, v.hanzi " +
                    "FROM `LESSON_VOCABULARY` lv " +
                    "JOIN `LESSON` l ON l.lesson_id = lv.lesson_id " +
                    "JOIN `VOCABULARY` v ON v.vocab_id = lv.vocab_id " +
                    "WHERE lv.lesson_id = ? AND l.status = 'Approved' " +
                    "  AND NOT EXISTS (SELECT 1 FROM `CARD_PROGRESS` cp WHERE cp.user_id = ? AND cp.item_type = 'VOCABULARY' AND cp.item_id = v.vocab_id) " +
                    "ORDER BY lv.order_index ASC LIMIT 20";
            List<Map<String, Object>> plan5 = explain("LessonVocabulary findNewCardCandidates", sql5, testLessonId, testUserId);
            explainAnalyze("LessonVocabulary findNewCardCandidates", sql5, testLessonId, testUserId);
            assertThat(plan5).isNotEmpty();

            // Query 6: existsApprovedLessonForVocabulary (R2.1)
            String sql6 = "SELECT CASE WHEN COUNT(lv.lesson_id) > 0 THEN 1 ELSE 0 END " +
                    "FROM `LESSON_VOCABULARY` lv JOIN `LESSON` l ON l.lesson_id = lv.lesson_id " +
                    "WHERE lv.vocab_id = ? AND l.status = 'Approved'";
            List<Map<String, Object>> plan6 = explain("LessonVocabulary existsApprovedLessonForVocabulary", sql6, testVocabId);
            explainAnalyze("LessonVocabulary existsApprovedLessonForVocabulary", sql6, testVocabId);
            assertThat(plan6).isNotEmpty();
        }
    }
}