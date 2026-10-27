package fu.he.training.fall2026.controller.preview;

import fu.he.training.fall2026.model.ui.UiPreviewCatalog;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Single entry for all UI preview routes.
 * Role screens are driven by {@link UiPreviewCatalog}; no per-role controller boilerplate.
 */
// quyen
@Controller
@Profile("ui-preview")
public class UiPreviewController {

    private static final String NOT_FOUND_BODY =
            "UI preview screen not found. Open /ui to choose a role.";

    private final UiPageRenderer renderer;

    public UiPreviewController(UiPageRenderer renderer) {
        this.renderer = renderer;
    }

    @GetMapping({"/ui", "/ui/", "/ui/urls"})
    public String index(Model model) {
        model.addAttribute("roleLinks", UiPreviewCatalog.allLinks());
        return "preview/index";
    }

    @GetMapping("/ui/auth/{page}")
    public String auth(@PathVariable String page, Model model) {
        if ("verify".equals(page) || "verify-otp".equals(page)) {
            return "redirect:/verify-otp";
        }
        if (!"sign-in".equals(page)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("screen", UiPreviewCatalog.auth(page));
        model.addAttribute("roles", UiPreviewCatalog.roles());
        model.addAttribute("route", "/ui/auth/" + page);
        return "auth/sign-in";
    }

    @GetMapping({"/ui/{role}", "/ui/{role}/"})
    public String roleHome(@PathVariable String role) {
        requireKnownRole(role);
        return "redirect:/ui/" + role + "/" + UiPreviewCatalog.homePage(role);
    }

    @GetMapping("/ui/{role}/{*page}")
    public String rolePage(
            @PathVariable String role,
            @PathVariable("page") String page,
            Model model) {
        requireKnownRole(role);
        return renderer.render(role, normalizePage(page), model);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> missingScreen(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(NOT_FOUND_BODY);
    }

    private void requireKnownRole(String role) {
        if (!UiPreviewCatalog.isKnownRole(role)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /** Spring `{*page}` includes a leading slash; catalog keys do not. */
    private String normalizePage(String page) {
        if (page == null || page.isBlank() || "/".equals(page)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return page.startsWith("/") ? page.substring(1) : page;
    }
}
