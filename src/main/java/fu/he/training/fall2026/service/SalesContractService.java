package fu.he.training.fall2026.service;
import fu.he.training.fall2026.dto.sales.*;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import org.springframework.data.domain.*;
public interface SalesContractService {
    SalesContractResponse create(SalesContractRequest request);
    SalesContractResponse get(Long id);
    Page<SalesContractResponse> list(ContractStatus status, String keyword, Pageable pageable);
    SalesContractResponse update(Long id, SalesContractRequest request);
}
