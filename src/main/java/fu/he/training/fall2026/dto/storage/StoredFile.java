package fu.he.training.fall2026.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Nguyễn Phúc Khang: kết quả sau khi lưu một tệp lên Cloudinary. */
@Getter
@Builder
@AllArgsConstructor
public class StoredFile {

    /** Khóa ngắn để lưu vào entity (Document.filePath, InspectionItem.photoPath...). */
    private final String path;

    /** URL https dùng ngay được, ví dụ để hiện lại ảnh vừa tải lên. */
    private final String url;

    /** Tên tệp gốc của người dùng, đã làm sạch, vừa với cột Document.fileName. */
    private final String fileName;
}
