package fu.he.training.fall2026.controller.web.consignment;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Alias cũ /consignment/consignments/** → /consignments/**. */
@Controller
public class ConsignmentDashboardController {

    @GetMapping("/consignment/dashboard")
    public String dashboard() {
        return "redirect:/consignments";
    }

    @GetMapping({"/consignment/consignments", "/consignment/consignments/"})
    public String legacyList(HttpServletRequest request) {
        return redirectWithQuery("/consignments", request);
    }

    @GetMapping("/consignment/consignments/{*path}")
    public String legacyNested(@PathVariable("path") String path, HttpServletRequest request) {
        String target = "/consignments" + (path == null || path.isBlank() ? "" : "/" + path);
        return redirectWithQuery(target, request);
    }

    private static String redirectWithQuery(String target, HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null || query.isBlank() ? "redirect:" + target : "redirect:" + target + "?" + query;
    }
}
