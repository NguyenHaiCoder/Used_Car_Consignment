package fu.he.training.fall2026.service;

import fu.he.training.fall2026.dto.consignment.AgreementForm;
import fu.he.training.fall2026.dto.consignment.AgreementResponse;
import java.util.List;
import java.util.Optional;

public interface ConsignmentAgreementService {

    /** Newest version of the consignment's agreement, if one was ever drafted. */
    Optional<AgreementResponse> findLatest(Long consignmentId);

    /** All versions, newest first. */
    List<AgreementResponse> versions(Long consignmentId);

    /** Form filled with the newest version, which must be a DRAFT. */
    AgreementForm formFor(Long consignmentId);

    /**
     * Creates version 1 as DRAFT.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when the consignment already has an agreement
     */
    void createDraft(Long consignmentId, AgreementForm form);

    /**
     * Updates the newest version while it is still a DRAFT.
     *
     * @throws fu.he.training.fall2026.exception.BusinessException when there is no agreement or it is no longer a DRAFT
     */
    void updateDraft(Long consignmentId, AgreementForm form);

    /** DRAFT → PENDING_SIGNATURE; content is locked from here on. */
    void submit(Long consignmentId);

    /** Signed (ACTIVE) terms are immutable: copy them into a new DRAFT version to amend. */
    void createAmendment(Long consignmentId);
}
