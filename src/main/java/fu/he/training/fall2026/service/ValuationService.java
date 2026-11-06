package fu.he.training.fall2026.service;

public interface ValuationService {

    /** Latest proposed price of the consignment formatted as "750.000.000", or "" when it has not been valued. */
    String listingPriceText(String consignmentCode);
}
