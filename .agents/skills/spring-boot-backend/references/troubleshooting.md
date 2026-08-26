# Backend Troubleshooting Matrix

## Symptom 1: Lỗi "No default constructor" / "Cannot construct instance"
- **Nguyên nhân**: DTO hoặc Entity thiếu empty constructor cho Jackson parse JSON.
- **Diagnosis & Fix**: Thêm `@NoArgsConstructor` (Lombok) hoặc tự tạo constructor rỗng.

## Symptom 2: Lỗi "Failed to lazily initialize a collection" (LazyInitializationException)
- **Nguyên nhân**: Bạn cố đọc list liên kết (như `user.getLessons()`) khi session Hibernate đã đóng (trong Controller).
- **Diagnosis & Fix**: Đưa vòng lặp đọc data vào bên trong Service (nơi còn `@Transactional`), HOẶC dùng `JOIN FETCH` trong Repository.

## Symptom 3: Ứng dụng Start Crash "UnsatisfiedDependencyException"
- **Nguyên nhân**: Spring không tìm thấy Bean để Inject (Lỗi AutoWired).
- **Diagnosis & Fix**: 
  - Quên đánh `@Service`, `@Repository` hoặc `@Component`.
  - Quên implement interface.
  - Vòng lặp Inject tròn (Circular Dependency: A gọi B, B gọi A) -> Phải tách logic ra class thứ 3.

## Symptom 4: "Infinite Recursion (StackOverflowError)"
- **Nguyên nhân**: Jackson cố Serialize Object A, Object A trỏ Object B, B trỏ lại A.
- **Diagnosis & Fix**: Trả DTO ra ngoài thay vì Entity, HOẶC dùng `@JsonIgnore` / `@JsonManagedReference` (Chỉ dùng tạm, DTO vẫn là chuẩn).
