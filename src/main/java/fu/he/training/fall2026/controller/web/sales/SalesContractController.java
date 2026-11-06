package fu.he.training.fall2026.controller.web.sales;

import fu.he.training.fall2026.dto.sales.SalesContractRequest;
import fu.he.training.fall2026.dto.sales.SalesContractResponse;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.SalesContractService;
import fu.he.training.fall2026.service.SalesPipelineViewService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

@Controller
@RequiredArgsConstructor
@RequestMapping({"/sales/contracts", "/sales-contracts"})
public class SalesContractController {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DETAIL_VIEW = "sales/pipeline/contract-details";
    private static final String DETAIL_REDIRECT = "redirect:/sales/contracts/";

    private final SalesContractService contractService;
    private final SalesPipelineViewService viewService;

    @GetMapping
    public String list(@RequestParam(required = false) ContractStatus status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size, Model model) {
        int pageSize = size == 10 || size == 20 || size == 50 || size == 100 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(Math.max(1, page) - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("rows", contractService.list(status, keyword, pageable));
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", ContractStatus.values());
        return "sales/pipeline/contracts";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long reservationId, Model model) {
        model.addAttribute("form", new SalesContractRequest(reservationId, null, ""));
        return renderForm(model);
    }

    @PostMapping
    public String create(@Validated(SalesContractRequest.Create.class) @ModelAttribute("form") SalesContractRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return renderForm(model);
        }
        try {
            SalesContractResponse contract = contractService.create(form);
            redirect.addFlashAttribute("success", "Đã lập hợp đồng nháp.");
            return DETAIL_REDIRECT + contract.getId();
        } catch (BusinessException | ConcurrencyFailureException exception) {
            SalesFormErrors.reject(binding, exception);
            return renderForm(model);
        }
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        SalesContractResponse contract = contractService.get(id);
        model.addAttribute("contract", contract);
        model.addAttribute("form", new SalesContractRequest(
                contract.getSalePrice(), contract.getTerms(), contract.getSellerSignaturePath(),
                contract.getBuyerSignaturePath(), contract.getSignedOn(), contract.getStatus()));
        return DETAIL_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") SalesContractRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return renderDetails(id, model);
        }
        // Empty signature inputs preserve the signatures already stored.
        SalesContractRequest request = new SalesContractRequest(
                form.getSalePrice(), form.getTerms(), nullIfBlank(form.getSellerSignaturePath()),
                nullIfBlank(form.getBuyerSignaturePath()), form.getSignedOn(), form.getStatus());
        try {
            contractService.update(id, request);
            redirect.addFlashAttribute("success", "Đã cập nhật hợp đồng.");
            return DETAIL_REDIRECT + id;
        } catch (BusinessException | ConcurrencyFailureException exception) {
            SalesFormErrors.reject(binding, exception);
            return renderDetails(id, model);
        }
    }

    private String renderForm(Model model) {
        model.addAttribute("options", viewService.availableReservations());
        return "sales/pipeline/contract-form";
    }

    private String renderDetails(Long id, Model model) {
        model.addAttribute("contract", contractService.get(id));
        return DETAIL_VIEW;
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
