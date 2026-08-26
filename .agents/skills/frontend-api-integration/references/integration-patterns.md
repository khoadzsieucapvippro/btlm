# Frontend API Integration Patterns

## 1. Central HTTP Configuration & Token Attachment
Không gọi `fetch` trần trụi. Tạo một custom wrapper (hoặc dùng thư viện như axios/jquery tùy dự án).
Wrapper này chịu trách nhiệm:
1. Thêm Base URL từ config.
2. Đọc Token và thêm vào Header.
3. Bắt lỗi chung.

Ví dụ logic:
```javascript
function apiFetch(endpoint, options = {}) {
    const token = localStorage.getItem('jwt');
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    
    return fetch(BASE_URL + endpoint, { ...options, headers })
        .then(async response => {
            if (response.status === 401) {
                // 401 Handling: Logout & Redirect
                localStorage.removeItem('jwt');
                window.location.href = '/login.html';
                throw new Error("Unauthorized");
            }
            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                // Error normalization
                throw new Error(errData.message || "Lỗi máy chủ");
            }
            return response.json();
        });
}
```

## 2. Separation of API logic and DOM rendering
Tách Domain API thành file riêng.
Ví dụ `lesson-api.js`:
```javascript
const LessonApi = {
    getAll: () => apiFetch('/lessons'),
    create: (data) => apiFetch('/lessons', { method: 'POST', body: JSON.stringify(data) })
};
```
File UI (ví dụ `lessons.html` script):
Chỉ gọi hàm `LessonApi.getAll()` và nhận kết quả để `renderDOM()`. Không biết gì về JWT hay URL.

## 3. UI State Handling (Loading / Empty / Error)
**Form Submit Duplicate Prevention & Race Conditions**:
```javascript
let isSubmitting = false;

async function handleSave() {
    if (isSubmitting) return; // Prevent race condition / duplicate
    isSubmitting = true;
    showSpinner();
    clearErrors();
    
    try {
        const result = await LessonApi.create(data);
        showSuccess();
    } catch (error) {
        showErrorState(error.message);
    } finally {
        isSubmitting = false;
        hideSpinner();
    }
}
```
**Empty state**: Khi API trả về `[]`, phải check `data.length === 0` và hiển thị khối HTML "Không có dữ liệu".

## 4. API Contract Change Detection
Nếu backend đổi cấu trúc, API Module có nhiệm vụ bắt và map lại:
```javascript
// Nếu BE đổi từ { name: "A" } sang { title: "A" }
const LessonApi = {
    getAll: async () => {
        const raw = await apiFetch('/lessons');
        return raw.map(item => ({
            name: item.title, // Normalize!
            ...item
        }));
    }
}
```
Giúp UI code không bị crash hàng loạt.
