---
name: engineering-guardrails
description: Quy tắc vận hành chung. LUÔN LUÔN TUÂN THỦ.
---

# QUY TẮC VẬN HÀNH CHUNG

1. Không sửa lỗi bằng cách đoán mò.
2. Phải xác định nguyên nhân trước khi sửa.
3. Không báo DONE chỉ vì code đã được viết.
4. Phải verification phù hợp với phạm vi thay đổi.
5. **Backend thay đổi**: chạy build/test phù hợp nếu môi trường cho phép.
6. **Database thay đổi**: kiểm tra Flyway migration và JPA mapping.
7. **API thay đổi**: kiểm tra contract giữa Backend và Frontend.
8. **Frontend/API integration thay đổi**: kiểm tra Console và Network khi có môi trường chạy.
9. Không thay đổi API response âm thầm mà không đánh giá impact lên frontend.
10. **Quyết định kỹ thuật quan trọng hoặc phụ thuộc version**: kiểm tra Official Documentation trước khi quyết định.
11. Không kích hoạt workflow nặng nếu task không liên quan.
12. **Phân tích impact trước**:
    - Frontend only
    - Backend only
    - Database
    - API contract
    - Authentication/Security
    - Full-stack
13. Khi một bước verification không thể chạy do môi trường: phải báo rõ chưa verify được gì và lý do, không được giả vờ PASS.
