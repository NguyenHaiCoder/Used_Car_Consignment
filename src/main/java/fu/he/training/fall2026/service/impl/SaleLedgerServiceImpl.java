package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.enums.PaymentStatus;
import fu.he.training.fall2026.entity.enums.RefundStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.PaymentRepository;
import fu.he.training.fall2026.repository.RefundRepository;
import fu.he.training.fall2026.repository.SaleLookupRepository;
import fu.he.training.fall2026.service.SaleLedgerService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleLedgerServiceImpl implements SaleLedgerService {

    /** A fully refunded payment turns REFUNDED but its money was received, so it still counts as paid. */
    private static final List<PaymentStatus> RECEIVED = List.of(PaymentStatus.PAID, PaymentStatus.REFUNDED);
    private static final List<PaymentStatus> IN_FLIGHT = List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final SaleLookupRepository saleLookupRepository;

    @Override
    @Transactional(readOnly = true)
    public ContractSummary summarize(SalesContract contract) {
        Long id = contract.getId();
        BigDecimal salePrice = nz(contract.getSalePrice());
        BigDecimal paid = paymentRepository.sumByContract(id, RECEIVED);
        BigDecimal pending = paymentRepository.sumByContract(id, IN_FLIGHT);
        BigDecimal refunded = refundRepository.sumByContract(id, List.of(RefundStatus.PROCESSED));
        BigDecimal cleared = paid.subtract(refunded);
        BigDecimal outstanding = salePrice.subtract(cleared).max(BigDecimal.ZERO);
        Vehicle vehicle = contract.getReservation().getVehicle();
        return ContractSummary.builder()
                .id(id)
                .code(contract.getCode())
                .status(contract.getStatus())
                .saleClosed(contract.isSaleClosed())
                .buyerName(contract.getReservation().getBuyer().getFullName())
                .vehicleLabel(vehicle.getDisplayLabel())
                .vin(vehicle.getVin())
                .salePrice(salePrice)
                .depositAmount(nz(contract.getDepositAmount()))
                .paidAmount(paid)
                .pendingAmount(pending)
                .refundedAmount(refunded)
                .clearedAmount(cleared)
                .outstandingAmount(outstanding)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSummary summary(Long contractId) {
        return summarize(saleLookupRepository.findContract(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng bán", contractId)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Consignment> consignmentOf(SalesContract contract) {
        return saleLookupRepository.findConsignmentsOfVehicle(contract.getReservation().getVehicle().getId())
                .stream().findFirst();
    }

    static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
