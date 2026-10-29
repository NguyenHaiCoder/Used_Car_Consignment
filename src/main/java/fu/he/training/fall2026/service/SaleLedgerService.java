package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.finance.ContractSummary;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.sales.SalesContract;
import java.util.Optional;

/** Money position of a sales contract and the consignment it sells; shared by payment, settlement and handover. */
public interface SaleLedgerService {

    /** Must be called inside a transaction (reads the contract's reservation, buyer and vehicle). */
    ContractSummary summarize(SalesContract contract);

    ContractSummary summary(Long contractId);

    /** The consignment of the vehicle sold under the contract (the newest one for that vehicle). */
    Optional<Consignment> consignmentOf(SalesContract contract);
}
