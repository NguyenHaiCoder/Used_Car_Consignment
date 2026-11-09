package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.constant.sales.SalesDealCatalog;
import fu.he.training.fall2026.dto.sales.DealListFilter;
import fu.he.training.fall2026.dto.sales.ReservationResponse;
import fu.he.training.fall2026.dto.sales.SalesContractForm;
import fu.he.training.fall2026.dto.sales.SalesContractResponse;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.service.PaymentService;
import fu.he.training.fall2026.service.ReservationService;
import fu.he.training.fall2026.service.SalesContractService;
import fu.he.training.fall2026.service.impl.ReservationServiceImpl;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.PermissionGuard;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Hợp đồng bán xe — controller mỏng, dữ liệu lấy từ {@link SalesContractService}. */
@Controller
@RequestMapping("/sales/contracts")
@RequiredArgsConstructor
public class SalesContractWebController {

    private static final String BASE = "/sales/contracts";
    private static final String LIST_VIEW = "sales/contracts/list";
    private static final String DOC_VIEW = "sales/contracts/create";

    private final SalesContractService salesContractService;
    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final PermissionGuard permissionGuard;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", SalesDealCatalog.CONTRACT_STATUSES);
    }

    @RequirePermission(Perm.SALES_CONTRACT_VIEW)
    @GetMapping
    public String list(@ModelAttribute DealListFilter filter, Model model) {
        salesContractService.bindList(model, BASE, filter);
        return LIST_VIEW;
    }

    @RequirePermission(Perm.SALES_CONTRACT_CREATE)
    @GetMapping("/new")
    public String createForm(
            @RequestParam(required = false) Long reservationId,
            Model model,
            RedirectAttributes flash) {
        if (reservationId == null) {
            model.addAttribute("contract", null);
            model.addAttribute("reservations", salesContractService.activeReservations());
            return DOC_VIEW;
        }
        String redirect = blockedBy(reservationId, flash);
        if (redirect != null) {
            return redirect;
        }
        return show(salesContractService.draftFromReservation(reservationId), toForm(null), null, model);
    }

    @RequirePermission(Perm.SALES_CONTRACT_CREATE)
    @PostMapping
    public String create(
            @RequestParam long reservationId,
            @Valid @ModelAttribute("form") SalesContractForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        requireSignPermission(form);
        String redirect = blockedBy(reservationId, flash);
        if (redirect != null) {
            return redirect;
        }
        SalesContractResponse draft = salesContractService.draftFromReservation(reservationId);
        applyDraftPreview(draft, form);
        if (errors.hasErrors()) {
            return show(draft, form, null, model);
        }
        try {
            Long id = salesContractService.create(reservationId, form, SessionAuthUtils.getUserId(session));
            return finish(salesContractService.findById(id), form, flash);
        } catch (BusinessException ex) {
            rejectContractBusiness(errors, ex.getMessage());
            return show(draft, form, null, model);
        }
    }

    @RequirePermission(Perm.SALES_CONTRACT_VIEW)
    @GetMapping("/{id}")
    public String open(@PathVariable long id, Model model) {
        SalesContractResponse contract = salesContractService.findById(id);
        return show(contract, toForm(contract), null, model);
    }

    @RequirePermission(Perm.SALES_CONTRACT_CREATE)
    @PostMapping("/{id}")
    public String save(
            @PathVariable long id,
            @Valid @ModelAttribute("form") SalesContractForm form,
            BindingResult errors,
            HttpSession session,
            Model model,
            RedirectAttributes flash) {
        requireSignPermission(form);
        SalesContractResponse contract = salesContractService.findById(id);
        if (!"DRAFT".equals(contract.getStatus())) {
            flash.addFlashAttribute("flashError", "Hợp đồng đã ký nên không sửa được.");
            return "redirect:" + BASE + "/" + id;
        }
        applyDraftPreview(contract, form);
        if (errors.hasErrors()) {
            return show(contract, form, null, model);
        }
        try {
            salesContractService.update(id, form, SessionAuthUtils.getUserId(session));
            return finish(salesContractService.findById(id), form, flash);
        } catch (BusinessException ex) {
            rejectContractBusiness(errors, ex.getMessage());
            return show(contract, form, null, model);
        }
    }

    /** Nguyễn Việt Hải: Lưu nháp và Ký chung một nút gửi, chỉ bấm Ký mới cần quyền ký. */
    private void requireSignPermission(SalesContractForm form) {
        if ("sign".equals(form.getIntent())) {
            permissionGuard.require(Perm.SALES_CONTRACT_SIGN);
        }
    }

    /** Sales gửi yêu cầu thu sang Tài chính. */
    @RequirePermission(Perm.SALES_PAYMENT_REQUEST)
    @PostMapping("/{id}/payment-request")
    public String requestPayment(@PathVariable long id, RedirectAttributes flash) {
        SalesContractResponse contract = salesContractService.findById(id);
        if (!"SIGNED".equals(contract.getStatus()) && !"ACTIVE".equals(contract.getStatus())) {
            flash.addFlashAttribute("flashError", "Chỉ gửi yêu cầu thu cho hợp đồng đã ký.");
            return "redirect:" + BASE + "/" + id;
        }
        try {
            paymentService.createRequest(id);
            flash.addFlashAttribute("flashMessage", "Đã gửi yêu cầu thu tiền sang Tài chính.");
        } catch (ResourceNotFoundException | BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    private String show(
            SalesContractResponse contract, SalesContractForm form, String error, Model model) {
        model.addAttribute("contract", contract);
        model.addAttribute("form", form != null ? form : toForm(contract));
        model.addAttribute("formError", error);
        model.addAttribute("editable", "DRAFT".equals(contract.getStatus()));
        model.addAttribute("isSigned", contract.getSignedOn() != null && !contract.getSignedOn().isBlank());
        model.addAttribute("steps", salesContractService.steps(contract));
        return DOC_VIEW;
    }

    private static SalesContractForm toForm(SalesContractResponse contract) {
        SalesContractForm form = new SalesContractForm();
        if (contract == null) {
            return form;
        }
        form.setSalePrice(ReservationServiceImpl.money(contract.getSalePrice()));
        form.setBuyerIdNumber(contract.getBuyerIdNumber());
        form.setBuyerAddress(contract.getBuyerAddress());
        form.setPaymentTerms(contract.getPaymentTerms());
        form.setHandoverTerms(contract.getHandoverTerms());
        form.setTerminationTerms(contract.getTerminationTerms());
        form.setSellerSignature(contract.getSellerSignature());
        form.setBuyerSignature(contract.getBuyerSignature());
        return form;
    }

    private static String finish(SalesContractResponse contract, SalesContractForm form, RedirectAttributes flash) {
        boolean wantedSign = "sign".equals(form.getIntent());
        if (wantedSign && !"SIGNED".equals(contract.getStatus()) && "PENDING".equals(contract.getFloorApprovalStatus())) {
            flash.addFlashAttribute("flashError", "Giá bán thấp hơn giá sàn của chủ xe nên chưa ký được. Đã lưu nháp và gửi "
                    + "Quản lý duyệt (" + contract.getFloorApprovalCode() + "), duyệt xong bấm Ký lại.");
        } else {
            flash.addFlashAttribute(
                    "flashMessage",
                    "SIGNED".equals(contract.getStatus())
                            ? "Đã ký hợp đồng " + contract.getCode() + "."
                            : "Đã lưu nháp hợp đồng " + contract.getCode() + ".");
        }
        return "redirect:" + BASE + "/" + contract.getId();
    }

    /** Phiếu đã có HĐ thì mở lại HĐ đó; phiếu không còn ACTIVE thì quay về phiếu. */
    private String blockedBy(Long reservationId, RedirectAttributes flash) {
        var existing = salesContractService.findByReservationId(reservationId);
        if (existing.isPresent()) {
            flash.addFlashAttribute(
                    "flashError",
                    "Phiếu đặt cọc này đã có hợp đồng, mình mở lại để cập nhật tiếp.");
            return "redirect:" + BASE + "/" + existing.get().getId();
        }
        ReservationResponse reservation = reservationService.findById(reservationId);
        if (!"ACTIVE".equals(reservation.getStatus())) {
            flash.addFlashAttribute(
                    "flashError", "Chỉ lập được hợp đồng từ phiếu đặt cọc còn hiệu lực.");
            return "redirect:/sales/reservations/" + reservationId;
        }
        return null;
    }

    private static void applyDraftPreview(SalesContractResponse draft, SalesContractForm form) {
        Long salePrice = ReservationServiceImpl.parseMoney(form.getSalePrice());
        if (salePrice != null) {
            draft.setSalePrice(salePrice);
            draft.setRemaining(salePrice - draft.getDeposit());
        }
        if (form.getBuyerIdNumber() != null) {
            draft.setBuyerIdNumber(form.getBuyerIdNumber().trim());
        }
        if (form.getBuyerAddress() != null) {
            draft.setBuyerAddress(form.getBuyerAddress().trim());
        }
        if (form.getPaymentTerms() != null && !form.getPaymentTerms().isBlank()) {
            draft.setPaymentTerms(form.getPaymentTerms().trim());
        }
        if (form.getHandoverTerms() != null && !form.getHandoverTerms().isBlank()) {
            draft.setHandoverTerms(form.getHandoverTerms().trim());
        }
        if (form.getTerminationTerms() != null && !form.getTerminationTerms().isBlank()) {
            draft.setTerminationTerms(form.getTerminationTerms().trim());
        }
        if (form.getSellerSignature() != null) {
            draft.setSellerSignature(form.getSellerSignature());
        }
        if (form.getBuyerSignature() != null) {
            draft.setBuyerSignature(form.getBuyerSignature());
        }
    }

    private static void rejectContractBusiness(BindingResult errors, String message) {
        if (message == null || message.isBlank()) {
            errors.reject("error", "Không lưu được hợp đồng.");
            return;
        }
        String lower = message.toLowerCase();
        if (lower.contains("giá bán") || lower.contains("tiền cọc")) {
            errors.rejectValue("salePrice", "salePrice", message);
        } else if (lower.contains("cccd")) {
            errors.rejectValue("buyerIdNumber", "buyerIdNumber", message);
        } else if (lower.contains("địa chỉ")) {
            errors.rejectValue("buyerAddress", "buyerAddress", message);
        } else if (lower.contains("chữ ký")) {
            errors.reject("error", message);
        } else {
            errors.reject("error", message);
        }
    }
}
