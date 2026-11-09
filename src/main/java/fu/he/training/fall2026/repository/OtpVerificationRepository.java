package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.OtpVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(String email);

    void deleteByUserId(Long userId);
}
