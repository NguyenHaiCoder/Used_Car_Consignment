package fu.he.training.fall2026.service.impl;

import fu.he.training.fall2026.dto.listing.ListingVehicleResponse;
import fu.he.training.fall2026.dto.listing.SaleNewsFilter;
import fu.he.training.fall2026.dto.listing.SaleNewsRequest;
import fu.he.training.fall2026.dto.listing.SaleNewsResponse;
import fu.he.training.fall2026.dto.listing.StaffOptionResponse;
import fu.he.training.fall2026.dto.storage.StoredFile;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.Inspection;
import fu.he.training.fall2026.entity.consignment.Valuation;
import fu.he.training.fall2026.entity.consignment.Vehicle;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.ConsignmentStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.exception.BusinessException;
import fu.he.training.fall2026.exception.ResourceNotFoundException;
import fu.he.training.fall2026.repository.ConsignmentRepository;
import fu.he.training.fall2026.repository.InspectionRepository;
import fu.he.training.fall2026.repository.UserRepository;
import fu.he.training.fall2026.repository.ValuationRepository;
import fu.he.training.fall2026.repository.VehicleSaleNewsRepository;
import fu.he.training.fall2026.service.FileStorageService;
import fu.he.training.fall2026.service.SaleNewsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SaleNewsServiceImpl implements SaleNewsService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final String PHOTO_FOLDER = "listings";
    private static final List<ListingStatus> EDITABLE_STATUSES = List.of(ListingStatus.LISTED, ListingStatus.PAUSED);

    private final VehicleSaleNewsRepository newsRepository;
    private final ConsignmentRepository consignmentRepository;
    private final InspectionRepository inspectionRepository;
    private final ValuationRepository valuationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public Page<SaleNewsResponse> search(SaleNewsFilter filter, int page, int size) {
        String keyword = filter.getQ();
        String pattern = keyword == null || keyword.isBlank()
                ? "%"
                : "%" + keyword.trim().toLowerCase() + "%";
        return newsRepository.search(
                        pattern,
                        filter.getStatus(),
                        filter.getStaffId(),
                        startOf(filter.getFrom()),
                        startOf(filter.getTo() == null ? null : filter.getTo().plusDays(1)),
                        millions(filter.getPriceMin()),
                        millions(filter.getPriceMax()),
                        PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(SaleNewsServiceImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ListingStatus, Long> countByStatus() {
        Map<ListingStatus, Long> counts = new EnumMap<>(ListingStatus.class);
        for (ListingStatus status : ListingStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : newsRepository.countByStatus()) {
            counts.put((ListingStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffOptionResponse> listStaffs() {
        return newsRepository.findStaffs().stream()
                .map(u -> StaffOptionResponse.builder().id(u.getId()).fullName(u.getFullName()).build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleNewsResponse get(Long id) {
        return toDto(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> imageUrls(Long id) {
        return fileStorageService.getUrls(load(id).getImagePaths());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingVehicleResponse> listReadyVehicles() {
        return consignmentRepository.findAllByStatusWithVehicle(ConsignmentStatus.INSPECTED).stream()
                .map(this::toVehicleDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingVehicleResponse> findReadyVehicle(Long consignmentId) {
        if (consignmentId == null) {
            return Optional.empty();
        }
        return consignmentRepository.findDetailById(consignmentId)
                .filter(c -> c.getStatus() == ConsignmentStatus.INSPECTED)
                .map(this::toVehicleDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingVehicleResponse getVehicle(Long consignmentId) {
        return toVehicleDto(loadConsignment(consignmentId));
    }

    @Override
    public SaleNewsRequest newForm(ListingVehicleResponse vehicle) {
        SaleNewsRequest form = new SaleNewsRequest();
        form.setConsignmentId(vehicle.getConsignmentId());
        form.setTitle(vehicle.getYear() == null ? vehicle.getCarLabel() : vehicle.getCarLabel() + " " + vehicle.getYear());
        form.setListPrice(vehicle.getProposedPrice());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public SaleNewsRequest formFor(Long id) {
        VehicleSaleNews news = load(id);
        SaleNewsRequest form = new SaleNewsRequest();
        form.setId(news.getId());
        form.setConsignmentId(news.getConsignment().getId());
        form.setTitle(news.getTitle());
        form.setBody(news.getBody());
        form.setListPrice(news.getListPrice());
        form.setStatus(news.getStatus());
        return form;
    }

    @Override
    @Transactional
    public Long create(SaleNewsRequest form, Long actorUserId) {
        Consignment consignment = loadConsignment(form.getConsignmentId());
        if (consignment.getStatus() != ConsignmentStatus.INSPECTED) {
            throw new BusinessException("Chỉ đăng tin cho hồ sơ đã kiểm định.");
        }
        requireEditableStatus(form.getStatus());
        requireAskingPrice(consignment.getExpectedPrice(), form.getListPrice());
        List<MultipartFile> images = selected(form.getImages());
        if (images.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 ảnh xe.");
        }
        User owner = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", actorUserId));

        List<StoredFile> photos = fileStorageService.uploadImages(images, PHOTO_FOLDER);
        try {
            VehicleSaleNews news = new VehicleSaleNews();
            news.setCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
            news.setConsignment(consignment);
            news.setAssignedTo(owner);
            news.setImagePaths(fileStorageService.joinPaths(photos));
            apply(news, form);
            newsRepository.save(news);
            news.setCode(String.format("TD-%06d", news.getId()));
            consignment.setStatus(ConsignmentStatus.LISTED);
            newsRepository.flush();
            return news.getId();
        } catch (RuntimeException ex) {
            photos.forEach(photo -> deleteQuietly(photo.getPath()));
            throw ex;
        }
    }

    @Override
    @Transactional
    public void update(Long id, SaleNewsRequest form) {
        VehicleSaleNews news = load(id);
        if (news.getStatus() == ListingStatus.SOLD) {
            throw new BusinessException("Tin " + news.getCode() + " đã bán, không sửa được.");
        }
        if (news.getStatus() == ListingStatus.RESERVED) {
            throw new BusinessException("Tin " + news.getCode() + " đang đặt cọc, không sửa được.");
        }
        requireEditableStatus(form.getStatus());
        requireAskingPrice(news.getConsignment().getExpectedPrice(), form.getListPrice());
        apply(news, form);

        List<MultipartFile> images = selected(form.getImages());
        if (images.isEmpty()) {
            if (news.getImagePaths() == null || news.getImagePaths().isBlank()) {
                throw new BusinessException("Vui lòng chọn ít nhất 1 ảnh xe.");
            }
            return;
        }
        String oldPaths = news.getImagePaths();
        List<StoredFile> photos = fileStorageService.uploadImages(images, PHOTO_FOLDER);
        try {
            news.setImagePaths(fileStorageService.joinPaths(photos));
            newsRepository.flush();
        } catch (RuntimeException ex) {
            photos.forEach(photo -> deleteQuietly(photo.getPath()));
            throw ex;
        }
        if (oldPaths != null) {
            for (String path : oldPaths.split(",")) {
                deleteQuietly(path.trim());
            }
        }
    }

    @Override
    @Transactional
    public SaleNewsResponse pause(Long id) {
        VehicleSaleNews news = load(id);
        if (news.getStatus() != ListingStatus.LISTED) {
            throw new BusinessException("Chỉ tạm ẩn được tin đang hiển thị.");
        }
        news.setStatus(ListingStatus.PAUSED);
        return toDto(news);
    }

    @Override
    @Transactional
    public SaleNewsResponse resume(Long id) {
        VehicleSaleNews news = load(id);
        if (news.getStatus() != ListingStatus.PAUSED) {
            throw new BusinessException("Chỉ hiển thị lại được tin đang tạm ẩn.");
        }
        news.setStatus(ListingStatus.LISTED);
        if (news.getPublishedAt() == null) {
            news.setPublishedAt(Instant.now());
        }
        return toDto(news);
    }

    private VehicleSaleNews load(Long id) {
        return newsRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tin đăng", id));
    }

    private Consignment loadConsignment(Long id) {
        return consignmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ ký gửi", id));
    }

    private ListingVehicleResponse toVehicleDto(Consignment c) {
        Vehicle vehicle = c.getVehicle();
        return ListingVehicleResponse.builder()
                .consignmentId(c.getId())
                .code(c.getCode())
                .carLabel(vehicle.getDisplayLabel())
                .year(vehicle.getYear())
                .ownerName(c.getConsignor().getFullName())
                .inspectedOn(inspectionRepository.findFirstByConsignmentIdOrderByIdDesc(c.getId())
                        .map(Inspection::getInspectedOn)
                        .orElse(null))
                .plate(vehicle.getLicensePlate())
                .odometerKm(vehicle.getOdometerKm())
                .color(vehicle.getColor())
                .fuelType(vehicle.getFuelType())
                .vin(vehicle.getVin())
                .proposedPrice(valuationRepository.findFirstByConsignmentIdOrderByIdDesc(c.getId())
                        .map(Valuation::getProposedPrice)
                        .orElse(null))
                .build();
    }

    private void deleteQuietly(String path) {
        try {
            fileStorageService.delete(path);
        } catch (BusinessException ignored) {
            // Leaves an orphan photo on storage; the listing data is already consistent.
        }
    }

    private static void apply(VehicleSaleNews target, SaleNewsRequest form) {
        target.setTitle(form.getTitle().trim());
        target.setBody(form.getBody().trim());
        target.setListPrice(form.getListPrice());
        target.setStatus(form.getStatus());
        if (form.getStatus() == ListingStatus.LISTED && target.getPublishedAt() == null) {
            target.setPublishedAt(Instant.now());
        }
    }

    private static void requireAskingPrice(java.math.BigDecimal expectedPrice, java.math.BigDecimal askingPrice) {
        if (expectedPrice != null && askingPrice != null && askingPrice.compareTo(expectedPrice) < 0) {
            throw new BusinessException("Giá chào bán không được thấp hơn giá mong muốn của chủ xe ("
                    + java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("vi-VN")).format(expectedPrice)
                    + " ₫).");
        }
    }

    private static void requireEditableStatus(ListingStatus status) {
        if (!EDITABLE_STATUSES.contains(status)) {
            throw new BusinessException("Trạng thái tin chỉ được là Đang hiển thị hoặc Tạm ẩn.");
        }
    }

    private static List<MultipartFile> selected(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private static Instant startOf(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZONE).toInstant();
    }

    private static BigDecimal millions(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value).multiply(MILLION);
    }

    private static SaleNewsResponse toDto(VehicleSaleNews n) {
        Consignment consignment = n.getConsignment();
        Vehicle vehicle = consignment.getVehicle();
        String car = vehicle.getYear() == null
                ? vehicle.getDisplayLabel()
                : vehicle.getDisplayLabel() + " " + vehicle.getYear();
        return SaleNewsResponse.builder()
                .id(n.getId())
                .code(n.getCode())
                .consignmentCode(consignment.getCode())
                .carLabel(car)
                .plate(vehicle.getLicensePlate())
                .odometerKm(vehicle.getOdometerKm())
                .listPrice(n.getListPrice())
                .createdAt(n.getCreatedAt() == null ? null : LocalDateTime.ofInstant(n.getCreatedAt(), ZONE))
                .staffName(n.getAssignedTo() == null ? null : n.getAssignedTo().getFullName())
                .status(n.getStatus())
                .build();
    }
}
