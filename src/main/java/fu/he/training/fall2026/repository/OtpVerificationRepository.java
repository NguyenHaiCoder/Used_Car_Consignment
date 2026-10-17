package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.auth.OtpVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByEmailIgnoreCaseAndConsumedAtIsNullOrderByCreatedAtDesc(String email);
}
