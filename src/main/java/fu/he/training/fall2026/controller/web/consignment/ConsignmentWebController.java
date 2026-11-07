package fu.he.training.fall2026.controller.web.consignment;

import fu.he.training.fall2026.dto.consignment.ConsignmentRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentResponse;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ConsignmentService;
import fu.he.training.fall2026.service.ConsignorService;
import fu.he.training.fall2026.service.ValuationService;
import fu.he.training.fall2026.service.VehicleModelService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

/** 02.02 Consignment Request List, 02.03 Create / Edit Draft and 02.05 Withdraw request. */
@Controller
@RequestMapping("/consignments")
@RequiredArgsConstructor
public class ConsignmentWebController {

    private static final String BASE = "/consignments";
    private static final String LIST_VIEW = "consignment/requests/list";
    private static final String CREATE_VIEW = "consignment/requests/create";
    private static final String DETAIL_VIEW = "consignment/requests/detail";
    private static final String EDIT_VIEW = "consignment/requests/edit";
    private static final String WITHDRAW_VIEW = "consignment/withdrawn/withdrawn-detail";

    private final ConsignmentService consignmentService;
    private final ConsignorService consignorService;
    private final ValuationService valuationService;
    private final VehicleModelService vehicleModelService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ConsignmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<ConsignmentResponse> result = CatalogPageSupport.lastPageIfBeyond(
                consignmentService.search(q, status, page, pageSize),
                last -> consignmentService.search(q, status, last, pageSize));
        model.addAttribute("page", result);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", from);
        model.addAttribute("toDate", to);
        model.addAttribute("size", pageSize);
        model.addAttribute("counts", consignmentService.countByStatus());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ConsignmentRequest form = new ConsignmentRequest();
        form.setIntakeDate(LocalDate.now());
        model.addAttribute("form", form);
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("vehicleModels", vehicleModelService.listActive());
        return CREATE_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var consignment = consignmentService.get(id);
        model.addAttribute("consignment", consignment);
        model.addAttribute("consignor", consignorService.get(consignment.getConsignorId()));
        model.addAttribute("withdrawRequest", consignmentService.findWithdrawRequest(id).orElse(null));
        model.addAttribute("listingPriceText", valuationService.listingPriceText(consignment.getCode()));
        return DETAIL_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var consignment = consignmentService.get(id);
        model.addAttribute("form", consignmentService.formFor(id));
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("vehicleModels", vehicleModelService.listActive());
        model.addAttribute("listingPriceText", valuationService.listingPriceText(consignment.getCode()));
        return EDIT_VIEW;
    }

    /** Creates the draft when the hidden id is empty, otherwise updates that draft. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") ConsignmentRequest form,
            BindingResult errors,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                if (form.getId() == null) {
                    Long id = consignmentService.createDraft(form);
                    flash.addFlashAttribute("flashMessage", "Đã tạo draft ký gửi.");
                    return "redirect:" + BASE + "/" + id;
                }
                consignmentService.updateDraft(form.getId(), form);
                flash.addFlashAttribute("flashMessage", "Đã lưu draft.");
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("consignors", consignorService.listAll());
        model.addAttribute("vehicles", consignmentService.listVehicles());
        model.addAttribute("vehicleModels", vehicleModelService.listActive());
        return form.getId() == null ? CREATE_VIEW : EDIT_VIEW;
    }

    @PostMapping("/submit")
    public String submit(@RequestParam Long id, RedirectAttributes flash) {
        consignmentService.submit(id);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt. Hồ sơ được khóa, không sửa trực tiếp nữa.");
        return "redirect:" + BASE + "/" + id;
    }

    /** Màn yêu cầu rút hồ sơ — mock UI, rút bất cứ lúc nào, trừ xe đã có khách đặt cọc hoặc ký hợp đồng. */
    @GetMapping("/{id}/withdraw")
    public String withdrawForm(@PathVariable Long id, Model model) {
        model.addAttribute("consignment", consignmentService.get(id));
        model.addAttribute("buyerHold", consignmentService.buyerHoldDetail(id));
        if (!model.containsAttribute("withdrawSent")) {
            model.addAttribute("withdrawSent", false);
        }
        return WITHDRAW_VIEW;
    }

    /** Mock submit — không gọi service BE. */
    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam Long id,
            @RequestParam String reason,
            @RequestParam(required = false) String returnDate,
            RedirectAttributes flash) {
        String buyerHold = consignmentService.buyerHoldDetail(id);
        if (!buyerHold.isEmpty()) {
            flash.addFlashAttribute("flashError", "Chưa thể gửi yêu cầu rút: " + buyerHold);
            return "redirect:" + BASE + "/" + id + "/withdraw";
        }
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("reason", reason);
        preview.put("requestedByName", "Hà Lệ Quyên");
        preview.put("createdAt", LocalDateTime.now());
        preview.put("returnDate",
                returnDate == null || returnDate.isBlank() ? null : LocalDate.parse(returnDate));
        flash.addFlashAttribute("withdrawSent", true);
        flash.addFlashAttribute("withdrawRequest", preview);
        return "redirect:" + BASE + "/" + id + "/withdraw";
    }
}
