package fu.he.training.fall2026.controller.preview;

import fu.he.training.fall2026.model.ui.UiModels.Action;
import fu.he.training.fall2026.model.ui.UiModels.Fact;
import fu.he.training.fall2026.model.ui.UiModels.Field;
import fu.he.training.fall2026.model.ui.UiModels.Link;
import fu.he.training.fall2026.model.ui.UiModels.Role;
import fu.he.training.fall2026.model.ui.UiModels.Row;
import fu.he.training.fall2026.model.ui.UiModels.Screen;
import fu.he.training.fall2026.model.ui.UiPreviewCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

/**
 * Assembles MVC model for role screens from {@link UiPreviewCatalog}.
 * Used by /ui preview and authenticated app routes (e.g. /admin/** for Hải).
 */
@Component
public class UiPageRenderer {

    private static final Set<String> SALES_HIDDEN_ACTION_LABELS = Set.of("Kiểm định", "Định giá", "Tin đăng");
    private static final String SALES_HIDDEN_INTAKE_URL = "/ui/inspector/vehicles/intake";

    /** Preview mode: URLs stay under /ui/{role}/… */
    public String render(String roleKey, String pageKey, Model model) {
        return renderInternal(roleKey, pageKey, model, false, null);
    }

    /**
     * Authenticated app mode: same templates, URLs under /{role}/… (dashboard-admin → /admin/dashboard).
     *
     * @param sessionRole optional overlay (name/initials from HttpSession)
     */
    public String renderApp(String roleKey, String pageKey, Model model, Role sessionRole) {
        return renderInternal(roleKey, pageKey, model, true, sessionRole);
    }

    private String renderInternal(
            String roleKey,
            String pageKey,
            Model model,
            boolean appShell,
            Role sessionRole) {
        Screen screen = UiPreviewCatalog.find(roleKey, pageKey);
        if (screen == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Screen not found");
        }

        screen = withRoleScopedLinks(roleKey, screen);
        screen = withRoleActionFilters(roleKey, screen);
        screen = withManagerSettlementDecision(roleKey, screen);

        if (appShell) {
            screen = rewriteScreenToApp(roleKey, screen);
        }

        String route = appShell ? appRoute(roleKey, pageKey) : "/ui/" + roleKey + "/" + pageKey;
        List<Link> navigation = UiPreviewCatalog.links(roleKey);
        if (appShell) {
            navigation = navigation.stream()
                    .map(link -> new Link(link.label(), rewriteToApp(roleKey, link.url()), link.module()))
                    .toList();
        }

        Role role = sessionRole != null ? sessionRole : UiPreviewCatalog.role(roleKey);

        model.addAttribute("screen", screen);
        model.addAttribute("role", role);
        model.addAttribute("navigation", navigation);
        model.addAttribute("activeNav", resolveActiveNav(route, navigation));
        model.addAttribute("roles", UiPreviewCatalog.roles());
        model.addAttribute("route", route);
        model.addAttribute("appShell", appShell);
        model.addAttribute("roleHome", appShell ? "/" + roleKey + "/dashboard" : "/ui/" + roleKey);
        return UiPreviewCatalog.template(roleKey, pageKey);
    }

    private Screen withRoleScopedLinks(String roleKey, Screen screen) {
        List<Row> rows = screen.rows().stream()
                .map(row -> new Row(row.id(), row.cells(), row.status(), sameRoleUrl(roleKey, row.url())))
                .toList();
        List<Action> actions = screen.actions().stream()
                .map(action -> new Action(
                        action.label(),
                        sameRoleUrl(roleKey, action.url()),
                        action.kind(),
                        action.enabled(),
                        action.reason()))
                .toList();
        List<Link> steps = screen.steps().stream()
                .map(step -> new Link(step.label(), sameRoleUrl(roleKey, step.url()), step.module()))
                .toList();
        return copy(screen, screen.kind(), screen.fields(), rows, screen.facts(), actions, steps);
    }

    private Screen withRoleActionFilters(String roleKey, Screen screen) {
        List<Action> actions = screen.actions().stream()
                .filter(action -> !isHiddenSalesAction(roleKey, action))
                .filter(action -> !isHiddenManagerDraftAction(roleKey, action))
                .toList();
        if (actions.size() == screen.actions().size()) {
            return screen;
        }
        return copy(screen, screen.kind(), screen.fields(), screen.rows(), screen.facts(), actions, screen.steps());
    }

    private Screen withManagerSettlementDecision(String roleKey, Screen screen) {
        if (!"manager".equals(roleKey) || !"settlement".equals(screen.kind())) {
            return screen;
        }

        List<Fact> facts = new ArrayList<>(screen.facts());
        screen.fields().forEach(field -> facts.add(new Fact(field.label(), field.value())));

        List<Field> decisionFields = List.of(
                new Field("decision", "Quyết định quyết toán", "select", true, "",
                        List.of("Yêu cầu bổ sung", "Phê duyệt", "Từ chối"), ""),
                new Field("reason", "Lý do quyết định", "textarea", true, "", List.of(), ""));

        return copy(screen, "decision", decisionFields, screen.rows(), List.copyOf(facts), screen.actions(), screen.steps());
    }

    private Screen rewriteScreenToApp(String roleKey, Screen screen) {
        List<Row> rows = screen.rows().stream()
                .map(row -> new Row(row.id(), row.cells(), row.status(), rewriteToApp(roleKey, row.url())))
                .toList();
        List<Action> actions = screen.actions().stream()
                .map(action -> new Action(
                        action.label(),
                        rewriteToApp(roleKey, action.url()),
                        action.kind(),
                        action.enabled(),
                        action.reason()))
                .toList();
        List<Link> steps = screen.steps().stream()
                .map(step -> new Link(step.label(), rewriteToApp(roleKey, step.url()), step.module()))
                .toList();
        return copy(screen, screen.kind(), screen.fields(), rows, screen.facts(), actions, steps);
    }

    /** /ui/admin/dashboard-admin → /admin/dashboard ; /ui/admin/employees → /admin/employees */
    static String rewriteToApp(String roleKey, String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String prefix = "/ui/" + roleKey + "/";
        if (!url.startsWith(prefix)) {
            return url;
        }
        String key = url.substring(prefix.length());
        return appRoute(roleKey, key);
    }

    static String appRoute(String roleKey, String pageKey) {
        if (("dashboard-" + roleKey).equals(pageKey) || "dashboard".equals(pageKey)) {
            return "/" + roleKey + "/dashboard";
        }
        if ("sign-in".equals(pageKey)) {
            return "/";
        }
        return "/" + roleKey + "/" + pageKey;
    }

    private boolean isHiddenSalesAction(String roleKey, Action action) {
        return "sales".equals(roleKey)
                && (SALES_HIDDEN_INTAKE_URL.equals(action.url())
                || SALES_HIDDEN_ACTION_LABELS.contains(action.label()));
    }

    private boolean isHiddenManagerDraftAction(String roleKey, Action action) {
        return "manager".equals(roleKey) && "Sửa draft".equals(action.label());
    }

    private String resolveActiveNav(String currentRoute, List<Link> navigation) {
        return navigation.stream()
                .filter(link -> currentRoute.equals(link.url()) || currentRoute.startsWith(link.url() + "/"))
                .max(Comparator.comparingInt(link -> link.url().length()))
                .map(Link::url)
                .orElse("");
    }

    private String sameRoleUrl(String roleKey, String target) {
        if (target == null || !target.startsWith("/ui/")) {
            return target;
        }
        int separator = target.indexOf('/', 4);
        if (separator < 0) {
            return target;
        }
        String key = target.substring(separator + 1);
        return UiPreviewCatalog.find(roleKey, key) == null
                ? target
                : "/ui/" + roleKey + "/" + key;
    }

    private Screen copy(
            Screen source,
            String kind,
            List<Field> fields,
            List<Row> rows,
            List<Fact> facts,
            List<Action> actions,
            List<Link> steps) {
        return new Screen(
                source.key(),
                source.title(),
                source.module(),
                source.description(),
                kind,
                source.status(),
                source.notice(),
                fields,
                source.columns(),
                rows,
                facts,
                source.checks(),
                actions,
                source.history(),
                source.metrics(),
                steps);
    }
}
