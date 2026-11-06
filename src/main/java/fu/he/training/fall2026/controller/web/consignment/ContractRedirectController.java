package fu.he.training.fall2026.controller.web.consignment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Alias cũ /consignment/agreements → /consignment/contracts. */
@Controller
public class ContractRedirectController {

    @GetMapping({"/consignment/agreements", "/consignment/agreements/"})
    public String agreementsAlias() {
        return "redirect:/consignment/contracts";
    }
}
