import pymysql # <-- Dùng thư viện mới
import sys

print("--- BẮT ĐẦU SCRIPT TEST (Dùng PyMySQL) ---")

# === HÃY ĐIỀN TRỰC TIẾP THÔNG TIN CỦA BẠN VÀO ĐÂY ===
db_host = "localhost"
db_user = "root"
db_pass = "duchung2004@@" # <--- ĐIỀN MẬT KHẨU CỦA BẠN
db_name = "memory_game_db"
# ======================================================

print(f"Đang thử kết nối tới: {db_user}@{db_host} | CSDL: {db_name}")

try:
    # Lệnh kết nối của PyMySQL (hơi khác một chút)
    mydb = pymysql.connect(
      host=db_host,
      user=db_user,
      password=db_pass,
      database=db_name,
      connect_timeout=5
    )
    
    print("\n=== KẾT NỐI THÀNH CÔNG! (PyMySQL hoạt động!) ===\n")
    
    mydb.close()

except pymysql.Error as err:
    print("\n--- SCRIPT ĐÃ CRASH VÀ BÁO LỖI (PyMySQL) ---")
    print(f"LỖI: {err}")
    print("\n(Nếu lỗi là 'Access Denied', là sai mật khẩu.)")
    print("(Nếu lỗi là 'Unknown database', là sai tên CSDL.)")
    
except Exception as e:
    print(f"Lỗi lạ: {e}")

print("\n--- SCRIPT KẾT THÚC ---")