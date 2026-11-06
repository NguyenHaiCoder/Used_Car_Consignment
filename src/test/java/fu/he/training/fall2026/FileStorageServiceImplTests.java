package fu.he.training.fall2026;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sun.net.httpserver.HttpServer;
import fu.he.training.fall2026.dto.storage.StoredFile;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.impl.FileStorageServiceImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nguyễn Phúc Khang: chạy SDK Cloudinary thật nhưng trỏ upload_prefix vào một server giả trên máy,
 * nên kiểm tra được cả tầng HTTP mà không cần key thật hay mạng internet.
 */
class FileStorageServiceImplTests {

    private static final Pattern PUBLIC_ID = Pattern.compile("name=\"public_id\"\\R(?:.*\\R)*?\\R?([^\\r\\n]+)\\R--");

    private final List<String> uploads = new ArrayList<>();
    private final List<String> destroys = new ArrayList<>();
    private HttpServer server;
    private FileStorageService service;
    private int failUploadAt = -1;

    @BeforeEach
    void startFakeCloudinary() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            String uri = exchange.getRequestURI().getPath();
            String resourceType = uri.contains("/raw/") ? "raw" : "image";
            String response;
            int status = 200;
            if (uri.endsWith("/destroy")) {
                destroys.add(resourceType + "/" + publicId(body));
                response = "{\"result\":\"ok\"}";
            } else if (uploads.size() == failUploadAt) {
                uploads.add(body);
                status = 400;
                response = "{\"error\":{\"message\":\"Invalid image file\"}}";
            } else {
                uploads.add(body);
                String id = publicId(body);
                response = "{\"public_id\":\"" + id + "\",\"resource_type\":\"" + resourceType
                        + "\",\"secure_url\":\"https://res.cloudinary.com/demo/" + resourceType + "/upload/v1/" + id + "\"}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        service = new FileStorageServiceImpl(new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "demo",
                "api_key", "key",
                "api_secret", "secret",
                "secure", true,
                "upload_prefix", "http://localhost:" + server.getAddress().getPort())));
    }

    @AfterEach
    void stopFakeCloudinary() {
        server.stop(0);
    }

    @Test
    void uploadImageSendsToChosenFolderAndReturnsShortPathWithUrl() {
        StoredFile saved = service.uploadImage(file("../truoc xe.JPG"), "vehicles");

        assertThat(saved.getPath()).matches("image/consigncar/vehicles/[0-9a-f]{32}");
        assertThat(saved.getUrl()).startsWith("https://res.cloudinary.com/demo/image/upload/");
        assertThat(saved.getFileName()).isEqualTo("truoc xe.JPG");
        assertThat(uploads.get(0)).contains("name=\"asset_folder\"").contains("consigncar/vehicles");
        assertThat(uploads.get(0)).contains("name=\"signature\"");
    }

    @Test
    void uploadDocumentKeepsExtensionAndGetUrlBuildsRawLink() {
        StoredFile saved = service.uploadDocument(file("Giay to xe.PDF"), "documents");

        assertThat(saved.getPath()).matches("raw/consigncar/documents/[0-9a-f]{32}\\.pdf");
        assertThat(service.getUrl(saved.getPath()))
                .startsWith("https://res.cloudinary.com/demo/raw/upload/")
                .contains(saved.getPath().substring("raw/".length()));
    }

    @Test
    void tenImagesFitTheImagePathsColumnAndResolveBackToUrls() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            files.add(file("anh" + i + ".png"));
        }

        String joined = service.joinPaths(service.uploadImages(files, "vehicles"));

        assertThat(joined.length()).isLessThanOrEqualTo(1000);
        assertThat(service.getUrls(joined)).hasSize(10)
                .allMatch(url -> url.startsWith("https://res.cloudinary.com/demo/image/upload/"));
    }

    @Test
    void uploadImagesSkipsEmptySlotsAndRejectsMoreThanTen() {
        MultipartFile emptySlot = new MockMultipartFile("files", "", "image/png", new byte[0]);
        assertThat(service.uploadImages(List.of(emptySlot), "vehicles")).isEmpty();

        List<MultipartFile> eleven = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            eleven.add(file("anh" + i + ".png"));
        }
        assertThatThrownBy(() -> service.uploadImages(eleven, "vehicles"))
                .isInstanceOf(BusinessException.class);
        assertThat(uploads).isEmpty();
    }

    @Test
    void failureMidBatchDeletesImagesAlreadyUploaded() {
        failUploadAt = 2;

        assertThatThrownBy(() -> service.uploadImages(
                List.of(file("a.png"), file("b.png"), file("c.png")), "vehicles"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));

        assertThat(destroys).hasSize(2).allMatch(path -> path.startsWith("image/consigncar/vehicles/"));
    }

    @Test
    void rejectsDisallowedTypesBeforeAnythingIsSent() {
        assertThatThrownBy(() -> service.uploadImage(file("hop-dong.pdf"), "vehicles"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.uploadDocument(file("trang.html"), "documents"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.uploadImages(List.of(file("a.png"), file("b.exe")), "vehicles"))
                .isInstanceOf(BusinessException.class);

        assertThat(uploads).isEmpty();
    }

    @Test
    void getUrlAndDeleteLeaveNonStorageValuesAlone() {
        assertThat(service.getUrl(null)).isNull();
        assertThat(service.getUrl("bien-ban-da-ky.pdf")).isEqualTo("bien-ban-da-ky.pdf");
        assertThat(service.getUrls(null)).isEmpty();

        service.delete("bien-ban-da-ky.pdf");
        assertThat(destroys).isEmpty();

        service.delete("raw/consigncar/documents/abc.pdf");
        assertThat(destroys).containsExactly("raw/consigncar/documents/abc.pdf");
    }

    @Test
    void missingKeysGiveClearErrorAtUseNotAtStartup() {
        FileStorageService unconfigured = new FileStorageServiceImpl(new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "", "api_key", "", "api_secret", "", "secure", true)));

        assertThatThrownBy(() -> unconfigured.uploadImage(file("a.png"), "vehicles"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private static MultipartFile file(String name) {
        return new MockMultipartFile("file", name, "application/octet-stream", new byte[] {1, 2, 3});
    }

    private static String publicId(String body) {
        Matcher matcher = PUBLIC_ID.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }
}
