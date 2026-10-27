package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignor.ConsignorRequest;
import fu.he.training.fall2026.dto.consignor.ConsignorResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ConsignorService {

    Page<ConsignorResponse> search(String keyword, int page, int size);

    ConsignorResponse get(Long id);

    /** All consignors by name, for pick-lists. */
    List<ConsignorResponse> listAll();

    ConsignorRequest formFor(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException when the phone or CCCD belongs to another consignor */
    Long create(ConsignorRequest form);

    void update(Long id, ConsignorRequest form);
}
