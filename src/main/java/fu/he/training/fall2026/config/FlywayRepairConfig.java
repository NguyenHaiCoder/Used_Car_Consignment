package fu.he.training.fall2026.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * repair() xóa bản ghi migration failed (vd. V10 cũ bị Flyway cắt câu SQL),
 * rồi migrate() chạy lại V10–V13 khi mở Spring Boot.
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
