package fu.he.training.fall2026.controller.web.consignmentcontract;

import fu.he.training.fall2026.dto.consignment.ConsignmentContractRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentContractResponse;
import fu.he.training.fall2026.dto.consignment.ConsignmentRequestResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.consignmentcontract.ConsignmentContractService;
import fu.he.training.fall2026.service.consignmentrequest.ConsignmentRequestService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Hợp đồng ký gửi: danh sách, màn soạn / xem hợp đồng của một hồ sơ, lưu bản thảo và gửi duyệt. */
@Controller
@RequestMapping("/consignment/contracts")
@RequiredArgsConstructor
public class ContractWebController {

    private static final String BASE = "/consignment/contracts";
    private static final String LIST_VIEW = "consignment/contracts/list";
    private static final String FORM_VIEW = "consignment/contracts/create";

    private static final BigDecimal DEFAULT_COMMISSION = new BigDecimal("1.5");
    private static final int DEFAULT_TERM_DAYS = 60;

    private static final String CUSTODY_TERMS = """
            4.1. Bên B có trách nhiệm bảo quản xe trong suốt thời gian ký gửi, đảm bảo xe không bị hư hỏng thêm so với tình trạng bàn giao.
            4.2. Bên B không được tự ý sửa chữa, cải tạo xe khi chưa có sự đồng ý bằng văn bản của Bên A.
            4.3. Bên A có quyền kiểm tra tình trạng xe vào các ngày làm việc với thông báo trước 24 giờ.
            4.4. Mọi rủi ro về hỏng hóc, trộm cắp trong quá trình bảo quản thuộc trách nhiệm của Bên B.""";

    private static final String TERMINATION_TERMS = """
            6.1. Hợp đồng chấm dứt khi xe được bán thành công hoặc hết thời hạn ký gửi.
            6.2. Bên A có quyền rút ký gửi trước hạn với thông báo trước 3 ngày làm việc; không phát sinh phí nếu xe chưa được bán.
            6.3. Bên B có quyền chấm dứt nếu Bên A vi phạm nghĩa vụ cung cấp giấy tờ pháp lý đầy đủ.
            6.4. Khi chấm dứt, Bên B bàn giao xe lại trong trạng thái nguyên vẹn trong vòng 24 giờ.""";

    private static final String PAYOUT_TERMS = """
            5.1. Sau khi xe được bán thành công, Bên B thanh toán cho Bên A số tiền bán xe trừ hoa hồng trong vòng 3 ngày làm việc.
            5.2. Hình thức thanh toán: Chuyển khoản ngân hàng — %s — %s — STK %s (chủ TK: %s).
            5.3. Bên B cung cấp đầy đủ chứng từ thanh toán và hóa đơn liên quan.""";

    private final ConsignmentContractService contractService;
    private final ConsignmentRequestService consignmentRequestService;

    @GetMapping
    public String list(Model model) {
        /* DataTables tìm kiếm / sắp xếp / phân trang ở trình duyệt nên lấy toàn bộ hợp đồng. */
        model.addAttribute("contracts", contractService.listAll());
        return LIST_VIEW;
    }

    /** Màn hợp đồng của một hồ sơ: điền sẵn mặc định, hoặc mở hợp đồng đã có của hồ sơ. */
    @GetMapping("/new")
    public String form(@RequestParam Long consignmentId, Model model) {
        ConsignmentRequestResponse consignment = consignmentRequestService.get(consignmentId);
        ConsignmentContractResponse agreement = contractService.findByConsignment(consignmentId).orElse(null);

        ConsignmentContractRequest form = new ConsignmentContractRequest();
        form.setConsignmentId(consignmentId);
        if (agreement != null) {
            // Đã có hợp đồng: điền theo hợp đồng đó.
            form.setId(agreement.getId());
            form.setCommissionRate(agreement.getCommissionRate());
            form.setPriceFloor(agreement.getPriceFloor());
            form.setValidFrom(agreement.getValidFrom());
            form.setValidTo(agreement.getValidTo());
            form.setCustodyTerms(agreement.getCustodyTerms());
            form.setTerminationTerms(agreement.getTerminationTerms());
            form.setPayoutTerms(agreement.getPayoutTerms());
        } else {
            // Chưa có hợp đồng: điền sẵn mặc định từ hồ sơ ký gửi.
            LocalDate from = consignment.getIntakeDate() != null ? consignment.getIntakeDate() : LocalDate.now();
            form.setPriceFloor(consignment.getExpectedPrice());
            form.setCommissionRate(DEFAULT_COMMISSION);
            form.setValidFrom(from);
            form.setValidTo(from.plusDays(DEFAULT_TERM_DAYS));
            form.setCustodyTerms(CUSTODY_TERMS);
            form.setTerminationTerms(TERMINATION_TERMS);
            form.setPayoutTerms(PAYOUT_TERMS.formatted(consignment.getBank(), consignment.getAccountName(),
                    consignment.getAccountNumber(), consignment.getConsignorName()));
        }
        model.addAttribute("form", form);
        model.addAttribute("consignment", consignment);
        model.addAttribute("agreement", agreement);
        return FORM_VIEW;
    }

    /** Lưu bản thảo: tạo mới khi chưa có hợp đồng, ngược lại sửa bản thảo. "Gửi duyệt" thì lưu rồi gửi duyệt luôn. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") ConsignmentContractRequest form,
            BindingResult errors,
            @RequestParam(defaultValue = "false") boolean submitAfter,
            Model model,
            RedirectAttributes flash) {
        if (form.getConsignmentId() == null) {
            return "redirect:/consignments";
        }
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    contractService.create(form);
                } else {
                    contractService.updateDraft(form);
                }
                if (submitAfter) {
                    contractService.submit(form.getConsignmentId());
                    flash.addFlashAttribute("flashMessage", "Đã lưu và gửi duyệt hợp đồng. Nội dung được khóa, chờ ký.");
                } else {
                    flash.addFlashAttribute("flashMessage", "Đã lưu nháp hợp đồng.");
                }
                return "redirect:" + BASE + "/new?consignmentId=" + form.getConsignmentId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignment", consignmentRequestService.get(form.getConsignmentId()));
        model.addAttribute("agreement", contractService.findByConsignment(form.getConsignmentId()).orElse(null));
        return FORM_VIEW;
    }

    /** DRAFT → PENDING_SIGNATURE (nút Gửi duyệt trên danh sách). */
    @PostMapping("/submit")
    public String submit(@RequestParam Long consignmentId, RedirectAttributes flash) {
        contractService.submit(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt hợp đồng. Nội dung được khóa, chờ ký.");
        return "redirect:" + BASE;
    }
}
