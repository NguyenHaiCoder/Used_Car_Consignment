package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.storage.StoredFile;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nguyễn Phúc Khang: dịch vụ dùng chung để lưu ảnh và tệp tài liệu, màn hình nào cần thì inject vào gọi.
 *
 * <p>Luồng dùng ở một màn hình:
 * <ol>
 *   <li>Controller nhận {@link MultipartFile} từ form ({@code enctype="multipart/form-data"}).</li>
 *   <li>Gọi hàm upload, nhận về {@link StoredFile}.</li>
 *   <li>Lưu {@code storedFile.getPath()} vào entity. Không lưu URL đầy đủ: cột imagePaths chỉ 1000 ký tự,
 *       10 URL đầy đủ sẽ tràn cột, còn 10 khóa ngắn thì vừa.</li>
 *   <li>Khi hiển thị, gọi {@link #getUrl(String)} với giá trị trong entity để lấy lại URL.</li>
 * </ol>
 *
 * <pre>{@code
 * // Một tệp
 * StoredFile saved = fileStorageService.uploadDocument(file, "documents");
 * document.setFilePath(saved.getPath());
 * document.setFileName(saved.getFileName());
 *
 * // Nhiều ảnh, gộp vào một cột
 * List<StoredFile> photos = fileStorageService.uploadImages(files, "vehicles");
 * news.setImagePaths(fileStorageService.joinPaths(photos));
 * List<String> urls = fileStorageService.getUrls(news.getImagePaths());
 * }</pre>
 *
 * <p>Trong template phải gọi qua th:with hoặc th:each, vì Thymeleaf chặn gọi bean ngay trong th:src và th:href:
 * <pre>{@code
 * <img th:with="url=${@fileStorageService.getUrl(item.photoPath)}" th:src="${url}">
 * <img th:each="url : ${@fileStorageService.getUrls(news.imagePaths)}" th:src="${url}">
 * }</pre>
 *
 * <p>Tệp lỗi hoặc không hợp lệ đều ném {@code BusinessException}, GlobalExceptionHandler sẽ hiển thị thông báo.
 */
public interface FileStorageService {

    /**
     * Lưu một ảnh JPG/PNG.
     *
     * @param folder thư mục con theo nghiệp vụ, ví dụ "vehicles", "inspections"
     */
    StoredFile uploadImage(MultipartFile file, String folder);

    /**
     * Lưu nhiều ảnh (tối đa 10), ô chọn tệp để trống sẽ được bỏ qua.
     * Một ảnh lỗi thì xóa luôn các ảnh đã lên trong lượt này: hoặc lưu đủ, hoặc không lưu ảnh nào.
     */
    List<StoredFile> uploadImages(List<MultipartFile> files, String folder);

    /** Lưu một tệp tài liệu: PDF, Word, Excel, hoặc ảnh chụp giấy tờ JPG/PNG. */
    StoredFile uploadDocument(MultipartFile file, String folder);

    /**
     * Đổi giá trị đang lưu trong entity thành URL để hiển thị.
     * Giá trị rỗng trả về null. Giá trị không phải khóa Cloudinary (dữ liệu nhập tay cũ) được trả lại nguyên vẹn.
     */
    String getUrl(String path);

    /** Giống {@link #getUrl(String)} nhưng cho cột gộp nhiều tệp như imagePaths, photoPaths. */
    List<String> getUrls(String paths);

    /** Gộp nhiều tệp thành một chuỗi để lưu vào cột như imagePaths, photoPaths. */
    String joinPaths(List<StoredFile> files);

    /** Xóa tệp trên Cloudinary. Giá trị không phải khóa Cloudinary thì bỏ qua. */
    void delete(String path);
}
