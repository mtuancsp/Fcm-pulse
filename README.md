# FCM Pulse

App Android nhỏ, không cần root. Mỗi N phút, một alarm đánh thức app trong vài giây để gửi
broadcast heartbeat tới Google Play Services (`MCS_HEARTBEAT`, `GTALK_HEARTBEAT`), rồi tự đặt
alarm kế tiếp. Không có foreground service, không có thông báo thường trực.

> Các broadcast này **không phải API công khai**. Chưa kiểm chứng trên ColorOS 16 / Play Services
> hiện tại. App có sẵn nhật ký và nút kiểm tra kết nối để bạn tự xác nhận.

## Lấy file APK

### Cách 1: để GitHub build (không cần cài gì)
1. Tạo repo GitHub mới (có thể để private), đẩy toàn bộ thư mục này lên nhánh `main`.
2. Vào tab **Actions** → workflow **Build APK** → chạy xong thì tải artifact `fcm-pulse-apk`.
3. Giải nén, chép `app-debug.apk` sang điện thoại và cài (cho phép "cài app không rõ nguồn").

### Cách 2: build trên máy tính
Cần JDK 17, Android SDK (platform 34, build-tools 34.0.0) và Gradle 8.9 trở lên.

```
echo "sdk.dir=/duong/dan/toi/Android/Sdk" > local.properties
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Hoặc mở thư mục này bằng Android Studio rồi chọn Build → Build APK(s).

## Cách dùng
1. Mở app, bấm **Bỏ tối ưu hóa pin cho app này**.
2. Trong cài đặt ứng dụng của ColorOS: Pin = Không hạn chế, bật Tự khởi động, khoá app trong
   màn hình đa nhiệm.
3. Nhập chu kỳ tuỳ ý từ 1 đến 1440 phút (bắt đầu với 15), tuỳ chọn giờ yên tĩnh ban đêm theo
   từng 30 phút (ví dụ 23:30 đến 06:30), bấm Lưu và áp dụng, rồi bật công tắc.
4. Bấm **Gửi heartbeat ngay**, mở nhật ký xem có dòng "đã gửi 2/2 broadcast".
5. Xác nhận từ máy tính (một lần): `adb logcat | grep -i -E "gms|gcm|mcs"` rồi bấm gửi, xem
   Play Services có phản ứng không.
6. Để qua đêm. Sáng hôm sau xem nhật ký: các dòng heartbeat có đều đặn không, cột "lệch" bao nhiêu giây.

## Ghi chú kỹ thuật
- Kiểu alarm "Tiết kiệm" dùng `setAndAllowWhileIdle` (không cần quyền, trong Doze bị giới hạn
  khoảng một lần mỗi 9 phút). Kiểu "Chính xác" dùng `setAlarmClock` (xuyên Doze tốt hơn nhưng
  hiện biểu tượng báo thức trên thanh trạng thái).
- Alarm được đặt lại sau khi khởi động máy và sau khi cập nhật app.
- Dùng giờ yên tĩnh: alarm kế tiếp được dời tới giờ kết thúc, nên máy không bị đánh thức ban đêm.
- Danh sách action sửa được trong app, nên có thể thử action khác nếu action mặc định không còn tác dụng.
