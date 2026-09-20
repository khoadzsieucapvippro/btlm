# Persistence Patterns & JPA

## 1. Vấn nạn N+1 Queries
- **Dấu hiệu**: Get 10 bài học (1 query), mỗi bài học gọi get Tác giả (10 queries). Tổng = 11 queries.
- **Phòng chống**:
  - Đặt MẶC ĐỊNH mọi liên kết `@OneToMany`, `@ManyToOne` là `FetchType.LAZY`.
  - Khi cần lấy ra cho DTO, dùng **Fetch Join**:
    `@Query("SELECT l FROM Lesson l JOIN FETCH l.author")`
  - Hoặc dùng `@EntityGraph(attributePaths = {"author"})`.

## 2. Pagination & Sorting (`PageResponse<T>`)
- Thay vì trả thẳng `Page<T>` của Spring Data ra HTTP (gây rò rỉ field nội bộ `number`, `content`), Controller bắt buộc phải bọc trong `PageResponse<T>` và phong bì `ApiResponse<PageResponse<T>>` (Invariant 6):
  ```java
  // Controller
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<LessonSummaryResponse>>> getLessons(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size) {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      PageResponse<LessonSummaryResponse> response = lessonService.getLessons(pageable);
      return ResponseEntity.ok(ApiResponse.success(response));
  }
  
  // Repository
  Page<Lesson> findAll(Pageable pageable);
  ```
- Map từ `Page<Lesson>` sang DTO và đóng gói vào `PageResponse<T>` (`page`, `size`, `totalElements`, `totalPages`, `items`), đảm bảo API contract ổn định cho Frontend.

## 3. Transaction Boundaries
- Đánh `@Transactional` trên hàm của **Service**, KHÔNG đánh trên Controller.
- Hàm chỉ đọc data: `@Transactional(readOnly = true)` để tối ưu Hibernate Cache.
- Nếu gọi hàm `private` cùng class có gắn `@Transactional`, transaction SẼ KHÔNG HOẠT ĐỘNG do cơ chế Spring AOP Proxy. Phải gọi từ bean khác hoặc hàm public của interface.

## 4. Concurrency & Locking Patterns (Project Hardening)
- **Optimistic Locking (`@Version`)**:
  - `CardProgress`: sử dụng `@Version private Long version;` để bảo vệ chống lost-update khi người học review thẻ SRS đồng thời trên nhiều tab hoặc thiết bị.
  - `Lesson`: sử dụng `@Version private Long version;` để bảo vệ chống xung đột giữa Creator chỉnh sửa bài học và Moderator duyệt/từ chối bài học đồng thời.
- **Pessimistic Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)**:
  - `AccountRepository.findByIdForUpdate(accountId)`: khóa bi quan cấp hàng (`SELECT ... FOR UPDATE`) trong `AdminAccountServiceImpl` và `AdminRoleServiceImpl` nhằm tuần tự hóa các thay đổi trạng thái tài khoản (`Active` <-> `Inactive`/`Banned`) và gán vai trò, ngăn chặn race condition và bảo đảm tính lũy đẳng (idempotency).
  - `VocabularyRepository.findByIdWithLock(vocabId)`: khóa bi quan trong `VocabularyServiceImpl` để bảo vệ kiểm tra ràng buộc trước khi xóa từ vựng, chống xung đột TOCTOU (Time-Of-Check to Time-Of-Use) khi thẻ học đang được tạo đồng thời.
- **Review Quota & Learning Integrity**:
  - `SrsServiceImpl.reviewCard()` thực thi kiểm tra hạn ngạch học tập hàng ngày (`newCardsPerDay`, `maxReviewPerDay`) và tính hợp lệ của bài học đã phê duyệt (`Approved` status check) bên trong ranh giới `@Transactional`, bảo đảm tính toàn vẹn của chu kỳ SM-2.
- **Token Invalidation on Mutation (`authorization_version`)**:
  - Khi cập nhật vai trò hoặc trạng thái tài khoản, gọi `account.incrementAuthorizationVersion()` dưới khóa bi quan để vô hiệu hóa tức thì các token JWT đang lưu hành qua chốt chặn `JwtAuthenticationFilter` (DEC-42).
