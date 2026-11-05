package fu.he.training.fall2026;

import fu.he.training.fall2026.controller.web.sales.BuyerWebController;
import fu.he.training.fall2026.dto.buyer.BuyerResponse;
import fu.he.training.fall2026.dto.buyer.DeleteBuyerRequest;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.ReservationStatus;
import fu.he.training.fall2026.entity.sales.Buyer;
import fu.he.training.fall2026.exception.BuyerException;
import fu.he.training.fall2026.repository.BuyerRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.repository.SalesContractRepository;
import fu.he.training.fall2026.repository.ViewingRepository;
import fu.he.training.fall2026.service.BuyerConsentRecorder;
import fu.he.training.fall2026.service.BuyerService;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.impl.BuyerServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuyerDeleteTests {
    private final BuyerRepository buyers = mock(BuyerRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final SalesContractRepository contracts = mock(SalesContractRepository.class);
    private final ViewingRepository viewings = mock(ViewingRepository.class);
    private final BuyerConsentRecorder consent = mock(BuyerConsentRecorder.class);
    private final BuyerServiceImpl service = new BuyerServiceImpl(buyers, reservations, contracts, viewings, consent);
    private Buyer buyer;

    @BeforeEach
    void setUp() {
        buyer = new Buyer();
        buyer.setId(7L);
        when(buyers.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(buyer));
    }

    @Test
    void scheduledTestDriveBlocksDeletion() {
        when(viewings.existsByInquiry_Buyer_IdAndViewingTypeAndStatusAndDeletedAtIsNull(
                7L, "TEST_DRIVE", "SCHEDULED")).thenReturn(true);

        BuyerException error = assertThrows(BuyerException.class, () -> service.delete(7L, "duplicate"));

        assertEquals(BuyerException.Reason.CONFLICT, error.getReason());
        assertEquals("Không thể xóa người mua này vì đang có lịch lái thử.", error.getMessage());
        assertNull(buyer.getDeletedAt());
        verify(buyers, never()).saveAndFlush(any());
    }

    @Test
    void activeReservationBlocksDeletion() {
        when(reservations.existsByBuyer_IdAndStatus(7L, ReservationStatus.ACTIVE)).thenReturn(true);

        BuyerException error = assertThrows(BuyerException.class, () -> service.delete(7L, "duplicate"));

        assertEquals("Không thể xóa người mua này vì đang có đặt xe.", error.getMessage());
        assertNull(buyer.getDeletedAt());
        verify(buyers, never()).saveAndFlush(any());
    }

    @Test
    void processingSalesContractBlocksDeletion() {
        when(contracts.existsByReservation_Buyer_IdAndStatusNotIn(
                7L, Set.of(ContractStatus.CLOSED, ContractStatus.CANCELLED))).thenReturn(true);

        BuyerException error = assertThrows(BuyerException.class, () -> service.delete(7L, "duplicate"));

        assertEquals("Không thể xóa người mua này vì đang có giao dịch mua xe.", error.getMessage());
        assertNull(buyer.getDeletedAt());
        verify(buyers, never()).saveAndFlush(any());
    }

    @Test
    void buyerWithoutBlockingRelationshipsIsSoftDeleted() {
        service.delete(7L, "  duplicate  ");

        assertEquals("duplicate", buyer.getDeleteReason());
        assertNotNull(buyer.getDeletedAt());
        verify(buyers).saveAndFlush(buyer);
        verify(viewings).existsByInquiry_Buyer_IdAndViewingTypeAndStatusAndDeletedAtIsNull(
                7L, "TEST_DRIVE", "SCHEDULED");
    }

    @Test
    void directDeleteRechecksRelationshipsAfterConfirmationPageOpened() {
        assertNull(service.deletionBlockReason(7L));
        when(viewings.existsByInquiry_Buyer_IdAndViewingTypeAndStatusAndDeletedAtIsNull(
                7L, "TEST_DRIVE", "SCHEDULED")).thenReturn(true);

        assertThrows(BuyerException.class, () -> service.delete(7L, "duplicate"));
        assertNull(buyer.getDeletedAt());
        verify(buyers, never()).saveAndFlush(any());
    }

    @Test
    void directPostConflictReturnsDeletePageWithReasonAndPreservedInput() {
        BuyerService buyerService = mock(BuyerService.class);
        InquiryService inquiryService = mock(InquiryService.class);
        BuyerWebController controller = new BuyerWebController(buyerService, inquiryService);
        String reason = "Không thể xóa người mua này vì đang có lịch lái thử.";
        when(buyerService.getDetails(7L)).thenReturn(new BuyerResponse());
        when(buyerService.deletionBlockReason(7L)).thenReturn(reason);
        doThrow(new BuyerException(BuyerException.Reason.CONFLICT, reason))
                .when(buyerService).delete(7L, "duplicate");
        DeleteBuyerRequest form = new DeleteBuyerRequest();
        form.setReason("duplicate");
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.delete(7L, form, new BeanPropertyBindingResult(form, "deleteForm"),
                model, new RedirectAttributesModelMap());

        assertEquals("sales/buyers/details", view);
        assertEquals(reason, model.get("deleteBlockedReason"));
        assertEquals(true, model.get("confirmDelete"));
        assertEquals("duplicate", form.getReason());
    }

    @Test
    void deleteConfirmationExposesBlockReasonForDisabledButton() {
        BuyerService buyerService = mock(BuyerService.class);
        InquiryService inquiryService = mock(InquiryService.class);
        BuyerWebController controller = new BuyerWebController(buyerService, inquiryService);
        String reason = "Không thể xóa người mua này vì đang có đặt xe.";
        when(buyerService.getDetails(7L)).thenReturn(new BuyerResponse());
        when(buyerService.deletionBlockReason(7L)).thenReturn(reason);
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("sales/buyers/details", controller.deleteConfirmation(7L, model));
        assertEquals(reason, model.get("deleteBlockedReason"));
        assertEquals(true, model.get("confirmDelete"));
        assertNotNull(model.get("deleteForm"));
    }
}
