# Cấu trúc giao diện Thymeleaf

Tất cả trang HTML của ứng dụng nằm trong thư mục này.

```text
templates/
  public/        # Home công khai
  auth/          # Đăng nhập, xác minh mẫu và OTP hiện có
  preview/       # Mục lục trang và file URL bấm kiểm tra theo role
  common/        # Layout, header, sidebar, footer và fragments tái sử dụng
  admin/         # Nhân sự, phân quyền, phiên truy cập, audit, báo cáo
  manager/       # Phê duyệt, điều hành, ký gửi, tin đăng, quyết toán
  consignment/   # Chủ xe, hồ sơ ký gửi, agreement, rút ký gửi
  inspector/     # Xe, tiếp nhận, kiểm định, định giá, tin đăng
  sales/         # Khách mua, tư vấn, lịch xem, reservation, hợp đồng bán
  finance/       # Khoản thu, hoàn tiền, quyết toán, chi trả
  handover/      # Checklist bàn giao, biên nhận, đóng sale
  documents/     # Checklist giấy tờ, upload/review, sang tên
```

Mỗi role có `dashboard.html`; bên trong từng module dùng `list.html`, `detail.html`, `create.html`, `edit.html` hoặc tên thao tác như `withdraw.html`, `cancel.html`, `receipt.html`, `close.html`. Không tạo file riêng theo mã bản ghi 101/102/103: dữ liệu khác nhau được truyền bằng Spring `Model`.

Ví dụ:

- `consignment/consignments/list.html`: danh sách hồ sơ ký gửi.
- `consignment/consignments/create.html`: tạo draft ký gửi.
- `consignment/consignments/detail.html`: chi tiết bất kỳ hồ sơ mẫu.
- `sales/reservations/cancel.html`: hủy/hết hạn và xử lý cọc.
- `finance/settlements/detail.html`: xem/tính quyết toán mẫu của Finance.
- `manager/settlements/detail.html`: Manager review quyết toán.
- `documents/documents/upload.html`: upload/thay thế giấy tờ mẫu.

Các file trang sử dụng fragment Thymeleaf để tái sử dụng layout và thành phần. Có thể chỉnh trực tiếp bố cục từng trang; không còn renderer trả chung `ui/page.html` cho tất cả màn hình.

CSS và JavaScript được phục vụ từ `src/main/resources/static/css/ui` và `static/js/ui`. Controllers chỉ GET để render UI, dùng profile `ui-preview` được bật tự động cùng profile `dev` mặc định. Chạy ứng dụng bình thường trong IntelliJ hoặc chạy `preview-ui.cmd`, rồi mở `http://localhost:6868/ui`. Các màn hình UI truy cập trực tiếp, không yêu cầu đăng nhập.

Bảng đối chiếu URL → template đầy đủ: `docs/frontend/TEMPLATE_STRUCTURE.md` tại workspace gốc.
