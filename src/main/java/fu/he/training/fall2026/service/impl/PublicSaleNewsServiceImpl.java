package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.listing.PublicListingCardResponse;
import fu.he.training.fall2026.dto.listing.PublicListingFilter;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.PublicSaleNewsService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicSaleNewsServiceImpl implements PublicSaleNewsService {

    private static final int PAGE_SIZE = 6;
    private static final int FEATURED_SIZE = 6;
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    /** Budget code → {min inclusive, max exclusive} in million VND; null means no bound. */
    private static final Map<String, Long[]> BUDGETS = Map.of(
            "under-500", new Long[] {null, 500L},
            "500-800", new Long[] {500L, 800L},
            "800-1000", new Long[] {800L, 1000L},
            "over-1000", new Long[] {1000L, null});

    private static final Map<String, Sort> SORTS = Map.of(
            "newest", Sort.by(Sort.Direction.DESC, "publishedAt"),
            "price-asc", Sort.by(Sort.Direction.ASC, "listPrice"),
            "price-desc", Sort.by(Sort.Direction.DESC, "listPrice"),
            "km-asc", Sort.by(Sort.Direction.ASC, "consignment.vehicle.odometerKm"));

    private final VehicleSaleNewsRepository newsRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicListingCardResponse> search(PublicListingFilter filter) {
        String keyword = filter.getQ();
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        String brand = filter.getBrand() == null || filter.getBrand().isBlank() ? null : filter.getBrand();
        Long[] budget = filter.getBudget() == null
                ? new Long[] {null, null}
                : BUDGETS.getOrDefault(filter.getBudget(), new Long[] {null, null});
        Sort sort = (filter.getSort() == null ? SORTS.get("newest") : SORTS.getOrDefault(filter.getSort(), SORTS.get("newest")))
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Page<VehicleSaleNews> result = search(pattern, brand, budget, PageRequest.of(Math.max(filter.getPage() - 1, 0), PAGE_SIZE, sort));
        if (result.getTotalPages() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = search(pattern, brand, budget, PageRequest.of(result.getTotalPages() - 1, PAGE_SIZE, sort));
        }
        return result.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listBrands() {
        return newsRepository.findBrands(ListingStatus.LISTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicListingCardResponse> featured() {
        return newsRepository.findNewest(ListingStatus.LISTED, PageRequest.of(0, FEATURED_SIZE)).stream()
                .map(this::toDto)
                .toList();
    }

    private Page<VehicleSaleNews> search(String pattern, String brand, Long[] budget, Pageable pageable) {
        return newsRepository.searchPublic(
                pattern, ListingStatus.LISTED, brand, millions(budget[0]), millions(budget[1]), pageable);
    }

    private PublicListingCardResponse toDto(VehicleSaleNews n) {
        Vehicle vehicle = n.getConsignment().getVehicle();
        List<String> photos = fileStorageService.getUrls(n.getImagePaths());
        return PublicListingCardResponse.builder()
                .id(n.getId())
                .code(n.getCode())
                .title(n.getTitle())
                .listPrice(n.getListPrice())
                .year(vehicle.getYear())
                .odometerKm(vehicle.getOdometerKm())
                .fuelType(vehicle.getFuelType())
                .coverUrl(photos.isEmpty() ? null : photos.get(0))
                .build();
    }

    private static BigDecimal millions(Long value) {
        return value == null ? null : BigDecimal.valueOf(value).multiply(MILLION);
    }
}
