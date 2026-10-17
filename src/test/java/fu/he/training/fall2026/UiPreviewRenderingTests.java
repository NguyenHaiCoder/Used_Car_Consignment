package fu.he.training.fall2026;

import fu.he.training.fall2026.model.ui.UiPreviewCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Render actual MVC responses with Security and Thymeleaf, not template string checks. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "debug=false")
@ActiveProfiles("dev")
class UiPreviewRenderingTests {
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void everyRoleScreenAndItsLocalLinksRender() throws Exception {
        Set<String> links = new HashSet<>();
        var pattern = Pattern.compile("(?:href|src)=\"(/[^\"]*)\"");
        for (var group : UiPreviewCatalog.allLinks()) {
            for (var link : group.links()) {
                var response = get(link.url());
                assertEquals(200, response.statusCode(), link.url());
                assertTrue(HtmlUtils.htmlUnescape(response.body()).contains("<title>" + link.label()), link.url());
                assertFalse(response.body().contains("th:replace="), link.url());
                assertTrue(response.body().contains("Bản xem trước UI"), link.url());
                assertFalse(response.body().contains("href=\"#\""), link.url());
                String view = UiPreviewCatalog.template(group.role().key(), link.url().substring(("/ui/" + group.role().key() + "/").length()));
                assertNotNull(getClass().getResource("/templates/" + view + ".html"), view);
                assertTrue(response.body().contains("data-template=\"" + view + "\""), link.url() + " should render its role/module template");
                var matcher = pattern.matcher(response.body());
                while (matcher.find()) links.add(matcher.group(1));
            }
        }
        for (String link : links) assertEquals(200, get(link).statusCode(), link);
        assertEquals(200, get("/ui/auth/sign-in").statusCode());
        assertEquals(200, get("/ui/auth/verify").statusCode());
        assertEquals(200, get("/home").statusCode());
        assertEquals(200, get("/verify-otp").statusCode());
    }

    @Test
    void unknownScreensDoNotBecomeGenericPages() throws Exception {
        assertEquals(404, get("/ui/consignment/consignments/999").statusCode());
        assertEquals(404, get("/ui/auth/unknown").statusCode());
        assertEquals(404, get("/ui/sales/vehicles/intake").statusCode());
    }

    @Test
    void roleVisibilityAndBlockedFixturesAreVisible() throws Exception {
        String sales = get("/ui/sales/vehicles").body();
        assertFalse(sales.contains(">Tiếp nhận xe</a>"));
        assertTrue(sales.contains("href=\"/ui/sales/vehicles/101\""));
        String blocked = get("/ui/inspector/listings/102").body();
        assertTrue(blocked.contains("data-passed=\"false\""));
        assertTrue(blocked.contains("PENDING_REVIEW"));
        assertTrue(get("/ui/sales/reservations/102").body().contains("disabled"));

        String settlement = HtmlUtils.htmlUnescape(get("/ui/manager/settlements/101").body());
        for (String label : Set.of("Quyết định quyết toán", "Yêu cầu bổ sung", "Phê duyệt", "Từ chối", "Lý do quyết định")) {
            assertTrue(settlement.contains(label), "Settlement must render Vietnamese correctly: " + label);
        }
        assertFalse(get("/ui/manager/consignments/102").body().contains("Sửa draft"));
    }

    @Test
    void previewDoesNotAcceptBusinessPosts() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/ui/finance/receipts/new"))
                .POST(HttpRequest.BodyPublishers.ofString("amount=1")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode()); // Existing CSRF protection remains enabled.
    }
}
