package fu.he.training.fall2026.controller.web.documents;

import fu.he.training.fall2026.controller.web.sales.SalesDealUiData;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.util.SessionAuthUtils;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Nguyễn Phúc Khang: Hồ sơ sang tên của Nhân viên hồ sơ, chạy trên mock trong bộ nhớ ({@link TransferUiData}), chưa ghi DB.
 * Mỗi trạng thái có đúng một nút: Bắt đầu xử lý (người bấm thành người phụ trách), rồi Hoàn tất sang tên (cần tệp bằng chứng).
 * Hoàn tất là một trong ba mốc để đóng sale.
 */
@Controller
@RequestMapping("/documents/transfers")
public class TransferWebController {

    private static final String BASE = "/documents/transfers";
    private static final String LIST_VIEW = "documents/transfers/list";
    private static final String DETAIL_VIEW = "documents/transfers/detail";
    private static final String CREATE_VIEW = "documents/transfers/create";
    private static final List<String> EVIDENCE_TYPES = List.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");
    private static final Map<String, MediaType> VIEWABLE_TYPES = Map.of(
            "pdf", MediaType.APPLICATION_PDF,
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "png", MediaType.IMAGE_PNG);

    @ModelAttribute
    public void dictionaries(Model model) {
        model.addAttribute("statuses", TransferUiData.STATUSES);
    }

    @GetMapping
    public String list(@RequestParam Map<String, String> params, Model model) {
        SalesDealUiData.bindList(model, BASE, TransferUiData.cases(), "salePrice", "deadline", TransferUiData.STATUSES, params);
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String contractId, Model model) {
        model.addAttribute("candidates", TransferUiData.candidates());
        Long id = SalesDealUiData.parseId(contractId);
        Map<String, Object> contract = id == null
                ? null
                : SalesDealUiData.contract(id).filter(TransferUiData::canCreate).orElse(null);
        model.addAttribute("item", contract == null ? null : TransferUiData.preview(contract));
        model.addAttribute("checks", contract == null ? List.of() : SalesDealUiData.transferChecks(contract));
        return CREATE_VIEW;
    }

    @PostMapping
    public String create(
            @RequestParam(required = false) String contractId,
            RedirectAttributes flash) {
        Long id = SalesDealUiData.parseId(contractId);
        Map<String, Object> contract = id == null ? null : SalesDealUiData.contract(id).orElse(null);
        if (contract == null) {
            flash.addFlashAttribute("flashError", "Vui lòng chọn hợp đồng bán xe.");
            return "redirect:" + BASE + "/new";
        }
        var existing = TransferUiData.caseOf(contract);
        if (existing.isPresent()) {
            flash.addFlashAttribute("flashMessage", "Hợp đồng này đã có hồ sơ sang tên.");
            return "redirect:" + BASE + "/" + id;
        }
        if (!TransferUiData.canCreate(contract)) {
            flash.addFlashAttribute("flashError", "Hợp đồng " + contract.get("code") + " chưa đủ điều kiện tạo hồ sơ sang tên.");
            return "redirect:" + BASE + "/new";
        }
        TransferUiData.create(contract);
        flash.addFlashAttribute("flashMessage", "Đã tạo hồ sơ sang tên cho hợp đồng " + contract.get("code") + ".");
        return "redirect:" + BASE + "/" + id;
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable long id, Model model) {
        Map<String, Object> item = find(id);
        model.addAttribute("item", item);
        model.addAttribute("steps", TransferUiData.steps(item));
        SalesDealUiData.contract(id).ifPresent(contract -> {
            model.addAttribute("checks", SalesDealUiData.transferChecks(contract));
            model.addAttribute("milestones", SalesDealUiData.saleMilestones(contract));
        });
        model.addAttribute("hasEvidenceFile", TransferUiData.evidence(id).isPresent());
        return DETAIL_VIEW;
    }

    /** PDF và ảnh mở ngay trên trình duyệt, Word và Excel thì tải về. */
    @GetMapping("/{id:\\d+}/evidence")
    public ResponseEntity<byte[]> evidence(@PathVariable long id) {
        TransferUiData.Evidence file = TransferUiData.evidence(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tệp bằng chứng", id));
        MediaType type = VIEWABLE_TYPES.getOrDefault(extension(file.name()), MediaType.APPLICATION_OCTET_STREAM);
        String mode = VIEWABLE_TYPES.containsKey(extension(file.name())) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.builder(mode).filename(file.name(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }

    @PostMapping("/{id:\\d+}/start")
    public String start(@PathVariable long id, HttpSession session, RedirectAttributes flash) {
        Map<String, Object> item = find(id);
        if (TransferUiData.PENDING.equals(item.get("status"))) {
            item.put("status", TransferUiData.IN_PROGRESS);
            item.put("staff", SessionAuthUtils.getFullName(session));
            flash.addFlashAttribute("flashMessage", "Đã nhận xử lý hồ sơ " + item.get("code") + ".");
        }
        return "redirect:" + BASE + "/" + id;
    }

    @PostMapping("/{id:\\d+}/complete")
    public String complete(
            @PathVariable long id,
            @RequestParam(defaultValue = "") String notes,
            @RequestParam(required = false) MultipartFile evidence,
            RedirectAttributes flash) throws IOException {
        Map<String, Object> item = find(id);
        if (!TransferUiData.IN_PROGRESS.equals(item.get("status"))) {
            return "redirect:" + BASE + "/" + id;
        }
        item.put("notes", notes.trim());
        String fileName = fileName(evidence);
        if (fileName.isEmpty() || !EVIDENCE_TYPES.contains(extension(fileName))) {
            flash.addFlashAttribute("flashError", fileName.isEmpty()
                    ? "Cần tải lên tệp bằng chứng trước khi hoàn tất sang tên."
                    : "Tệp bằng chứng chỉ nhận PDF, Word, Excel, JPG hoặc PNG.");
            return "redirect:" + BASE + "/" + id;
        }
        TransferUiData.saveEvidence(id, fileName, evidence.getBytes());
        item.put("evidence", fileName);
        item.put("status", TransferUiData.COMPLETED);
        item.put("completedOn", LocalDate.now().toString());
        SalesDealUiData.markTransferred(id);
        flash.addFlashAttribute("flashMessage", "Đã hoàn tất sang tên hồ sơ " + item.get("code") + ".");
        return "redirect:" + BASE + "/" + id;
    }

    private static String fileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String name = StringUtils.getFilename(file.getOriginalFilename());
        return name == null ? "" : name;
    }

    private static String extension(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> find(long id) {
        return TransferUiData.item(id).orElseThrow(() -> new ResourceNotFoundException("Hồ sơ sang tên", id));
    }
}
