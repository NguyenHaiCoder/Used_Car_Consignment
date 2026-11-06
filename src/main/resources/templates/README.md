# Cấu trúc giao diện Thymeleaf

Tất cả trang HTML của ứng dụng nằm trong thư mục này.

```text
templates/
  public/        # Home công khai
  auth/          # Đăng nhập, xác minh và OTP
  common/        # Layout, header, sidebar, footer và fragments tái sử dụng
  admin/         # Nhân sự, phân quyền, phiên truy cập, audit, báo cáo
  manager/       # Phê duyệt, điều hành, ký gửi, tin đăng, quyết toán
  consignment/   # Chủ xe, hồ sơ ký gửi, agreement, rút ký gửi
  inspector/     # Xe, tiếp nhận, kiểm định, định giá, tin đăng
  sales/         # Khách mua, tư vấn, lịch xem, reservation, hợp đồng bán
  finance/       # Khoản thu, hoàn tiền, quyết toán, chi trả
  handover/      # Checklist bàn giao, biên nhận, đóng sale
  documents/     # Checklist giấy tờ, upload/review, sang tên
  app/           # Dashboard theo role khi đăng nhập thật
```

Mỗi role có `dashboard.html`; bên trong từng module dùng `list.html`, `detail.html`, `create.html`, `edit.html` hoặc tên thao tác như `withdraw.html`, `cancel.html`, `receipt.html`, `close.html`. Không tạo file riêng theo mã bản ghi 101/102/103: dữ liệu khác nhau được truyền bằng Spring `Model`.

Ví dụ:

- `consignment/requests/list.html`: danh sách / workflow hồ sơ ký gửi (shell layout).
- `sales/reservations/cancel.html`: hủy/hết hạn và xử lý cọc.
- `finance/settlements/detail.html`: xem/tính quyết toán của Finance.
- `manager/settlements/detail.html`: Manager review quyết toán.
- `documents/documents/upload.html`: upload/thay thế giấy tờ.

Các file trang sử dụng fragment Thymeleaf để tái sử dụng layout và thành phần.

CSS và JavaScript được phục vụ từ `src/main/resources/static/css/ui` và `static/js/ui` (`shell.js` cho sidebar/toast). Màn hình nghiệp vụ chạy qua route đã xác thực (`/admin/**`, `/manager/**`, `/consignment/**`, …), không còn workspace `/ui` preview.

## Header và sidebar dùng chung

`common/sidebar.html :: sidebar` và `common/header.html :: header` là nguồn chính cho giao diện ConsignCar. `shell/layout.html` cũng gọi hai fragment này. Không tạo bản sao trong `shell/`.

Truyền `active` bằng tên, ví dụ `~{common/sidebar :: sidebar(active='consignments')}`; truyền tiêu đề và số thông báo qua `~{common/header :: header(heading='Hồ sơ ký gửi', alerts=1)}`. Các trang dùng `Screen` có thể gọi `header` trực tiếp để lấy `screen.title`.

`common/header :: head(title)` giữ CSS nội dung hiện có và nạp `css/app/chrome.css` cho các thành phần dùng chung. Layout workflow vẫn dùng `css/app/shell.css`.
