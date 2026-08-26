# Testing Anti-Patterns (Những sai lầm cần tránh khi viết Test)

**Load tham chiếu này khi:** Cần viết hoặc thay đổi test, dùng mocks, hoặc cần bổ sung các hàm helper/cleanup cho test.

## 1. Mirror Assertion (Assert qua gương)
Không được dùng chung một logic/builder cho cả đầu vào và kết quả kỳ vọng. Nếu code sai, test vẫn luôn xanh.
- ❌ **Sai:** `assertEquals(buildQuery(req), buildQuery(req))`
- ✅ **Đúng:** Dùng hardcoded/literal data. `assertEquals("tag:urgent", buildQuery(req))`

## 2. Change Detectors (Máy dò thay đổi, không phải test behavior)
Nếu test bị fail chỉ vì bạn thay đổi cấu trúc nội bộ, đổi tên biến private hoặc sửa wording của error message, thì đó là test tồi.
- ❌ **Sai:** `assertEquals(5, MAX_RETRIES)`
- ✅ **Đúng:** Test logic phụ thuộc vào nó. "Gọi failed function 6 lần thì lần 6 sẽ ném exception."

## 3. Your code, not the framework (Chỉ test code của mình)
Không viết test để chứng minh framework hoạt động. Ví dụ, không test việc Spring Boot `@RequestMapping` có map đúng đường dẫn không (đó là việc của team Spring Boot). 
- Chỉ test logic mapping, validation, và kết quả trả về ở boundary của Controller.

## 4. The mock earns no assertions (Không assert trên mock quá đà)
Một mock object sinh ra để phục vụ thay thế dependency chậm/ngoài hệ thống. Đừng viết test chỉ để chứng minh "mock đã được gọi", mà hãy chứng minh "component của mình hoạt động đúng khi mock trả về X".
- ❌ **Sai:** Chỉ `verify(mockRepository, times(1)).save(any())` mà không assert kết quả thực tế trả về cho frontend/DTO.
- ✅ **Đúng:** Stub repository trả về một user, gọi service, và `assertEquals` DTO trả về đúng format.

<!-- Adapted from obra/superpowers (MIT License) -->
