# Backend Design Patterns & Implementations

## 1. Layer Responsibilities
- **Controller Responsibility**:
  - KHÔNG chứa business logic.
  - Nhận Request, kích hoạt Validation (`@Valid`).
  - Lấy Request DTO, gọi Service tương ứng.
  - Nhận data từ Service, map sang Response DTO (nếu Service chưa map).
  - Định tuyến các status code HTTP.
- **Service Responsibility**:
  - Chứa 100% Business Logic.
  - Xử lý transaction (`@Transactional`).
  - Bắn ra custom Exception (`ResourceNotFoundException`, `BusinessException`) nếu logic sai.
- **Repository Usage**:
  - Giao tiếp với Database qua Spring Data JPA interfaces.
  - Chỉ chứa JPQL/Native SQL queries, không chứa if-else logic.

## 2. DTO Boundaries & Validation
- **Entity**: Đại diện cho bảng Database. KHÔNG ĐƯỢC return trực tiếp ra HTTP Response (để tránh lọt thông tin nhạy cảm và Infinite Recursion).
- **DTO**: Đại diện cho API Contract. Phải dùng DTO để hứng request và trả response.
- **Validation**: Đặt JSR-380 (`@NotBlank`, `@Size`) vào Request DTO.

## 3. Global Exception Handling
- Tạo `@RestControllerAdvice`.
- Chặn các `EntityNotFoundException` và trả HTTP 404 chuẩn JSON.
- Chặn các `MethodArgumentNotValidException` và trả HTTP 400 kèm mảng chi tiết lỗi các field.
- Không để Tomcat ném ra error HTML page mặc định.

## 4. Entity Relationship & N+1 Risk
- **Rủi ro**: Khi `@OneToMany` hoặc `@ManyToOne` để `EAGER`, hoặc vòng lặp query `LAZY` trên collection, DB sẽ bị dội hàng trăm query (N+1).
- **Khắc phục**: 
  - Mặc định dùng `FetchType.LAZY`.
  - Khi cần load data liên kết để trả DTO, dùng `@Query("SELECT e FROM Entity e JOIN FETCH e.relation")` hoặc `@EntityGraph`.

## 5. Pagination
- Frontend gọi: `?page=0&size=10`.
- Controller nhận: `Pageable pageable`.
- Repository định nghĩa: `Page<Entity> findAll(Pageable pageable)`.
- Kết quả map thành `Page<DTO>` và trả ra cục metadata.

## 6. Test Strategy
- Service: Dùng `@ExtendWith(MockitoExtension.class)`, `@Mock` cho repository. Unit test logic độc lập.
- Controller: Dùng `@WebMvcTest`, dùng `@MockBean` (hoặc `@MockitoBean` tuỳ phiên bản Boot) để mock Service. Kiểm tra HTTP Status, JSON Match.
- Repository: Dùng `@DataJpaTest`, sử dụng Data.sql hoặc code insert trực tiếp để test query.
