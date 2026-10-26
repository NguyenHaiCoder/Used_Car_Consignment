package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignor.ConsignorForm;
import fu.he.training.fall2026.entity.consignment.Consignor;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ConsignorService {

    Page<Consignor> search(String keyword, int page, int size);

    Consignor get(Long id);

    /** All consignors by name, for pick-lists. */
    List<Consignor> listAll();

    ConsignorForm formFor(Long id);

    /** @throws fu.he.training.fall2026.exception.BusinessException when the phone or CCCD belongs to another consignor */
    Long create(ConsignorForm form);

    void update(Long id, ConsignorForm form);
}
