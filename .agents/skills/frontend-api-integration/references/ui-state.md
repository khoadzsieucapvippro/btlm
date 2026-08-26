# UI State Management

## 1. Double Submit Protection & Race Conditions
Ngăn ngừa người dùng bấm liên tục vào nút "Lưu" gây ra hàng loạt API calls.

```javascript
let isSubmitting = false;

async function handleSubmit(event) {
    event.preventDefault();
    
    if (isSubmitting) return; // Chặn Double Submit
    
    const btn = document.getElementById('btnSubmit');
    const spinner = document.getElementById('spinner');
    
    // UI State: PRE
    isSubmitting = true;
    btn.disabled = true;
    spinner.classList.remove('d-none');
    hideErrorBox();
    
    try {
        const result = await UserApi.create(data);
        showSuccessMessage("Tạo thành công!");
    } catch (error) {
        // UI State: ERROR
        showErrorBox(error.message);
    } finally {
        // UI State: POST (Luôn luôn thực thi dù success hay error)
        isSubmitting = false;
        btn.disabled = false;
        spinner.classList.add('d-none');
    }
}
```

## 2. Empty State
- Nếu gọi list API trả về `[]` (Mảng rỗng), không được để màn hình trống trơn.
- Phải hiển thị UI báo hiệu cho người dùng:
```javascript
if (data.items.length === 0) {
    document.getElementById('tableContainer').innerHTML = 
        `<div class="text-center p-5 text-muted">
            <i class="bi bi-inbox fs-1"></i>
            <p>Không có dữ liệu</p>
         </div>`;
    return;
}
```
