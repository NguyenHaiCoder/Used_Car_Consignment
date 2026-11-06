package fu.he.training.fall2026;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.assertj.core.api.Assertions.assertThat;

class CommonChromeRenderingTests {
    private String render(String markup, Map<String, Object> variables, String role) {
        var files = new ClassLoaderTemplateResolver();
        files.setPrefix("templates/");
        files.setSuffix(".html");
        files.setCharacterEncoding("UTF-8");
        files.setCheckExistence(true);
        files.setOrder(1);
        var strings = new StringTemplateResolver();
        strings.setOrder(2);
        var engine = new SpringTemplateEngine();
        engine.addTemplateResolver(files);
        engine.addTemplateResolver(strings);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        request.getSession().setAttribute("AUTH_ROLE_CODE", role);
        request.getSession().setAttribute("AUTH_FULL_NAME", "Demo Consignment");
        var exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, new MockHttpServletResponse());
        return engine.process(markup, new WebContext(exchange, Locale.forLanguageTag("vi"), variables));
    }

    @Test
    void sharedFragmentsAcceptNamedArgumentsAndOnlySelectOneMenuItem() {
        String html = render("""
                <aside th:replace="~{common/sidebar :: sidebar(active='consignments')}"></aside>
                <header th:replace="~{common/header :: header(heading='Workflow', alerts=1)}"></header>
                """, Map.of(), "CONSIGNMENT");
        assertThat(html).contains("common-sidebar", "common-header", "Workflow", "Demo Consignment", "bell-count")
                .containsOnlyOnce("aria-current=\"page\"")
                .containsPattern("<a[^>]*class=\"nav-item is-active\"[^>]*href=\"/consignments\"[^>]*>");
    }

    @Test
    void workflowLayoutUsesTheSharedFragments() {
        String html = render("""
                <html th:replace="~{shell/layout :: page('Workflow', 'Ký gửi', 'contracts', null, ~{}, ~{::#content})}">
                  <section id="content">Page content</section>
                </html>
                """, Map.of(), "CONSIGNMENT");
        assertThat(html).contains("common-sidebar", "common-header", "Page content", "Ký gửi")
                .containsOnlyOnce("aria-current=\"page\"")
                .containsPattern("<a[^>]*class=\"nav-item is-active\"[^>]*href=\"/consignment/contracts\"[^>]*>");
    }

    @Test
    void existingCallersKeepRoleNavigationAndScreenTitleWithoutArguments() {
        String html = render("""
                <aside th:replace="~{common/sidebar :: sidebar}"></aside>
                <header th:replace="~{common/header :: header}"></header>
                """, Map.of("screen", Map.of("title", "Nhân sự"),
                "navigation", List.of(Map.of("label", "Nhân sự", "url", "/admin/employees")),
                "activeNav", "/admin/employees"), "ADMIN");
        assertThat(html).contains("Nhân sự", "href=\"/admin/employees\"")
                .containsOnlyOnce("aria-current=\"page\"")
                .doesNotContain("Quản lý ký gửi</span>", "bell-count", "action=\"/consignments\"");
    }
}
