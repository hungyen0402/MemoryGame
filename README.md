# MEMORYGAME Project

Đây là dự án game kiểm tra trí nhớ, sử dụng Java (JavaFX, Socket, JDBC) cho phần mềm và MySQL cho cơ sở dữ liệu.

## 🚀 Hướng dẫn cài đặt và chạy (Phía Java)

Phần này dành cho việc chạy chương trình (Server và Client) sau khi CSDL đã được thiết lập.

1.  **Clone dự án:**
    ```bash
    git clone [ĐƯỜNG DẪN REPO CỦA BẠN]
    ```
2.  **Mở dự án:**
    * Mở dự án bằng IntelliJ IDEA hoặc Eclipse.
    * Chọn "Open..." và trỏ vào thư mục `MEMORYGAME`.
    * IDE sẽ tự động nhận diện file `pom.xml` và hiểu đây là một dự án Maven.
3.  **Đồng bộ Maven:**
    * Chờ IDE tự động tải về các thư viện (dependencies) đã khai báo trong `pom.xml` (như JavaFX, MySQL Connector cho Java, Gson...).
4.  **Cấu hình CSDL cho Java:**
    * Tìm file `src/main/resources/config.properties`.
    * Sửa lại `db.password` thành mật khẩu CSDL của bạn.
5.  **Chạy chương trình:**
    * Chạy class `ServerApp.java` (hoặc tên class Server chính của bạn) để khởi động máy chủ.
    * Chạy class `ClientApp.java` (hoặc tên class Client chính của bạn) để khởi động game.

---

## ⚠️ THIẾT LẬP CSDL (Chỉ chạy 1 lần đầu)

**QUAN TRỌNG:** Trước khi có thể chạy dự án Java, bạn phải cài đặt MySQL và nạp dữ liệu từ vựng.

### 1. Cài đặt MySQL Server

* Đảm bảo bạn đã cài đặt **MySQL Server** (phiên bản 8.0+) và **MySQL Workbench**.
* Đảm bảo dịch vụ (service) MySQL đang chạy.

### 2. Tạo Cấu trúc Bảng (Schema)

1.  Mở **MySQL Workbench** và kết nối vào CSDL (ví dụ: `Local instance MySQL80`).
2.  Vào menu **File > Open SQL Script...**
3.  Tìm và mở file `database/data/scheme.sql` trong dự án này.
4.  Nhấn biểu tượng **sấm sét (⚡)** để chạy toàn bộ file.
5.  Thao tác này sẽ tạo ra CSDL `memory_game_db` và 3 bảng: `Player`, `Vocabulary`, `MatchHistory`.

### 3. Nạp Dữ liệu Từ vựng (Bằng kịch bản Python)

Dự án này sử dụng một kịch bản Python để tự động đọc file `.docx` và nạp từ vựng vào CSDL.

1.  **Cài đặt Python:** Đảm bảo bạn đã cài Python 3.
2.  **📦 Cài đặt thư viện Python:** Mở `cmd` (Terminal) và chạy lệnh sau để cài các thư viện cần thiết:
    ```bash
    pip install pymysql python-docx cryptography
    ```
3.  **🔑 Cấu hình mật khẩu (Bảo mật):**
    * Trong thư mục `database/data/`, tìm file `config.py.example`.
    * Tạo một bản sao của file này và đổi tên thành `config.py` (File này đã được thêm vào `.gitignore` và sẽ không bị đẩy lên GitHub).
    * Mở file `config.py` và điền mật khẩu CSDL của bạn vào biến `db_password`.
4.  **🏃 Chạy kịch bản:**
    * Mở `cmd` (Terminal) và `cd` (di chuyển) vào đúng thư mục chứa kịch bản:
        ```bash
        cd duong/dan/toi/MEMORYGAME/database/data
        ```
    * Chạy file Python:
        ```bash
    python english_phrases.py
        ```
5.  Script sẽ kết nối CSDL, đọc file `vocabulary.docx`, và tự động nạp tất cả các cụm từ tiếng Anh vào bảng `Vocabulary`.

**Bây giờ CSDL của bạn đã hoàn toàn sẵn sàng!** Bạn có thể quay lại và thực hiện các bước trong phần "Hướng dẫn cài đặt và chạy (Phía Java)".

---

### Lưu ý khi chạy lại kịch bản Python

Nếu bạn muốn chạy lại kịch bản `english_phrases.py` để nạp lại dữ liệu, bạn phải xóa dữ liệu cũ trước. Dùng lệnh sau trong MySQL Workbench để xóa sạch bảng:

```sql
USE memory_game_db;
TRUNCATE TABLE Vocabulary;