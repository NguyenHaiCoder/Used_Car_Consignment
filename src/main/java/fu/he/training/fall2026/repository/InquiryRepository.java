package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.sales.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
}
