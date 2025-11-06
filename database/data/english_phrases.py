import pymysql
from docx import Document
import re  # <-- 1. Chúng ta cần thư viện 're' (Regular Expression)
import config
import sys

print("Bắt đầu kịch bản nạp từ vựng (Dùng PyMySQL) [V2 - Sửa lỗi logic]...")

mydb = None
cursor = None

try:
    connected = False 
    try:
        print("Đang đọc file config.py...")
        db_host = config.db_host 
        db_user = config.db_user
        db_pass = config.db_password
        db_name = config.db_name
        print(f"Đã đọc xong. Đang thử kết nối tới: {db_user}@{db_host} | CSDL: {db_name}")

        mydb = pymysql.connect(
          host=db_host,
          user=db_user,
          password=db_pass,
          database=db_name,
          connect_timeout=5
        )
        cursor = mydb.cursor()
        connected = True
        print("\n=== KẾT NỐI MYSQL THÀNH CÔNG! ===\n")
    except Exception as e:
        print(f"\n--- LỖI KẾT NỐI ---: {e}")
        print("Vui lòng kiểm tra file config.py (mật khẩu, tên CSDL) hoặc đảm bảo thư viện 'cryptography' đã được cài (pip install cryptography)")
        sys.exit()

    # --- PHẦN 2 & 3: ĐỌC FILE VÀ NẠP DỮ LIỆU (ĐÃ SỬA LOGIC) ---
    if connected:
        try:
            sql_query = "INSERT INTO Vocabulary (phrase, length) VALUES (%s, %s)"
            doc = Document('vocabulary.docx') 
            count = 0
            print("Bắt đầu đọc file vocabulary.docx (Logic mới)...")
            
            for para in doc.paragraphs:
                raw_text = para.text.strip()
                
                # --- 2. ĐÂY LÀ LOGIC MỚI (THAY ĐỔI QUAN TRỌNG) ---
                # Chỉ xử lý các dòng BẮT ĐẦU BẰNG MỘT CON SỐ (ví dụ: "1 ", "133 ")
                # re.match(r'^\d+\s', ...) có nghĩa là: 
                # ^     = Bắt đầu của dòng
                # \d+   = Một hoặc nhiều chữ số
                # \s    = Một khoảng trắng
                
                if raw_text and re.match(r'^\d+\s', raw_text):
                    
                    # Tách cụm từ ra khỏi số ở đầu tiên
                    parts = raw_text.split(' ', 1)
                    
                    # Đảm bảo rằng có 2 phần (số và cụm từ)
                    if len(parts) == 2:
                        phrase = parts[1].strip() # Lấy phần text (Tiếng Anh)
                        
                        # Bỏ qua các dòng trống (ví dụ: "150" không có gì)
                        if phrase:
                            # Tính độ dài không bao gồm dấu cách
                            length = len(phrase.replace(" ", ""))
                            
                            try:
                                cursor.execute(sql_query, (phrase, length))
                                count += 1
                            except Exception as e:
                                print(f"Bỏ qua (lỗi khi chèn): {phrase} | Lỗi: {e}")
                    else:
                        print(f"Bỏ qua (dòng chỉ có số): {raw_text}")
                # Nếu dòng không bắt đầu bằng số (ví dụ: "Dĩ nhiên") -> tự động bỏ qua
            
            mydb.commit() # Lưu thay đổi vào CSDL
            print(f"\n=== HOÀN TẤT! Đã nạp thành công {count} cụm từ vựng (Tiếng Anh)! ===")

        except FileNotFoundError:
            print(f"\n--- LỖI TÌM FILE ---")
            print(f"Không tìm thấy file 'vocabulary.docx'.")
        except Exception as e:
            print(f"Có lỗi nghiêm trọng xảy ra trong PHẦN 2/3: {e}")
            if mydb:
                mydb.rollback()

finally:
    if mydb and mydb.open:
        cursor.close()
        mydb.close()
        print("\nĐã đóng kết nối MySQL.")
    else:
        print("\nScript kết thúc.")