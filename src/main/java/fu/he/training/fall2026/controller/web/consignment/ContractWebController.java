package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.controller.web.inspector.InspectionUiData;
import fu.he.training.fall2026.dto.consignment.AgreementRequest;
import fu.he.training.fall2026.dto.consignment.AgreementResponse;
import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.service.ConsignmentAgreementService;
import fu.he.training.fall2026.service.ConsignmentService;
import fu.he.training.fall2026.service.ConsignorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UI hợp đồng ký gửi (contracts): danh sách + màn soạn tạo.
 * Dữ liệu lấy từ consignment_agreements; tạo mới gắn theo hồ sơ ký gửi.
 */
@Controller
@RequestMapping("/consignment/contracts")
@RequiredArgsConstructor
public class ContractWebController {

    private static final String LIST_VIEW = "consignment/contracts/list";
    private static final String CREATE_VIEW = "consignment/contracts/create";

    private static final BigDecimal DEFAULT_COMMISSION = new BigDecimal("1.5");
    private static final int DEFAULT_TERM_DAYS = 60;

    private final ConsignmentAgreementRepository agreementRepository;
    private final ConsignmentAgreementService agreementService;
    private final ConsignmentService consignmentService;
    private final ConsignorService consignorService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AgreementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        String pattern = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase() + "%";
        Sort newestFirst = Sort.by(Sort.Direction.DESC, "id");
        Page<ConsignmentAgreement> result = CatalogPageSupport.lastPageIfBeyond(
                agreementRepository.search(pattern, status, PageRequest.of(Math.max(page, 0), pageSize, newestFirst)),
                last -> agreementRepository.search(pattern, status, PageRequest.of(last, pageSize, newestFirst)));

        Map<AgreementStatus, Long> counts = new LinkedHashMap<>();
        for (AgreementStatus s : AgreementStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : agreementRepository.countByStatus()) {
            counts.put((AgreementStatus) row[0], (Long) row[1]);
        }

        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        model.addAttribute("size", pageSize);
        model.addAttribute("counts", counts);
        model.addAttribute("total", counts.values().stream().mapToLong(Long::longValue).sum());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long consignmentId,
            Model model) {
        if (consignmentId == null) {
            model.addAttribute("form", new AgreementRequest());
            model.addAttribute("consignmentId", null);
            model.addAttribute("consignment", null);
            model.addAttribute("consignor", null);
            model.addAttribute("agreement", null);
            model.addAttribute("termDays", DEFAULT_TERM_DAYS);
            return CREATE_VIEW;
        }
        return bindContractScreen(consignmentId, model);
    }

    @GetMapping("/{id}")
    public String open(@PathVariable Long id, Model model) {
        ConsignmentAgreement agreement = agreementRepository.findByIdWithConsignment(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng: " + id));
        return bindContractScreen(agreement.getConsignment().getId(), model);
    }

    /** Điền sẵn hợp đồng từ hồ sơ ký gửi (+ draft agreement nếu đã có). */
    private String bindContractScreen(Long consignmentId, Model model) {
        ConsignmentResponse consignment = consignmentService.get(consignmentId);
        Optional<AgreementResponse> latest = agreementService.findLatest(consignmentId);
        AgreementRequest form = latest
                .map(a -> formFromAgreement(consignmentId, a))
                .orElseGet(() -> formFromConsignment(consignment));

        model.addAttribute("form", form);
        model.addAttribute("consignmentId", consignmentId);
        model.addAttribute("consignment", consignment);
        model.addAttribute("consignor", consignorService.get(consignment.getConsignorId()));
        model.addAttribute("agreement", latest.orElse(null));
        model.addAttribute("termDays", DEFAULT_TERM_DAYS);
        model.addAttribute("listingPriceText", InspectionUiData.listingPriceText(consignment.getCode()));
        return CREATE_VIEW;
    }

    private static AgreementRequest formFromAgreement(Long consignmentId, AgreementResponse a) {
        AgreementRequest form = new AgreementRequest();
        form.setId(a.getId());
        form.setConsignmentId(consignmentId);
        form.setCommissionRate(a.getCommissionRate());
        form.setFixedFee(a.getFixedFee());
        form.setPriceFloor(a.getPriceFloor());
        form.setValidFrom(a.getValidFrom());
        form.setValidTo(a.getValidTo());
        form.setCustodyTerms(a.getCustodyTerms());
        form.setTerminationTerms(a.getTerminationTerms());
        form.setPayoutTerms(a.getPayoutTerms());
        form.setSignatureEvidencePath(a.getSignatureEvidencePath());
        return form;
    }

    private AgreementRequest formFromConsignment(ConsignmentResponse consignment) {
        var consignor = consignorService.get(consignment.getConsignorId());
        LocalDate from = consignment.getIntakeDate() != null ? consignment.getIntakeDate() : LocalDate.now();
        LocalDate to = from.plusDays(DEFAULT_TERM_DAYS);

        AgreementRequest form = new AgreementRequest();
        form.setConsignmentId(consignment.getId());
        form.setPriceFloor(consignment.getExpectedPrice());
        form.setCommissionRate(DEFAULT_COMMISSION);
        form.setValidFrom(from);
        form.setValidTo(to);
        form.setCustodyTerms("""
                4.1. Bên B có trách nhiệm bảo quản xe trong suốt thời gian ký gửi, đảm bảo xe không bị hư hỏng thêm so với tình trạng bàn giao.
                4.2. Bên B không được tự ý sửa chữa, cải tạo xe khi chưa có sự đồng ý bằng văn bản của Bên A.
                4.3. Bên A có quyền kiểm tra tình trạng xe vào các ngày làm việc với thông báo trước 24 giờ.
                4.4. Mọi rủi ro về hỏng hóc, trộm cắp trong quá trình bảo quản thuộc trách nhiệm của Bên B.""");
        form.setTerminationTerms("""
                6.1. Hợp đồng chấm dứt khi xe được bán thành công hoặc hết thời hạn ký gửi.
                6.2. Bên A có quyền rút ký gửi trước hạn với thông báo trước 3 ngày làm việc; không phát sinh phí nếu xe chưa được bán.
                6.3. Bên B có quyền chấm dứt nếu Bên A vi phạm nghĩa vụ cung cấp giấy tờ pháp lý đầy đủ.
                6.4. Khi chấm dứt, Bên B bàn giao xe lại trong trạng thái nguyên vẹn trong vòng 24 giờ.""");
        form.setPayoutTerms("""
                5.1. Sau khi xe được bán thành công, Bên B thanh toán cho Bên A số tiền bán xe trừ hoa hồng trong vòng 3 ngày làm việc.
                5.2. Hình thức thanh toán: Chuyển khoản ngân hàng — %s — %s — STK %s (chủ TK: %s).
                5.3. Bên B cung cấp đầy đủ chứng từ thanh toán và hóa đơn liên quan."""
                .formatted(
                        consignor.getBank(),
                        consignor.getAccountName(),
                        consignor.getAccountNumber(),
                        consignor.getFullName()));
        return form;
    }

    /** DRAFT → PENDING_SIGNATURE. Chỉnh sửa hợp đồng hiện out of scope. */
    @PostMapping("/submit")
    public String submit(@RequestParam Long consignmentId, RedirectAttributes flash) {
        agreementService.submit(consignmentId);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt hợp đồng. Nội dung được khóa, chờ ký.");
        return "redirect:/consignment/contracts";
    }
}
