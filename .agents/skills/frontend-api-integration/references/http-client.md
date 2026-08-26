# Central HTTP Client & Fetch Patterns

## 1. Mẫu Central Fetch Wrapper
Thay vì gọi `fetch('http://localhost.../api/x')` khắp nơi, hãy tạo một Wrapper tập trung (VD: `js/core/apiClient.js`):

```javascript
const BASE_URL = 'http://localhost:8080/api';

async function apiClient(endpoint, options = {}) {
    const token = localStorage.getItem('token');
    
    // Default headers
    const headers = { ...options.headers };
    
    // Nếu Body không phải FormData, set Content-Type JSON
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = headers['Content-Type'] || 'application/json';
    }
    
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = { ...options, headers };

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, config);
        
        // 401 Handling Global
        if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login.html';
            throw new Error("Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.");
        }
        
        // 403, 400, 422, 500 Handling Global
        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.message || `Lỗi HTTP ${response.status}`);
        }
        
        // Kiểm tra xem backend trả về rỗng hay JSON
        const contentType = response.headers.get("content-type");
        if (contentType && contentType.indexOf("application/json") !== -1) {
            return await response.json();
        } else {
            return await response.text();
        }
    } catch (error) {
        // Ném lỗi ra để Domain API Module bắt và xử lý tiếp
        console.error("API Client Error:", error);
        throw error;
    }
}
```

## 2. File Upload (FormData Handling)
- Khi dùng `FormData` để upload file, **TUYỆT ĐỐI KHÔNG** set `Content-Type: application/json` hay tự ý set `Content-Type: multipart/form-data`.
- Trình duyệt sẽ tự động set đúng `multipart/form-data` và inject `boundary` token vào header. Wrapper bên trên đã tính toán trường hợp này.
