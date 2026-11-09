package fu.he.training.fall2026.controller.web.consignmentrequest;

import fu.he.training.fall2026.dto.catalog.VehicleModelResponse;
import fu.he.training.fall2026.dto.consignment.ConsignmentCreateRequest;
import fu.he.training.fall2026.dto.consignment.ConsignmentRequestResponse;
import fu.he.training.fall2026.dto.consignment.WithdrawRequest;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.ValuationService;
import fu.he.training.fall2026.service.VehicleModelService;
import fu.he.training.fall2026.service.VehicleTypeService;
import fu.he.training.fall2026.service.consignmentrequest.ConsignmentRequestService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final ConsignmentRequestService consignmentRequestService;
    private final ValuationService valuationService;
    private final VehicleModelService vehicleModelService;
    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public String list(Model model) {
        /* DataTables tìm kiếm / sắp xếp / phân trang ở trình duyệt nên lấy toàn bộ hồ sơ. */
        model.addAttribute("consignments", consignmentRequestService.listAll());
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ConsignmentCreateRequest form = new ConsignmentCreateRequest();
        form.setIntakeDate(LocalDate.now());
        model.addAttribute("form", form);
        addFormOptions(model);
        return CREATE_VIEW;
    }

    /** Tạo cùng lúc chủ xe + xe + hồ sơ (DRAFT). "Gửi duyệt" cũng tạo rồi gửi duyệt luôn. */
    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("form") ConsignmentCreateRequest form,
            BindingResult errors,
            @RequestParam(defaultValue = "false") boolean submitAfter,
            Model model,
            RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                Long id = consignmentRequestService.create(form);
                if (submitAfter) {
                    consignmentRequestService.submit(id);
                    flash.addFlashAttribute("flashMessage", "Đã tạo hồ sơ và gửi duyệt.");
                } else {
                    flash.addFlashAttribute("flashMessage", "Đã tạo draft ký gửi.");
                }
                return "redirect:" + BASE + "/" + id;
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addFormOptions(model);
        return CREATE_VIEW;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var consignment = consignmentRequestService.get(id);
        model.addAttribute("consignment", consignment);
        model.addAttribute("listingPriceText", valuationService.listingPriceText(consignment.getCode()));
        return DETAIL_VIEW;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ConsignmentRequestResponse c = consignmentRequestService.get(id);
        if (c.getStatus() != ConsignmentStatus.DRAFT) {
            throw new BusinessException("Chỉ hồ sơ DRAFT mới được sửa hoặc gửi duyệt.", HttpStatus.CONFLICT);
        }
        ConsignmentCreateRequest form = new ConsignmentCreateRequest();
        form.setId(c.getId());
        form.setConsignorName(c.getConsignorName());
        form.setPhone(c.getPhone());
        form.setEmail(c.getEmail());
        form.setIdNumber(c.getIdNumber());
        form.setAddress(c.getAddress());
        form.setBank(c.getBank());
        form.setAccountNumber(c.getAccountNumber());
        form.setAccountName(c.getAccountName());
        form.setConsentStored(c.isConsentStored());
        // Xe chỉ lưu tên hãng + dòng: tìm lại dòng xe trong danh mục để chọn sẵn ô "Dòng xe" (không khớp thì để trống).
        form.setVehicleModelId(vehicleModelService.listActive().stream()
                .filter(m -> m.getName().equalsIgnoreCase(String.valueOf(c.getModel()))
                        && m.getBrandName().equalsIgnoreCase(String.valueOf(c.getMake())))
                .map(VehicleModelResponse::getId)
                .findFirst()
                .orElse(null));
        form.setLicensePlate(c.getLicensePlate());
        form.setYear(c.getYear());
        form.setColor(c.getColor());
        form.setOdometerKm(c.getOdometerKm());
        form.setVin(c.getVin());
        form.setEngineNumber(c.getEngineNumber());
        form.setVehicleType(c.getVehicleType());
        form.setTransmission(c.getTransmission());
        form.setFuelType(c.getFuelType());
        form.setLocation(c.getLocation());
        form.setIntakeCondition(c.getIntakeCondition());
        form.setIntakeDate(c.getIntakeDate());
        form.setCustodyLocation(c.getCustodyLocation());
        form.setExpectedPrice(c.getExpectedPrice());
        form.setTermNotes(c.getTermNotes());
        model.addAttribute("form", form);
        addFormOptions(model);
        return EDIT_VIEW;
    }

    /** Lưu sửa hồ sơ DRAFT (cùng form với màn tạo). "Gửi duyệt" thì lưu rồi gửi duyệt luôn. */
    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") ConsignmentCreateRequest form,
            BindingResult errors,
            @RequestParam(defaultValue = "false") boolean submitAfter,
            Model model,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE + "/new";
        }
        if (!errors.hasErrors()) {
            try {
                consignmentRequestService.updateDraft(form.getId(), form);
                if (submitAfter) {
                    consignmentRequestService.submit(form.getId());
                    flash.addFlashAttribute("flashMessage", "Đã lưu và gửi duyệt hồ sơ.");
                } else {
                    flash.addFlashAttribute("flashMessage", "Đã lưu draft.");
                }
                return "redirect:" + BASE + "/" + form.getId();
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            }
        }
        addFormOptions(model);
        return EDIT_VIEW;
    }

    /** Danh sách chọn cho form tạo và form sửa: dòng xe + loại xe đang hoạt động. */
    private void addFormOptions(Model model) {
        model.addAttribute("vehicleModels", vehicleModelService.listActive());
        model.addAttribute("vehicleTypes", vehicleTypeService.listActive());
    }

    @PostMapping("/submit")
    public String submit(@RequestParam Long id, RedirectAttributes flash) {
        consignmentRequestService.submit(id);
        flash.addFlashAttribute("flashMessage", "Đã gửi duyệt. Hồ sơ được khóa, không sửa trực tiếp nữa.");
        return "redirect:" + BASE + "/" + id;
    }

    /** Màn yêu cầu rút hồ sơ: hồ sơ đang WITHDRAW_REQUESTED thì hiện yêu cầu đã gửi, ngược lại hiện form gửi. */
    @GetMapping("/{id}/withdraw")
    public String withdrawForm(@PathVariable Long id, Model model) {
        model.addAttribute("consignment", consignmentRequestService.get(id));
        model.addAttribute("buyerHold", consignmentRequestService.buyerHoldDetail(id));
        return WITHDRAW_VIEW;
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @Valid @ModelAttribute WithdrawRequest form,
            BindingResult errors,
            HttpSession session,
            RedirectAttributes flash) {
        if (form.getId() == null) {
            return "redirect:" + BASE;
        }
        String back = "redirect:" + BASE + "/" + form.getId() + "/withdraw";
        if (errors.hasErrors()) {
            flash.addFlashAttribute("flashError", errors.getFieldError().getDefaultMessage());
            return back;
        }
        try {
            consignmentRequestService.requestWithdraw(form.getId(), form, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã gửi yêu cầu rút, chờ quản lý duyệt.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return back;
    }
}
