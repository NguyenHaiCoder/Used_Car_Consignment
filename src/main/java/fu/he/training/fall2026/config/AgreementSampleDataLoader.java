package fu.he.training.fall2026.config;

import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.repository.ConsignmentAgreementRepository;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed hợp đồng mẫu theo flow trạng thái khi DB chưa có agreement,
 * gắn với các hồ sơ ký gửi sẵn có.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class AgreementSampleDataLoader implements ApplicationRunner {

    private static final AgreementStatus[] DEMO_FLOW = {
            AgreementStatus.DRAFT,
            AgreementStatus.PENDING_SIGNATURE,
            AgreementStatus.ACTIVE,
            AgreementStatus.ACTIVE,
            AgreementStatus.TERMINATED
    };

    private final ConsignmentRepository consignmentRepository;
    private final ConsignmentAgreementRepository agreementRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (agreementRepository.count() > 0) {
            return;
        }
        List<Consignment> consignments = consignmentRepository.findAll();
        if (consignments.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int index = 0;
        for (Consignment consignment : consignments) {
            AgreementStatus status = DEMO_FLOW[index % DEMO_FLOW.length];
            ConsignmentAgreement agreement = new ConsignmentAgreement();
            agreement.setCode("HDKG-" + String.format("%06d", consignment.getId()));
            agreement.setConsignment(consignment);
            agreement.setStatus(status);
            agreement.setCommissionRate(new BigDecimal("1.5000"));
            agreement.setFixedFee(BigDecimal.ZERO);
            agreement.setPriceFloor(new BigDecimal("700000000"));
            agreement.setValidFrom(today.minusDays(10));
            // vị trí thứ 4 trong chu kỳ là ACTIVE đã quá ngày kết thúc → hiện "Hết hạn"
            agreement.setValidTo(index % DEMO_FLOW.length == 3 ? today.minusDays(1) : today.plusDays(50));
            agreement.setCustodyTerms("Bên B quản lý, trưng bày và chào bán xe trong thời hạn ký gửi.");
            agreement.setTerminationTerms("Chấm dứt trước hạn theo thỏa thuận hai bên hoặc vi phạm điều khoản.");
            agreement.setPayoutTerms("Thanh toán cho chủ xe sau khi giao dịch bán thành công, trừ hoa hồng.");
            agreementRepository.save(agreement);
            index++;
        }
    }
}