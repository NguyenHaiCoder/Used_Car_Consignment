package fu.he.training.fall2026.controller.web.documents;

import fu.he.training.fall2026.controller.web.consignment.CatalogPageSupport;
import fu.he.training.fall2026.dto.documents.TransferResponse;
import fu.he.training.fall2026.entity.enums.TransferStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.TransferService;
import fu.he.training.fall2026.util.SessionAuthUtils;
import fu.he.training.fall2026.security.Perm;
import fu.he.training.fall2026.security.RequirePermission;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Nguyễn Phúc Khang: Hồ sơ sang tên — list / tạo / chi tiết / bắt đầu / hoàn tất (BE).
 */
@Controller
@RequestMapping("/documents/transfers")
@RequiredArgsConstructor
public class TransferWebController {

    private static final String BASE = "/documents/transfers";
    private static final String LIST_VIEW = "documents/transfers/list";
    private static final String DETAIL_VIEW = "documents/transfers/detail";
    private static final String CREATE_VIEW = "documents/transfers/create";

    private final TransferService transferService;

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", transferService.statusMeta());
    }

    @RequirePermission(Perm.TRANSFER_VIEW)
    @GetMapping
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) String staff,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long amountMin,
            @RequestParam(required = false) Long amountMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model) {
        int pageSize = CatalogPageSupport.normalizeSize(size);
        Page<TransferResponse> result = CatalogPageSupport.lastPageIfBeyond(
                transferService.search(q, status, staff, from, to, amountMin, amountMax, page, pageSize),
                last -> transferService.search(q, status, staff, from, to, amountMin, amountMax, last, pageSize));

        Map<TransferStatus, Long> byStatus = transferService.countByStatus();
        Map<String, Long> counts = new LinkedHashMap<>();
        transferService.statusMeta().keySet().forEach(key -> counts.put(key, byStatus.getOrDefault(TransferStatus.valueOf(key), 0L)));

        model.addAttribute("base", BASE);
        model.addAttribute("page", result);
        model.addAttribute("counts", counts);
        model.addAttribute("staffs", transferService.listStaffNames());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status.name());
        model.addAttribute("staff", staff == null ? "" : staff);
        model.addAttribute("dateFrom", from == null ? "" : from.toString());
        model.addAttribute("dateTo", to == null ? "" : to.toString());
        model.addAttribute("amountMin", amountMin == null ? "" : amountMin.toString());
        model.addAttribute("amountMax", amountMax == null ? "" : amountMax.toString());
        model.addAttribute("size", pageSize);
        return LIST_VIEW;
    }

    @RequirePermission(Perm.TRANSFER_CREATE)
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long contractId, Model model) {
        model.addAttribute("candidates", transferService.candidates());
        if (contractId == null) {
            model.addAttribute("item", null);
            model.addAttribute("checks", java.util.List.of());
            return CREATE_VIEW;
        }
        try {
            TransferResponse preview = transferService.preview(contractId);
            model.addAttribute("item", preview);
            model.addAttribute("checks", transferService.transferChecks(contractId));
        } catch (BusinessException ex) {
            model.addAttribute("item", null);
            model.addAttribute("checks", java.util.List.of());
            model.addAttribute("flashError", ex.getMessage());
        }
        return CREATE_VIEW;
    }

    @RequirePermission(Perm.TRANSFER_CREATE)
    @PostMapping
    public String create(@RequestParam(required = false) Long contractId, RedirectAttributes flash) {
        if (contractId == null) {
            flash.addFlashAttribute("flashError", "Vui lòng chọn hợp đồng bán xe.");
            return "redirect:" + BASE + "/new";
        }
        try {
            Long id = transferService.create(contractId);
            flash.addFlashAttribute("flashMessage", "Đã tạo hồ sơ sang tên.");
            return "redirect:" + BASE + "/" + id;
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:" + BASE + "/new?contractId=" + contractId;
        }
    }

    @RequirePermission(Perm.TRANSFER_VIEW)
    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        TransferResponse item = transferService.get(id);
        model.addAttribute("item", item);
        model.addAttribute("steps", transferService.steps(TransferStatus.valueOf(item.getStatus())));
        model.addAttribute("checks", transferService.transferChecks(item.getSalesContractId()));
        model.addAttribute("milestones", transferService.saleMilestones(item.getSalesContractId()));
        model.addAttribute("hasEvidenceFile", item.isHasEvidenceFile());
        return DETAIL_VIEW;
    }

    @RequirePermission(Perm.TRANSFER_VIEW)
    @GetMapping("/{id:\\d+}/evidence")
    public RedirectView evidence(@PathVariable long id) {
        String url = transferService.evidenceUrl(id);
        return new RedirectView(url);
    }

    @RequirePermission(Perm.TRANSFER_START)
    @PostMapping("/{id:\\d+}/start")
    public String start(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        try {
            transferService.start(id, SessionAuthUtils.getUserId(session));
            flash.addFlashAttribute("flashMessage", "Đã nhận xử lý hồ sơ.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }

    @RequirePermission(Perm.TRANSFER_COMPLETE)
    @PostMapping("/{id:\\d+}/complete")
    public String complete(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String notes,
            @RequestParam(required = false) MultipartFile evidence,
            RedirectAttributes flash) {
        try {
            transferService.complete(id, notes, evidence);
            flash.addFlashAttribute("flashMessage", "Đã hoàn tất sang tên hồ sơ.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:" + BASE + "/" + id;
    }
}
