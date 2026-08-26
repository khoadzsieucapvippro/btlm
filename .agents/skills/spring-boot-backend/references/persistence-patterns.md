# Persistence Patterns & JPA

## 1. Vấn nạn N+1 Queries
- **Dấu hiệu**: Get 10 bài học (1 query), mỗi bài học gọi get Tác giả (10 queries). Tổng = 11 queries.
- **Phòng chống**:
  - Đặt MẶC ĐỊNH mọi liên kết `@OneToMany`, `@ManyToOne` là `FetchType.LAZY`.
  - Khi cần lấy ra cho DTO, dùng **Fetch Join**:
    `@Query("SELECT l FROM Lesson l JOIN FETCH l.author")`
  - Hoặc dùng `@EntityGraph(attributePaths = {"author"})`.

## 2. Pagination & Sorting
- Thay vì lấy toàn bộ list, hãy dùng `Pageable`:
  ```java
  // Controller
  @GetMapping
  public ResponseEntity<Page<LessonResponseDTO>> getLessons(Pageable pageable) { ... }
  
  // Repository
  Page<Lesson> findAll(Pageable pageable);
  ```
- Map từ `Page<Lesson>` sang `Page<LessonResponseDTO>` bằng hàm `.map()`.

## 3. Transaction Boundaries
- Đánh `@Transactional` trên hàm của **Service**, KHÔNG đánh trên Controller.
- Hàm chỉ đọc data: `@Transactional(readOnly = true)` để tối ưu Hibernate Cache.
- Nếu gọi hàm `private` cùng class có gắn `@Transactional`, transaction SẼ KHÔNG HOẠT ĐỘNG do cơ chế Spring AOP Proxy. Phải gọi từ bean khác hoặc hàm public của interface.
