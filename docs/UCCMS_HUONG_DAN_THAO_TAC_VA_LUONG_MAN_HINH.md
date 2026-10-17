# UCCMS — Hướng dẫn thao tác màn hình theo luồng nghiệp vụ và vai trò

Ngày cập nhật: **15/10/2026**. Phạm vi: giao diện Thymeleaf hiện có trong `src/main/resources/templates`.

Tài liệu trả lời bốn câu hỏi: **Ai làm? Mở màn hình nào? Nhập/bấm gì? Sau đó chuyển cho ai, mở màn hình nào tiếp theo?**

Nguồn nghiệp vụ chính: [UCCMS_Business_Driven_Conceptual_ERD.html](../../docs/UCCMS_Business_Driven_Conceptual_ERD.html), đặc biệt mục 2, mục 4 và các ghi chú cuối tài liệu. Phân vai/màn hình được đối chiếu thêm với workbook tracking 30 màn hình. Nút, trường nhập, URL và điều kiện hiển thị được đối chiếu với catalog, controller, các fragment và JavaScript của ứng dụng.

## Mục lục

- [1. Cách mở UI và cách đọc tài liệu](#cach-doc)
- [2. Ai chịu trách nhiệm công việc nào?](#vai-tro)
- [3. Luồng tổng thể và điểm bàn giao giữa các role](#tong-the)
- [4. Thao tác chung trên mọi màn hình](#thao-tac-chung)
- [5. Tiếp nhận chủ xe và mở hồ sơ ký gửi](#ky-gui)
- [6. Tiếp nhận xe, kiểm định, giấy tờ, agreement và đăng bán](#dang-ban)
- [7. Sales: khách mua → tư vấn → xem xe → đặt cọc → hợp đồng](#sales)
- [8. Hủy/hết hạn giữ xe và xử lý tiền cọc](#huy-coc)
- [9. Finance và Manager: thu tiền → quyết toán → chi trả](#tai-chinh)
- [10. Sang tên, bàn giao và đóng sale](#ban-giao)
- [11. Rút ký gửi và trả xe cho chủ xe](#rut-ky-gui)
- [12. Công việc của Admin và Manager](#quan-tri)
- [13. Kịch bản kiểm tra và những điểm chưa nối backend](#kiem-tra)
- [14. Tra cứu chi tiết từng màn hình, từng nút và trang đích](#tra-cuu)

<a id="cach-doc"></a>
## 1. Cách mở UI và cách đọc tài liệu

Chạy ứng dụng bình thường trong IntelliJ hoặc chạy [preview-ui.cmd](../preview-ui.cmd), chờ ứng dụng chạy ở cổng 6868, rồi mở [Bộ UI](http://localhost:6868/ui). Profile `dev` mặc định tự bật `ui-preview`; các màn hình không yêu cầu đăng nhập. Dùng `/ui/urls` để xem các đường dẫn theo vai trò.

Nếu chạy từ PowerShell trong thư mục ứng dụng:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### 1.1. Phân biệt nghiệp vụ đích với khả năng của bản UI

| Ký hiệu trong tài liệu | Hiểu như thế nào? |
|---|---|
| **Nghiệp vụ** | Công việc hệ thống cần phục vụ theo tài liệu ERD; kết quả thực tế cần backend xử lý. |
| **Mở trang** | Link/nút điều hướng đang có; bấm sẽ mở trang được ghi trong cột đích. |
| **Mở từ menu/URL** | Bước kế tiếp về nghiệp vụ nhưng hiện không có nút trực tiếp từ trang trước; dùng menu, `/ui/urls` hoặc URL trong tài liệu. |
| **Mô phỏng tại chỗ** | Nhập form → kiểm tra → xác nhận trong hộp thoại → hiện kết quả ngay trên trang; không tự mở trang khác. |
| **Chuyển role** | Sang giao diện của người xử lý tiếp theo để xem luồng; đây là điều hướng preview, không phải đăng nhập hoặc cấp quyền thật. |
| **Bị chặn** | Chưa đủ điều kiện hoàn tất. Xem mục “Điều kiện & kiểm tra”, thông báo lỗi hoặc lý do dưới nút bị vô hiệu hóa. |

**Lưu ý khi đi hết luồng:** dữ liệu các trang là fixture độc lập. Tạo chủ xe mẫu không tự thêm dòng vào danh sách; duyệt ở Manager không tự đổi trạng thái bên Finance; chọn giấy tờ không upload S3. Các trạng thái/badge, lịch sử và số liệu mẫu không được thay đổi trên server sau khi xác nhận form. Vì vậy các bước sau đây mô tả đúng trách nhiệm nghiệp vụ, kèm cách xem từng bước trong UI, không khẳng định giao dịch đã được thực hiện.

### 1.2. Các mã mẫu dùng để kiểm tra

| Nhóm bản ghi | Xe / hồ sơ | Điểm cần kiểm tra |
|---|---|---|
| `101` | Toyota Camry · KG-101 · RS-101 · HĐB-101 | Ví dụ có đủ điều kiện ở nhiều bước: xem hồ sơ, tin đăng, quyết toán, bàn giao. Các fixture từng module vẫn có thể mô tả những thời điểm khác nhau. |
| `102` | Mazda CX-5 · KG-102 · RS-102 · HĐB-102 | Draft được sửa; thiếu/chờ review giấy tờ; reservation hết hạn; có bước bị chặn. |
| `103` | Honda City · KG-103 · RS-103 · HĐB-103 | Xe đang được giữ, rút ký gửi còn blocker, thanh toán chưa đủ nên chưa thể bàn giao. |

Không suy ra rằng tất cả trang cùng mã đã đồng bộ vào một thời điểm. Ví dụ KG-101 có thể là SUBMITTED trong hồ sơ ký gửi nhưng tin đăng mẫu 101 hiển thị LISTED. Đây là dữ liệu để xem các trạng thái UI, không phải một database giao dịch thống nhất.

<a id="vai-tro"></a>
## 2. Ai chịu trách nhiệm công việc nào?

| Role | Việc chính cần làm | Nhận công việc từ | Chuyển kết quả cho | Mở tổng quan |
|---|---|---|---|---|
| **Consignment Staff — Nhân viên ký gửi** | Tạo/chọn chủ xe, mở/sửa draft, quản lý agreement và phiên bản, theo dõi hồ sơ, đề nghị rút/trả xe. | Nhu cầu ký gửi/định giá của chủ xe. | Inspector, Document Clerk, Manager. | [Ký gửi](http://localhost:8080/ui/consignment) |
| **Inspector — Kiểm định viên** | Tiếp nhận xe vật lý, ghi VIN/odo/tình trạng, kiểm định từng hạng mục, định giá, chuẩn bị/kiểm soát tin đăng. | Hồ sơ ký gửi và xe được tiếp nhận. | Document Clerk phối hợp bổ sung hồ sơ; Manager xem điều kiện/ngoại lệ; Sales nhận xe có thể bán. | [Kiểm định](http://localhost:8080/ui/inspector) |
| **Document Clerk — Nhân viên hồ sơ** | Kiểm tra giấy tờ theo intake/sale/transfer, upload/thay thế version, review, theo dõi deadline/evidence sang tên. | Hồ sơ ký gửi, hợp đồng bán, giấy tờ từ các bên. | Inspector/Manager xem eligibility; Handover Staff đối chiếu giấy tờ trước giao xe. | [Hồ sơ](http://localhost:8080/ui/documents) |
| **Salesperson — Sales** | Tạo/chọn buyer, ghi inquiry đúng xe, đặt lịch xem/lái thử, giữ xe/đặt cọc, xử lý ngữ cảnh hủy/hết hạn và chuẩn bị hợp đồng bán. | Xe đủ điều kiện bán và nhu cầu khách mua. | Finance thu/hoàn tiền; Manager xử lý quyết định cần duyệt; Document Clerk/Handover Staff sau hợp đồng. | [Sales](http://localhost:8080/ui/sales) |
| **Finance Staff — Tài chính** | Ghi khoản thu, đối soát payment/request/transaction/webhook, chuẩn bị hoàn tiền, tính quyết toán theo agreement, theo dõi chi trả chủ xe. | Hợp đồng bán, khoản thu, quyết định cọc, agreement. | Manager xem quyết toán/ngoại lệ; Handover Staff kiểm tra điều kiện thanh toán. | [Finance](http://localhost:8080/ui/finance) |
| **Handover Staff — Bàn giao** | Đối chiếu điều kiện, ghi odo/chìa khóa/phụ kiện/ảnh/chữ ký, biên nhận, kiểm tra đủ mốc đóng sale. | Hợp đồng bán, kết quả thanh toán và giấy tờ. | Manager theo dõi kết quả vận hành; lưu lịch sử đóng sale. | [Bàn giao](http://localhost:8080/ui/handover) |
| **Manager — Quản lý** | Xem hàng đợi; đọc hồ sơ/blocker/evidence; duyệt, từ chối hoặc yêu cầu bổ sung; điều phối inquiry; review quyết toán; theo dõi rút ký gửi, tin đăng, báo cáo, audit. | Yêu cầu từ các nhân viên và cảnh báo vận hành. | Trả lại người lập hồ sơ hoặc cho phép vai trò phụ trách tiếp tục xử lý theo nghiệp vụ. | [Manager](http://localhost:8080/ui/manager) |
| **Admin — Quản trị viên** | Tài khoản nhân viên, vai trò/quyền, xem phiên truy cập, audit, báo cáo. | Nhu cầu quản trị truy cập. | Nhân viên nhận vai trò chính; Manager tiếp tục điều hành nghiệp vụ. | [Admin](http://localhost:8080/ui/admin) |

**Sales không làm thay việc Manager/Inspector/Finance:** Sales tự tạo Inquiry được gán cho chính Sales đó; chỉ Manager hoặc người có quyền điều phối mới gán lại. Buyer là hồ sơ do nhân viên tạo, không có portal tự phục vụ trong MVP. Shop là bên nhận ký gửi/trung gian, không mặc nhiên là chủ sở hữu xe.

<a id="tong-the"></a>
## 3. Luồng tổng thể và điểm bàn giao giữa các role

Các bước kiểm định, thu thập giấy tờ và soạn agreement có thể phối hợp song song; tất cả phải đạt điều kiện trước khi đăng bán. Tương tự, thanh toán và hồ sơ sang tên được theo dõi riêng; đóng sale cần đủ các mốc cuối, không chỉ một bước.

| Bước | Ai làm? | Màn hình A → màn hình B | Kết quả nghiệp vụ cần có trước khi chuyển |
|---|---|---|---|
| 1 | Ký gửi | Chủ xe → Tạo draft ký gửi → Chi tiết hồ sơ | Xác định đúng chủ xe, xe vật lý và lần ký gửi. |
| 2 | Inspector | Tiếp nhận xe → Checklist kiểm định → Định giá | Có VIN, tình trạng, kết quả từng hạng mục và đề xuất giá. |
| 3 | Hồ sơ | Checklist giấy tờ → Upload/thay thế → Review | Giấy tờ bắt buộc được xác minh theo đúng version. |
| 4 | Ký gửi | Chi tiết hồ sơ → Agreement | Có điều khoản, giá sàn, hoa hồng, thời hạn và bằng chứng chữ ký. |
| 5 | Manager + Inspector | Hàng đợi phê duyệt → Hồ sơ liên quan → Tin đăng | Giải quyết blocker, đủ eligibility trước publish. |
| 6 | Sales | Showroom → Buyer → Inquiry → Viewing | Biết khách nào quan tâm xe nào, Sales nào phụ trách. |
| 7 | Sales | Inquiry → Reservation | Đúng buyer/vehicle/inquiry; tối đa một reservation ACTIVE mỗi xe. |
| 8 | Sales | Reservation còn hiệu lực → SalesContract | Quyền bên bán hợp lệ, giá/tiền cọc/phần còn lại rõ ràng. |
| 9 | Finance | Hợp đồng → Khoản thu/đối soát | Có transaction/evidence; không dùng redirect thay xác nhận tiền. |
| 10 | Finance + Manager | Quyết toán → Review → Payout | Áp dụng đúng agreement, duyệt quyết toán trước chi trả theo nghiệp vụ. |
| 11 | Hồ sơ | Hợp đồng → TransferCase | Theo dõi người phụ trách, deadline và evidence chuyển quyền. |
| 12 | Bàn giao | Hàng đợi → Checklist → Biên nhận → Đóng sale | Đủ tiền, giấy tờ và bàn giao thực tế; lưu lịch sử. |

Hai nhánh ngoại lệ quan trọng: **reservation bị hủy/hết hạn → quyết định cọc → Finance hoàn/ghi nhận xử lý tiền**; **chủ xe rút ký gửi → kiểm tra listing/reservation/tiền → quyết định → trả xe và lưu lịch sử**.

<a id="thao-tac-chung"></a>
## 4. Thao tác chung trên mọi màn hình

### 4.1. Điều hướng và danh sách

**Bổ sung UI danh sách:** mỗi dòng có thêm nút cây bút **Sửa** và thùng rác **Xóa** bên cạnh nút xem. Hai nút mới chỉ để hiển thị giao diện theo yêu cầu, chưa có xử lý khi bấm, không đổi trang và không sửa/xóa dữ liệu. Các thao tác sửa nghiệp vụ đã có ở trang chi tiết vẫn hoạt động như mô tả riêng bên dưới.

**Quy ước icon trên toàn bộ UI:** con mắt = Xem, cây bút = Sửa/thay thế phiên bản, thùng rác = Xóa/bỏ nháp. Với nút chỉ hiện icon, rê chuột để đọc tooltip hoặc dùng nhãn hỗ trợ truy cập để xác định đúng thao tác. Tên nút trong các bảng hướng dẫn dưới đây cũng là tên tooltip/nhãn của icon tương ứng. Chỉ hiển thị thao tác đã được màn hình hỗ trợ; không tự thêm quyền xóa hồ sơ tài chính hoặc dữ liệu nghiệp vụ.

| Vị trí / thao tác | Cách thực hiện | Kết quả hiện tại |
|---|---|---|
| Bộ UI / URL theo role | Bấm “URL theo role”, chọn role, mở “Tất cả URL của role”. | Mở đúng màn hình đã khai báo trong controller. |
| Sidebar | Mở/thu nhóm nghiệp vụ, bấm mục cần xử lý. | Trang hiện tại có mục active; trên mobile mở bằng nút menu, đóng bằng nút đóng/overlay/Escape. |
| Đổi role xem trước | Bấm mục cuối sidebar hoặc menu người dùng. | Quay về bộ UI để chọn vai trò khác; không thay quyền tài khoản thật. |
| Tìm kiếm | Nhập mã hồ sơ, tên khách, xe hoặc thông tin xuất hiện trong bảng. | Lọc các dòng đang được render, không gọi API tìm kiếm. |
| Lọc trạng thái | Chọn trạng thái ở ô “Trạng thái”. | Chỉ hiện các dòng mẫu khớp trạng thái. |
| Sắp xếp | Bấm tiêu đề cột có ký hiệu ↕. | Đổi tăng/giảm của cột; không làm đổi dữ liệu gốc. |
| Phân trang | Chọn “Mỗi trang”, bấm “Trước”/“Sau”. | Chuyển phần dữ liệu mẫu đang hiển thị; có thể chọn 2 dòng để thử với bảng 3 dòng. |
| Không tìm thấy | Bấm “Xóa bộ lọc”. | Trả lại các dòng mẫu. |
| Xem chi tiết | Bấm link ở cuối dòng. | Mở record tương ứng; phần tra cứu ghi đầy đủ URL đích. |
| Xuất CSV mẫu | Bấm “Xuất CSV mẫu”. | Tải file CSV của dữ liệu khớp bộ lọc; đây là thao tác tải file thật trên trình duyệt. |
| In bản mẫu / biên nhận | Bấm nút in. | Mở hộp thoại in của trình duyệt; không tạo chứng từ đã ký trên hệ thống. |

### 4.2. Nhập form, lưu nháp và xác nhận

1. Đọc thông tin hồ sơ, trạng thái và các điều kiện trước khi nhập. Trường có `*` là bắt buộc trong bản UI này.
2. Nhập dữ liệu; với file, chọn PDF/JPG/PNG tối đa 10 MB. Nội dung file chỉ ở thiết bị, không được upload.
3. Nếu chưa xong, bấm **Lưu nháp trên trình duyệt**. Nháp gắn với URL đang mở và có thể khôi phục trong phiên trình duyệt; không tạo bản ghi nghiệp vụ trong danh sách.
4. Bấm **Kiểm tra & mô phỏng gửi**. Nếu thiếu/sai dữ liệu, UI đánh dấu trường và đưa focus về lỗi đầu tiên. Nếu còn blocker, UI có thể chặn mô phỏng hoàn tất.
5. Nếu hợp lệ, hộp thoại **Kiểm tra trước khi mô phỏng** hiện các giá trị vừa nhập. Bấm **Quay lại**/nút đóng/Escape để sửa, hoặc **Xác nhận mô phỏng**.
6. Kết quả hiển thị **ngay tại trang hiện tại**, dưới form. Mở “Xem thông tin vừa nhập” để kiểm tra; muốn đi bước tiếp theo phải bấm liên kết trong “Các bước liên quan”, nút điều hướng, menu hoặc URL tương ứng.

**Xóa nháp** cần xác nhận; sau đó form trở về giá trị mẫu ban đầu. Mật khẩu và nội dung file không lưu trong nháp; tải lại phải chọn lại file. Khi có chỉnh sửa chưa lưu, rời trang có thể hiện cảnh báo của trình duyệt. Nếu JavaScript tắt, nút gửi mô phỏng bị khóa.

### 4.3. Những kiểm tra hiện có trong UI

UI kiểm tra bắt buộc, email, số điện thoại 10–11 chữ số, CCCD 12 chữ số, VIN 17 ký tự không gồm I/O/Q, OTP 6 chữ số, số không âm, odo/số chìa khóa/số km nguyên, năm sản xuất 1900–2026 trong bản hiện tại, hoa hồng 0–100%, khoảng ngày hợp lệ, loại/kích thước file. Có kiểm tra cọc không vượt giá hợp đồng, tổng hoàn + giữ bằng cọc mẫu và số tiền hoàn không vượt khoản thu mẫu. Quy tắc cấp quyền, trùng VIN/khách, trùng lịch, reservation đồng thời, hết hạn và trạng thái tài chính thật cần backend kiểm tra độc lập.

<a id="ky-gui"></a>
## 5. Tiếp nhận chủ xe và mở hồ sơ ký gửi

Vai trò chính: **Consignment Staff**. Theo ERD: nhu cầu ký gửi/định giá xe của chủ xe phải mở `Consignment`, không tạo `Inquiry` bán xe.

| Thứ tự | Mở màn hình | Cần thao tác gì? | Bấm gì / đi đâu tiếp? |
|---|---|---|---|
| 1 | [Chủ xe ký gửi](http://localhost:8080/ui/consignment/consignors) | Tìm theo tên/liên hệ, xem đã có chủ xe phù hợp chưa. | “Thêm chủ xe” → form tạo; “Xem chi tiết” → hồ sơ chủ xe. |
| 2 | [Tạo hồ sơ chủ xe](http://localhost:8080/ui/consignment/consignors/new) | Nhập họ tên, điện thoại, CCCD, thông tin ngân hàng/tài khoản, xác nhận đồng ý lưu dữ liệu; email/địa chỉ tùy trường. | Lưu nháp/review/mô phỏng tại chỗ. “Danh sách chủ xe” quay về danh sách. Chưa có bản ghi mới tự xuất hiện. |
| 3 | [Chi tiết chủ xe 101](http://localhost:8080/ui/consignment/consignors/101) | Đọc thông tin chủ sở hữu, liên hệ, thông tin chi trả và hồ sơ liên quan. | “Hồ sơ ký gửi” → KG-101; “Tạo lần ký gửi” → form tạo draft. |
| 4 | [Tạo draft ký gửi](http://localhost:8080/ui/consignment/consignments/new) | Chọn chủ xe/xe, nhập giá kỳ vọng, ngày tiếp nhận, yêu cầu điều khoản và consent. | Review/mô phỏng tại chỗ; trong “Các bước liên quan” mở “Tiếp nhận xe” hoặc “Lập hợp đồng”. |
| 5 | [Hồ sơ ký gửi](http://localhost:8080/ui/consignment/consignments) | Tìm/lọc hồ sơ và đọc trạng thái. | “Tạo draft ký gửi” → form; “Xem chi tiết” → KG-101/102/103. |
| 6 | [Chi tiết KG-102](http://localhost:8080/ui/consignment/consignments/102) | KG-102 đang DRAFT: đối chiếu chủ xe, giá, vị trí và điều kiện. | “Sửa draft” → [form sửa](http://localhost:8080/ui/consignment/consignments/102/edit). |
| 7 | [Chi tiết KG-101](http://localhost:8080/ui/consignment/consignments/101) | Xem trạng thái SUBMITTED, kiểm định, giấy tờ, agreement và lịch sử. | “Sửa draft” bị khóa. Mở các bước “Hợp đồng ký gửi”, “Kiểm định xe”, “Giấy tờ”, “Phê duyệt”, “Đăng bán”. |

**Kết quả nghiệp vụ cần có:** đúng chủ xe pháp lý, đúng xe vật lý, đúng lần ký gửi. Một chủ xe có thể ký gửi nhiều lần; cùng một xe có thể có nhiều lần ký gửi ở thời điểm khác nhau. Sau submit, thay đổi phải qua version hoặc approval. Nút gửi hiện tại là mô phỏng chung, chưa phải một API submit thay đổi trạng thái hồ sơ.

<a id="dang-ban"></a>
## 6. Tiếp nhận xe, kiểm định, giấy tờ, agreement và đăng bán

### 6.1. Inspector tiếp nhận và kiểm định

| Màn hình A | Thao tác | Màn hình B / người xử lý tiếp |
|---|---|---|
| [Xe tại showroom](http://localhost:8080/ui/inspector/vehicles) | Tìm xe, đọc trạng thái, bấm “Tiếp nhận xe” hoặc “Xem chi tiết”. | [Tiếp nhận](http://localhost:8080/ui/inspector/vehicles/intake) hoặc chi tiết xe theo dòng. |
| Tiếp nhận xe | Chọn KG; nhập VIN, số khung/máy, hãng/dòng xe, năm, km, màu, vị trí, tình trạng tiếp nhận. | Review tại chỗ; bước “Kiểm định” → [KD-101](http://localhost:8080/ui/inspector/inspections/101). |
| [Chi tiết xe 101](http://localhost:8080/ui/inspector/vehicles/101) | Đọc VIN, năm/km, chủ xe, giá, vị trí và điều kiện bán. | “Kiểm định” → checklist; “Định giá” → valuation; “Tin đăng” → listing; “Ghi nhận tư vấn” chuyển sang Sales. |
| [Hàng đợi kiểm định](http://localhost:8080/ui/inspector/inspections) | Chọn phiếu cần kiểm tra. | “Xem chi tiết” → KD tương ứng. |
| [Checklist KD-102](http://localhost:8080/ui/inspector/inspections/102) | Chọn kết luận tổng thể; nhập kết quả, mức độ, ghi chú/ảnh cho từng nhóm khung vỏ, máy/hộp số, nội thất/điện, tai nạn và ngập nước. | Review tại chỗ; “Hồ sơ xe” quay lại xe; “Định giá” mở giá; “Giấy tờ” chuyển Document Clerk. |
| [Định giá KG-101](http://localhost:8080/ui/inspector/valuations/101) | Nhập tham chiếu thị trường, giá sàn, giá đề xuất và cơ sở định giá; xem agreement và lịch sử. | “Phê duyệt giá” → Manager PD-102; “Tin đăng” → biên tập tin. |

Giá dưới sàn phải được xử lý qua approval theo tài liệu hỗ trợ nghiệp vụ. Bản UI có link “Phê duyệt giá”, nhưng **chưa tự chặn mọi trường hợp giá đề xuất dưới sàn** trong form valuation. Không coi việc form được review là giá đã được Manager chấp thuận.

### 6.2. Document Clerk thu thập và review giấy tờ

| Màn hình A | Thao tác | Màn hình B / kết quả |
|---|---|---|
| [Checklist hồ sơ](http://localhost:8080/ui/documents/checklists) | Xác định checklist thuộc Intake hay Transfer; xem tiến độ/trạng thái. | “Xem chi tiết” → CK-101/102/103. |
| [CK-102](http://localhost:8080/ui/documents/checklists/102) | Đọc giấy tờ bắt buộc nào còn thiếu/chưa verified. | “Bổ sung / thay thế” → upload; “Review phiên bản” → DOC-102. |
| [Upload / thay thế](http://localhost:8080/ui/documents/documents/upload) | Chọn checklist, loại giấy tờ, file, phiên bản thay thế, hạn và ghi chú. Xem trước file đã chọn trên máy. | Review/mô phỏng tại chỗ; “Checklist” trở lại danh sách. Muốn review, mở màn “Review giấy tờ” từ menu/URL. |
| [Danh sách review](http://localhost:8080/ui/documents/documents) | Đọc loại file, hồ sơ/version, người upload, trạng thái. | “Xem chi tiết” → document/version. |
| [DOC-102 v2](http://localhost:8080/ui/documents/documents/102) | Đọc file/version/người upload, chọn VERIFIED hoặc REJECTED, nhập nhận xét/lý do. | Review tại chỗ. “Thay thế phiên bản” mở upload; “Checklist” mở CK-102. |

**Quy tắc ERD:** upload không đồng nghĩa verified. Replace phải giữ version cũ; thiếu hoặc rejected giấy tờ bắt buộc phải chặn eligibility. File thực tế sẽ lưu S3 và cần metadata/version/reviewer; UI hiện không mở file S3 hoặc biến document thành verified sau khi bạn bấm xác nhận.

### 6.3. Nhân viên ký gửi quản lý agreement

Mở [HĐKG-101 v2](http://localhost:8080/ui/consignment/agreements/101) từ chi tiết KG-101. Đối chiếu chủ xe, hồ sơ, hoa hồng, phí, giá sàn, thời hạn, điều khoản lưu giữ/chấm dứt/chi trả, chữ ký và lịch sử phiên bản. Agreement ACTIVE không sửa đè: form phía dưới dùng để **tạo nháp phiên bản sửa đổi**. Nhập điều khoản và bằng chứng chữ ký, lưu nháp/review tại chỗ; “Hồ sơ ký gửi” quay lại KG; “In bản mẫu” mở hộp thoại in.

Bản UI chưa có quy trình ký số, kích hoạt/terminate agreement thật hoặc nút activate gọi backend. Việc version/giá sàn được công nhận phải xử lý ở nghiệp vụ tương lai.

### 6.4. Manager phê duyệt và Inspector/Manager kiểm soát tin đăng

1. Manager mở [Hàng đợi phê duyệt](http://localhost:8080/ui/manager/approvals), chọn dòng để mở [PD-101](http://localhost:8080/ui/manager/approvals/101), [PD-102](http://localhost:8080/ui/manager/approvals/102) hoặc [PD-103](http://localhost:8080/ui/manager/approvals/103).
2. Đọc loại yêu cầu, đối tượng, người lập, checklist và lịch sử. Bấm “Hồ sơ ký gửi” để xem hồ sơ liên quan. Nếu Manager có route tương ứng, UI giữ trang trong role Manager.
3. Chọn **Yêu cầu bổ sung**, **Phê duyệt** hoặc **Từ chối**, nhập lý do. Khi còn blocker, bản UI cho kiểm tra phương án yêu cầu bổ sung/từ chối; phê duyệt hoàn tất có thể bị chặn.
4. Xác nhận chỉ hiển thị kết quả tại trang. Nghiệp vụ tiếp theo là trả việc cho người phụ trách; chưa có gửi notification, giao việc hoặc cập nhật trạng thái thật.
5. Inspector hoặc Manager mở [Tin đăng](http://localhost:8080/ui/inspector/listings) / [Tin đăng của Manager](http://localhost:8080/ui/manager/listings), chọn chi tiết.
6. Ở [TĐ-101](http://localhost:8080/ui/inspector/listings/101), kiểm tra inspection, document VERIFIED, agreement hiệu lực và reservation; nhập tiêu đề, nội dung công khai, giá và ảnh. Nút review mô phỏng bước chuẩn bị/hoàn tất UI, không publish thật. “Mô phỏng dừng tin” mở hộp xác nhận tại chỗ.
7. [TĐ-102](http://localhost:8080/ui/inspector/listings/102) bị chặn vì kiểm định/giấy tờ; [TĐ-103](http://localhost:8080/ui/inspector/listings/103) đang có reservation ACTIVE. Không tự bỏ qua blocker để bán.

**Bàn giao cho Sales:** khi xe đủ điều kiện, Sales xem trong showroom và mở tư vấn. Không có thao tác đồng bộ hay gửi việc thật trong bản preview.

<a id="sales"></a>
## 7. Sales: khách mua → tư vấn → xem xe → đặt cọc → hợp đồng

### 7.1. Sales cần làm gì theo thứ tự?

| Bước | Màn hình | Dữ liệu cần đọc/nhập | Nút chuyển trang / bước kế tiếp |
|---|---|---|---|
| 1 | [Showroom của Sales](http://localhost:8080/ui/sales/vehicles) | Xe, năm/km, giá, trạng thái bán. | “Xem chi tiết” → hồ sơ xe trong role Sales. Sales không có nút tiếp nhận/kiểm định/định giá/biên tập tin. |
| 2 | [Chi tiết xe Sales 101](http://localhost:8080/ui/sales/vehicles/101) | Kiểm tra VIN, chủ xe, giá, khả năng bán. | “Ghi nhận tư vấn” → form Inquiry. Nếu chưa có buyer, mở mục “Khách mua xe” từ menu trước. |
| 3 | [Khách mua xe](http://localhost:8080/ui/sales/buyers) | Tìm buyer để tránh tạo trùng. | “Thêm khách mua” → [form buyer](http://localhost:8080/ui/sales/buyers/new); “Xem chi tiết” → buyer. |
| 4 | Form buyer | Nhập họ tên, điện thoại, CCCD, email/địa chỉ nếu có và privacy consent. | Review tại chỗ; “Danh sách khách” quay lại. Sau đó mở Inquiry từ menu. |
| 5 | [Chi tiết buyer 101](http://localhost:8080/ui/sales/buyers/101) | Đọc liên hệ và consent của buyer. | “Tư vấn liên quan” → TV-101; “Đặt cọc liên quan” → RS-101. |
| 6 | [Tư vấn & lịch xem](http://localhost:8080/ui/sales/inquiries) | Xem buyer, xe và Sales đang phụ trách. | “Tạo tư vấn” → [form Inquiry](http://localhost:8080/ui/sales/inquiries/new); “Lịch xem xe” → lịch; “Xem chi tiết” → TV. |
| 7 | Form Inquiry | Chọn buyer/vehicle, kênh, nhu cầu hỏi giá/xem xe/lái thử, ghi chú. Phụ trách hiện tại là Sales đang xem. | Review tại chỗ; bước “Đặt lịch xem xe” mở form lịch. |
| 8 | [TV-101](http://localhost:8080/ui/sales/inquiries/101) | Xem buyer, xe, phụ trách, kênh và lịch sử. Sales không thấy form điều phối/gán lại. | “Đặt lịch xem” → form Viewing; “Tạo reservation” → form giữ xe. |
| 9 | [Lịch xem xe/lái thử](http://localhost:8080/ui/sales/viewings) | Xem thời gian, khách/xe, loại lịch. | “Đặt lịch” → [form Viewing](http://localhost:8080/ui/sales/viewings/new); link chi tiết dòng mở Inquiry gốc, không phải trang Viewing riêng. |
| 10 | Form Viewing | Chọn Inquiry, thời gian hẹn, loại lịch và ghi chú. | Review tại chỗ; “Lịch xem xe” quay lại; bước “Đặt cọc giữ xe” mở Reservation. |
| 11 | [Tạo Reservation](http://localhost:8080/ui/sales/reservations/new) | Kiểm tra đúng buyer/xe/Inquiry, nhập cọc, hạn giữ xe, điều khoản và consent; đọc eligibility. | Review tại chỗ; “Danh sách giữ xe” mở danh sách; bước “Hợp đồng bán” mở HĐB-101. |
| 12 | [Danh sách giữ xe](http://localhost:8080/ui/sales/reservations) → [RS-101](http://localhost:8080/ui/sales/reservations/101) | Đọc khoản cọc, hiệu lực, Inquiry gốc và điều khoản. | “Hợp đồng bán” → HĐB-101; “Hủy / hết hạn & xử lý cọc” → form xử lý cọc. |
| 13 | [HĐB-101](http://localhost:8080/ui/sales/contracts/101) | Đối chiếu reservation ACTIVE, buyer, chủ xe, agreement, bên bán, giá/cọc/phần còn lại, ngày ký và chữ ký. | Review tại chỗ; các bước “Thanh toán”, “Sang tên”, “Bàn giao” mở UI của role tiếp theo. |

### 7.2. Quy tắc cần nhớ khi Sales làm việc

- Inquiry luôn là khách mua + xe quan tâm + Sales phụ trách. Muốn ký gửi xe của chủ xe thì quay về luồng Consignment.
- Một Inquiry có thể có nhiều lịch xem; không cần tạo buyer mới mỗi lần hẹn.
- Chỉ Manager/người có quyền điều phối được assign/reassign. Manager mở [Inquiry TV-101](http://localhost:8080/ui/manager/inquiries/101), chọn người nhận, nhập lý do rồi review mô phỏng. Sales không tự đổi sang đồng nghiệp.
- Một xe tối đa một reservation ACTIVE. UI không làm khóa đồng thời; backend tương lai phải bảo đảm điều kiện này ngay lúc tạo.
- [RS-102 hết hạn](http://localhost:8080/ui/sales/reservations/102) có nút hợp đồng bị khóa. Không dùng reservation hết hạn để tạo sale.
- Shop không tự là bên bán hợp pháp. Nếu chọn “Đại diện được ủy quyền” ở hợp đồng, trường giấy ủy quyền xuất hiện và bắt buộc. Cọc không vượt giá thỏa thuận; phần còn lại được tính để xem trước.
- Các form tạo đang dùng lựa chọn fixture: đi từ TV-102/103 sang “Tạo reservation” vẫn mở form mẫu chung, hiện chỉ có TV-101. Chưa có tự truyền context theo dòng/record mới tạo. Cần kiểm tra lại lựa chọn, không coi đó là dữ liệu của TV-102/103.

**Kết quả bàn giao nghiệp vụ:** Finance nhận hợp đồng/khoản cần thu; Document Clerk nhận hồ sơ sang tên; Handover Staff nhận kế hoạch bàn giao. Các liên kết chỉ mở trang preview, chưa giao việc thật.

<a id="huy-coc"></a>
## 8. Hủy/hết hạn giữ xe và xử lý tiền cọc

| Bước | Người làm | Màn hình / thao tác | Điều kiện / bước tiếp |
|---|---|---|---|
| 1 | Sales | Mở chi tiết RS, bấm “Hủy / hết hạn & xử lý cọc”. Ví dụ [RS-103/cancel](http://localhost:8080/ui/sales/reservations/103/cancel). | Phân biệt khách hủy và hết hạn. |
| 2 | Sales | Chọn ngữ cảnh, phương án “Hoàn toàn bộ”, “Giữ toàn bộ” hoặc “Hoàn một phần”; nhập lý do. | Hoàn + giữ = cọc gốc mẫu 50 triệu. Chọn toàn bộ thì UI tự điền/khóa phần số tiền; chọn một phần thì nhập hai số. |
| 3 | Sales / người có quyền quyết định | Review thông tin và lưu evidence/quyết định theo nghiệp vụ. | Bản UI chỉ mô phỏng, chưa chuyển trạng thái reservation hay phát sinh Refund. |
| 4 | Finance | Bấm bước “Finance ghi nhận hoàn” → [Ghi nhận hoàn tiền](http://localhost:8080/ui/finance/refunds/new). | Chọn khoản thu gốc và reservation, nhập tiền/lý do/quyết định/evidence. |
| 5 | Manager khi có yêu cầu duyệt | Xem hồ sơ, lý do, chính sách và evidence trước quyết định. | Chưa có route approval riêng cho từng khoản refund/cọc; phải xác định quyền/ngưỡng duyệt khi thiết kế backend. |
| 6 | Finance | Đối chiếu khoản hoàn với Payment và Reservation. | Fixture form refund hiện thiếu quyết định được duyệt nên bị chặn hoàn tất; không chuyển tiền. |

Theo ERD, tiền cọc là khoản phải trả, không tự là doanh thu. Refund cần tham chiếu Payment gốc và Reservation phát sinh nhu cầu hoàn. Không sửa âm thầm Payment cũ, không chỉ đổi badge để coi như đã hoàn tiền. Hủy/expiry cần xử lý trạng thái giữ xe và lịch sử khi có backend; UI chưa tự giải phóng xe hoặc chạy scheduler hết hạn.

<a id="tai-chinh"></a>
## 9. Finance và Manager: thu tiền → quyết toán → chi trả

### 9.1. Ghi nhận khoản thu và đối soát

1. Từ hợp đồng bán bấm bước “Thanh toán” → [Giao dịch thanh toán](http://localhost:8080/ui/finance/payments).
2. Finance dùng tìm/lọc hoặc “Xem chi tiết” để mở [TT-101](http://localhost:8080/ui/finance/payments/101). Đọc hợp đồng, payer, phương thức, tham chiếu, số tiền, các điều kiện transaction/webhook và lịch sử.
3. Muốn xem form ghi nhận, bấm “Ghi nhận khoản thu” → [form receipt](http://localhost:8080/ui/finance/receipts/new). Chọn hợp đồng, cọc/phần còn lại, phương thức; nhập số tiền, reference, payer, thời gian và bằng chứng. Review kết quả tại chỗ.
4. Muốn xem form hoàn, bấm “Hoàn tiền” → form refund đã mô tả ở mục 8.
5. Bấm “Quyết toán” từ chi tiết Payment → settlement cùng mã mẫu. Việc click mở settlement **không chứng minh rằng hợp đồng đã được trả đủ tiền**; vẫn phải đọc điều kiện.

**Nghiệp vụ payOS:** PaymentRequest là yêu cầu thu; Transaction và Webhook là bằng chứng đối soát; Payment là ghi nhận nghiệp vụ nội bộ. Webhook được xác minh mới là nguồn tin thanh toán, không phải việc khách quay lại `returnUrl`. UI chỉ trình bày chuỗi này, không tạo QR/payment link, xác minh chữ ký hoặc xác nhận nhận tiền thật. Không có nút tạo link payOS thật trong bản hiện tại.

### 9.2. Finance lập quyết toán, Manager review

| Người làm | Màn hình | Cần làm gì? | Bàn giao / trang tiếp |
|---|---|---|---|
| Finance | [Danh sách quyết toán](http://localhost:8080/ui/finance/settlements) | Chọn đúng hợp đồng/chủ xe; xem trạng thái. | “Xem chi tiết” → QT tương ứng. |
| Finance | [QT-101](http://localhost:8080/ui/finance/settlements/101) | Đọc agreement version, tài khoản chủ xe, giá cơ sở; nhập/đối chiếu hoa hồng, phí, chi phí được duyệt, hoàn đã ghi, cọc giữ. | Xem số ròng tính tạm; lưu nháp/review tại chỗ. Chưa có nút “Gửi Manager” riêng; mở Manager bằng danh mục URL khi kiểm tra vai trò kế tiếp. |
| Manager | [QT-101 phía Manager](http://localhost:8080/ui/manager/settlements/101) | Đọc thông số Finance lập dưới dạng thông tin; kiểm tra agreement và khoản thu; chọn Yêu cầu bổ sung/Phê duyệt/Từ chối và nhập lý do. | Review tại chỗ. Manager không có form sửa các số tiền Finance. Không tự đồng bộ kết quả duyệt sang trang Finance. |
| Finance | QT-101 đã duyệt trong dữ liệu mẫu | Bấm “Lệnh chi”. | Mở [PC-101](http://localhost:8080/ui/finance/payouts/101). Một số trạng thái khác có nút bị khóa; đọc lý do dưới nút. |
| Finance | [Danh sách chi trả](http://localhost:8080/ui/finance/payouts) → PC | Đối chiếu settlement, người nhận, số tiền, tài khoản đã che bớt và reference; nhập nội dung/evidence. | Review mô phỏng lệnh chi; không gọi ngân hàng/payOS payout. |

Theo ERD, settlement dựa trên **agreement đang hiệu lực**, không chỉ lấy giá bán rồi tự suy ra tiền chủ xe. Công thức tính hiện trên UI là giả định UX cho bản xem trước; chính sách phân bổ phí/cọc/hoàn phải được chốt nghiệp vụ trước khi dùng cho quyết toán thật.

<a id="ban-giao"></a>
## 10. Sang tên, bàn giao và đóng sale

### 10.1. Document Clerk xử lý sang tên

Mở bước “Sang tên” từ hợp đồng bán hoặc mở [Danh sách sang tên](http://localhost:8080/ui/documents/transfers), chọn [ST-101](http://localhost:8080/ui/documents/transfers/101). Đọc hợp đồng, buyer, xe, trạng thái và điều kiện; nhập/chọn giai đoạn, bên chịu trách nhiệm, deadline, evidence và ghi chú/blocker. Bấm “Checklist liên quan” để đối chiếu CK; “Danh sách sang tên” quay lại danh sách. Bước “Bàn giao vật lý” mở checklist bàn giao cùng mã mẫu.

ST-102 có blocker giấy tờ/deadline mẫu cần xem. Bản UI cho lưu/review thông tin giai đoạn nhưng chưa có hành động hoàn tất sang tên độc lập được nối backend. Tên giai đoạn chi tiết hiện là giả định UX, không thay thế xác nhận pháp lý thực tế.

### 10.2. Handover Staff bàn giao xe

| Bước | Màn hình | Thao tác cần làm | Bấm gì / mở màn hình nào? |
|---|---|---|---|
| 1 | [Hàng đợi bàn giao](http://localhost:8080/ui/handover/handovers) | Xem hợp đồng, xe, khách và trạng thái đủ điều kiện/blocked. Finance cũng có hàng đợi để đối chiếu tiền. | “Xem chi tiết” → BG tương ứng, mở UI của Handover Staff. |
| 2 | [BG-101](http://localhost:8080/ui/handover/handovers/101) | Đối chiếu thanh toán, giấy tờ, xe thực tế; nhập odo, số chìa khóa, phụ kiện, ảnh, evidence chữ ký người nhận/người giao, thời gian và xác nhận. | Review mô phỏng tại chỗ. Nếu còn blocker thì chưa mô phỏng hoàn tất. |
| 3 | BG-101 | Bấm “Biên nhận mẫu”. | Mở [biên nhận](http://localhost:8080/ui/handover/handovers/101/receipt). Đây là chứng từ DRAFT mẫu; không tự mang các giá trị vừa nhập ở form sang. |
| 4 | Biên nhận mẫu | Đọc thông tin người nhận/xe/VIN/odo/chìa khóa/phụ kiện/chữ ký. | “In biên nhận mẫu” mở hộp thoại in; “Checklist” trở lại BG-101. |
| 5 | BG-101 | Bấm bước “Đóng sale”. | Mở [kiểm tra đóng sale HĐB-101](http://localhost:8080/ui/handover/handovers/101/close). |
| 6 | Đóng sale | Kiểm tra riêng ba mốc: thanh toán hoàn tất, sang tên hoàn tất, bàn giao thực tế hoàn tất; nhập ghi chú. | Review tại chỗ. “Bàn giao” quay lại BG. Chưa cập nhật Consignment/SalesContract/SOLD thật. |

Sang tên và bàn giao không đồng nghĩa nhau. Không chỉ thấy “đã giao chìa khóa” mà bỏ qua giấy tờ, hoặc thấy “sang tên xong” mà coi đã bàn giao xe. [BG-102](http://localhost:8080/ui/handover/handovers/102) thiếu giấy tờ và [BG-103](http://localhost:8080/ui/handover/handovers/103) chưa đủ tiền là hai nhánh cần xem lý do trước khi thao tác.

<a id="rut-ky-gui"></a>
## 11. Rút ký gửi và trả xe cho chủ xe

1. Consignment Staff hoặc Manager mở chi tiết KG, bấm **Rút / đóng ký gửi**. Ví dụ [KG-103/withdraw](http://localhost:8080/ui/consignment/consignments/103/withdraw).
2. Đọc chủ xe/xe và blocker: còn reservation ACTIVE không, đã có phương án khoản tiền chưa, có thể dừng tin đăng không.
3. Nếu có đặt cọc, bấm **Xử lý reservation** để sang Sales. Link hiện tại là mẫu cố định RS-103/cancel; không suy ra đây luôn là reservation của bất kỳ KG đang mở.
4. Sales và Finance giải quyết hủy/cọc/hoàn theo mục 8; Manager xem/duyệt khi có yêu cầu theo nghiệp vụ. Chưa có cập nhật liên trang, nên KG-103 fixture vẫn giữ blocker sau khi bạn thử form ở các trang khác.
5. Nhân viên ký gửi nhập lý do rút/đóng, ngày dự kiến trả xe, tình trạng xe và evidence trả xe; review tại chỗ. Khi còn blocker, thao tác hoàn tất bị chặn.
6. Theo nghiệp vụ, phải dừng listing, xử lý reservation và tiền, ghi nhận việc trả xe, actor/time/reason/evidence, chuyển trạng thái Consignment và giữ lịch sử. Bản UI chưa tự thực hiện những thay đổi này.

Không tạo một nghiệp vụ mua lại xe cho shop. Tài liệu conceptual chính xử lý withdrawal trên `Consignment` cùng Approval/Reservation/Payment/Refund/AuditEvent, không yêu cầu entity `WithdrawalRecord` riêng.

<a id="quan-tri"></a>
## 12. Công việc của Admin và Manager

### 12.1. Admin

| Màn hình | Thao tác | Điều hướng / giới hạn |
|---|---|---|
| [Nhân sự & tài khoản](http://localhost:8080/ui/admin/employees) | Tìm nhân viên, xem vai trò/trạng thái, chọn “Thêm nhân viên”. | [Tạo nhân viên](http://localhost:8080/ui/admin/employees/new): nhập họ tên, email, điện thoại, vai trò chính; review tại chỗ. |
| [Hồ sơ nhân viên 101](http://localhost:8080/ui/admin/employees/101) | Xem email, role, xác minh, phiên và lịch sử. | “Xem phiên đăng nhập” → danh sách phiên; “Mô phỏng khóa tài khoản” chỉ xác nhận/toast, không khóa thật. Không phải mọi hồ sơ mẫu đều có cùng nút. |
| [Vai trò & quyền](http://localhost:8080/ui/admin/permissions) | Xem/chọn checkbox Xem công việc, Xử lý module, Phê duyệt, Quản trị quyền cho từng role. | “Mô phỏng lưu ma trận” mở review rồi toast. Chưa lưu quyền, chưa áp dụng quyền này sang các trang khác; permission code là nội dung cần chốt. |
| [Phiên đăng nhập & xác minh](http://localhost:8080/ui/admin/sessions) | Đọc nhân viên/thiết bị/hạn/trạng thái. | Chi tiết dòng mở hồ sơ nhân viên. Chưa có nút revoke phiên thật. |
| [Audit](http://localhost:8080/ui/admin/audit) | Tìm/lọc thời gian, actor, thao tác, đối tượng. | Chi tiết mở đối tượng liên quan ở role xử lý; không phải trang chỉnh audit. |
| [Báo cáo](http://localhost:8080/ui/admin/reports) | Xem số xe/hoa hồng/cọc/sang tên trễ, chọn khoảng ngày, xuất CSV. | Xem khoảng ngày mẫu; chỉ số không được truy vấn lại từ database. |

### 12.2. Checklist công việc của Manager

1. Mở tổng quan và **Hàng đợi phê duyệt**: chọn yêu cầu đang chờ, đọc loại/đối tượng/người lập, mở hồ sơ liên quan khi cần.
2. Đọc **blocker và evidence** trước khi quyết định; không chỉ nhìn một badge. Nhập lý do, chọn yêu cầu bổ sung/phê duyệt/từ chối và review.
3. Với **inquiry cần điều phối**, mở danh sách Inquiry của Manager, mở chi tiết, chọn Sales nhận và nhập lý do. Sales không tự làm thao tác này.
4. Với **quyết toán**, mở danh sách Settlement của Manager, đối chiếu agreement/tiền/evidence; chọn quyết định và lý do. Manager review số liệu Finance, không nhập thay bảng tính của Finance.
5. Với **rút ký gửi**, xem reservation/tiền/listing và việc trả xe. Không duyệt rút bỏ qua khoản cọc đang giữ.
6. Với **tin đăng**, xem eligibility; các trang Manager có phần kiểm soát nội dung/tin đăng theo phân vai workbook. Chưa có backend publish hoặc duyệt ngoại lệ thật.
7. Mở **báo cáo** và **audit** để kiểm tra xe bán/rút, hoa hồng, cọc, hồ sơ trễ và người đã thực hiện hành động.

Danh mục màn hình của Manager không đồng nghĩa Admin: quản lý vận hành/phê duyệt không tự được hiểu là quyền quản trị tài khoản hoặc cấp permission. Các quyền thực thi thật phải kiểm tra ở backend khi triển khai.

### 12.3. Đăng nhập / xác minh

[Đăng nhập mẫu](http://localhost:8080/ui/auth/sign-in): nhập email/mật khẩu → review mô phỏng; “Xác minh email / OTP” mở [màn xác minh](http://localhost:8080/ui/auth/verify). Màn xác minh nhập email và OTP 6 số, có mô phỏng gửi lại và link về đăng nhập. Hai form không phát hành token, không gửi email, không xác thực OTP và không tự đăng nhập vào role. Muốn xem role, mở `/ui`. Link “Mở đăng nhập Spring Security hiện có” là màn login mặc định khác của ứng dụng.

<a id="kiem-tra"></a>
## 13. Kịch bản kiểm tra và những điểm chưa nối backend

### 13.1. Sáu kịch bản nên đi thử

| Kịch bản | Đi theo màn hình nào? | Điều cần quan sát |
|---|---|---|
| Xe đủ điều kiện để bán | KG-101 → KD-101 → CK/DOC-101 → Agreement-101 → PD-101 → TĐ-101 | Có hồ sơ liên quan, người phụ trách, điều kiện và link đúng bước. Xác nhận form không làm badge trang khác đổi. |
| Sales chốt bán | Buyer-101 → TV-101 → tạo Viewing → tạo Reservation → RS-101 → HĐB-101 | Inquiry liên kết buyer/xe; Sales không thấy gán lại; cọc/phần còn lại và giấy ủy quyền có điều kiện. |
| Thiếu giấy tờ | CK-102 → upload → DOC-102 → TĐ-102 | Upload khác VERIFIED; tin đăng chưa được hoàn tất khi còn blocker. |
| Reservation hết hạn | RS-102 → xử lý cọc → form refund | Nút hợp đồng ở RS-102 khóa; hoàn + giữ phải bằng cọc mẫu; refund còn chờ quyết định. |
| Rút xe đang có cọc | KG-103 → withdraw → RS-103/cancel → refund | Rút ký gửi bị chặn; không coi việc mô phỏng ở form khác là blocker đã được giải quyết. |
| Hoàn tất sau bán | TT-101 → QT-101 Finance → QT-101 Manager → PC-101 → ST-101 → BG-101 → biên nhận → đóng sale | Tách tiền, quyết toán, chuyển quyền và giao xe; số liệu/công thức là mẫu. |

### 13.2. Các giới hạn cần biết để không thao tác nhầm

| Nội dung nghiệp vụ | Bản UI hiện tại thể hiện gì? | Chưa thực hiện |
|---|---|---|
| Tạo/sửa/lưu bản ghi | Form, kiểm tra, nháp trong phiên, review/result tại chỗ. | Lưu database, thêm dòng danh sách, đồng bộ dữ liệu liên trang. |
| Submit/approval/state transition | Chọn quyết định, lý do, điều kiện và blocker; một số nút bị khóa. | Transition thật, version immutable, audit mới, gửi notification/giao việc. |
| Giấy tờ | Chọn file, xem trước local, mô tả version/reviewer. | Upload/download S3, tạo version thật, cập nhật VERIFIED thật. |
| Giá dưới sàn | Giá sàn/đề xuất và đường dẫn phê duyệt. | Chặn đầy đủ mọi case giá dưới sàn tại form valuation. |
| Viewing/reservation | Chọn mẫu, đọc hiệu lực, kiểm tra cơ bản phía trình duyệt. | Trùng lịch, giữ xe đồng thời, scheduler expiry, tự truyền context đầy đủ. |
| Thanh toán/refund/payout | Thông tin/evidence, chuỗi đối soát mẫu, form và blocker. | Gọi payOS, xác minh webhook, chống trùng, chuyển/hoàn tiền. |
| Quyết toán | Tính số mẫu, Manager review riêng. | Chính sách kế toán chính thức, quyết toán được ghi nhận/chi trả thật. |
| Handover/receipt/close sale | Form/evidence/điều kiện/biên nhận mẫu, in từ trình duyệt. | Ký số, chứng từ có hiệu lực, tự chép form sang biên nhận, cập nhật SOLD/COMPLETED. |
| Phân quyền và xác thực | Điều hướng theo role preview và ẩn một số thao tác; login/OTP mẫu. | RBAC nghiệp vụ thật, JWT/refresh/revoke, khóa tài khoản thật. |
| Báo cáo | Bộ lọc ngày UI, chỉ số và bảng mẫu, CSV. | Tổng hợp/query theo dữ liệu hoặc khoảng ngày thật. |

Các enum/trạng thái chi tiết, approval threshold, permission code, bộ giấy tờ bắt buộc theo từng case, giai đoạn sang tên và công thức quyết toán phải được chốt ở logical/business design. Tài liệu conceptual là nguồn nghiệp vụ, không cung cấp mọi quyết định triển khai. Không mặc nhiên coi các giá trị fixture là quy định chính thức.

<a id="tra-cuu"></a>
## 14. Tra cứu chi tiết từng màn hình, từng nút và trang đích

Phần này liệt kê từng màn hình đang có theo role/module. Mỗi mục gồm URL, file HTML, dữ liệu hiển thị/nhập, nút thao tác, điều kiện và trang đích. Bản ghi khác ID dùng chung template; khác trạng thái thì nút/điều kiện có thể khác. Cột điều hướng ghi đúng URL hiện tại, kể cả những link mẫu cố định hoặc chuyển role.

<!-- SCREEN_REFERENCE_START -->

### 14.1. Chọn role để tra cứu

- [Quản lý](#role-manager)
- [Nhân viên kinh doanh](#role-sales)
- [Nhân viên ký gửi](#role-consignment)
- [Kiểm định viên](#role-inspector)
- [Nhân viên hồ sơ](#role-documents)
- [Nhân viên tài chính](#role-finance)
- [Nhân viên bàn giao](#role-handover)
- [Quản trị viên](#role-admin)
- [Đăng nhập, xác minh và các trang chung](#trang-chung)

Mỗi link trong cột đích mở trang thật của bản preview sau khi ứng dụng đã chạy. Một trang có nhiều liên kết sang role khác không có nghĩa role đang xem được phép xử lý nghiệp vụ thay người đó. Với thao tác tại chỗ, trang hiện tại được giữ nguyên.

<a id="role-manager"></a>
### Quản lý (`manager`)

Tra nhanh:

- [Tổng quan](#screen-manager-dashboard)
- [Đánh giá yêu cầu PD-101](#screen-manager-approvals-detail)
- [Hàng đợi phê duyệt](#screen-manager-approvals-list)
- [Nhật ký hoạt động](#screen-manager-audit-list)
- [Hồ sơ KG-101 · Toyota Camry 2.5Q](#screen-manager-consignments-detail)
- [Hồ sơ ký gửi](#screen-manager-consignments-list)
- [Rút / đóng ký gửi KG-101](#screen-manager-consignments-withdraw)
- [Tư vấn TV-101 · Trần Hoàng Nam](#screen-manager-inquiries-detail)
- [Tư vấn & lịch xem xe](#screen-manager-inquiries-list)
- [Biên tập tin đăng TĐ-101](#screen-manager-listings-detail)
- [Tin đăng & khả năng bán](#screen-manager-listings-list)
- [Báo cáo vận hành](#screen-manager-reports-list)
- [Quyết toán QT-101](#screen-manager-settlements-detail)
- [Hoa hồng & quyết toán chủ xe](#screen-manager-settlements-list)

<a id="screen-manager-dashboard"></a>
#### Tổng quan · Quản lý — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [manager/dashboard.html](../src/main/resources/templates/manager/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/dashboard-manager](http://localhost:8080/ui/manager/dashboard-manager) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Yêu cầu; Đối tượng; Người đề nghị; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Chờ phê duyệt; Đang đăng bán; Sang tên trễ. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Hàng đợi phê duyệt — **Quản lý** — [/ui/manager/approvals](http://localhost:8080/ui/manager/approvals) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Điều kiện ký gửi**: Xem chi tiết | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Giá dưới sàn**: Xem chi tiết | Mở Đánh giá yêu cầu PD-102 — **Quản lý** — [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Rút ký gửi**: Xem chi tiết | Mở Đánh giá yêu cầu PD-103 — **Quản lý** — [/ui/manager/approvals/103](http://localhost:8080/ui/manager/approvals/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-manager-approvals-detail"></a>
#### Đánh giá yêu cầu PD-101 — `approvals/detail`

**Mục đích:** Quyết định cần lý do, người duyệt và evidence; blocker phải được giải quyết.

**File HTML:** [manager/approvals/detail.html](../src/main/resources/templates/manager/approvals/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | PENDING | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | PENDING | Giấy tờ đã xác minh: Giấy đăng ký của KG-102 đang chờ review. |
| [/ui/manager/approvals/103](http://localhost:8080/ui/manager/approvals/103) | PENDING | Không có đặt cọc đang hiệu lực: KG-103 còn reservation ACTIVE; cần xử lý cọc trước. |

**Thông tin cần đọc:** Đối tượng; Người yêu cầu; Loại phê duyệt.

**Checklist cần đọc:** Kiểm định hợp lệ; Giấy tờ đã xác minh; Không có đặt cọc đang hiệu lực. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Quyết định | Có | Chọn: Yêu cầu bổ sung; Phê duyệt; Từ chối |
| Lý do quyết định | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hàng đợi** | Mở Hàng đợi phê duyệt — **Quản lý** — [/ui/manager/approvals](http://localhost:8080/ui/manager/approvals) | Áp dụng mẫu approvals/101 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Quản lý** — [/ui/manager/consignments/101](http://localhost:8080/ui/manager/consignments/101) | Áp dụng mẫu approvals/101 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Quản lý** — [/ui/manager/consignments/102](http://localhost:8080/ui/manager/consignments/102) | Áp dụng mẫu approvals/102 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-103 · Honda City RS — **Quản lý** — [/ui/manager/consignments/103](http://localhost:8080/ui/manager/consignments/103) | Áp dụng mẫu approvals/103 |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-approvals-list"></a>
#### Hàng đợi phê duyệt — `approvals/list`

**Mục đích:** Kiểm tra bằng chứng, blocker và lý do trước mỗi quyết định.

**File HTML:** [manager/approvals/list.html](../src/main/resources/templates/manager/approvals/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/approvals](http://localhost:8080/ui/manager/approvals) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Yêu cầu; Đối tượng; Người đề nghị; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **Điều kiện ký gửi**: Xem chi tiết | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Giá dưới sàn**: Xem chi tiết | Mở Đánh giá yêu cầu PD-102 — **Quản lý** — [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Rút ký gửi**: Xem chi tiết | Mở Đánh giá yêu cầu PD-103 — **Quản lý** — [/ui/manager/approvals/103](http://localhost:8080/ui/manager/approvals/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-manager-audit-list"></a>
#### Nhật ký hoạt động — `audit/list`

**Mục đích:** Lịch sử mẫu có người thực hiện, thời gian, đối tượng, lý do và trạng thái.

**File HTML:** [manager/audit/list.html](../src/main/resources/templates/manager/audit/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/audit](http://localhost:8080/ui/manager/audit) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Thời gian; Người thực hiện; Thao tác; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **15/10/2026 09:15**: Xem chi tiết | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Liên kết đúng như UI hiện tại. |
| Dòng **14/10/2026 14:30**: Xem chi tiết | Mở Giấy tờ DOC-102 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/102](http://localhost:8080/ui/documents/documents/102) | Liên kết đúng như UI hiện tại. |

<a id="screen-manager-consignments-detail"></a>
#### Hồ sơ KG-101 · Toyota Camry 2.5Q — `consignments/detail`

**Mục đích:** Tổng hợp chủ xe, trạng thái lần ký gửi, hợp đồng, kiểm định và giấy tờ.

**File HTML:** [manager/consignments/detail.html](../src/main/resources/templates/manager/consignments/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/consignments/101](http://localhost:8080/ui/manager/consignments/101) | SUBMITTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/consignments/102](http://localhost:8080/ui/manager/consignments/102) | DRAFT | Kiểm định hoàn tất; Giấy tờ đã xác minh; Agreement còn hiệu lực |
| [/ui/manager/consignments/103](http://localhost:8080/ui/manager/consignments/103) | RESERVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Chủ xe pháp lý; VIN; Giá kỳ vọng; Vị trí lưu giữ.

**Checklist cần đọc:** Kiểm định hoàn tất; Giấy tờ đã xác minh; Agreement còn hiệu lực. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Chủ xe** | Mở Chủ xe · Đỗ Minh Đức — **Nhân viên ký gửi** — [/ui/consignment/consignors/101](http://localhost:8080/ui/consignment/consignors/101) | Áp dụng mẫu consignments/101 |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-101 — **Quản lý** — [/ui/manager/consignments/101/withdraw](http://localhost:8080/ui/manager/consignments/101/withdraw) | Áp dụng mẫu consignments/101 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-101 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/101](http://localhost:8080/ui/consignment/agreements/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Giấy tờ** | Mở Checklist hồ sơ giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Đăng bán** | Mở Tin đăng & khả năng bán — **Kiểm định viên** — [/ui/inspector/listings](http://localhost:8080/ui/inspector/listings) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Chủ xe** | Mở Chủ xe · Lê Thị Mai — **Nhân viên ký gửi** — [/ui/consignment/consignors/102](http://localhost:8080/ui/consignment/consignors/102) | Áp dụng mẫu consignments/102 |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-102 — **Quản lý** — [/ui/manager/consignments/102/withdraw](http://localhost:8080/ui/manager/consignments/102/withdraw) | Áp dụng mẫu consignments/102 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-102 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/102](http://localhost:8080/ui/consignment/agreements/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-102 — **Kiểm định viên** — [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-102 — **Quản lý** — [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Chủ xe** | Mở Chủ xe · Nguyễn Quang Huy — **Nhân viên ký gửi** — [/ui/consignment/consignors/103](http://localhost:8080/ui/consignment/consignors/103) | Áp dụng mẫu consignments/103 |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-103 — **Quản lý** — [/ui/manager/consignments/103/withdraw](http://localhost:8080/ui/manager/consignments/103/withdraw) | Áp dụng mẫu consignments/103 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-103 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/103](http://localhost:8080/ui/consignment/agreements/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-103 — **Kiểm định viên** — [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-103 — **Quản lý** — [/ui/manager/approvals/103](http://localhost:8080/ui/manager/approvals/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-consignments-list"></a>
#### Hồ sơ ký gửi — `consignments/list`

**Mục đích:** Theo dõi từng lần ký gửi từ draft đến đóng hồ sơ; xe có thể được ký gửi lại.

**File HTML:** [manager/consignments/list.html](../src/main/resources/templates/manager/consignments/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/consignments](http://localhost:8080/ui/manager/consignments) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Mã · xe; Chủ xe; Ngày tiếp nhận; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tạo draft ký gửi** | Mở Tạo draft ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignments/new](http://localhost:8080/ui/consignment/consignments/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **KG-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Quản lý** — [/ui/manager/consignments/101](http://localhost:8080/ui/manager/consignments/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Quản lý** — [/ui/manager/consignments/102](http://localhost:8080/ui/manager/consignments/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-103 · Honda City RS**: Xem chi tiết | Mở Hồ sơ KG-103 · Honda City RS — **Quản lý** — [/ui/manager/consignments/103](http://localhost:8080/ui/manager/consignments/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-manager-consignments-withdraw"></a>
#### Rút / đóng ký gửi KG-101 — `consignments/withdraw`

**Mục đích:** Kiểm tra listing, reservation và tiền cọc trước khi trả xe cho chủ sở hữu.

**File HTML:** [manager/consignments/withdraw.html](../src/main/resources/templates/manager/consignments/withdraw.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/consignments/101/withdraw](http://localhost:8080/ui/manager/consignments/101/withdraw) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/consignments/102/withdraw](http://localhost:8080/ui/manager/consignments/102/withdraw) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/consignments/103/withdraw](http://localhost:8080/ui/manager/consignments/103/withdraw) | ACTIVE | Không có reservation ACTIVE: KG-103 còn tiền cọc 50.000.000 ₫ cần quyết định xử lý.; Có phương án xử lý khoản tiền |

**Thông tin cần đọc:** Xe; Chủ xe.

**Checklist cần đọc:** Không có reservation ACTIVE; Có phương án xử lý khoản tiền; Có thể dừng tin đăng. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Lý do rút / đóng | Có | Nhập nội dung nhiều dòng |
| Ngày dự kiến trả xe | Có | Chọn ngày |
| Tình trạng xe khi trả | Có | Nhập nội dung nhiều dòng |
| Bằng chứng bàn giao trả xe | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Quản lý** — [/ui/manager/consignments/101](http://localhost:8080/ui/manager/consignments/101) | Áp dụng mẫu consignments/101/withdraw |
| Đầu trang: **Xử lý reservation** | Mở Xử lý cọc RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103/cancel](http://localhost:8080/ui/sales/reservations/103/cancel) | Áp dụng mẫu consignments/101/withdraw |
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Quản lý** — [/ui/manager/consignments/102](http://localhost:8080/ui/manager/consignments/102) | Áp dụng mẫu consignments/102/withdraw |
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-103 · Honda City RS — **Quản lý** — [/ui/manager/consignments/103](http://localhost:8080/ui/manager/consignments/103) | Áp dụng mẫu consignments/103/withdraw |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-inquiries-detail"></a>
#### Tư vấn TV-101 · Trần Hoàng Nam — `inquiries/detail`

**Mục đích:** Theo dõi tư vấn, lịch xem và reservation. Chỉ người điều phối được gán lại.

**File HTML:** [manager/inquiries/detail.html](../src/main/resources/templates/manager/inquiries/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/inquiries/101](http://localhost:8080/ui/manager/inquiries/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/inquiries/102](http://localhost:8080/ui/manager/inquiries/102) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/inquiries/103](http://localhost:8080/ui/manager/inquiries/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Khách; Xe; Phụ trách; Kênh.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Người được phân công | Có | Chọn: Đỗ Lan Anh; Nguyễn Minh Sơn |
| Lý do điều phối | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Đặt lịch xem** | Mở Đặt lịch xem xe / lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings/new](http://localhost:8080/ui/sales/viewings/new) | Áp dụng mẫu inquiries/101 |
| Đầu trang: **Tạo reservation** | Mở Tạo reservation & cọc — **Nhân viên kinh doanh** — [/ui/sales/reservations/new](http://localhost:8080/ui/sales/reservations/new) | Áp dụng mẫu inquiries/101 |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-inquiries-list"></a>
#### Tư vấn & lịch xem xe — `inquiries/list`

**Mục đích:** Mỗi inquiry gắn buyer, vehicle, salesperson; lịch xem phát sinh từ inquiry.

**File HTML:** [manager/inquiries/list.html](../src/main/resources/templates/manager/inquiries/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/inquiries](http://localhost:8080/ui/manager/inquiries) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Tư vấn · khách; Xe quan tâm; Phụ trách; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tạo tư vấn** | Mở Ghi nhận tư vấn mua xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries/new](http://localhost:8080/ui/sales/inquiries/new) | Theo dữ liệu và trạng thái hiện tại. |
| Đầu trang: **Lịch xem xe** | Mở Lịch xem xe & lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings](http://localhost:8080/ui/sales/viewings) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **TV-101 · Trần Hoàng Nam**: Xem chi tiết | Mở Tư vấn TV-101 · Trần Hoàng Nam — **Quản lý** — [/ui/manager/inquiries/101](http://localhost:8080/ui/manager/inquiries/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-102 · Phạm Ngọc Anh**: Xem chi tiết | Mở Tư vấn TV-102 · Phạm Ngọc Anh — **Quản lý** — [/ui/manager/inquiries/102](http://localhost:8080/ui/manager/inquiries/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-103 · Bùi Đức Long**: Xem chi tiết | Mở Tư vấn TV-103 · Bùi Đức Long — **Quản lý** — [/ui/manager/inquiries/103](http://localhost:8080/ui/manager/inquiries/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-manager-listings-detail"></a>
#### Biên tập tin đăng TĐ-101 — `listings/detail`

**Mục đích:** Kiểm tra eligibility trước khi publish; trạng thái RESERVED phải giữ khả năng bán đúng.

**File HTML:** [manager/listings/detail.html](../src/main/resources/templates/manager/listings/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/listings/101](http://localhost:8080/ui/manager/listings/101) | LISTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/listings/102](http://localhost:8080/ui/manager/listings/102) | BLOCKED | Kiểm định hợp lệ; Giấy tờ VERIFIED: Giấy đăng ký còn PENDING_REVIEW. |
| [/ui/manager/listings/103](http://localhost:8080/ui/manager/listings/103) | RESERVED | Không có reservation active: Đã giữ cho Bùi Đức Long. |

**Thông tin cần đọc:** Xe; Giá đăng bán.

**Checklist cần đọc:** Kiểm định hợp lệ; Giấy tờ VERIFIED; Agreement active, chưa hết hạn; Không có reservation active. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Tiêu đề tin | Có | Nhập thông tin |
| Nội dung công khai | Có | Nhập nội dung nhiều dòng |
| Giá đăng bán (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Ảnh xe | Không / theo điều kiện | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tất cả tin đăng** | Mở Tin đăng & khả năng bán — **Quản lý** — [/ui/manager/listings](http://localhost:8080/ui/manager/listings) | Áp dụng mẫu listings/101 |
| Đầu trang: **Mô phỏng dừng tin** | Mở hộp thoại xác nhận → xác nhận thì thông báo mô phỏng tại chỗ, không mở trang khác. | Chỉ tin đang đăng bán có thể dừng. |
| Đầu trang: **Mô phỏng dừng tin** | Không mở trang; nút bị khóa. | Chỉ tin đang đăng bán có thể dừng. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-listings-list"></a>
#### Tin đăng & khả năng bán — `listings/list`

**Mục đích:** Chỉ cho phép đăng bán khi kiểm định, giấy tờ và hợp đồng hợp lệ.

**File HTML:** [manager/listings/list.html](../src/main/resources/templates/manager/listings/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/listings](http://localhost:8080/ui/manager/listings) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Tin đăng · xe; Giá; Điều kiện; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **TĐ-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Biên tập tin đăng TĐ-101 — **Quản lý** — [/ui/manager/listings/101](http://localhost:8080/ui/manager/listings/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TĐ-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Biên tập tin đăng TĐ-102 — **Quản lý** — [/ui/manager/listings/102](http://localhost:8080/ui/manager/listings/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TĐ-103 · Honda City RS**: Xem chi tiết | Mở Biên tập tin đăng TĐ-103 — **Quản lý** — [/ui/manager/listings/103](http://localhost:8080/ui/manager/listings/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-manager-reports-list"></a>
#### Báo cáo vận hành — `reports/list`

**Mục đích:** Tách tiền cọc khỏi hoa hồng; theo dõi xe bán, rút ký gửi và sang tên trễ hạn.

**File HTML:** [manager/reports/list.html](../src/main/resources/templates/manager/reports/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/reports](http://localhost:8080/ui/manager/reports) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Chỉ số; Giá trị; Diễn giải; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Đang đăng bán; Đã bán; Hoa hồng; Sang tên trễ. Chỉ số là dữ liệu mẫu.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Từ ngày | Có | Chọn ngày |
| Đến ngày | Có | Chọn ngày |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Xuất CSV mẫu** | Tải CSV mẫu theo dữ liệu đang lọc; ở lại màn hình. | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Xem khoảng ngày mẫu** | Kiểm tra khoảng ngày → review → kết quả tại chỗ. | Chỉ số vẫn cố định, không có truy vấn theo ngày thật. |

<a id="screen-manager-settlements-detail"></a>
#### Quyết toán QT-101 — `settlements/detail`

**Mục đích:** Bảng tính minh họa theo agreement v2, cần đối chiếu chứng từ và duyệt trước chi trả.

**File HTML:** [manager/settlements/detail.html](../src/main/resources/templates/manager/settlements/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/settlements/101](http://localhost:8080/ui/manager/settlements/101) | APPROVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/manager/settlements/102](http://localhost:8080/ui/manager/settlements/102) | PENDING | Khoản thu đã đối soát |
| [/ui/manager/settlements/103](http://localhost:8080/ui/manager/settlements/103) | PAID | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Hợp đồng; Agreement; Chủ xe; Giá cơ sở; Tài khoản chi trả; Giá cơ sở (VNĐ); Hoa hồng (%); Phí cố định (VNĐ); Chi phí đã duyệt (VNĐ); Hoàn tiền đã ghi nhận (VNĐ); Cọc giữ lại (VNĐ).

**Checklist cần đọc:** Agreement hiệu lực; Khoản thu đã đối soát. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Quyết định quyết toán | Có | Chọn: Yêu cầu bổ sung; Phê duyệt; Từ chối |
| Lý do quyết định | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách** | Mở Hoa hồng & quyết toán chủ xe — **Quản lý** — [/ui/manager/settlements](http://localhost:8080/ui/manager/settlements) | Áp dụng mẫu settlements/101 |
| Đầu trang: **Lệnh chi** | Mở Lệnh chi PC-101 — **Nhân viên tài chính** — [/ui/finance/payouts/101](http://localhost:8080/ui/finance/payouts/101) | Cần quyết toán APPROVED trước khi lập lệnh chi. |
| Đầu trang: **Lệnh chi** | Không mở trang; nút bị khóa. | Cần quyết toán APPROVED trước khi lập lệnh chi. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-manager-settlements-list"></a>
#### Hoa hồng & quyết toán chủ xe — `settlements/list`

**Mục đích:** Áp dụng đúng phiên bản agreement active; theo dõi net payout riêng với hoa hồng.

**File HTML:** [manager/settlements/list.html](../src/main/resources/templates/manager/settlements/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/manager/settlements](http://localhost:8080/ui/manager/settlements) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Quyết toán · hợp đồng; Chủ xe; Số tiền ròng; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **QT-101 · HĐB-101**: Xem chi tiết | Mở Quyết toán QT-101 — **Quản lý** — [/ui/manager/settlements/101](http://localhost:8080/ui/manager/settlements/101) | Liên kết đúng như UI hiện tại. |
| Dòng **QT-102 · HĐB-102**: Xem chi tiết | Mở Quyết toán QT-102 — **Quản lý** — [/ui/manager/settlements/102](http://localhost:8080/ui/manager/settlements/102) | Liên kết đúng như UI hiện tại. |
| Dòng **QT-103 · HĐB-103**: Xem chi tiết | Mở Quyết toán QT-103 — **Quản lý** — [/ui/manager/settlements/103](http://localhost:8080/ui/manager/settlements/103) | Liên kết đúng như UI hiện tại. |

<a id="role-sales"></a>
### Nhân viên kinh doanh (`sales`)

Tra nhanh:

- [Tổng quan](#screen-sales-dashboard)
- [Thêm khách mua xe](#screen-sales-buyers-create)
- [Khách mua · Trần Hoàng Nam](#screen-sales-buyers-detail)
- [Khách mua xe](#screen-sales-buyers-list)
- [Hợp đồng bán HĐB-101](#screen-sales-contracts-detail)
- [Hợp đồng bán xe](#screen-sales-contracts-list)
- [Ghi nhận tư vấn mua xe](#screen-sales-inquiries-create)
- [Tư vấn TV-101 · Trần Hoàng Nam](#screen-sales-inquiries-detail)
- [Tư vấn & lịch xem xe](#screen-sales-inquiries-list)
- [Xử lý cọc RS-101](#screen-sales-reservations-cancel)
- [Tạo reservation & cọc](#screen-sales-reservations-create)
- [Reservation RS-101](#screen-sales-reservations-detail)
- [Đặt cọc & giữ xe](#screen-sales-reservations-list)
- [Toyota Camry 2.5Q · 30H-628.19](#screen-sales-vehicles-detail)
- [Xe tại showroom](#screen-sales-vehicles-list)
- [Đặt lịch xem xe / lái thử](#screen-sales-viewings-create)
- [Lịch xem xe & lái thử](#screen-sales-viewings-list)

<a id="screen-sales-dashboard"></a>
#### Tổng quan · Nhân viên kinh doanh — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [sales/dashboard.html](../src/main/resources/templates/sales/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/dashboard-sales](http://localhost:8080/ui/sales/dashboard-sales) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Tư vấn · khách; Xe quan tâm; Phụ trách; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Đang tư vấn; Lịch xem xe; Đang giữ xe. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Tư vấn & lịch xem xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries](http://localhost:8080/ui/sales/inquiries) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **TV-101 · Trần Hoàng Nam**: Xem chi tiết | Mở Tư vấn TV-101 · Trần Hoàng Nam — **Nhân viên kinh doanh** — [/ui/sales/inquiries/101](http://localhost:8080/ui/sales/inquiries/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-102 · Phạm Ngọc Anh**: Xem chi tiết | Mở Tư vấn TV-102 · Phạm Ngọc Anh — **Nhân viên kinh doanh** — [/ui/sales/inquiries/102](http://localhost:8080/ui/sales/inquiries/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-103 · Bùi Đức Long**: Xem chi tiết | Mở Tư vấn TV-103 · Bùi Đức Long — **Nhân viên kinh doanh** — [/ui/sales/inquiries/103](http://localhost:8080/ui/sales/inquiries/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-sales-buyers-create"></a>
#### Thêm khách mua xe — `buyers/create`

**Mục đích:** Ghi nhận nhu cầu và đồng ý lưu dữ liệu; kiểm tra trùng cần backend.

**File HTML:** [sales/buyers/create.html](../src/main/resources/templates/sales/buyers/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/buyers/new](http://localhost:8080/ui/sales/buyers/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Họ và tên | Có | Nhập thông tin |
| Số điện thoại | Có | Nhập số điện thoại. 10–11 chữ số; kiểm tra trùng khi tích hợp backend. |
| Email | Không / theo điều kiện | Nhập email |
| Số CCCD | Có | Nhập thông tin. 12 chữ số. |
| Địa chỉ | Không / theo điều kiện | Nhập nội dung nhiều dòng |
| Đã nhận sự đồng ý lưu thông tin cá nhân | Có | Đánh dấu xác nhận |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách khách** | Mở Khách mua xe — **Nhân viên kinh doanh** — [/ui/sales/buyers](http://localhost:8080/ui/sales/buyers) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-sales-buyers-detail"></a>
#### Khách mua · Trần Hoàng Nam — `buyers/detail`

**Mục đích:** Xem các tư vấn, lịch xem và đặt cọc của khách.

**File HTML:** [sales/buyers/detail.html](../src/main/resources/templates/sales/buyers/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/buyers/101](http://localhost:8080/ui/sales/buyers/101) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/buyers/102](http://localhost:8080/ui/sales/buyers/102) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/buyers/103](http://localhost:8080/ui/sales/buyers/103) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Tên khách; Liên hệ; Đồng ý lưu dữ liệu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tư vấn liên quan** | Mở Tư vấn TV-101 · Trần Hoàng Nam — **Nhân viên kinh doanh** — [/ui/sales/inquiries/101](http://localhost:8080/ui/sales/inquiries/101) | Áp dụng mẫu buyers/101 |
| Đầu trang: **Đặt cọc liên quan** | Mở Reservation RS-101 — **Nhân viên kinh doanh** — [/ui/sales/reservations/101](http://localhost:8080/ui/sales/reservations/101) | Áp dụng mẫu buyers/101 |
| Đầu trang: **Tư vấn liên quan** | Mở Tư vấn TV-102 · Phạm Ngọc Anh — **Nhân viên kinh doanh** — [/ui/sales/inquiries/102](http://localhost:8080/ui/sales/inquiries/102) | Áp dụng mẫu buyers/102 |
| Đầu trang: **Đặt cọc liên quan** | Mở Reservation RS-102 — **Nhân viên kinh doanh** — [/ui/sales/reservations/102](http://localhost:8080/ui/sales/reservations/102) | Áp dụng mẫu buyers/102 |
| Đầu trang: **Tư vấn liên quan** | Mở Tư vấn TV-103 · Bùi Đức Long — **Nhân viên kinh doanh** — [/ui/sales/inquiries/103](http://localhost:8080/ui/sales/inquiries/103) | Áp dụng mẫu buyers/103 |
| Đầu trang: **Đặt cọc liên quan** | Mở Reservation RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103](http://localhost:8080/ui/sales/reservations/103) | Áp dụng mẫu buyers/103 |

<a id="screen-sales-buyers-list"></a>
#### Khách mua xe — `buyers/list`

**Mục đích:** Nhân viên tạo buyer; không có portal buyer tự phục vụ trong MVP.

**File HTML:** [sales/buyers/list.html](../src/main/resources/templates/sales/buyers/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/buyers](http://localhost:8080/ui/sales/buyers) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Khách hàng; Liên hệ; Nhu cầu; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Thêm khách mua** | Mở Thêm khách mua xe — **Nhân viên kinh doanh** — [/ui/sales/buyers/new](http://localhost:8080/ui/sales/buyers/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Trần Hoàng Nam**: Xem chi tiết | Mở Khách mua · Trần Hoàng Nam — **Nhân viên kinh doanh** — [/ui/sales/buyers/101](http://localhost:8080/ui/sales/buyers/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Phạm Ngọc Anh**: Xem chi tiết | Mở Khách mua · Phạm Ngọc Anh — **Nhân viên kinh doanh** — [/ui/sales/buyers/102](http://localhost:8080/ui/sales/buyers/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Bùi Đức Long**: Xem chi tiết | Mở Khách mua · Bùi Đức Long — **Nhân viên kinh doanh** — [/ui/sales/buyers/103](http://localhost:8080/ui/sales/buyers/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-sales-contracts-detail"></a>
#### Hợp đồng bán HĐB-101 — `contracts/detail`

**Mục đích:** Shop là bên trung gian; cần xác định người bán hợp pháp và giấy ủy quyền khi có.

**File HTML:** [sales/contracts/detail.html](../src/main/resources/templates/sales/contracts/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/contracts/101](http://localhost:8080/ui/sales/contracts/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/contracts/102](http://localhost:8080/ui/sales/contracts/102) | DRAFT | Reservation ACTIVE: RS-102 đã hết hạn.; Xe đủ điều kiện bán |
| [/ui/sales/contracts/103](http://localhost:8080/ui/sales/contracts/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Reservation; Buyer; Chủ xe pháp lý; Agreement áp dụng; Cọc đang giữ.

**Checklist cần đọc:** Reservation ACTIVE; Xe đủ điều kiện bán; Quyền người bán / ủy quyền. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Bên bán | Có | Chọn: Chủ xe trực tiếp; Đại diện được ủy quyền |
| Giấy ủy quyền | Không / theo điều kiện | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server. Bắt buộc khi bên bán là đại diện được ủy quyền; ẩn khi chủ xe trực tiếp. |
| Vai trò shop | Có | Nhập thông tin |
| Giá thỏa thuận (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Cọc đang giữ (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Ngày ký | Có | Chọn ngày |
| Bằng chứng chữ ký | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Reservation** | Mở Reservation RS-101 — **Nhân viên kinh doanh** — [/ui/sales/reservations/101](http://localhost:8080/ui/sales/reservations/101) | Áp dụng mẫu contracts/101 |
| Đầu trang: **In hợp đồng mẫu** | Mở hộp thoại in của trình duyệt; ở lại màn hình. | Áp dụng mẫu contracts/101 |
| Các bước liên quan: **Thanh toán** | Mở Giao dịch thanh toán — **Nhân viên tài chính** — [/ui/finance/payments](http://localhost:8080/ui/finance/payments) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-101 — **Nhân viên hồ sơ** — [/ui/documents/transfers/101](http://localhost:8080/ui/documents/transfers/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Bàn giao** | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Reservation** | Mở Reservation RS-102 — **Nhân viên kinh doanh** — [/ui/sales/reservations/102](http://localhost:8080/ui/sales/reservations/102) | Áp dụng mẫu contracts/102 |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-102 — **Nhân viên hồ sơ** — [/ui/documents/transfers/102](http://localhost:8080/ui/documents/transfers/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Bàn giao** | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Reservation** | Mở Reservation RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103](http://localhost:8080/ui/sales/reservations/103) | Áp dụng mẫu contracts/103 |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-103 — **Nhân viên hồ sơ** — [/ui/documents/transfers/103](http://localhost:8080/ui/documents/transfers/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Bàn giao** | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Tính tại chỗ:** số còn lại = giá thỏa thuận − cọc đang giữ. Cọc vượt giá bị báo lỗi. Chọn bên bán được ủy quyền thì phải chọn file ủy quyền.

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-sales-contracts-list"></a>
#### Hợp đồng bán xe — `contracts/list`

**Mục đích:** Tạo hợp đồng từ active reservation; đối chiếu giá sàn và quyền đại diện người bán.

**File HTML:** [sales/contracts/list.html](../src/main/resources/templates/sales/contracts/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/contracts](http://localhost:8080/ui/sales/contracts) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hợp đồng; Khách · xe; Giá thỏa thuận; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **HĐB-101**: Xem chi tiết | Mở Hợp đồng bán HĐB-101 — **Nhân viên kinh doanh** — [/ui/sales/contracts/101](http://localhost:8080/ui/sales/contracts/101) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-102**: Xem chi tiết | Mở Hợp đồng bán HĐB-102 — **Nhân viên kinh doanh** — [/ui/sales/contracts/102](http://localhost:8080/ui/sales/contracts/102) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-103**: Xem chi tiết | Mở Hợp đồng bán HĐB-103 — **Nhân viên kinh doanh** — [/ui/sales/contracts/103](http://localhost:8080/ui/sales/contracts/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-sales-inquiries-create"></a>
#### Ghi nhận tư vấn mua xe — `inquiries/create`

**Mục đích:** Tư vấn mua/xem xe là Inquiry. Nhu cầu ký gửi cần mở hồ sơ Consignment.

**File HTML:** [sales/inquiries/create.html](../src/main/resources/templates/sales/inquiries/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/inquiries/new](http://localhost:8080/ui/sales/inquiries/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Phụ trách tự gán.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Khách mua | Có | Chọn: Trần Hoàng Nam; Phạm Ngọc Anh; Bùi Đức Long |
| Xe quan tâm | Có | Chọn: Toyota Camry 2.5Q; Mazda CX-5 Premium; Honda City RS |
| Kênh liên hệ | Có | Chọn: Tại showroom; Điện thoại; Website |
| Nhu cầu | Có | Chọn: Hỏi giá; Xem xe; Lái thử |
| Ghi chú | Không / theo điều kiện | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách tư vấn** | Mở Tư vấn & lịch xem xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries](http://localhost:8080/ui/sales/inquiries) | Theo dữ liệu và trạng thái hiện tại. |
| Các bước liên quan: **Đặt lịch xem xe** | Mở Đặt lịch xem xe / lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings/new](http://localhost:8080/ui/sales/viewings/new) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-sales-inquiries-detail"></a>
#### Tư vấn TV-101 · Trần Hoàng Nam — `inquiries/detail`

**Mục đích:** Theo dõi tư vấn, lịch xem và reservation. Chỉ người điều phối được gán lại.

**File HTML:** [sales/inquiries/detail.html](../src/main/resources/templates/sales/inquiries/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/inquiries/101](http://localhost:8080/ui/sales/inquiries/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/inquiries/102](http://localhost:8080/ui/sales/inquiries/102) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/inquiries/103](http://localhost:8080/ui/sales/inquiries/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Khách; Xe; Phụ trách; Kênh.

**Không có form phân công cho Sales:** form chọn người phụ trách/lý do được ẩn. Muốn điều phối phải mở giao diện Manager.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Đặt lịch xem** | Mở Đặt lịch xem xe / lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings/new](http://localhost:8080/ui/sales/viewings/new) | Áp dụng mẫu inquiries/101 |
| Đầu trang: **Tạo reservation** | Mở Tạo reservation & cọc — **Nhân viên kinh doanh** — [/ui/sales/reservations/new](http://localhost:8080/ui/sales/reservations/new) | Áp dụng mẫu inquiries/101 |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-sales-inquiries-list"></a>
#### Tư vấn & lịch xem xe — `inquiries/list`

**Mục đích:** Mỗi inquiry gắn buyer, vehicle, salesperson; lịch xem phát sinh từ inquiry.

**File HTML:** [sales/inquiries/list.html](../src/main/resources/templates/sales/inquiries/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/inquiries](http://localhost:8080/ui/sales/inquiries) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Tư vấn · khách; Xe quan tâm; Phụ trách; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tạo tư vấn** | Mở Ghi nhận tư vấn mua xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries/new](http://localhost:8080/ui/sales/inquiries/new) | Theo dữ liệu và trạng thái hiện tại. |
| Đầu trang: **Lịch xem xe** | Mở Lịch xem xe & lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings](http://localhost:8080/ui/sales/viewings) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **TV-101 · Trần Hoàng Nam**: Xem chi tiết | Mở Tư vấn TV-101 · Trần Hoàng Nam — **Nhân viên kinh doanh** — [/ui/sales/inquiries/101](http://localhost:8080/ui/sales/inquiries/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-102 · Phạm Ngọc Anh**: Xem chi tiết | Mở Tư vấn TV-102 · Phạm Ngọc Anh — **Nhân viên kinh doanh** — [/ui/sales/inquiries/102](http://localhost:8080/ui/sales/inquiries/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TV-103 · Bùi Đức Long**: Xem chi tiết | Mở Tư vấn TV-103 · Bùi Đức Long — **Nhân viên kinh doanh** — [/ui/sales/inquiries/103](http://localhost:8080/ui/sales/inquiries/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-sales-reservations-cancel"></a>
#### Xử lý cọc RS-101 — `reservations/cancel`

**Mục đích:** Hủy/hết hạn cần quyết định hoàn hoặc giữ cọc rõ ràng, có lý do và người duyệt.

**File HTML:** [sales/reservations/cancel.html](../src/main/resources/templates/sales/reservations/cancel.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/reservations/101/cancel](http://localhost:8080/ui/sales/reservations/101/cancel) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/reservations/102/cancel](http://localhost:8080/ui/sales/reservations/102/cancel) | EXPIRED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/reservations/103/cancel](http://localhost:8080/ui/sales/reservations/103/cancel) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Tiền cọc đã thu; Khách; Reservation.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Ngữ cảnh xử lý | Có | Chọn: Khách hủy; Hết hạn giữ xe |
| Phương án cọc | Có | Chọn: Hoàn toàn bộ; Giữ toàn bộ; Hoàn một phần |
| Số tiền hoàn (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Số tiền giữ (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Lý do quyết định | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Reservation** | Mở Reservation RS-101 — **Nhân viên kinh doanh** — [/ui/sales/reservations/101](http://localhost:8080/ui/sales/reservations/101) | Áp dụng mẫu reservations/101/cancel |
| Các bước liên quan: **Finance ghi nhận hoàn** | Mở Ghi nhận hoàn tiền — **Nhân viên tài chính** — [/ui/finance/refunds/new](http://localhost:8080/ui/finance/refunds/new) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Reservation** | Mở Reservation RS-102 — **Nhân viên kinh doanh** — [/ui/sales/reservations/102](http://localhost:8080/ui/sales/reservations/102) | Áp dụng mẫu reservations/102/cancel |
| Đầu trang: **Reservation** | Mở Reservation RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103](http://localhost:8080/ui/sales/reservations/103) | Áp dụng mẫu reservations/103/cancel |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Kiểm tra tiền cọc:** tổng hoàn + giữ phải bằng 50.000.000 ₫ của fixture; hai phương án toàn bộ tự điền và khóa số tiền, hoàn một phần cho nhập cả hai.

<a id="screen-sales-reservations-create"></a>
#### Tạo reservation & cọc — `reservations/create`

**Mục đích:** Tạo từ inquiry, giữ đúng khách và xe; cọc chưa phải doanh thu của shop.

**File HTML:** [sales/reservations/create.html](../src/main/resources/templates/sales/reservations/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/reservations/new](http://localhost:8080/ui/sales/reservations/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Khách mua; Xe; Nhân viên.

**Checklist cần đọc:** Xe đủ điều kiện bán; Không có reservation ACTIVE khác. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Phiếu tư vấn | Có | Chọn: TV-101 · Trần Hoàng Nam · Toyota Camry |
| Tiền cọc (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Hết hạn giữ xe | Có | Chọn ngày và giờ |
| Điều khoản cọc | Có | Nhập nội dung nhiều dòng |
| Khách đã đồng ý điều khoản giữ xe | Có | Đánh dấu xác nhận |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách giữ xe** | Mở Đặt cọc & giữ xe — **Nhân viên kinh doanh** — [/ui/sales/reservations](http://localhost:8080/ui/sales/reservations) | Theo dữ liệu và trạng thái hiện tại. |
| Các bước liên quan: **Hợp đồng bán** | Mở Hợp đồng bán HĐB-101 — **Nhân viên kinh doanh** — [/ui/sales/contracts/101](http://localhost:8080/ui/sales/contracts/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

<a id="screen-sales-reservations-detail"></a>
#### Reservation RS-101 — `reservations/detail`

**Mục đích:** Theo dõi tiền cọc, hiệu lực giữ xe và đường dẫn chuyển thành hợp đồng.

**File HTML:** [sales/reservations/detail.html](../src/main/resources/templates/sales/reservations/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/reservations/101](http://localhost:8080/ui/sales/reservations/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/reservations/102](http://localhost:8080/ui/sales/reservations/102) | EXPIRED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/reservations/103](http://localhost:8080/ui/sales/reservations/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Khách; Xe; Tiền cọc; Hết hạn; Inquiry gốc; Điều khoản cọc.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hủy / hết hạn & xử lý cọc** | Mở Xử lý cọc RS-101 — **Nhân viên kinh doanh** — [/ui/sales/reservations/101/cancel](http://localhost:8080/ui/sales/reservations/101/cancel) | Áp dụng mẫu reservations/101 |
| Đầu trang: **Hợp đồng bán** | Mở Hợp đồng bán HĐB-101 — **Nhân viên kinh doanh** — [/ui/sales/contracts/101](http://localhost:8080/ui/sales/contracts/101) | Reservation đã hết hạn; không thể chuyển thành hợp đồng. |
| Đầu trang: **Hủy / hết hạn & xử lý cọc** | Mở Xử lý cọc RS-102 — **Nhân viên kinh doanh** — [/ui/sales/reservations/102/cancel](http://localhost:8080/ui/sales/reservations/102/cancel) | Áp dụng mẫu reservations/102 |
| Đầu trang: **Hợp đồng bán** | Không mở trang; nút bị khóa. | Reservation đã hết hạn; không thể chuyển thành hợp đồng. |
| Đầu trang: **Hủy / hết hạn & xử lý cọc** | Mở Xử lý cọc RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103/cancel](http://localhost:8080/ui/sales/reservations/103/cancel) | Áp dụng mẫu reservations/103 |
| Đầu trang: **Hợp đồng bán** | Mở Hợp đồng bán HĐB-103 — **Nhân viên kinh doanh** — [/ui/sales/contracts/103](http://localhost:8080/ui/sales/contracts/103) | Reservation đã hết hạn; không thể chuyển thành hợp đồng. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-sales-reservations-list"></a>
#### Đặt cọc & giữ xe — `reservations/list`

**Mục đích:** Một xe tối đa một reservation ACTIVE; theo dõi ngày hết hạn và xử lý tiền cọc.

**File HTML:** [sales/reservations/list.html](../src/main/resources/templates/sales/reservations/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/reservations](http://localhost:8080/ui/sales/reservations) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Reservation · khách; Xe; Tiền cọc; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tạo reservation** | Mở Tạo reservation & cọc — **Nhân viên kinh doanh** — [/ui/sales/reservations/new](http://localhost:8080/ui/sales/reservations/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **RS-101 · Trần Hoàng Nam**: Xem chi tiết | Mở Reservation RS-101 — **Nhân viên kinh doanh** — [/ui/sales/reservations/101](http://localhost:8080/ui/sales/reservations/101) | Liên kết đúng như UI hiện tại. |
| Dòng **RS-102 · Phạm Ngọc Anh**: Xem chi tiết | Mở Reservation RS-102 — **Nhân viên kinh doanh** — [/ui/sales/reservations/102](http://localhost:8080/ui/sales/reservations/102) | Liên kết đúng như UI hiện tại. |
| Dòng **RS-103 · Bùi Đức Long**: Xem chi tiết | Mở Reservation RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103](http://localhost:8080/ui/sales/reservations/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-sales-vehicles-detail"></a>
#### Toyota Camry 2.5Q · 30H-628.19 — `vehicles/detail`

**Mục đích:** Hồ sơ xe vật lý, kiểm định, giấy tờ và lịch sử ký gửi.

**File HTML:** [sales/vehicles/detail.html](../src/main/resources/templates/sales/vehicles/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/vehicles/101](http://localhost:8080/ui/sales/vehicles/101) | LISTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/sales/vehicles/102](http://localhost:8080/ui/sales/vehicles/102) | DOCUMENT_REVIEW | Kiểm định; Giấy tờ được xác minh |
| [/ui/sales/vehicles/103](http://localhost:8080/ui/sales/vehicles/103) | RESERVED | Không được giữ bởi khách khác |

**Thông tin cần đọc:** VIN; Năm sản xuất; Số km; Chủ xe pháp lý; Giá niêm yết; Vị trí lưu giữ.

**Checklist cần đọc:** Kiểm định; Giấy tờ được xác minh; Không được giữ bởi khách khác. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Ghi nhận tư vấn** | Mở Ghi nhận tư vấn mua xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries/new](http://localhost:8080/ui/sales/inquiries/new) | Áp dụng mẫu vehicles/101 |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-sales-vehicles-list"></a>
#### Xe tại showroom — `vehicles/list`

**Mục đích:** Định danh xe vật lý theo VIN; theo dõi từng lần ký gửi và khả năng bán.

**File HTML:** [sales/vehicles/list.html](../src/main/resources/templates/sales/vehicles/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/vehicles](http://localhost:8080/ui/sales/vehicles) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Xe · biển số; Năm · odo; Giá niêm yết; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **Toyota Camry 2.5Q · 30H-628.19**: Xem chi tiết | Mở Toyota Camry 2.5Q · 30H-628.19 — **Nhân viên kinh doanh** — [/ui/sales/vehicles/101](http://localhost:8080/ui/sales/vehicles/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Mazda CX-5 Premium · 30K-215.68**: Xem chi tiết | Mở Mazda CX-5 Premium · 30K-215.68 — **Nhân viên kinh doanh** — [/ui/sales/vehicles/102](http://localhost:8080/ui/sales/vehicles/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Honda City RS · 29A-718.22**: Xem chi tiết | Mở Honda City RS · 29A-718.22 — **Nhân viên kinh doanh** — [/ui/sales/vehicles/103](http://localhost:8080/ui/sales/vehicles/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-sales-viewings-create"></a>
#### Đặt lịch xem xe / lái thử — `viewings/create`

**Mục đích:** Chọn inquiry và thời gian; kiểm tra trùng lịch toàn hệ thống cần backend.

**File HTML:** [sales/viewings/create.html](../src/main/resources/templates/sales/viewings/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/viewings/new](http://localhost:8080/ui/sales/viewings/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Phiếu tư vấn | Có | Chọn: TV-101 · Trần Hoàng Nam; TV-102 · Phạm Ngọc Anh; TV-103 · Bùi Đức Long |
| Thời gian hẹn | Có | Chọn ngày và giờ |
| Loại lịch | Có | Chọn: Xem xe; Lái thử |
| Ghi chú | Không / theo điều kiện | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Lịch xem xe** | Mở Lịch xem xe & lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings](http://localhost:8080/ui/sales/viewings) | Theo dữ liệu và trạng thái hiện tại. |
| Các bước liên quan: **Đặt cọc giữ xe** | Mở Tạo reservation & cọc — **Nhân viên kinh doanh** — [/ui/sales/reservations/new](http://localhost:8080/ui/sales/reservations/new) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-sales-viewings-list"></a>
#### Lịch xem xe & lái thử — `viewings/list`

**Mục đích:** Lịch thuộc inquiry; hiển thị thời gian, khách, xe và trạng thái.

**File HTML:** [sales/viewings/list.html](../src/main/resources/templates/sales/viewings/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/sales/viewings](http://localhost:8080/ui/sales/viewings) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Thời gian; Khách · xe; Loại lịch; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Đặt lịch** | Mở Đặt lịch xem xe / lái thử — **Nhân viên kinh doanh** — [/ui/sales/viewings/new](http://localhost:8080/ui/sales/viewings/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **15/10/2026 10:30**: Xem chi tiết | Mở Tư vấn TV-101 · Trần Hoàng Nam — **Nhân viên kinh doanh** — [/ui/sales/inquiries/101](http://localhost:8080/ui/sales/inquiries/101) | Liên kết đúng như UI hiện tại. |
| Dòng **16/10/2026 14:00**: Xem chi tiết | Mở Tư vấn TV-102 · Phạm Ngọc Anh — **Nhân viên kinh doanh** — [/ui/sales/inquiries/102](http://localhost:8080/ui/sales/inquiries/102) | Liên kết đúng như UI hiện tại. |
| Dòng **17/10/2026 09:00**: Xem chi tiết | Mở Tư vấn TV-103 · Bùi Đức Long — **Nhân viên kinh doanh** — [/ui/sales/inquiries/103](http://localhost:8080/ui/sales/inquiries/103) | Liên kết đúng như UI hiện tại. |

<a id="role-consignment"></a>
### Nhân viên ký gửi (`consignment`)

Tra nhanh:

- [Tổng quan](#screen-consignment-dashboard)
- [Hợp đồng ký gửi HĐKG-101 · v2](#screen-consignment-agreements-detail)
- [Tạo draft ký gửi](#screen-consignment-consignments-create)
- [Hồ sơ KG-101 · Toyota Camry 2.5Q](#screen-consignment-consignments-detail)
- [Sửa draft KG-102](#screen-consignment-consignments-edit)
- [Hồ sơ ký gửi](#screen-consignment-consignments-list)
- [Rút / đóng ký gửi KG-101](#screen-consignment-consignments-withdraw)
- [Tạo hồ sơ chủ xe](#screen-consignment-consignors-create)
- [Chủ xe · Đỗ Minh Đức](#screen-consignment-consignors-detail)
- [Chủ xe ký gửi](#screen-consignment-consignors-list)

<a id="screen-consignment-dashboard"></a>
#### Tổng quan · Nhân viên ký gửi — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [consignment/dashboard.html](../src/main/resources/templates/consignment/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/dashboard-consignment](http://localhost:8080/ui/consignment/dashboard-consignment) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Mã · xe; Chủ xe; Ngày tiếp nhận; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Hồ sơ mới; Chờ chữ ký; Yêu cầu rút. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Hồ sơ ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignments](http://localhost:8080/ui/consignment/consignments) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **KG-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Nhân viên ký gửi** — [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-103 · Honda City RS**: Xem chi tiết | Mở Hồ sơ KG-103 · Honda City RS — **Nhân viên ký gửi** — [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-consignment-agreements-detail"></a>
#### Hợp đồng ký gửi HĐKG-101 · v2 — `agreements/detail`

**Mục đích:** Agreement active giữ nguyên nội dung; sửa đổi tạo phiên bản mới.

**File HTML:** [consignment/agreements/detail.html](../src/main/resources/templates/consignment/agreements/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/agreements/101](http://localhost:8080/ui/consignment/agreements/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/agreements/102](http://localhost:8080/ui/consignment/agreements/102) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/agreements/103](http://localhost:8080/ui/consignment/agreements/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Hồ sơ; Chủ xe; Hoa hồng; Phí cố định; Giá sàn; Hiệu lực; Chữ ký; Điều khoản lưu giữ; Điều khoản chấm dứt; Điều khoản chi trả.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Hoa hồng (%) | Có | Nhập số; UI không nhận số âm |
| Phí cố định (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Giá sàn (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Hiệu lực từ | Có | Chọn ngày |
| Hiệu lực đến | Có | Chọn ngày |
| Điều khoản lưu giữ | Có | Nhập nội dung nhiều dòng |
| Điều khoản chấm dứt | Có | Nhập nội dung nhiều dòng |
| Điều khoản chi trả | Có | Nhập nội dung nhiều dòng |
| Bằng chứng chữ ký | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Nhân viên ký gửi** — [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | Áp dụng mẫu agreements/101 |
| Đầu trang: **In bản mẫu** | Mở hộp thoại in của trình duyệt; ở lại màn hình. | Áp dụng mẫu agreements/101 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Áp dụng mẫu agreements/102 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-103 · Honda City RS — **Nhân viên ký gửi** — [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | Áp dụng mẫu agreements/103 |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-consignment-consignments-create"></a>
#### Tạo draft ký gửi — `consignments/create`

**Mục đích:** Chọn chủ xe và xe vật lý; có thể sửa trước khi gửi duyệt.

**File HTML:** [consignment/consignments/create.html](../src/main/resources/templates/consignment/consignments/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignments/new](http://localhost:8080/ui/consignment/consignments/new) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Chủ xe | Có | Chọn: Đỗ Minh Đức; Lê Thị Mai; Nguyễn Quang Huy |
| Xe vật lý | Có | Chọn: Toyota Camry 2.5Q; Mazda CX-5 Premium; Honda City RS |
| Giá kỳ vọng (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Ngày dự kiến tiếp nhận | Có | Chọn ngày |
| Yêu cầu / điều khoản | Không / theo điều kiện | Nhập nội dung nhiều dòng |
| Chủ xe đồng ý cung cấp thông tin | Có | Đánh dấu xác nhận |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách hồ sơ** | Mở Hồ sơ ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignments](http://localhost:8080/ui/consignment/consignments) | Theo dữ liệu và trạng thái hiện tại. |
| Các bước liên quan: **Tiếp nhận xe** | Mở Tiếp nhận xe ký gửi — **Kiểm định viên** — [/ui/inspector/vehicles/intake](http://localhost:8080/ui/inspector/vehicles/intake) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Lập hợp đồng** | Mở Hợp đồng ký gửi HĐKG-101 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/101](http://localhost:8080/ui/consignment/agreements/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-consignment-consignments-detail"></a>
#### Hồ sơ KG-101 · Toyota Camry 2.5Q — `consignments/detail`

**Mục đích:** Tổng hợp chủ xe, trạng thái lần ký gửi, hợp đồng, kiểm định và giấy tờ.

**File HTML:** [consignment/consignments/detail.html](../src/main/resources/templates/consignment/consignments/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | SUBMITTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | DRAFT | Kiểm định hoàn tất; Giấy tờ đã xác minh; Agreement còn hiệu lực |
| [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | RESERVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Chủ xe pháp lý; VIN; Giá kỳ vọng; Vị trí lưu giữ.

**Checklist cần đọc:** Kiểm định hoàn tất; Giấy tờ đã xác minh; Agreement còn hiệu lực. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Chủ xe** | Mở Chủ xe · Đỗ Minh Đức — **Nhân viên ký gửi** — [/ui/consignment/consignors/101](http://localhost:8080/ui/consignment/consignors/101) | Áp dụng mẫu consignments/101 |
| Đầu trang: **Sửa draft** | Không mở trang; nút bị khóa. | Chỉ DRAFT được sửa trực tiếp. |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-101 — **Nhân viên ký gửi** — [/ui/consignment/consignments/101/withdraw](http://localhost:8080/ui/consignment/consignments/101/withdraw) | Áp dụng mẫu consignments/101 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-101 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/101](http://localhost:8080/ui/consignment/agreements/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Giấy tờ** | Mở Checklist hồ sơ giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Đăng bán** | Mở Tin đăng & khả năng bán — **Kiểm định viên** — [/ui/inspector/listings](http://localhost:8080/ui/inspector/listings) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Chủ xe** | Mở Chủ xe · Lê Thị Mai — **Nhân viên ký gửi** — [/ui/consignment/consignors/102](http://localhost:8080/ui/consignment/consignors/102) | Áp dụng mẫu consignments/102 |
| Đầu trang: **Sửa draft** | Mở Sửa draft KG-102 — **Nhân viên ký gửi** — [/ui/consignment/consignments/102/edit](http://localhost:8080/ui/consignment/consignments/102/edit) | Chỉ DRAFT được sửa trực tiếp. |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-102 — **Nhân viên ký gửi** — [/ui/consignment/consignments/102/withdraw](http://localhost:8080/ui/consignment/consignments/102/withdraw) | Áp dụng mẫu consignments/102 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-102 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/102](http://localhost:8080/ui/consignment/agreements/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-102 — **Kiểm định viên** — [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-102 — **Quản lý** — [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Chủ xe** | Mở Chủ xe · Nguyễn Quang Huy — **Nhân viên ký gửi** — [/ui/consignment/consignors/103](http://localhost:8080/ui/consignment/consignors/103) | Áp dụng mẫu consignments/103 |
| Đầu trang: **Rút / đóng ký gửi** | Mở Rút / đóng ký gửi KG-103 — **Nhân viên ký gửi** — [/ui/consignment/consignments/103/withdraw](http://localhost:8080/ui/consignment/consignments/103/withdraw) | Áp dụng mẫu consignments/103 |
| Các bước liên quan: **Hợp đồng ký gửi** | Mở Hợp đồng ký gửi HĐKG-103 · v2 — **Nhân viên ký gửi** — [/ui/consignment/agreements/103](http://localhost:8080/ui/consignment/agreements/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Kiểm định xe** | Mở Checklist kiểm định KD-103 — **Kiểm định viên** — [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Phê duyệt** | Mở Đánh giá yêu cầu PD-103 — **Quản lý** — [/ui/manager/approvals/103](http://localhost:8080/ui/manager/approvals/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-consignment-consignments-edit"></a>
#### Sửa draft KG-102 — `consignments/edit`

**Mục đích:** Chỉ draft được sửa trực tiếp. Sau submit phải có version hoặc approval.

**File HTML:** [consignment/consignments/edit.html](../src/main/resources/templates/consignment/consignments/edit.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignments/102/edit](http://localhost:8080/ui/consignment/consignments/102/edit) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Chủ xe | Có | Chọn: Đỗ Minh Đức; Lê Thị Mai; Nguyễn Quang Huy |
| Giá kỳ vọng (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Ngày tiếp nhận | Có | Chọn ngày |
| Điều khoản dự thảo | Không / theo điều kiện | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Xem hồ sơ** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-consignment-consignments-list"></a>
#### Hồ sơ ký gửi — `consignments/list`

**Mục đích:** Theo dõi từng lần ký gửi từ draft đến đóng hồ sơ; xe có thể được ký gửi lại.

**File HTML:** [consignment/consignments/list.html](../src/main/resources/templates/consignment/consignments/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignments](http://localhost:8080/ui/consignment/consignments) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Mã · xe; Chủ xe; Ngày tiếp nhận; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tạo draft ký gửi** | Mở Tạo draft ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignments/new](http://localhost:8080/ui/consignment/consignments/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **KG-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Nhân viên ký gửi** — [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-103 · Honda City RS**: Xem chi tiết | Mở Hồ sơ KG-103 · Honda City RS — **Nhân viên ký gửi** — [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-consignment-consignments-withdraw"></a>
#### Rút / đóng ký gửi KG-101 — `consignments/withdraw`

**Mục đích:** Kiểm tra listing, reservation và tiền cọc trước khi trả xe cho chủ sở hữu.

**File HTML:** [consignment/consignments/withdraw.html](../src/main/resources/templates/consignment/consignments/withdraw.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignments/101/withdraw](http://localhost:8080/ui/consignment/consignments/101/withdraw) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/consignments/102/withdraw](http://localhost:8080/ui/consignment/consignments/102/withdraw) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/consignments/103/withdraw](http://localhost:8080/ui/consignment/consignments/103/withdraw) | ACTIVE | Không có reservation ACTIVE: KG-103 còn tiền cọc 50.000.000 ₫ cần quyết định xử lý.; Có phương án xử lý khoản tiền |

**Thông tin cần đọc:** Xe; Chủ xe.

**Checklist cần đọc:** Không có reservation ACTIVE; Có phương án xử lý khoản tiền; Có thể dừng tin đăng. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Lý do rút / đóng | Có | Nhập nội dung nhiều dòng |
| Ngày dự kiến trả xe | Có | Chọn ngày |
| Tình trạng xe khi trả | Có | Nhập nội dung nhiều dòng |
| Bằng chứng bàn giao trả xe | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Nhân viên ký gửi** — [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | Áp dụng mẫu consignments/101/withdraw |
| Đầu trang: **Xử lý reservation** | Mở Xử lý cọc RS-103 — **Nhân viên kinh doanh** — [/ui/sales/reservations/103/cancel](http://localhost:8080/ui/sales/reservations/103/cancel) | Áp dụng mẫu consignments/101/withdraw |
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Áp dụng mẫu consignments/102/withdraw |
| Đầu trang: **Hồ sơ** | Mở Hồ sơ KG-103 · Honda City RS — **Nhân viên ký gửi** — [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | Áp dụng mẫu consignments/103/withdraw |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-consignment-consignors-create"></a>
#### Tạo hồ sơ chủ xe — `consignors/create`

**Mục đích:** Thông tin liên hệ, đồng ý lưu dữ liệu và tài khoản nhận tiền.

**File HTML:** [consignment/consignors/create.html](../src/main/resources/templates/consignment/consignors/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignors/new](http://localhost:8080/ui/consignment/consignors/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Họ và tên | Có | Nhập thông tin |
| Số điện thoại | Có | Nhập số điện thoại. 10–11 chữ số; kiểm tra trùng khi tích hợp backend. |
| Email | Không / theo điều kiện | Nhập email |
| Số CCCD | Có | Nhập thông tin. 12 chữ số. |
| Địa chỉ | Không / theo điều kiện | Nhập nội dung nhiều dòng |
| Ngân hàng | Có | Nhập thông tin |
| Số tài khoản | Có | Nhập thông tin |
| Tên chủ tài khoản | Có | Nhập thông tin |
| Đã nhận sự đồng ý lưu thông tin cá nhân | Có | Đánh dấu xác nhận |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách chủ xe** | Mở Chủ xe ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignors](http://localhost:8080/ui/consignment/consignors) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-consignment-consignors-detail"></a>
#### Chủ xe · Đỗ Minh Đức — `consignors/detail`

**Mục đích:** Thông tin chủ xe và các lần ký gửi liên quan.

**File HTML:** [consignment/consignors/detail.html](../src/main/resources/templates/consignment/consignors/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignors/101](http://localhost:8080/ui/consignment/consignors/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/consignors/102](http://localhost:8080/ui/consignment/consignors/102) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/consignment/consignors/103](http://localhost:8080/ui/consignment/consignors/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Họ tên; Liên hệ; Ngân hàng; Đồng ý lưu thông tin.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-101 · Toyota Camry 2.5Q — **Nhân viên ký gửi** — [/ui/consignment/consignments/101](http://localhost:8080/ui/consignment/consignments/101) | Áp dụng mẫu consignors/101 |
| Đầu trang: **Tạo lần ký gửi** | Mở Tạo draft ký gửi — **Nhân viên ký gửi** — [/ui/consignment/consignments/new](http://localhost:8080/ui/consignment/consignments/new) | Áp dụng mẫu consignors/101 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-102 · Mazda CX-5 Premium — **Nhân viên ký gửi** — [/ui/consignment/consignments/102](http://localhost:8080/ui/consignment/consignments/102) | Áp dụng mẫu consignors/102 |
| Đầu trang: **Hồ sơ ký gửi** | Mở Hồ sơ KG-103 · Honda City RS — **Nhân viên ký gửi** — [/ui/consignment/consignments/103](http://localhost:8080/ui/consignment/consignments/103) | Áp dụng mẫu consignors/103 |

<a id="screen-consignment-consignors-list"></a>
#### Chủ xe ký gửi — `consignors/list`

**Mục đích:** Quản lý chủ sở hữu pháp lý và thông tin chi trả, không coi shop là chủ xe.

**File HTML:** [consignment/consignors/list.html](../src/main/resources/templates/consignment/consignors/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/consignment/consignors](http://localhost:8080/ui/consignment/consignors) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Chủ xe; Liên hệ; Xe ký gửi; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Thêm chủ xe** | Mở Tạo hồ sơ chủ xe — **Nhân viên ký gửi** — [/ui/consignment/consignors/new](http://localhost:8080/ui/consignment/consignors/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Đỗ Minh Đức**: Xem chi tiết | Mở Chủ xe · Đỗ Minh Đức — **Nhân viên ký gửi** — [/ui/consignment/consignors/101](http://localhost:8080/ui/consignment/consignors/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Lê Thị Mai**: Xem chi tiết | Mở Chủ xe · Lê Thị Mai — **Nhân viên ký gửi** — [/ui/consignment/consignors/102](http://localhost:8080/ui/consignment/consignors/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Nguyễn Quang Huy**: Xem chi tiết | Mở Chủ xe · Nguyễn Quang Huy — **Nhân viên ký gửi** — [/ui/consignment/consignors/103](http://localhost:8080/ui/consignment/consignors/103) | Liên kết đúng như UI hiện tại. |

<a id="role-inspector"></a>
### Kiểm định viên (`inspector`)

Tra nhanh:

- [Tổng quan](#screen-inspector-dashboard)
- [Checklist kiểm định KD-101](#screen-inspector-inspections-detail)
- [Hàng đợi kiểm định](#screen-inspector-inspections-list)
- [Biên tập tin đăng TĐ-101](#screen-inspector-listings-detail)
- [Tin đăng & khả năng bán](#screen-inspector-listings-list)
- [Định giá KG-101](#screen-inspector-valuations-detail)
- [Định giá & giá đăng bán](#screen-inspector-valuations-list)
- [Toyota Camry 2.5Q · 30H-628.19](#screen-inspector-vehicles-detail)
- [Tiếp nhận xe ký gửi](#screen-inspector-vehicles-intake)
- [Xe tại showroom](#screen-inspector-vehicles-list)

<a id="screen-inspector-dashboard"></a>
#### Tổng quan · Kiểm định viên — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [inspector/dashboard.html](../src/main/resources/templates/inspector/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/dashboard-inspector](http://localhost:8080/ui/inspector/dashboard-inspector) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Phiếu · xe; Kiểm định viên; Thời gian; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Chờ kiểm định; Đủ điều kiện; Đang được giữ. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Hàng đợi kiểm định — **Kiểm định viên** — [/ui/inspector/inspections](http://localhost:8080/ui/inspector/inspections) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **KD-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KD-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist kiểm định KD-102 — **Kiểm định viên** — [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KD-103 · Honda City RS**: Xem chi tiết | Mở Checklist kiểm định KD-103 — **Kiểm định viên** — [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-inspector-inspections-detail"></a>
#### Checklist kiểm định KD-101 — `inspections/detail`

**Mục đích:** Mỗi hạng mục lưu kết quả, mức độ, ảnh và ghi chú. Hạng mục critical cần xử lý rõ.

**File HTML:** [inspector/inspections/detail.html](../src/main/resources/templates/inspector/inspections/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | APPROVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | DRAFT | Dấu hiệu tai nạn · critical: Cần bổ sung ảnh khung gầm. |
| [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | APPROVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Xe; VIN; Kiểm định viên.

**Checklist cần đọc:** Khung vỏ / body; Động cơ & hộp số; Nội thất & điện; Dấu hiệu tai nạn · critical; Dấu hiệu ngập nước · critical. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Kết luận tổng thể | Có | Chọn: Đạt; Cần xử lý; Không đạt |
| Ghi chú kết luận | Có | Nhập nội dung nhiều dòng |

**Nhập thêm ở từng hạng mục kiểm định:** kết quả Đạt/Cần xử lý/Không đạt và mức độ Không có lỗi/Nhẹ/Nghiêm trọng là bắt buộc; có ô ghi chú và ảnh. Kết luận tổng thể không thay cho kết quả từng hạng mục.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hồ sơ xe** | Mở Toyota Camry 2.5Q · 30H-628.19 — **Kiểm định viên** — [/ui/inspector/vehicles/101](http://localhost:8080/ui/inspector/vehicles/101) | Áp dụng mẫu inspections/101 |
| Các bước liên quan: **Định giá** | Mở Định giá KG-101 — **Kiểm định viên** — [/ui/inspector/valuations/101](http://localhost:8080/ui/inspector/valuations/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Giấy tờ** | Mở Checklist hồ sơ giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Hồ sơ xe** | Mở Mazda CX-5 Premium · 30K-215.68 — **Kiểm định viên** — [/ui/inspector/vehicles/102](http://localhost:8080/ui/inspector/vehicles/102) | Áp dụng mẫu inspections/102 |
| Các bước liên quan: **Định giá** | Mở Định giá KG-102 — **Kiểm định viên** — [/ui/inspector/valuations/102](http://localhost:8080/ui/inspector/valuations/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Hồ sơ xe** | Mở Honda City RS · 29A-718.22 — **Kiểm định viên** — [/ui/inspector/vehicles/103](http://localhost:8080/ui/inspector/vehicles/103) | Áp dụng mẫu inspections/103 |
| Các bước liên quan: **Định giá** | Mở Định giá KG-103 — **Kiểm định viên** — [/ui/inspector/valuations/103](http://localhost:8080/ui/inspector/valuations/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-inspector-inspections-list"></a>
#### Hàng đợi kiểm định — `inspections/list`

**Mục đích:** Kiểm tra body, máy, nội thất, tai nạn và ngập nước; lưu kết quả từng hạng mục.

**File HTML:** [inspector/inspections/list.html](../src/main/resources/templates/inspector/inspections/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/inspections](http://localhost:8080/ui/inspector/inspections) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Phiếu · xe; Kiểm định viên; Thời gian; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **KD-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KD-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist kiểm định KD-102 — **Kiểm định viên** — [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KD-103 · Honda City RS**: Xem chi tiết | Mở Checklist kiểm định KD-103 — **Kiểm định viên** — [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-inspector-listings-detail"></a>
#### Biên tập tin đăng TĐ-101 — `listings/detail`

**Mục đích:** Kiểm tra eligibility trước khi publish; trạng thái RESERVED phải giữ khả năng bán đúng.

**File HTML:** [inspector/listings/detail.html](../src/main/resources/templates/inspector/listings/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/listings/101](http://localhost:8080/ui/inspector/listings/101) | LISTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/inspector/listings/102](http://localhost:8080/ui/inspector/listings/102) | BLOCKED | Kiểm định hợp lệ; Giấy tờ VERIFIED: Giấy đăng ký còn PENDING_REVIEW. |
| [/ui/inspector/listings/103](http://localhost:8080/ui/inspector/listings/103) | RESERVED | Không có reservation active: Đã giữ cho Bùi Đức Long. |

**Thông tin cần đọc:** Xe; Giá đăng bán.

**Checklist cần đọc:** Kiểm định hợp lệ; Giấy tờ VERIFIED; Agreement active, chưa hết hạn; Không có reservation active. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Tiêu đề tin | Có | Nhập thông tin |
| Nội dung công khai | Có | Nhập nội dung nhiều dòng |
| Giá đăng bán (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Ảnh xe | Không / theo điều kiện | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tất cả tin đăng** | Mở Tin đăng & khả năng bán — **Kiểm định viên** — [/ui/inspector/listings](http://localhost:8080/ui/inspector/listings) | Áp dụng mẫu listings/101 |
| Đầu trang: **Mô phỏng dừng tin** | Mở hộp thoại xác nhận → xác nhận thì thông báo mô phỏng tại chỗ, không mở trang khác. | Chỉ tin đang đăng bán có thể dừng. |
| Đầu trang: **Mô phỏng dừng tin** | Không mở trang; nút bị khóa. | Chỉ tin đang đăng bán có thể dừng. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-inspector-listings-list"></a>
#### Tin đăng & khả năng bán — `listings/list`

**Mục đích:** Chỉ cho phép đăng bán khi kiểm định, giấy tờ và hợp đồng hợp lệ.

**File HTML:** [inspector/listings/list.html](../src/main/resources/templates/inspector/listings/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/listings](http://localhost:8080/ui/inspector/listings) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Tin đăng · xe; Giá; Điều kiện; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **TĐ-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Biên tập tin đăng TĐ-101 — **Kiểm định viên** — [/ui/inspector/listings/101](http://localhost:8080/ui/inspector/listings/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TĐ-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Biên tập tin đăng TĐ-102 — **Kiểm định viên** — [/ui/inspector/listings/102](http://localhost:8080/ui/inspector/listings/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TĐ-103 · Honda City RS**: Xem chi tiết | Mở Biên tập tin đăng TĐ-103 — **Kiểm định viên** — [/ui/inspector/listings/103](http://localhost:8080/ui/inspector/listings/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-inspector-valuations-detail"></a>
#### Định giá KG-101 — `valuations/detail`

**Mục đích:** Giá dưới sàn phải đi qua phê duyệt; không tự động thay giá đã duyệt.

**File HTML:** [inspector/valuations/detail.html](../src/main/resources/templates/inspector/valuations/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/valuations/101](http://localhost:8080/ui/inspector/valuations/101) | PENDING | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/inspector/valuations/102](http://localhost:8080/ui/inspector/valuations/102) | PENDING | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/inspector/valuations/103](http://localhost:8080/ui/inspector/valuations/103) | PENDING | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Xe; Agreement; Giá sàn chủ xe.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Ước tính thị trường (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Giá sàn (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Giá đề xuất (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Cơ sở định giá | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Lịch sử định giá** | Mở Định giá & giá đăng bán — **Kiểm định viên** — [/ui/inspector/valuations](http://localhost:8080/ui/inspector/valuations) | Áp dụng mẫu valuations/101 |
| Các bước liên quan: **Phê duyệt giá** | Mở Đánh giá yêu cầu PD-102 — **Quản lý** — [/ui/manager/approvals/102](http://localhost:8080/ui/manager/approvals/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Tin đăng** | Mở Biên tập tin đăng TĐ-101 — **Kiểm định viên** — [/ui/inspector/listings/101](http://localhost:8080/ui/inspector/listings/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Tin đăng** | Mở Biên tập tin đăng TĐ-102 — **Kiểm định viên** — [/ui/inspector/listings/102](http://localhost:8080/ui/inspector/listings/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Tin đăng** | Mở Biên tập tin đăng TĐ-103 — **Kiểm định viên** — [/ui/inspector/listings/103](http://localhost:8080/ui/inspector/listings/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-inspector-valuations-list"></a>
#### Định giá & giá đăng bán — `valuations/list`

**Mục đích:** So sánh giá thị trường với giá sàn chủ xe; lịch sử định giá theo lần ký gửi.

**File HTML:** [inspector/valuations/list.html](../src/main/resources/templates/inspector/valuations/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/valuations](http://localhost:8080/ui/inspector/valuations) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hồ sơ · xe; Giá thị trường; Giá đề xuất; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **KG-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Định giá KG-101 — **Kiểm định viên** — [/ui/inspector/valuations/101](http://localhost:8080/ui/inspector/valuations/101) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Định giá KG-102 — **Kiểm định viên** — [/ui/inspector/valuations/102](http://localhost:8080/ui/inspector/valuations/102) | Liên kết đúng như UI hiện tại. |
| Dòng **KG-103 · Honda City RS**: Xem chi tiết | Mở Định giá KG-103 — **Kiểm định viên** — [/ui/inspector/valuations/103](http://localhost:8080/ui/inspector/valuations/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-inspector-vehicles-detail"></a>
#### Toyota Camry 2.5Q · 30H-628.19 — `vehicles/detail`

**Mục đích:** Hồ sơ xe vật lý, kiểm định, giấy tờ và lịch sử ký gửi.

**File HTML:** [inspector/vehicles/detail.html](../src/main/resources/templates/inspector/vehicles/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/vehicles/101](http://localhost:8080/ui/inspector/vehicles/101) | LISTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/inspector/vehicles/102](http://localhost:8080/ui/inspector/vehicles/102) | DOCUMENT_REVIEW | Kiểm định; Giấy tờ được xác minh |
| [/ui/inspector/vehicles/103](http://localhost:8080/ui/inspector/vehicles/103) | RESERVED | Không được giữ bởi khách khác |

**Thông tin cần đọc:** VIN; Năm sản xuất; Số km; Chủ xe pháp lý; Giá niêm yết; Vị trí lưu giữ.

**Checklist cần đọc:** Kiểm định; Giấy tờ được xác minh; Không được giữ bởi khách khác. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Kiểm định** | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Áp dụng mẫu vehicles/101 |
| Đầu trang: **Định giá** | Mở Định giá KG-101 — **Kiểm định viên** — [/ui/inspector/valuations/101](http://localhost:8080/ui/inspector/valuations/101) | Áp dụng mẫu vehicles/101 |
| Đầu trang: **Tin đăng** | Mở Biên tập tin đăng TĐ-101 — **Kiểm định viên** — [/ui/inspector/listings/101](http://localhost:8080/ui/inspector/listings/101) | Áp dụng mẫu vehicles/101 |
| Đầu trang: **Ghi nhận tư vấn** | Mở Ghi nhận tư vấn mua xe — **Nhân viên kinh doanh** — [/ui/sales/inquiries/new](http://localhost:8080/ui/sales/inquiries/new) | Áp dụng mẫu vehicles/101 |
| Đầu trang: **Kiểm định** | Mở Checklist kiểm định KD-102 — **Kiểm định viên** — [/ui/inspector/inspections/102](http://localhost:8080/ui/inspector/inspections/102) | Áp dụng mẫu vehicles/102 |
| Đầu trang: **Định giá** | Mở Định giá KG-102 — **Kiểm định viên** — [/ui/inspector/valuations/102](http://localhost:8080/ui/inspector/valuations/102) | Áp dụng mẫu vehicles/102 |
| Đầu trang: **Tin đăng** | Mở Biên tập tin đăng TĐ-102 — **Kiểm định viên** — [/ui/inspector/listings/102](http://localhost:8080/ui/inspector/listings/102) | Áp dụng mẫu vehicles/102 |
| Đầu trang: **Kiểm định** | Mở Checklist kiểm định KD-103 — **Kiểm định viên** — [/ui/inspector/inspections/103](http://localhost:8080/ui/inspector/inspections/103) | Áp dụng mẫu vehicles/103 |
| Đầu trang: **Định giá** | Mở Định giá KG-103 — **Kiểm định viên** — [/ui/inspector/valuations/103](http://localhost:8080/ui/inspector/valuations/103) | Áp dụng mẫu vehicles/103 |
| Đầu trang: **Tin đăng** | Mở Biên tập tin đăng TĐ-103 — **Kiểm định viên** — [/ui/inspector/listings/103](http://localhost:8080/ui/inspector/listings/103) | Áp dụng mẫu vehicles/103 |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-inspector-vehicles-intake"></a>
#### Tiếp nhận xe ký gửi — `vehicles/intake`

**Mục đích:** Ghi nhận VIN, chassis, odo, chủ xe và vị trí lưu giữ.

**File HTML:** [inspector/vehicles/intake.html](../src/main/resources/templates/inspector/vehicles/intake.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/vehicles/intake](http://localhost:8080/ui/inspector/vehicles/intake) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Hồ sơ ký gửi | Có | Chọn: KG-101; KG-102; KG-103 |
| VIN | Có | Nhập thông tin. 17 ký tự, không gồm I, O, Q. |
| Số khung | Có | Nhập thông tin |
| Số máy | Có | Nhập thông tin |
| Hãng xe | Có | Nhập thông tin |
| Dòng xe | Có | Nhập thông tin |
| Năm sản xuất | Có | Nhập số; UI không nhận số âm |
| Số km đã chạy | Có | Nhập số; UI không nhận số âm |
| Màu xe | Có | Nhập thông tin |
| Vị trí lưu giữ | Có | Nhập thông tin |
| Tình trạng tiếp nhận | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách xe** | Mở Xe tại showroom — **Kiểm định viên** — [/ui/inspector/vehicles](http://localhost:8080/ui/inspector/vehicles) | Theo dữ liệu và trạng thái hiện tại. |
| Các bước liên quan: **Kiểm định** | Mở Checklist kiểm định KD-101 — **Kiểm định viên** — [/ui/inspector/inspections/101](http://localhost:8080/ui/inspector/inspections/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-inspector-vehicles-list"></a>
#### Xe tại showroom — `vehicles/list`

**Mục đích:** Định danh xe vật lý theo VIN; theo dõi từng lần ký gửi và khả năng bán.

**File HTML:** [inspector/vehicles/list.html](../src/main/resources/templates/inspector/vehicles/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/inspector/vehicles](http://localhost:8080/ui/inspector/vehicles) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Xe · biển số; Năm · odo; Giá niêm yết; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Tiếp nhận xe** | Mở Tiếp nhận xe ký gửi — **Kiểm định viên** — [/ui/inspector/vehicles/intake](http://localhost:8080/ui/inspector/vehicles/intake) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Toyota Camry 2.5Q · 30H-628.19**: Xem chi tiết | Mở Toyota Camry 2.5Q · 30H-628.19 — **Kiểm định viên** — [/ui/inspector/vehicles/101](http://localhost:8080/ui/inspector/vehicles/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Mazda CX-5 Premium · 30K-215.68**: Xem chi tiết | Mở Mazda CX-5 Premium · 30K-215.68 — **Kiểm định viên** — [/ui/inspector/vehicles/102](http://localhost:8080/ui/inspector/vehicles/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Honda City RS · 29A-718.22**: Xem chi tiết | Mở Honda City RS · 29A-718.22 — **Kiểm định viên** — [/ui/inspector/vehicles/103](http://localhost:8080/ui/inspector/vehicles/103) | Liên kết đúng như UI hiện tại. |

<a id="role-documents"></a>
### Nhân viên hồ sơ (`documents`)

Tra nhanh:

- [Tổng quan](#screen-documents-dashboard)
- [Checklist CK-101](#screen-documents-checklists-detail)
- [Checklist hồ sơ giấy tờ](#screen-documents-checklists-list)
- [Giấy tờ DOC-101 · v2](#screen-documents-documents-detail)
- [Review giấy tờ](#screen-documents-documents-list)
- [Upload / thay thế giấy tờ](#screen-documents-documents-upload)
- [Hồ sơ sang tên ST-101](#screen-documents-transfers-detail)
- [Hồ sơ sang tên](#screen-documents-transfers-list)

<a id="screen-documents-dashboard"></a>
#### Tổng quan · Nhân viên hồ sơ — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [documents/dashboard.html](../src/main/resources/templates/documents/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/dashboard-documents](http://localhost:8080/ui/documents/dashboard-documents) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Checklist · xe; Loại hồ sơ; Tiến độ; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Checklist; Chờ review; Sang tên bị chặn. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Checklist hồ sơ giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **CK-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist CK-101 — **Nhân viên hồ sơ** — [/ui/documents/checklists/101](http://localhost:8080/ui/documents/checklists/101) | Liên kết đúng như UI hiện tại. |
| Dòng **CK-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist CK-102 — **Nhân viên hồ sơ** — [/ui/documents/checklists/102](http://localhost:8080/ui/documents/checklists/102) | Liên kết đúng như UI hiện tại. |
| Dòng **CK-103 · Honda City RS**: Xem chi tiết | Mở Checklist CK-103 — **Nhân viên hồ sơ** — [/ui/documents/checklists/103](http://localhost:8080/ui/documents/checklists/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-documents-checklists-detail"></a>
#### Checklist CK-101 — `checklists/detail`

**Mục đích:** Các giấy tờ bắt buộc phải VERIFIED; giữ tất cả file/version liên quan.

**File HTML:** [documents/checklists/detail.html](../src/main/resources/templates/documents/checklists/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/checklists/101](http://localhost:8080/ui/documents/checklists/101) | VERIFIED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/documents/checklists/102](http://localhost:8080/ui/documents/checklists/102) | MISSING | Đăng ký xe · bắt buộc; Đăng kiểm · bắt buộc; Ủy quyền / sang tên · theo hồ sơ |
| [/ui/documents/checklists/103](http://localhost:8080/ui/documents/checklists/103) | MISSING | Đăng kiểm · bắt buộc; Ủy quyền / sang tên · theo hồ sơ |

**Thông tin cần đọc:** Xe; Loại hồ sơ.

**Checklist cần đọc:** CCCD chủ xe · bắt buộc; Đăng ký xe · bắt buộc; Đăng kiểm · bắt buộc; Ủy quyền / sang tên · theo hồ sơ. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Bổ sung / thay thế** | Mở Upload / thay thế giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/documents/upload](http://localhost:8080/ui/documents/documents/upload) | Áp dụng mẫu checklists/101 |
| Đầu trang: **Review phiên bản** | Mở Giấy tờ DOC-101 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/101](http://localhost:8080/ui/documents/documents/101) | Áp dụng mẫu checklists/101 |
| Đầu trang: **Review phiên bản** | Mở Giấy tờ DOC-102 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/102](http://localhost:8080/ui/documents/documents/102) | Áp dụng mẫu checklists/102 |
| Đầu trang: **Review phiên bản** | Mở Giấy tờ DOC-103 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/103](http://localhost:8080/ui/documents/documents/103) | Áp dụng mẫu checklists/103 |

<a id="screen-documents-checklists-list"></a>
#### Checklist hồ sơ giấy tờ — `checklists/list`

**Mục đích:** Checklist theo intake, sale, transfer. Upload khác VERIFIED; thiếu/reject phải block listing.

**File HTML:** [documents/checklists/list.html](../src/main/resources/templates/documents/checklists/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Checklist · xe; Loại hồ sơ; Tiến độ; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **CK-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist CK-101 — **Nhân viên hồ sơ** — [/ui/documents/checklists/101](http://localhost:8080/ui/documents/checklists/101) | Liên kết đúng như UI hiện tại. |
| Dòng **CK-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist CK-102 — **Nhân viên hồ sơ** — [/ui/documents/checklists/102](http://localhost:8080/ui/documents/checklists/102) | Liên kết đúng như UI hiện tại. |
| Dòng **CK-103 · Honda City RS**: Xem chi tiết | Mở Checklist CK-103 — **Nhân viên hồ sơ** — [/ui/documents/checklists/103](http://localhost:8080/ui/documents/checklists/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-documents-documents-detail"></a>
#### Giấy tờ DOC-101 · v2 — `documents/detail`

**Mục đích:** Bản v2 chưa ghi đè v1. Review bằng evidence và lý do; URL file thật do backend cung cấp.

**File HTML:** [documents/documents/detail.html](../src/main/resources/templates/documents/documents/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/documents/101](http://localhost:8080/ui/documents/documents/101) | VERIFIED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/documents/documents/102](http://localhost:8080/ui/documents/documents/102) | PENDING_REVIEW | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/documents/documents/103](http://localhost:8080/ui/documents/documents/103) | REJECTED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Loại giấy tờ; File gốc; Phiên bản; Upload bởi; Dung lượng; Ngày hết hạn; Checksum.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Kết quả review | Có | Chọn: VERIFIED; REJECTED |
| Lý do / nhận xét | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Thay thế phiên bản** | Mở Upload / thay thế giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/documents/upload](http://localhost:8080/ui/documents/documents/upload) | Áp dụng mẫu documents/101 |
| Đầu trang: **Checklist** | Mở Checklist CK-101 — **Nhân viên hồ sơ** — [/ui/documents/checklists/101](http://localhost:8080/ui/documents/checklists/101) | Áp dụng mẫu documents/101 |
| Đầu trang: **Checklist** | Mở Checklist CK-102 — **Nhân viên hồ sơ** — [/ui/documents/checklists/102](http://localhost:8080/ui/documents/checklists/102) | Áp dụng mẫu documents/102 |
| Đầu trang: **Checklist** | Mở Checklist CK-103 — **Nhân viên hồ sơ** — [/ui/documents/checklists/103](http://localhost:8080/ui/documents/checklists/103) | Áp dụng mẫu documents/103 |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-documents-documents-list"></a>
#### Review giấy tờ — `documents/list`

**Mục đích:** Review phiên bản cụ thể; VERIFIED và REJECTED phải có người kiểm tra, thời gian, lý do.

**File HTML:** [documents/documents/list.html](../src/main/resources/templates/documents/documents/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/documents](http://localhost:8080/ui/documents/documents) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Giấy tờ; Hồ sơ · phiên bản; Người upload; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **CCCD chủ xe**: Xem chi tiết | Mở Giấy tờ DOC-101 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/101](http://localhost:8080/ui/documents/documents/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Đăng ký xe**: Xem chi tiết | Mở Giấy tờ DOC-102 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/102](http://localhost:8080/ui/documents/documents/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Giấy tờ sang tên**: Xem chi tiết | Mở Giấy tờ DOC-103 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/103](http://localhost:8080/ui/documents/documents/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-documents-documents-upload"></a>
#### Upload / thay thế giấy tờ — `documents/upload`

**Mục đích:** Thay thế tạo phiên bản mới; giữ bản cũ, thông tin upload và review.

**File HTML:** [documents/documents/upload.html](../src/main/resources/templates/documents/documents/upload.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/documents/upload](http://localhost:8080/ui/documents/documents/upload) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Checklist | Có | Chọn: CK-101 · Intake; CK-102 · Intake; CK-103 · Transfer |
| Loại giấy tờ | Có | Chọn: Đăng ký xe; Đăng kiểm; CCCD chủ xe; Giấy ủy quyền; Giấy tờ sang tên |
| Chọn file | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |
| Phiên bản thay thế | Không / theo điều kiện | Chọn: Bản mới; DOC-102 · v1 |
| Ngày hết hạn | Không / theo điều kiện | Chọn ngày |
| Ghi chú bổ sung | Không / theo điều kiện | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Checklist** | Mở Checklist hồ sơ giấy tờ — **Nhân viên hồ sơ** — [/ui/documents/checklists](http://localhost:8080/ui/documents/checklists) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Xem trước file:** sau khi chọn file hợp lệ, UI hiển thị tên/dung lượng, ảnh nếu là hình và link mở file local. Tải lại phải chọn lại file; không có file S3 thật.

<a id="screen-documents-transfers-detail"></a>
#### Hồ sơ sang tên ST-101 — `transfers/detail`

**Mục đích:** Theo dõi stage, bên chịu trách nhiệm, deadline và bằng chứng hoàn tất.

**File HTML:** [documents/transfers/detail.html](../src/main/resources/templates/documents/transfers/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/transfers/101](http://localhost:8080/ui/documents/transfers/101) | COMPLETED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/documents/transfers/102](http://localhost:8080/ui/documents/transfers/102) | BLOCKED | Giấy tờ đầy đủ; Có bằng chứng chuyển quyền |
| [/ui/documents/transfers/103](http://localhost:8080/ui/documents/transfers/103) | IN_PROGRESS | Có bằng chứng chuyển quyền |

**Thông tin cần đọc:** Hợp đồng; Xe; Buyer; Trạng thái bàn giao.

**Checklist cần đọc:** Giấy tờ đầy đủ; Có bằng chứng chuyển quyền. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Giai đoạn | Có | Chọn: Thu thập hồ sơ; Nộp hồ sơ; Chờ kết quả; Nhận xác nhận sang tên |
| Bên chịu trách nhiệm | Có | Nhập thông tin |
| Deadline | Có | Chọn ngày |
| Bằng chứng sang tên | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |
| Ghi chú / blocker | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách sang tên** | Mở Hồ sơ sang tên — **Nhân viên hồ sơ** — [/ui/documents/transfers](http://localhost:8080/ui/documents/transfers) | Áp dụng mẫu transfers/101 |
| Đầu trang: **Checklist liên quan** | Mở Checklist CK-101 — **Nhân viên hồ sơ** — [/ui/documents/checklists/101](http://localhost:8080/ui/documents/checklists/101) | Áp dụng mẫu transfers/101 |
| Các bước liên quan: **Bàn giao vật lý** | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Checklist liên quan** | Mở Checklist CK-102 — **Nhân viên hồ sơ** — [/ui/documents/checklists/102](http://localhost:8080/ui/documents/checklists/102) | Áp dụng mẫu transfers/102 |
| Các bước liên quan: **Bàn giao vật lý** | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Checklist liên quan** | Mở Checklist CK-103 — **Nhân viên hồ sơ** — [/ui/documents/checklists/103](http://localhost:8080/ui/documents/checklists/103) | Áp dụng mẫu transfers/103 |
| Các bước liên quan: **Bàn giao vật lý** | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-documents-transfers-list"></a>
#### Hồ sơ sang tên — `transfers/list`

**Mục đích:** Theo dõi deadline, bên chịu trách nhiệm và evidence. Sang tên tách khỏi bàn giao vật lý.

**File HTML:** [documents/transfers/list.html](../src/main/resources/templates/documents/transfers/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/documents/transfers](http://localhost:8080/ui/documents/transfers) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hồ sơ · hợp đồng; Người phụ trách; Deadline; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **ST-101 · HĐB-101**: Xem chi tiết | Mở Hồ sơ sang tên ST-101 — **Nhân viên hồ sơ** — [/ui/documents/transfers/101](http://localhost:8080/ui/documents/transfers/101) | Liên kết đúng như UI hiện tại. |
| Dòng **ST-102 · HĐB-102**: Xem chi tiết | Mở Hồ sơ sang tên ST-102 — **Nhân viên hồ sơ** — [/ui/documents/transfers/102](http://localhost:8080/ui/documents/transfers/102) | Liên kết đúng như UI hiện tại. |
| Dòng **ST-103 · HĐB-103**: Xem chi tiết | Mở Hồ sơ sang tên ST-103 — **Nhân viên hồ sơ** — [/ui/documents/transfers/103](http://localhost:8080/ui/documents/transfers/103) | Liên kết đúng như UI hiện tại. |

<a id="role-finance"></a>
### Nhân viên tài chính (`finance`)

Tra nhanh:

- [Tổng quan](#screen-finance-dashboard)
- [Hàng đợi bàn giao xe](#screen-finance-handovers-list)
- [Đối soát giao dịch TT-101](#screen-finance-payments-detail)
- [Giao dịch thanh toán](#screen-finance-payments-list)
- [Lệnh chi PC-101](#screen-finance-payouts-detail)
- [Chi trả chủ xe](#screen-finance-payouts-list)
- [Ghi nhận khoản thu](#screen-finance-receipts-create)
- [Ghi nhận hoàn tiền](#screen-finance-refunds-create)
- [Quyết toán QT-101](#screen-finance-settlements-detail)
- [Hoa hồng & quyết toán chủ xe](#screen-finance-settlements-list)

<a id="screen-finance-dashboard"></a>
#### Tổng quan · Nhân viên tài chính — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [finance/dashboard.html](../src/main/resources/templates/finance/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/dashboard-finance](http://localhost:8080/ui/finance/dashboard-finance) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Giao dịch · hợp đồng; Người trả; Số tiền; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Khoản thu; Chờ quyết toán; Cọc phải xử lý. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Giao dịch thanh toán — **Nhân viên tài chính** — [/ui/finance/payments](http://localhost:8080/ui/finance/payments) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **TT-101 · HĐB-101**: Xem chi tiết | Mở Đối soát giao dịch TT-101 — **Nhân viên tài chính** — [/ui/finance/payments/101](http://localhost:8080/ui/finance/payments/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TT-102 · HĐB-102**: Xem chi tiết | Mở Đối soát giao dịch TT-102 — **Nhân viên tài chính** — [/ui/finance/payments/102](http://localhost:8080/ui/finance/payments/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TT-103 · HĐB-103**: Xem chi tiết | Mở Đối soát giao dịch TT-103 — **Nhân viên tài chính** — [/ui/finance/payments/103](http://localhost:8080/ui/finance/payments/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-finance-handovers-list"></a>
#### Hàng đợi bàn giao xe — `handovers/list`

**Mục đích:** Thanh toán, sang tên và bàn giao là các mốc riêng; kiểm tra điều kiện trước khi giao.

**File HTML:** [finance/handovers/list.html](../src/main/resources/templates/finance/handovers/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/handovers](http://localhost:8080/ui/finance/handovers) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hợp đồng · xe; Khách mua; Điều kiện; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **HĐB-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-103 · Honda City RS**: Xem chi tiết | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-finance-payments-detail"></a>
#### Đối soát giao dịch TT-101 — `payments/detail`

**Mục đích:** Xem payment request, transaction và webhook evidence; giữ lịch sử tiền.

**File HTML:** [finance/payments/detail.html](../src/main/resources/templates/finance/payments/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/payments/101](http://localhost:8080/ui/finance/payments/101) | PAID | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/finance/payments/102](http://localhost:8080/ui/finance/payments/102) | PENDING | Chữ ký webhook hợp lệ; Transaction khớp số tiền |
| [/ui/finance/payments/103](http://localhost:8080/ui/finance/payments/103) | PARTIALLY_PAID | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Hợp đồng; Người trả; Phương thức; Mã tham chiếu; Số tiền; Redirect.

**Checklist cần đọc:** Chữ ký webhook hợp lệ; Transaction khớp số tiền; Không trùng mã tham chiếu. Dòng chưa đạt có giải thích tại UI.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Ghi nhận khoản thu** | Mở Ghi nhận khoản thu — **Nhân viên tài chính** — [/ui/finance/receipts/new](http://localhost:8080/ui/finance/receipts/new) | Áp dụng mẫu payments/101 |
| Đầu trang: **Hoàn tiền** | Mở Ghi nhận hoàn tiền — **Nhân viên tài chính** — [/ui/finance/refunds/new](http://localhost:8080/ui/finance/refunds/new) | Áp dụng mẫu payments/101 |
| Đầu trang: **Quyết toán** | Mở Quyết toán QT-101 — **Nhân viên tài chính** — [/ui/finance/settlements/101](http://localhost:8080/ui/finance/settlements/101) | Áp dụng mẫu payments/101 |
| Đầu trang: **Quyết toán** | Mở Quyết toán QT-102 — **Nhân viên tài chính** — [/ui/finance/settlements/102](http://localhost:8080/ui/finance/settlements/102) | Áp dụng mẫu payments/102 |
| Đầu trang: **Quyết toán** | Mở Quyết toán QT-103 — **Nhân viên tài chính** — [/ui/finance/settlements/103](http://localhost:8080/ui/finance/settlements/103) | Áp dụng mẫu payments/103 |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-finance-payments-list"></a>
#### Giao dịch thanh toán — `payments/list`

**Mục đích:** Đối soát khoản thu, hoàn tiền và bằng chứng; redirect không xác nhận tiền đã thu.

**File HTML:** [finance/payments/list.html](../src/main/resources/templates/finance/payments/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/payments](http://localhost:8080/ui/finance/payments) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Giao dịch · hợp đồng; Người trả; Số tiền; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Ghi nhận khoản thu** | Mở Ghi nhận khoản thu — **Nhân viên tài chính** — [/ui/finance/receipts/new](http://localhost:8080/ui/finance/receipts/new) | Theo dữ liệu và trạng thái hiện tại. |
| Đầu trang: **Hoàn tiền** | Mở Ghi nhận hoàn tiền — **Nhân viên tài chính** — [/ui/finance/refunds/new](http://localhost:8080/ui/finance/refunds/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **TT-101 · HĐB-101**: Xem chi tiết | Mở Đối soát giao dịch TT-101 — **Nhân viên tài chính** — [/ui/finance/payments/101](http://localhost:8080/ui/finance/payments/101) | Liên kết đúng như UI hiện tại. |
| Dòng **TT-102 · HĐB-102**: Xem chi tiết | Mở Đối soát giao dịch TT-102 — **Nhân viên tài chính** — [/ui/finance/payments/102](http://localhost:8080/ui/finance/payments/102) | Liên kết đúng như UI hiện tại. |
| Dòng **TT-103 · HĐB-103**: Xem chi tiết | Mở Đối soát giao dịch TT-103 — **Nhân viên tài chính** — [/ui/finance/payments/103](http://localhost:8080/ui/finance/payments/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-finance-payouts-detail"></a>
#### Lệnh chi PC-101 — `payouts/detail`

**Mục đích:** Đối chiếu người nhận, số tiền và quyết toán trước khi gửi lệnh chi thật.

**File HTML:** [finance/payouts/detail.html](../src/main/resources/templates/finance/payouts/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/payouts/101](http://localhost:8080/ui/finance/payouts/101) | PENDING | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/finance/payouts/103](http://localhost:8080/ui/finance/payouts/103) | PAID | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Quyết toán; Chủ xe; Tài khoản; Số tiền; Mã tham chiếu.

**Checklist cần đọc:** Quyết toán đã duyệt; Tài khoản chủ xe được đối chiếu. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Nội dung lệnh chi | Có | Nhập nội dung nhiều dòng |
| Bằng chứng chi trả | Không / theo điều kiện | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách chi trả** | Mở Chi trả chủ xe — **Nhân viên tài chính** — [/ui/finance/payouts](http://localhost:8080/ui/finance/payouts) | Áp dụng mẫu payouts/101 |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

<a id="screen-finance-payouts-list"></a>
#### Chi trả chủ xe — `payouts/list`

**Mục đích:** Chi trả theo quyết toán đã duyệt; hiển thị tài khoản nhận tiền đã che bớt.

**File HTML:** [finance/payouts/list.html](../src/main/resources/templates/finance/payouts/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/payouts](http://localhost:8080/ui/finance/payouts) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Lệnh chi; Người nhận; Số tiền; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **PC-101 · QT-101**: Xem chi tiết | Mở Lệnh chi PC-101 — **Nhân viên tài chính** — [/ui/finance/payouts/101](http://localhost:8080/ui/finance/payouts/101) | Liên kết đúng như UI hiện tại. |
| Dòng **PC-103 · QT-103**: Xem chi tiết | Mở Lệnh chi PC-103 — **Nhân viên tài chính** — [/ui/finance/payouts/103](http://localhost:8080/ui/finance/payouts/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-finance-receipts-create"></a>
#### Ghi nhận khoản thu — `receipts/create`

**Mục đích:** Lưu mã tham chiếu và evidence; khoản thu cần phê duyệt/đối soát.

**File HTML:** [finance/receipts/create.html](../src/main/resources/templates/finance/receipts/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/receipts/new](http://localhost:8080/ui/finance/receipts/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Hợp đồng bán | Có | Chọn: HĐB-101; HĐB-103 |
| Loại khoản thu | Có | Chọn: Cọc; Thanh toán phần còn lại |
| Phương thức | Có | Chọn: Chuyển khoản; Tiền mặt; payOS |
| Số tiền (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Mã tham chiếu | Có | Nhập thông tin |
| Người thanh toán | Có | Nhập thông tin |
| Thời gian thu | Có | Chọn ngày và giờ |
| Bằng chứng khoản thu | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Giao dịch** | Mở Giao dịch thanh toán — **Nhân viên tài chính** — [/ui/finance/payments](http://localhost:8080/ui/finance/payments) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-finance-refunds-create"></a>
#### Ghi nhận hoàn tiền — `refunds/create`

**Mục đích:** Hoàn tiền tham chiếu khoản thu gốc và reservation; không sửa lịch sử khoản thu.

**File HTML:** [finance/refunds/create.html](../src/main/resources/templates/finance/refunds/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/refunds/new](http://localhost:8080/ui/finance/refunds/new) | Không có badge trạng thái riêng | Có quyết định xử lý cọc: Cần Manager duyệt trước khi hoàn tiền thật. |

**Checklist cần đọc:** Có khoản thu gốc; Có quyết định xử lý cọc. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Khoản thu gốc | Có | Chọn: TT-102 · 50.000.000 ₫ |
| Reservation liên quan | Có | Chọn: RS-102 · đã hết hạn; RS-103 · khách hủy |
| Số tiền hoàn (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Lý do hoàn | Có | Nhập nội dung nhiều dòng |
| Quyết định phê duyệt | Có | Nhập thông tin |
| Bằng chứng hoàn tiền | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Giao dịch** | Mở Giao dịch thanh toán — **Nhân viên tài chính** — [/ui/finance/payments](http://localhost:8080/ui/finance/payments) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

<a id="screen-finance-settlements-detail"></a>
#### Quyết toán QT-101 — `settlements/detail`

**Mục đích:** Bảng tính minh họa theo agreement v2, cần đối chiếu chứng từ và duyệt trước chi trả.

**File HTML:** [finance/settlements/detail.html](../src/main/resources/templates/finance/settlements/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/settlements/101](http://localhost:8080/ui/finance/settlements/101) | APPROVED | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/finance/settlements/102](http://localhost:8080/ui/finance/settlements/102) | PENDING | Khoản thu đã đối soát |
| [/ui/finance/settlements/103](http://localhost:8080/ui/finance/settlements/103) | PAID | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Hợp đồng; Agreement; Chủ xe; Giá cơ sở; Tài khoản chi trả.

**Checklist cần đọc:** Agreement hiệu lực; Khoản thu đã đối soát. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Giá cơ sở (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Hoa hồng (%) | Có | Nhập số; UI không nhận số âm |
| Phí cố định (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Chi phí đã duyệt (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Hoàn tiền đã ghi nhận (VNĐ) | Có | Nhập số; UI không nhận số âm |
| Cọc giữ lại (VNĐ) | Có | Nhập số; UI không nhận số âm |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Danh sách** | Mở Hoa hồng & quyết toán chủ xe — **Nhân viên tài chính** — [/ui/finance/settlements](http://localhost:8080/ui/finance/settlements) | Áp dụng mẫu settlements/101 |
| Đầu trang: **Lệnh chi** | Mở Lệnh chi PC-101 — **Nhân viên tài chính** — [/ui/finance/payouts/101](http://localhost:8080/ui/finance/payouts/101) | Cần quyết toán APPROVED trước khi lập lệnh chi. |
| Đầu trang: **Lệnh chi** | Không mở trang; nút bị khóa. | Cần quyết toán APPROVED trước khi lập lệnh chi. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Tính tại chỗ:** thay đổi số tiền/hoa hồng cập nhật số ròng dự kiến. Công thức là giả định UX, không phải quyết toán đã được duyệt. Mở đúng trang Manager trong phần 9 khi cần xem người duyệt làm gì.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-finance-settlements-list"></a>
#### Hoa hồng & quyết toán chủ xe — `settlements/list`

**Mục đích:** Áp dụng đúng phiên bản agreement active; theo dõi net payout riêng với hoa hồng.

**File HTML:** [finance/settlements/list.html](../src/main/resources/templates/finance/settlements/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/finance/settlements](http://localhost:8080/ui/finance/settlements) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Quyết toán · hợp đồng; Chủ xe; Số tiền ròng; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **QT-101 · HĐB-101**: Xem chi tiết | Mở Quyết toán QT-101 — **Nhân viên tài chính** — [/ui/finance/settlements/101](http://localhost:8080/ui/finance/settlements/101) | Liên kết đúng như UI hiện tại. |
| Dòng **QT-102 · HĐB-102**: Xem chi tiết | Mở Quyết toán QT-102 — **Nhân viên tài chính** — [/ui/finance/settlements/102](http://localhost:8080/ui/finance/settlements/102) | Liên kết đúng như UI hiện tại. |
| Dòng **QT-103 · HĐB-103**: Xem chi tiết | Mở Quyết toán QT-103 — **Nhân viên tài chính** — [/ui/finance/settlements/103](http://localhost:8080/ui/finance/settlements/103) | Liên kết đúng như UI hiện tại. |

<a id="role-handover"></a>
### Nhân viên bàn giao (`handover`)

Tra nhanh:

- [Tổng quan](#screen-handover-dashboard)
- [Đóng sale HĐB-101](#screen-handover-handovers-close)
- [Checklist bàn giao BG-101](#screen-handover-handovers-detail)
- [Hàng đợi bàn giao xe](#screen-handover-handovers-list)
- [Biên nhận bàn giao BG-101](#screen-handover-handovers-receipt)

<a id="screen-handover-dashboard"></a>
#### Tổng quan · Nhân viên bàn giao — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [handover/dashboard.html](../src/main/resources/templates/handover/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/handover/dashboard-handover](http://localhost:8080/ui/handover/dashboard-handover) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hợp đồng · xe; Khách mua; Điều kiện; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Chờ bàn giao; Đủ điều kiện; Đang bị chặn. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Hàng đợi bàn giao xe — **Nhân viên bàn giao** — [/ui/handover/handovers](http://localhost:8080/ui/handover/handovers) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **HĐB-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-103 · Honda City RS**: Xem chi tiết | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-handover-handovers-close"></a>
#### Đóng sale HĐB-101 — `handovers/close`

**Mục đích:** Chỉ đóng sale khi thanh toán, giấy tờ chuyển quyền và bàn giao hoàn tất.

**File HTML:** [handover/handovers/close.html](../src/main/resources/templates/handover/handovers/close.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/handover/handovers/101/close](http://localhost:8080/ui/handover/handovers/101/close) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/handover/handovers/102/close](http://localhost:8080/ui/handover/handovers/102/close) | ACTIVE | Thanh toán hoàn tất; Sang tên hoàn tất; Bàn giao thực tế hoàn tất |
| [/ui/handover/handovers/103/close](http://localhost:8080/ui/handover/handovers/103/close) | ACTIVE | Thanh toán hoàn tất; Sang tên hoàn tất; Bàn giao thực tế hoàn tất |

**Checklist cần đọc:** Thanh toán hoàn tất; Sang tên hoàn tất; Bàn giao thực tế hoàn tất. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Ghi chú đóng sale | Có | Nhập nội dung nhiều dòng |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Bàn giao** | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Áp dụng mẫu handovers/101/close |
| Đầu trang: **Bàn giao** | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Áp dụng mẫu handovers/102/close |
| Đầu trang: **Bàn giao** | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Áp dụng mẫu handovers/103/close |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-handover-handovers-detail"></a>
#### Checklist bàn giao BG-101 — `handovers/detail`

**Mục đích:** Ghi odo, chìa khóa, phụ kiện, ảnh tình trạng, chữ ký và biên nhận bàn giao.

**File HTML:** [handover/handovers/detail.html](../src/main/resources/templates/handover/handovers/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | BLOCKED | Đã đủ thanh toán; Đủ điều kiện giấy tờ: HĐB-102 còn thiếu giấy tờ sang tên. |
| [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | BLOCKED | Đã đủ thanh toán: HĐB-103 còn thiếu khoản thu.; Đủ điều kiện giấy tờ |

**Thông tin cần đọc:** Hợp đồng; Xe; Khách nhận; Nhân viên giao.

**Checklist cần đọc:** Đã đủ thanh toán; Đủ điều kiện giấy tờ; Đã đối chiếu xe thực tế. Dòng chưa đạt có giải thích tại UI.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Odo khi bàn giao (km) | Có | Nhập số; UI không nhận số âm |
| Số chìa khóa | Có | Nhập số; UI không nhận số âm |
| Phụ kiện đi kèm | Có | Nhập nội dung nhiều dòng |
| Ảnh tình trạng xe | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |
| Bằng chứng chữ ký người nhận | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |
| Bằng chứng chữ ký người giao | Có | Chọn PDF/JPG/PNG tối đa 10 MB; không upload lên server |
| Thời gian bàn giao | Có | Chọn ngày và giờ |
| Đã đối chiếu danh sách bàn giao với khách | Có | Đánh dấu xác nhận |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Hàng đợi** | Mở Hàng đợi bàn giao xe — **Nhân viên bàn giao** — [/ui/handover/handovers](http://localhost:8080/ui/handover/handovers) | Áp dụng mẫu handovers/101 |
| Đầu trang: **Biên nhận mẫu** | Mở Biên nhận bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101/receipt](http://localhost:8080/ui/handover/handovers/101/receipt) | Áp dụng mẫu handovers/101 |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-101 — **Nhân viên hồ sơ** — [/ui/documents/transfers/101](http://localhost:8080/ui/documents/transfers/101) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Đóng sale** | Mở Đóng sale HĐB-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101/close](http://localhost:8080/ui/handover/handovers/101/close) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Biên nhận mẫu** | Mở Biên nhận bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102/receipt](http://localhost:8080/ui/handover/handovers/102/receipt) | Áp dụng mẫu handovers/102 |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-102 — **Nhân viên hồ sơ** — [/ui/documents/transfers/102](http://localhost:8080/ui/documents/transfers/102) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Đóng sale** | Mở Đóng sale HĐB-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102/close](http://localhost:8080/ui/handover/handovers/102/close) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Đầu trang: **Biên nhận mẫu** | Mở Biên nhận bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103/receipt](http://localhost:8080/ui/handover/handovers/103/receipt) | Áp dụng mẫu handovers/103 |
| Các bước liên quan: **Sang tên** | Mở Hồ sơ sang tên ST-103 — **Nhân viên hồ sơ** — [/ui/documents/transfers/103](http://localhost:8080/ui/documents/transfers/103) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Các bước liên quan: **Đóng sale** | Mở Đóng sale HĐB-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103/close](http://localhost:8080/ui/handover/handovers/103/close) | Điều hướng preview; không tự chuyển dữ liệu sau khi gửi form. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

**Khi bị chặn:** hoàn tất form bắt buộc trước; UI sẽ kiểm tra các dòng điều kiện chưa đạt. Trên form quyết định, “Yêu cầu bổ sung”/“Từ chối” có thể review khi còn blocker; thao tác phê duyệt/hoàn tất thì không được bỏ qua blocker. Không có liên kết nào tự xóa blocker của fixture.

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-handover-handovers-list"></a>
#### Hàng đợi bàn giao xe — `handovers/list`

**Mục đích:** Thanh toán, sang tên và bàn giao là các mốc riêng; kiểm tra điều kiện trước khi giao.

**File HTML:** [handover/handovers/list.html](../src/main/resources/templates/handover/handovers/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/handover/handovers](http://localhost:8080/ui/handover/handovers) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Hợp đồng · xe; Khách mua; Điều kiện; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **HĐB-101 · Toyota Camry 2.5Q**: Xem chi tiết | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-102 · Mazda CX-5 Premium**: Xem chi tiết | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Liên kết đúng như UI hiện tại. |
| Dòng **HĐB-103 · Honda City RS**: Xem chi tiết | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-handover-handovers-receipt"></a>
#### Biên nhận bàn giao BG-101 — `handovers/receipt`

**Mục đích:** Biên nhận dữ liệu mẫu để kiểm tra bố cục in, không phải chứng từ đã ký.

**File HTML:** [handover/handovers/receipt.html](../src/main/resources/templates/handover/handovers/receipt.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/handover/handovers/101/receipt](http://localhost:8080/ui/handover/handovers/101/receipt) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/handover/handovers/102/receipt](http://localhost:8080/ui/handover/handovers/102/receipt) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/handover/handovers/103/receipt](http://localhost:8080/ui/handover/handovers/103/receipt) | DRAFT | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Khách nhận; Xe; VIN; Odo; Chìa khóa; Phụ kiện; Chữ ký.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **In biên nhận mẫu** | Mở hộp thoại in của trình duyệt; ở lại màn hình. | Áp dụng mẫu handovers/101/receipt |
| Đầu trang: **Checklist** | Mở Checklist bàn giao BG-101 — **Nhân viên bàn giao** — [/ui/handover/handovers/101](http://localhost:8080/ui/handover/handovers/101) | Áp dụng mẫu handovers/101/receipt |
| Đầu trang: **Checklist** | Mở Checklist bàn giao BG-102 — **Nhân viên bàn giao** — [/ui/handover/handovers/102](http://localhost:8080/ui/handover/handovers/102) | Áp dụng mẫu handovers/102/receipt |
| Đầu trang: **Checklist** | Mở Checklist bàn giao BG-103 — **Nhân viên bàn giao** — [/ui/handover/handovers/103](http://localhost:8080/ui/handover/handovers/103) | Áp dụng mẫu handovers/103/receipt |

<a id="role-admin"></a>
### Quản trị viên (`admin`)

Tra nhanh:

- [Tổng quan](#screen-admin-dashboard)
- [Nhật ký hoạt động](#screen-admin-audit-list)
- [Tạo tài khoản nhân viên](#screen-admin-employees-create)
- [Hồ sơ Nguyễn Việt Hải](#screen-admin-employees-detail)
- [Nhân sự & tài khoản](#screen-admin-employees-list)
- [Vai trò & quyền truy cập](#screen-admin-permissions-list)
- [Báo cáo vận hành](#screen-admin-reports-list)
- [Phiên đăng nhập & xác minh](#screen-admin-sessions-list)

<a id="screen-admin-dashboard"></a>
#### Tổng quan · Quản trị viên — `dashboard`

**Mục đích:** Công việc cần xử lý hôm nay và các bước tiếp theo theo vai trò.

**File HTML:** [admin/dashboard.html](../src/main/resources/templates/admin/dashboard.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/dashboard-admin](http://localhost:8080/ui/admin/dashboard-admin) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Nhân viên; Email; Vai trò; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Nhân viên; Vai trò; Phiên truy cập. Chỉ số là dữ liệu mẫu.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mở công việc** | Mở Nhân sự & tài khoản — **Quản trị viên** — [/ui/admin/employees](http://localhost:8080/ui/admin/employees) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Nguyễn Việt Hải**: Xem chi tiết | Mở Hồ sơ Nguyễn Việt Hải — **Quản trị viên** — [/ui/admin/employees/101](http://localhost:8080/ui/admin/employees/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Nguyễn Văn Khoa**: Xem chi tiết | Mở Hồ sơ Nguyễn Văn Khoa — **Quản trị viên** — [/ui/admin/employees/102](http://localhost:8080/ui/admin/employees/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Hà Lệ Quyên**: Xem chi tiết | Mở Hồ sơ Hà Lệ Quyên — **Quản trị viên** — [/ui/admin/employees/103](http://localhost:8080/ui/admin/employees/103) | Liên kết đúng như UI hiện tại. |
| Khối Luồng công việc theo vai trò | Bấm tên module để mở trang module cùng role. | Các dòng công việc còn có link chi tiết riêng bên trên. |

<a id="screen-admin-audit-list"></a>
#### Nhật ký hoạt động — `audit/list`

**Mục đích:** Lịch sử mẫu có người thực hiện, thời gian, đối tượng, lý do và trạng thái.

**File HTML:** [admin/audit/list.html](../src/main/resources/templates/admin/audit/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/audit](http://localhost:8080/ui/admin/audit) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Thời gian; Người thực hiện; Thao tác; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **15/10/2026 09:15**: Xem chi tiết | Mở Đánh giá yêu cầu PD-101 — **Quản lý** — [/ui/manager/approvals/101](http://localhost:8080/ui/manager/approvals/101) | Liên kết đúng như UI hiện tại. |
| Dòng **14/10/2026 14:30**: Xem chi tiết | Mở Giấy tờ DOC-102 · v2 — **Nhân viên hồ sơ** — [/ui/documents/documents/102](http://localhost:8080/ui/documents/documents/102) | Liên kết đúng như UI hiện tại. |

<a id="screen-admin-employees-create"></a>
#### Tạo tài khoản nhân viên — `employees/create`

**Mục đích:** Gán một vai trò chính trong MVP; xác minh email thuộc luồng riêng.

**File HTML:** [admin/employees/create.html](../src/main/resources/templates/admin/employees/create.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/employees/new](http://localhost:8080/ui/admin/employees/new) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Họ tên | Có | Nhập thông tin |
| Email công việc | Có | Nhập email |
| Số điện thoại | Có | Nhập số điện thoại |
| Vai trò chính | Có | Chọn: Quản trị viên; Quản lý; Nhân viên ký gửi; Kiểm định viên; Nhân viên kinh doanh; Nhân viên tài chính; Nhân viên bàn giao; Nhân viên hồ sơ |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Về danh sách** | Mở Nhân sự & tài khoản — **Quản trị viên** — [/ui/admin/employees](http://localhost:8080/ui/admin/employees) | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Lưu nháp trên trình duyệt** | Lưu dữ liệu nhập trong phiên của URL này; ở lại trang. | Không tạo record; không lưu file/mật khẩu. |
| Form: **Xóa nháp** | Xác nhận rồi bỏ nháp, trả giá trị mẫu ban đầu; ở lại trang. | Không xóa hồ sơ ở database. |
| Form: **Kiểm tra & mô phỏng gửi** | Kiểm tra → hộp review → “Xác nhận mô phỏng” → kết quả tại chỗ. | Không tự mở trang kế tiếp hoặc đổi trạng thái server. Xem mục 4 và 13. |

<a id="screen-admin-employees-detail"></a>
#### Hồ sơ Nguyễn Việt Hải — `employees/detail`

**Mục đích:** Xem vai trò, trạng thái xác minh và lịch sử truy cập của nhân viên.

**File HTML:** [admin/employees/detail.html](../src/main/resources/templates/admin/employees/detail.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/employees/101](http://localhost:8080/ui/admin/employees/101) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/admin/employees/102](http://localhost:8080/ui/admin/employees/102) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |
| [/ui/admin/employees/103](http://localhost:8080/ui/admin/employees/103) | ACTIVE | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** Email; Vai trò; Xác minh email; Phiên đang hoạt động; Họ tên.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Xem phiên đăng nhập** | Mở Phiên đăng nhập & xác minh — **Quản trị viên** — [/ui/admin/sessions](http://localhost:8080/ui/admin/sessions) | Áp dụng mẫu employees/101 |
| Đầu trang: **Mô phỏng khóa tài khoản** | Mở hộp thoại xác nhận → xác nhận thì thông báo mô phỏng tại chỗ, không mở trang khác. | Áp dụng mẫu employees/101 |

**Lịch sử:** xem actor/thời gian/nội dung trong “Lịch sử & bằng chứng”. Lịch sử là mẫu chỉ đọc; mô phỏng form không chèn event mới.

<a id="screen-admin-employees-list"></a>
#### Nhân sự & tài khoản — `employees/list`

**Mục đích:** Quản lý tài khoản nhân viên, vai trò chính và trạng thái truy cập.

**File HTML:** [admin/employees/list.html](../src/main/resources/templates/admin/employees/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/employees](http://localhost:8080/ui/admin/employees) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Nhân viên; Email; Vai trò; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Thêm nhân viên** | Mở Tạo tài khoản nhân viên — **Quản trị viên** — [/ui/admin/employees/new](http://localhost:8080/ui/admin/employees/new) | Theo dữ liệu và trạng thái hiện tại. |
| Dòng **Nguyễn Việt Hải**: Xem chi tiết | Mở Hồ sơ Nguyễn Việt Hải — **Quản trị viên** — [/ui/admin/employees/101](http://localhost:8080/ui/admin/employees/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Nguyễn Văn Khoa**: Xem chi tiết | Mở Hồ sơ Nguyễn Văn Khoa — **Quản trị viên** — [/ui/admin/employees/102](http://localhost:8080/ui/admin/employees/102) | Liên kết đúng như UI hiện tại. |
| Dòng **Hà Lệ Quyên**: Xem chi tiết | Mở Hồ sơ Hà Lệ Quyên — **Quản trị viên** — [/ui/admin/employees/103](http://localhost:8080/ui/admin/employees/103) | Liên kết đúng như UI hiện tại. |

<a id="screen-admin-permissions-list"></a>
#### Vai trò & quyền truy cập — `permissions/list`

**Mục đích:** Ma trận quyền xem và thao tác. Điều chỉnh tại đây chỉ mô phỏng UI.

**File HTML:** [admin/permissions/list.html](../src/main/resources/templates/admin/permissions/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/permissions](http://localhost:8080/ui/admin/permissions) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Thông tin cần đọc:** MVP; Quyền nghiệp vụ.

**Cần chọn:** checkbox quyền Xem công việc, Xử lý module, Phê duyệt, Quản trị quyền theo từng role. Ma trận chỉ là minh họa; không lưu vào hệ thống.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Mô phỏng lưu ma trận** | Mở hộp thoại xác nhận → xác nhận thì thông báo mô phỏng tại chỗ, không mở trang khác. | Theo dữ liệu và trạng thái hiện tại. |

<a id="screen-admin-reports-list"></a>
#### Báo cáo vận hành — `reports/list`

**Mục đích:** Tách tiền cọc khỏi hoa hồng; theo dõi xe bán, rút ký gửi và sang tên trễ hạn.

**File HTML:** [admin/reports/list.html](../src/main/resources/templates/admin/reports/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/reports](http://localhost:8080/ui/admin/reports) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Chỉ số; Giá trị; Diễn giải; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Chỉ số hiển thị:** Đang đăng bán; Đã bán; Hoa hồng; Sang tên trễ. Chỉ số là dữ liệu mẫu.

**Cần nhập/chọn:** dấu bắt buộc là quy định của form UI hiện tại, không mặc nhiên là đặc tả đầy đủ của ERD.

| Trường | Bắt buộc | Cách thao tác / lựa chọn |
|---|---|---|
| Từ ngày | Có | Chọn ngày |
| Đến ngày | Có | Chọn ngày |

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Đầu trang: **Xuất CSV mẫu** | Tải CSV mẫu theo dữ liệu đang lọc; ở lại màn hình. | Theo dữ liệu và trạng thái hiện tại. |
| Form: **Xem khoảng ngày mẫu** | Kiểm tra khoảng ngày → review → kết quả tại chỗ. | Chỉ số vẫn cố định, không có truy vấn theo ngày thật. |

<a id="screen-admin-sessions-list"></a>
#### Phiên đăng nhập & xác minh — `sessions/list`

**Mục đích:** Theo dõi vòng đời token; không hiển thị token hoặc mật khẩu.

**File HTML:** [admin/sessions/list.html](../src/main/resources/templates/admin/sessions/list.html).

| URL mở trang | Trạng thái mẫu | Điều kiện chưa đáp ứng trong mẫu |
|---|---|---|
| [/ui/admin/sessions](http://localhost:8080/ui/admin/sessions) | Không có badge trạng thái riêng | Không có blocker được khai báo tại trang này; không suy ra đã đủ mọi điều kiện nghiệp vụ. |

**Các cột chính:** Nhân viên; Thiết bị; Hết hạn; trạng thái; thao tác. Có tìm kiếm/lọc/sắp xếp/phân trang/xuất CSV theo mục 4.

**Nhập liệu:** không có form nhập nghiệp vụ riêng; dùng các nút và link dưới đây để xem/xử lý tiếp.

**Bấm gì và màn hình nào được mở?**

| Vị trí / nút | Kết quả khi bấm | Điều kiện / chú ý |
|---|---|---|
| Dòng **Nguyễn Việt Hải**: Xem chi tiết | Mở Hồ sơ Nguyễn Việt Hải — **Quản trị viên** — [/ui/admin/employees/101](http://localhost:8080/ui/admin/employees/101) | Liên kết đúng như UI hiện tại. |
| Dòng **Nguyễn Văn Khoa**: Xem chi tiết | Mở Hồ sơ Nguyễn Văn Khoa — **Quản trị viên** — [/ui/admin/employees/102](http://localhost:8080/ui/admin/employees/102) | Liên kết đúng như UI hiện tại. |

<a id="trang-chung"></a>
### Đăng nhập, xác minh và các trang chung

| Trang | Thao tác | Mở trang nào / kết quả |
|---|---|---|
| [/ui](http://localhost:8080/ui) / [/ui/urls](http://localhost:8080/ui/urls) | Tìm màn hình/role; mở “Tất cả URL của role”; bấm “Mở tổng quan” hoặc tên màn hình. | Mở đúng URL role. Nhập tìm kiếm chỉ lọc danh mục; không tìm database. |
| [/ui/auth/sign-in](http://localhost:8080/ui/auth/sign-in) | Nhập email/mật khẩu mẫu, lưu nháp/review mô phỏng; “Xác minh email / OTP”. | Review ở lại trang; link xác minh mở [/ui/auth/verify](http://localhost:8080/ui/auth/verify). Link Spring Security mở `/login`. |
| [/ui/auth/verify](http://localhost:8080/ui/auth/verify) | Nhập email + OTP 6 số; review; “Mô phỏng gửi lại mã”; “Đăng nhập”. | Gửi lại chỉ xác nhận/toast; đăng nhập mở [/ui/auth/sign-in](http://localhost:8080/ui/auth/sign-in); không xác thực OTP thật. |
| `/ui/{role}/sign-in` của 8 role | Dùng chung `auth/sign-in.html`; thao tác như đăng nhập mẫu. | Không đăng nhập vào role thật. Mở `/ui` để chọn role xem trước. |
| Đăng nhập mẫu theo role Quản lý: [/ui/manager/sign-in](http://localhost:8080/ui/manager/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Nhân viên kinh doanh: [/ui/sales/sign-in](http://localhost:8080/ui/sales/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Nhân viên ký gửi: [/ui/consignment/sign-in](http://localhost:8080/ui/consignment/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Kiểm định viên: [/ui/inspector/sign-in](http://localhost:8080/ui/inspector/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Nhân viên hồ sơ: [/ui/documents/sign-in](http://localhost:8080/ui/documents/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Nhân viên tài chính: [/ui/finance/sign-in](http://localhost:8080/ui/finance/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Nhân viên bàn giao: [/ui/handover/sign-in](http://localhost:8080/ui/handover/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| Đăng nhập mẫu theo role Quản trị viên: [/ui/admin/sign-in](http://localhost:8080/ui/admin/sign-in) | Nhập email/mật khẩu, lưu nháp/review mô phỏng. | Dùng `auth/sign-in.html`; link xác minh mở `/ui/auth/verify`, không tự mở dashboard. |
| [/home](http://localhost:8080/home) (đồng thời `/`) | Menu Trang chủ/Giới thiệu/Quy trình/Xe ký gửi/Đánh giá/Liên hệ; xem quy trình; slider trước/sau; đặt lịch xem; đổi sáng/tối; về đầu trang. | Các menu và CTA cuộn/mở phần tương ứng tại Home; đặt lịch trên thẻ xe dẫn về phần liên hệ, không tự tạo Viewing. Theme được nhớ localStorage. |
| Home: Đăng nhập/Đăng ký | Mở modal; nhập thông tin và bấm gửi. | Form cũ điều hướng sang `/verify-otp?mode=login` hoặc `mode=register`, kèm email; không tạo tài khoản/JWT thật. |
| Home: Gửi tư vấn | Mở modal, nhập liên hệ/nhu cầu, tìm/chọn gợi ý xe, bấm “Gửi tư vấn”. | Mô phỏng chờ rồi hiện thông báo, reset/đóng modal; giới hạn gửi lại 30 giây ở trình duyệt. Không tạo Inquiry/Consignment tự động. |
| Home: newsletter/social/profile/history/settings/logout cũ | Các mục của template public giữ nguyên. | Chưa có nghiệp vụ backend hoặc màn hình đích hoàn chỉnh cho các anchor này; không xem là chức năng portal buyer của MVP. |
| [/verify-otp](http://localhost:8080/verify-otp) | Màn OTP cũ có **3 ô**, nút “Xác nhận”, gửi lại đếm ngược 30 giây; điều hướng bàn phím/dán mã. | Mã demo hardcoded `123` hiển thị thành công rồi mở `/home`; nhập sai báo lỗi. Đây là demo local cũ, không phải tài khoản/token thật. |

**Hai màn OTP khác nhau:** `/ui/auth/verify` dùng form preview **6 số**; `/verify-otp` là template cũ **3 số** với mã demo. ERD không chốt độ dài OTP. Cần thống nhất đặc tả khi triển khai xác thực; tài liệu này ghi lại đúng UI đang có, không tự thay quy tắc nghiệp vụ.

### Nguồn tra cứu và cách cập nhật

- [ERD nghiệp vụ gốc](../../docs/UCCMS_Business_Driven_Conceptual_ERD.html).
- [URL theo role và đường dẫn template](../../docs/frontend/URLS_BY_ROLE.md).
- [Bảng URL → HTML](../../docs/frontend/TEMPLATE_STRUCTURE.md).
- [Phân tích nghiệp vụ, giả định và hợp đồng backend còn thiếu](../../docs/frontend/BUSINESS_ANALYSIS.md).
- Các chương 1–13 là hướng dẫn nghiệp vụ được viết riêng; mục 14 đối chiếu dữ liệu/nút/đường dẫn từ catalog, xử lý khác biệt role theo `UiPageRenderer` và các thành phần dùng chung.
- Khi UI đổi, chỉnh phần hướng dẫn liên quan rồi chạy `python -X utf8 docs/frontend/update_operation_reference.py` từ workspace gốc để cập nhật phụ lục. Script chỉ sửa phần sau marker trong tài liệu này, không chạy ứng dụng hoặc ghi đè source UI.

<!-- SCREEN_REFERENCE_END -->
