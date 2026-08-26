# Distinct Product Surfaces: User vs Admin

## 1. Bản chất sự khác biệt
Hệ thống eLearning này chia thành 2 bề mặt sản phẩm (Product Surfaces) hoàn toàn khác nhau. KHÔNG ĐƯỢC thiết kế gộp chung hay lấy râu ông nọ cắm cằm bà kia.

### A. USER INTERFACE (Bề mặt Học Tập)
- **Mục tiêu**: Tối ưu sự tập trung, tương tác cao, game hóa (gamification).
- **Tính năng**: Xem bài học, Học từ vựng, Xem bộ thủ, Flashcard tương tác, Quiz, Thanh Tiến độ (Progress Bar).
- **Nguyên tắc UI**:
  - Không nhồi nhét dữ liệu vào bảng (Tables).
  - Dùng dạng Card, Grid.
  - Phím to, rõ, có animation (WOW/OwlCarousel hiện có) để khen ngợi/báo sai.
  - Typograpy to, rõ ràng đặc biệt cho chữ Hanzi / Pinyin.

### B. ADMIN INTERFACE (Bề mặt Quản trị)
- **Mục tiêu**: Tối ưu năng suất, mật độ dữ liệu cao, thao tác nhanh.
- **Tính năng**: Dashboard tổng quan, CRUD (Tạo/Xem/Sửa/Xóa) User, Quản lý Lesson, Moderation, Thống kê.
- **Nguyên tắc UI**:
  - Sidebar cố định, Topbar chứa Breadcrumb.
  - Tích cực sử dụng DataTables.
  - Form nhập liệu dài, nhiều tab.
  - Có các Modal xác nhận (Confirmation) khi Delete.
  - Ít animation thừa, ưu tiên tốc độ hiển thị dữ liệu.
