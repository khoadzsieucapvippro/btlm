# Two-Step Excel Import Workflow & UX Architecture

## 1. Architectural Boundary: Preview != Persistence

The Creator Excel Import system implements a strict **two-step transaction boundary** to protect database integrity and prevent partial or corrupt data persistence:

```text
Step 1 (Preview)                Step 2 (Confirm)
+-----------------------+       +-------------------------+
| Dropzone / File Select| ----> | User Reviews Report     | ----> [Atomic Save]
| (Max 10MB, .xlsx)     |       | Enters Lesson Title     |       (Creates Lesson 'Draft',
+-----------------------+       | Re-uploads File         |        Vocabularies, Relations)
           │                    +-------------------------+
           ▼                                 │
POST /api/v1/creator/                       ▼
   lessons/import               POST /api/v1/creator/
[ZERO DB MUTATION]             lessons/import/confirm
                                [TRANSACTIONAL PERSISTENCE]
```

### Strict UX Invariant
> **The UI MUST explicitly state that uploading a file in Step 1 does NOT save any data to the database.**
> Display a visible informational banner:
> `ℹ️ Bước Xem Trước: File đang được phân tích cú pháp để kiểm tra tính hợp lệ. Chưa có bất kỳ dữ liệu nào được lưu vào hệ thống.`

---

## 2. Step 1: Upload & Validation Preview

### API Contract
- **Endpoint**: `POST /api/v1/creator/lessons/import`
- **Method**: `POST`
- **Payload**: `multipart/form-data` with parameter `@RequestParam("file") MultipartFile file` (max 10MB, mime-type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).
- **File & Data Constraints**:
  - **File Size Limit**: Max 10MB (`10 * 1024 * 1024` bytes). Exceeding this limit triggers HTTP 413 Payload Too Large (`FILE_TOO_LARGE`).
  - **Data Rows Limit**: Max 5,000 data rows (`MAX_DATA_ROWS = 5000` excluding header row) per OWASP API4:2023 DoS prevention.
  - **Supported Header Aliases**: The parser normalizes accents and special characters, matching these column names:
    - *Hanzi*: `hanzi`, `chuhan`, `tuvung`, `chinese`, `chuhantu`
    - *Pinyin*: `pinyin`, `phienam`
    - *Meaning Han-Viet*: `meaninghanviet`, `hanviet`, `amhanviet`, `nghiahanviet`
    - *Meaning Vietnamese*: `meaningvi`, `meaning`, `nghia`, `nghiatiengviet`, `giainghia`, `vietnamesemeaning`
    - *Example Sentence*: `examplesentence`, `example`, `cauvidu`, `vidu`
    - *Example Translation*: `exampletranslation`, `translation`, `dichnghia`, `dichvidu`, `dichcauvidu`
- **Response**: `ApiResponse<ImportValidationReport>` (HTTP 200 OK)
  - **CRITICAL CONTRACT BEHAVIOR**: Row validation errors DO NOT trigger HTTP 4xx/5xx in Step 1. The backend returns **HTTP 200 OK** with `data.isValid = false`, `data.fileStatus = "INVALID"`, and detailed row errors in `data.errors[]`.
  ```json
  {
    "code": "SUCCESS",
    "message": "Thao tác thành công",
    "data": {
      "isValid": false,
      "totalRows": 25,
      "validRowsCount": 23,
      "invalidRowsCount": 2,
      "newVocabCount": 15,
      "existingVocabCount": 8,
      "fileStatus": "INVALID",
      "summaryMessage": "Phát hiện lỗi: 2/25 dòng không hợp lệ, 23 dòng hợp lệ",
      "rows": [
        {
          "rowNumber": 2,
          "hanzi": "你",
          "pinyin": "nǐ",
          "pinyinRaw": "ni",
          "meaningHanViet": "nhĩ",
          "meaningVi": "bạn, anh, chị",
          "exampleSentence": "你好！",
          "exampleTranslation": "Xin chào!",
          "isExisting": false,
          "existingVocabId": null,
          "isValid": true,
          "errors": []
        }
      ],
      "errors": [
        {
          "rowNumber": 4,
          "columnName": "pinyin",
          "errorCode": "INVALID_PINYIN",
          "errorMessage": "Pinyin không đúng định dạng thanh điệu"
        },
        {
          "rowNumber": 12,
          "columnName": "hanzi",
          "errorCode": "BLANK_HANZI",
          "errorMessage": "Thiếu chữ Hán"
        }
      ]
    },
    "errors": []
  }
  ```

### Frontend State & Preview Table Rendering
- Show upload progress bar or spinner with timeout of 60 seconds (`apiClient(..., { timeout: 60000 })`).
- Store the selected `File` object in local component state (e.g. `currentImportFile`) for re-submission in Step 2.
- If `data.isValid === false` or `data.invalidRowsCount > 0`:
  - Render an error alert detailing how many rows failed (`data.invalidRowsCount` out of `data.totalRows`).
  - Render the issues table from `data.errors` detailing `rowNumber`, `columnName`, and `errorMessage`.
  - Disable the `Xác nhận Import` button until a corrected file is provided.
- If `data.isValid === true` (or `data.invalidRowsCount === 0`):
  - Display a green badge: `Toàn bộ ${data.validRowsCount} từ vựng hợp lệ! (${data.newVocabCount} mới, ${data.existingVocabCount} đã có)`.
  - Enable the Lesson Title input field and the `Xác nhận tạo bài học` button.

---

## 3. Step 2: Confirm & Transactional Creation

### API Contract
- **Endpoint**: `POST /api/v1/creator/lessons/import/confirm`
- **Method**: `POST`
- **Payload**: `multipart/form-data` containing two fields:
  - `title` (String): Lesson title (**max 100 characters** enforced by `CreatorLessonServiceImpl:383-384`; note that manual lesson creation allows max 200 characters).
  - `file` (MultipartFile): The original `.xlsx` file
- **Backend Transaction**: Spring Boot `CreatorLessonController.importConfirm` delegates to `CreatorLessonService.importLessonFromExcel(title, file)` in a single atomic `@Transactional` operation:
  1. Creates the new `Lesson` with status `Draft`.
  2. Persists new `Vocabulary` entities (preserving Hanzi, Pinyin, PinyinRaw, meanings, example sentences).
  3. Inserts `LessonVocabulary` join records preserving the exact Excel row sequence (`order_index`).
- **Client Implementation Pattern**:
  ```javascript
  async function confirmExcelImport(lessonTitle, fileObject) {
    const formData = new FormData();
    formData.append('title', lessonTitle.trim());
    formData.append('file', fileObject);

    // Note: Do NOT set Content-Type header manually; browser sets boundary automatically
    const lessonDetail = await apiClient('/creator/lessons/import/confirm', {
      method: 'POST',
      body: formData,
      timeout: 60000
    });

    return lessonDetail; // LessonDetailResponse
  }
  ```
- **Client Handling**:
  - Disable the confirm button to prevent duplicate submission.
  - On HTTP 201 Created (`response.code === "SUCCESS"`): Show success toast and redirect to `creator-lesson-editor.html?id=${lessonDetail.lessonId}` (NOTE: use `lessonDetail.lessonId`, NOT `lessonDetail.id`).
  - On HTTP 400/409/422/500: Re-enable button, display exact failure message, zero orphan records left on backend.
