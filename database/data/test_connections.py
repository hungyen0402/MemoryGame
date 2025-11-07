import mysql.connector

print("--- BẮT ĐẦU SCRIPT TEST V2 (CÓ TIMEOUT) ---")

db_host = "127.0.0.1"
db_user = "root"
db_pass = "duchung2004@@" # <--- ĐIỀN MẬT KHẨU CỦA BẠN
db_name = "memory_game_db"

print(f"Đang thử kết nối... (Sẽ tự hủy nếu quá 5 giây)")

# === KHÁC BIỆT Ở ĐÂY ===
# Thêm 'connect_timeout=5' để buộc nó phải lỗi sau 5 giây nếu bị treo
try:
    mydb = mysql.connector.connect(
      host=db_host,
      user=db_user,
      password=db_pass,
      database=db_name,
      connect_timeout=5  # <-- Thêm thời gian chờ 5 giây
    )
    
    # Nếu bạn thấy dòng này, bạn đã THÀNH CÔNG
    print("\n=== KẾT NỐI THÀNH CÔNG! ===\n")
    mydb.close()

except mysql.connector.Error as err:
    # Nó sẽ 100% nhảy vào đây nếu thất bại
    print("\n--- SCRIPT ĐÃ CRASH VÀ BÁO LỖI ---")
    
    if err.errno == 2003: # Lỗi 'Can't connect'
        print(f"LỖI: {err}")
        print("\nVẤN ĐỀ: Không thể kết nối tới 'localhost' (port 3306).")
        print("GIẢI PHÁP: Rất có thể Windows Firewall đang chặn Python. Khi chạy script, hãy tìm cửa sổ pop-up của Firewall và nhấn 'Allow access'.")
    elif err.errno == 1045: # Lỗi 'Access Denied'
        print(f"LỖI: {err}")
        print("\nVẤN ĐỀ: Sai MẬT KHẨU (password).")
    elif err.errno == 1049: # Lỗi 'Unknown Database'
        print(f"LỖI: {err}")
        print(f"\nVẤN ĐỀ: Sai TÊN DATABASE. Script đang tìm '{db_name}', hãy kiểm tra lại.")
    else:
        print(f"LỖI KHÁC: {err}")

except Exception as e:
    print(f"Lỗi lạ: {e}")

print("\n--- SCRIPT KẾT THÚC ---")