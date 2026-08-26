# API Contract Mismatch Troubleshooting

## 1. Tình huống thực tế
- Backend thay đổi cấu trúc JSON Response (Ví dụ: Đổi `content` thành `data`, hoặc lồng thêm lớp phân trang).
- Frontend đang cố đọc data từ DOM render function và bị lỗi: `TypeError: Cannot read properties of undefined (reading 'map')`.

## 2. Chẩn đoán (Diagnosis)
1. Bật F12 -> Network -> Xem API Response trả về cái gì.
2. So sánh với code ở hàm Render UI.

## 3. Cách khắc phục chuẩn
- **Sai Lầm**: Lục lọi sửa rải rác từng file HTML hoặc hàm Render DOM.
- **Chuẩn Mực**: Sửa ở lớp giao thoa (Lớp Normalize Error/Normalize Data của API Module).

Ví dụ sửa lỗi tại tầng Module (`lesson-api.js`):
```javascript
const LessonApi = {
    getAll: async () => {
        const raw = await apiClient('/lessons');
        
        // Backend mới đổi từ trả mảng Array[] sang { success: true, data: { items: [] } }
        // Ta Normalize (chuẩn hóa) lại data trước khi trả về cho UI
        
        if (raw.data && raw.data.items) {
            return raw.data.items; // UI vẫn nhận được mảng như cũ, ko bị sập
        }
        
        return raw; 
    }
}
```
