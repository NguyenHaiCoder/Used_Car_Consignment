package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.entity.consignment.Valuation;
import fu.he.training.fall2026.repository.ValuationRepository;
import fu.he.training.fall2026.service.ValuationService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ValuationServiceImpl implements ValuationService {

    private final ValuationRepository valuationRepository;

    @Override
    @Transactional(readOnly = true)
    public String listingPriceText(String consignmentCode) {
        if (consignmentCode == null || consignmentCode.isBlank()) {
            return "";
        }
        return latestProposedPrice(consignmentCode).map(ValuationServiceImpl::formatVnd).orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> latestProposedPrice(String consignmentCode) {
        if (consignmentCode == null || consignmentCode.isBlank()) {
            return Optional.empty();
        }
        return valuationRepository.findFirstByConsignmentCodeOrderByIdDesc(consignmentCode.trim())
                .map(Valuation::getProposedPrice);
    }

    private static String formatVnd(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(amount);
    }
}
