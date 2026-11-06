package fu.he.training.fall2026.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nguyễn Phúc Khang: tạo sẵn bean {@link Cloudinary} để {@code FileStorageServiceImpl} dùng lại.
 *
 * <p>Luồng lấy key: biến môi trường hoặc file .env → app.cloudinary.* trong application.yml → bean này.
 * Thiếu key thì ứng dụng vẫn khởi động được, để người chưa có key vẫn chạy được các màn hình khác;
 * chỉ khi gọi tới upload thì service mới báo lỗi "chưa cấu hình".
 */
@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}") String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret) {
        // secure=true: mọi URL sinh ra đều là https
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }
}
