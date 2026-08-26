# Condition-Based Waiting (Chờ theo điều kiện)

**Load tham chiếu này khi:** Test hoặc logic Frontend gọi API bị chập chờn (flaky), lúc pass lúc fail do các vấn đề về timing/độ trễ mạng.

## 1. Overview
Lỗi flaky thường xảy ra do viết script dựa trên việc đoán thời gian chờ (arbitrary delays) bằng `setTimeout`, `Thread.sleep()`. Điều này tạo ra race condition: code chạy đúng trên máy mạnh, nhưng fail trên máy yếu hoặc khi mạng chậm.
**Nguyên tắc cốt lõi:** Chờ đợi chính xác cái điều kiện mà bạn cần, thay vì đoán xem mất bao lâu.

## 2. Core Pattern (Mẫu thiết kế)
Thay vì dùng hàm sleep cứng:

- ❌ **BEFORE: Đoán thời gian (Bad)**
```javascript
// Gửi API xong, đoán mất 2s để DOM cập nhật
await fetchData();
setTimeout(() => {
    let text = document.getElementById("result").innerText;
    console.assert(text === "Success");
}, 2000);
```

- ✅ **AFTER: Chờ theo điều kiện (Good)**
```javascript
// Chờ cho đến khi Element có text (tối đa 5s)
await fetchData();
await waitForCondition(() => document.getElementById("result").innerText !== "", 5000);
console.assert(document.getElementById("result").innerText === "Success");
```

## 3. Cách implement hàm waitForCondition (JavaScript)
Dùng cho Browser testing hoặc Frontend JS:
```javascript
async function waitForCondition(conditionFn, timeoutMs = 5000) {
    const startTime = Date.now();
    while (Date.now() - startTime < timeoutMs) {
        if (conditionFn()) {
            return true;
        }
        // Chờ 50ms rồi thử lại
        await new Promise(r => setTimeout(r, 50));
    }
    throw new Error("Timeout waiting for condition");
}
```

## 4. Trong Java (Backend Integration Test)
Thay vì dùng `Thread.sleep(2000)`, hãy dùng thư viện Awaitility (nếu có) hoặc tự viết loop tương tự, hoặc sử dụng cơ chế event/callback.

<!-- Adapted from obra/superpowers (MIT License) -->
