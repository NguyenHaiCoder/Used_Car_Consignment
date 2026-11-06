package fu.he.training.fall2026.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import fu.he.training.fall2026.dto.storage.StoredFile;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.service.FileStorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nguyễn Phúc Khang: lưu tệp lên Cloudinary.
 *
 * <p>Giá trị lưu trong entity có dạng {@code loại/publicId}, ví dụ
 * {@code image/consigncar/vehicles/3f2a...} hoặc {@code raw/consigncar/documents/9c1b....pdf}.
 * Phần "loại" là resource_type của Cloudinary: ảnh là "image", tài liệu là "raw".
 * Cloudinary cần biết cả hai thông tin này để dựng URL và để xóa tệp, nên ghép chung vào một chuỗi.
 *
 * <p>Đặt tên bean "fileStorageService" để template gọi được bằng {@code @fileStorageService}.
 */
@Slf4j
@Service("fileStorageService")
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final String RESOURCE_IMAGE = "image";
    private static final String RESOURCE_RAW = "raw";
    private static final String ROOT_FOLDER = "consigncar";
    private static final String PATH_SEPARATOR = ",";
    private static final int MAX_IMAGES_PER_UPLOAD = 10;
    // Bằng độ dài cột Document.fileName
    private static final int MAX_FILE_NAME_LENGTH = 160;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> DOCUMENT_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    private final Cloudinary cloudinary;

    @Override
    public StoredFile uploadImage(MultipartFile file, String folder) {
        // Bước 1: chặn tệp sai định dạng trước khi gửi đi
        requireExtension(file, IMAGE_EXTENSIONS);
        // Bước 2: đặt tên ngẫu nhiên, không có đuôi vì Cloudinary tự quản lý định dạng ảnh
        return upload(file, folder, RESOURCE_IMAGE, randomName());
    }

    @Override
    public List<StoredFile> uploadImages(List<MultipartFile> files, String folder) {
        // Bước 1: bỏ ô trống, vì form không chọn tệp vẫn gửi lên một phần tử rỗng
        List<MultipartFile> selected = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (selected.size() > MAX_IMAGES_PER_UPLOAD) {
            throw new BusinessException("Chỉ được tải tối đa " + MAX_IMAGES_PER_UPLOAD + " ảnh.");
        }
        // Bước 2: kiểm tra đủ cả lượt trước, tránh tải được vài ảnh rồi mới phát hiện ảnh sau sai định dạng
        selected.forEach(file -> requireExtension(file, IMAGE_EXTENSIONS));

        // Bước 3: tải lần lượt, lỗi giữa chừng thì dọn các ảnh đã lên để không sót tệp thừa trên Cloudinary
        List<StoredFile> stored = new ArrayList<>();
        try {
            for (MultipartFile file : selected) {
                stored.add(uploadImage(file, folder));
            }
        } catch (BusinessException ex) {
            stored.forEach(saved -> deleteQuietly(saved.getPath()));
            throw ex;
        }
        return stored;
    }

    @Override
    public StoredFile uploadDocument(MultipartFile file, String folder) {
        String extension = requireExtension(file, DOCUMENT_EXTENSIONS);
        // Với loại raw, Cloudinary yêu cầu publicId phải có đuôi tệp thì mới tải về đúng định dạng
        return upload(file, folder, RESOURCE_RAW, randomName() + "." + extension);
    }

    @Override
    public String getUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        // Một số cột đang cho nhập tay (ví dụ signatureEvidencePath), giữ nguyên để màn hình cũ không lỗi
        if (!isStoragePath(path)) {
            return path;
        }
        requireConfigured();
        return cloudinary.url()
                .secure(true)
                .resourceType(resourceTypeOf(path))
                .generate(publicIdOf(path));
    }

    @Override
    public List<String> getUrls(String paths) {
        if (!StringUtils.hasText(paths)) {
            return List.of();
        }
        return Arrays.stream(paths.split(PATH_SEPARATOR)).map(this::getUrl).toList();
    }

    @Override
    public String joinPaths(List<StoredFile> files) {
        return files.stream().map(StoredFile::getPath).collect(Collectors.joining(PATH_SEPARATOR));
    }

    @Override
    public void delete(String path) {
        if (!StringUtils.hasText(path) || !isStoragePath(path)) {
            return;
        }
        requireConfigured();
        try {
            // invalidate=true: xóa cả bản đang cache trên CDN để link cũ không còn mở được.
            // Tệp đã bị xóa từ trước thì Cloudinary trả "not found", không tính là lỗi.
            cloudinary.uploader().destroy(publicIdOf(path), ObjectUtils.asMap(
                    "resource_type", resourceTypeOf(path),
                    "invalidate", true));
        } catch (IOException | RuntimeException ex) {
            log.error("Không xóa được tệp trên Cloudinary: {}", path, ex);
            throw new BusinessException("Không xóa được tệp, vui lòng thử lại.", HttpStatus.BAD_GATEWAY);
        }
    }

    /** Hàm chung của mọi kiểu upload: gửi tệp lên Cloudinary rồi đổi kết quả thành {@link StoredFile}. */
    private StoredFile upload(MultipartFile file, String folder, String resourceType, String name) {
        requireConfigured();
        String assetFolder = ROOT_FOLDER + "/" + folder;
        try {
            // public_id quyết định URL của tệp, asset_folder quyết định thư mục hiện trong Cloudinary Console.
            // overwrite=false để tệp đã lưu không bao giờ bị ghi đè.
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", assetFolder + "/" + name,
                    "asset_folder", assetFolder,
                    "resource_type", resourceType,
                    "overwrite", false));

            // Lấy thông tin từ phản hồi của Cloudinary, không tự suy ra từ tham số đã gửi
            return StoredFile.builder()
                    .path(result.get("resource_type") + "/" + result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .fileName(cleanFileName(file.getOriginalFilename()))
                    .build();
        } catch (IOException | RuntimeException ex) {
            // SDK báo lỗi từ Cloudinary (sai key, tệp hỏng...) bằng RuntimeException, lỗi mạng bằng IOException
            log.error("Không tải được tệp {} lên Cloudinary", file.getOriginalFilename(), ex);
            throw new BusinessException("Không tải được tệp lên, vui lòng thử lại.", HttpStatus.BAD_GATEWAY);
        }
    }

    /** Dọn tệp khi rollback: lỗi ở đây chỉ ghi log, để lỗi upload ban đầu vẫn được báo cho người dùng. */
    private void deleteQuietly(String path) {
        try {
            delete(path);
        } catch (BusinessException ex) {
            log.warn("Còn sót tệp trên Cloudinary sau khi rollback: {}", path);
        }
    }

    /** Thiếu key thì báo rõ ngay, thay vì để SDK ném lỗi khó hiểu. */
    private void requireConfigured() {
        if (!StringUtils.hasText(cloudinary.config.cloudName)
                || !StringUtils.hasText(cloudinary.config.apiKey)
                || !StringUtils.hasText(cloudinary.config.apiSecret)) {
            throw new BusinessException(
                    "Hệ thống chưa cấu hình Cloudinary nên chưa xử lý được tệp.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Kiểm tra có tệp và đuôi tệp hợp lệ, trả về đuôi tệp viết thường. */
    private static String requireExtension(MultipartFile file, Set<String> allowed) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn tệp cần tải lên.");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !allowed.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("Định dạng tệp không được hỗ trợ: " + file.getOriginalFilename());
        }
        return extension.toLowerCase(Locale.ROOT);
    }

    /** Tên ngẫu nhiên giúp URL khó đoán và không lộ tên gốc của tệp (có thể chứa tên, CCCD chủ xe). */
    private static String randomName() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** Bỏ phần đường dẫn trong tên tệp. Tên quá dài thì giữ phần cuối để còn đuôi tệp. */
    private static String cleanFileName(String originalName) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(originalName));
        return name.length() <= MAX_FILE_NAME_LENGTH
                ? name
                : name.substring(name.length() - MAX_FILE_NAME_LENGTH);
    }

    private static boolean isStoragePath(String path) {
        return path.startsWith(RESOURCE_IMAGE + "/") || path.startsWith(RESOURCE_RAW + "/");
    }

    private static String resourceTypeOf(String path) {
        return path.substring(0, path.indexOf('/'));
    }

    private static String publicIdOf(String path) {
        return path.substring(path.indexOf('/') + 1);
    }
}
