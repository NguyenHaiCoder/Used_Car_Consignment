package fu.he.training.fall2026.service;
import fu.he.training.fall2026.dto.sales.*;
public interface ReservationService {
    ReservationEligibilityResponse eligibility(Long inquiryId);
    ReservationResponse create(ReservationRequest request);
    ReservationEditRequest editForm(Long id);
    ReservationResponse update(Long id, ReservationEditRequest request);
    DepositPolicyResponse depositPolicy(Long id);
    ReservationResponse cancel(Long id, ReservationRequest request);
    ReservationResponse expire(Long id, ReservationRequest request);
}
