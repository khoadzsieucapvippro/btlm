# DOM Rendering & Event Patterns

## 1. Event Delegation
- Tránh gắn sự kiện cho hàng trăm dòng trong bảng sinh ra bởi JS. Dùng Event Delegation (Gắn sự kiện vào thằng cha).

**Cách đúng (Vanilla JS)**:
```javascript
document.getElementById('lessonTableBody').addEventListener('click', function(e) {
    if (e.target.closest('.btn-delete')) {
        const id = e.target.closest('.btn-delete').dataset.id;
        handleDelete(id);
    }
});
```
**Cách đúng (jQuery)**:
```javascript
$('#lessonTableBody').on('click', '.btn-delete', function() {
    const id = $(this).data('id');
    handleDelete(id);
});
```

## 2. Tránh XSS khi render từ Data (Vanilla)
- Dùng `textContent` thay vì `innerHTML` nếu dữ liệu đó người dùng tự nhập được (Ví dụ: tên user).
- Dùng `innerHTML` cho thẻ bao bọc (Wrapper), kết hợp Template Literal.

```javascript
function renderUsers(users) {
    const tbody = document.getElementById('userTable');
    tbody.innerHTML = ''; // Clear cũ
    
    users.forEach(u => {
        const tr = document.createElement('tr');
        
        const tdName = document.createElement('td');
        tdName.textContent = u.name; // Tránh XSS
        
        const tdAction = document.createElement('td');
        tdAction.innerHTML = `<button data-id="${u.id}" class="btn btn-sm btn-danger btn-delete">Xóa</button>`;
        
        tr.appendChild(tdName);
        tr.appendChild(tdAction);
        tbody.appendChild(tr);
    });
}
```

## 3. Bootstrap 5 Modals (No jQuery)
Bootstrap 5 không còn phụ thuộc jQuery. Dùng JS thuần để thao tác Modal.
```javascript
// Bật Modal
const deleteModal = new bootstrap.Modal(document.getElementById('deleteModal'));
deleteModal.show();

// Tắt Modal
deleteModal.hide();
```
