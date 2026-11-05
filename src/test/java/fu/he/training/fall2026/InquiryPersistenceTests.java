package fu.he.training.fall2026;

import fu.he.training.fall2026.dto.inquiry.InquiryAssignmentRequest;
import fu.he.training.fall2026.dto.inquiry.InquirySaveRequest;
import fu.he.training.fall2026.entity.auth.Role;
import fu.he.training.fall2026.entity.auth.User;
import fu.he.training.fall2026.entity.consignment.*;
import fu.he.training.fall2026.entity.enums.*;
import fu.he.training.fall2026.entity.sales.*;
import fu.he.training.fall2026.exception.SalesSchedulingException;
import fu.he.training.fall2026.repository.InquiryRepository;
import fu.he.training.fall2026.repository.ReservationRepository;
import fu.he.training.fall2026.service.InquiryService;
import fu.he.training.fall2026.service.SalesTimeService;
import fu.he.training.fall2026.service.impl.SalesAssignmentPolicy;
import fu.he.training.fall2026.service.impl.SalesSchedulingServiceImpl;
import fu.he.training.fall2026.service.impl.InquiryServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in SQL Server checks: validate the schema and roll back every fixture and mutation. */
@EnabledIfEnvironmentVariable(named = "INQUIRY_DATABASE_TESTS", matches = "true")
@SpringBootTest(classes = InquiryPersistenceTests.JpaConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=never",
                "logging.level.org.hibernate.SQL=WARN", "logging.level.org.springframework=WARN"})
@ActiveProfiles({"dev", "inquiry-persistence-tests"})
@Transactional
class InquiryPersistenceTests {
    @Configuration
    @Profile("inquiry-persistence-tests")
    @EnableAutoConfiguration
    @EntityScan("fu.he.training.fall2026.entity")
    @EnableJpaRepositories("fu.he.training.fall2026.repository")
    @Import({InquiryServiceImpl.class, SalesSchedulingServiceImpl.class, SalesAssignmentPolicy.class, SalesTimeService.class})
    static class JpaConfig { }

    @Autowired EntityManager em;
    @Autowired InquiryService service;
    @Autowired InquiryRepository inquiries;
    @Autowired ReservationRepository reservations;
    @Autowired SalesTimeService time;
    private Buyer buyer;
    private Vehicle vehicle;
    private Consignment consignment;
    private User sales;

    @BeforeEach
    void fixtures() {
        String key = UUID.randomUUID().toString().replace("-", "");
        Role role = em.createQuery("select r from Role r where r.code = :code", Role.class)
                .setParameter("code", RoleCode.SALES).getResultStream().findFirst().orElseGet(() -> {
                    Role created = new Role(); created.setCode(RoleCode.SALES); created.setName("Inquiry test role");
                    em.persist(created); return created;
                });
        sales = new User(); sales.setFullName("Inquiry test sales"); sales.setEmail(key + "@example.test");
        sales.setRole(role); sales.setPasswordHash("test-only"); em.persist(sales);
        buyer = new Buyer(); buyer.setFullName("Inquiry test buyer"); buyer.setPhone("0901234567");
        buyer.setConsentStored(true); em.persist(buyer);
        Consignor owner = new Consignor(); owner.setFullName("Inquiry test consignor"); owner.setPhone("0902345678");
        owner.setIdNumber(key.substring(0, 20)); owner.setBank("Test bank"); owner.setAccountName("Test owner");
        owner.setAccountNumber("123456"); owner.setConsentStored(true); em.persist(owner);
        vehicle = new Vehicle(); vehicle.setVin(key); vehicle.setMake("Toyota"); vehicle.setModel("Vios"); em.persist(vehicle);
        consignment = new Consignment(); consignment.setCode("IT-" + key); consignment.setConsignor(owner);
        consignment.setVehicle(vehicle); consignment.setStatus(ConsignmentStatus.LISTED); em.persist(consignment);
        em.flush(); em.clear();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                sales.getEmail(), "", List.of(new SimpleGrantedAuthority("ROLE_SALES"))));
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    private InquirySaveRequest form() {
        InquirySaveRequest form = new InquirySaveRequest(); form.setBuyerId(buyer.getId().toString()); form.setVehicleId(vehicle.getId().toString());
        form.setSalespersonId(sales.getId()); form.setInquiryDate(time.today()); form.setNotes("Persisted inquiry notes");
        form.setChannel("PHONE"); form.setNeed("VIEWING");
        return form;
    }

    private Reservation hold(ReservationStatus status, boolean expired, boolean missingExpiry, Buyer holder) {
        Reservation hold = new Reservation(); hold.setCode("IT-" + UUID.randomUUID());
        hold.setBuyer(em.getReference(Buyer.class, holder.getId()));
        hold.setVehicle(em.getReference(Vehicle.class, vehicle.getId())); hold.setStatus(status);
        hold.setHoldUntil(missingExpiry ? null : time.today().plusDays(expired ? -1 : 0));
        em.persist(hold); em.flush(); em.clear(); return hold;
    }

    @Test
    void inquiryPersistsRelationshipsAndRemainsInHistoryAfterVehicleIsSold() {
        long id = service.create(form()); em.clear();
        Inquiry persisted = inquiries.findById(id).orElseThrow();
        assertEquals(buyer.getId(), persisted.getBuyer().getId());
        assertEquals(vehicle.getId(), persisted.getVehicle().getId());
        assertEquals(sales.getId(), persisted.getAssignedTo().getId());
        assertEquals("Persisted inquiry notes", persisted.getNotes());
        var filtered = service.list(persisted.getCode(), InquiryStatus.OPEN, sales.getId(), 1, 1);
        assertEquals(1, filtered.getTotalElements());
        assertEquals(id, filtered.getContent().getFirst().getId());
        assertTrue(service.list(persisted.getCode(), InquiryStatus.CLOSED, sales.getId(), 1, 1).isEmpty());
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.SOLD);
        em.find(Buyer.class, buyer.getId()).setDeletedAt(Instant.now()); em.flush(); em.clear();
        assertEquals(id, service.getDetails(id).getId());
        assertEquals(id, service.getBuyerHistory(buyer.getId(), 1, 10).getData().getFirst().getInquiryId());
        InquiryAssignmentRequest assignment = new InquiryAssignmentRequest(); assignment.setSalespersonId(sales.getId());
        service.assign(id, assignment); em.clear();
        assertEquals(sales.getId(), inquiries.findById(id).orElseThrow().getAssignedTo().getId());
    }

    @Test
    void reservedForBuyerThroughTodayCanBeSaved() {
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.RESERVED);
        hold(ReservationStatus.ACTIVE, false, false, buyer);
        assertNotNull(inquiries.findById(service.create(form())).orElseThrow());
    }

    @ParameterizedTest
    @EnumSource(value = ReservationStatus.class, names = {"CANCELLED", "EXPIRED", "CONVERTED"})
    void terminalReservationsCannotEstablishAReservedVehicleHold(ReservationStatus status) {
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.RESERVED);
        hold(status, false, false, buyer);
        assertTrue(reservations.findCurrentHolds(vehicle.getId(), time.today()).isEmpty());
        assertEquals("vehicleId", assertThrows(SalesSchedulingException.class, () -> service.create(form())).getField());
    }

    @Test
    void overdueActiveHoldCannotEstablishEligibility() {
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.RESERVED);
        hold(ReservationStatus.ACTIVE, true, false, buyer);
        assertTrue(reservations.findCurrentHolds(vehicle.getId(), time.today()).isEmpty());
        assertThrows(SalesSchedulingException.class, () -> service.create(form()));
    }

    @Test
    void unknownExpiryIsReturnedAsABlockerAndRejected() {
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.RESERVED);
        hold(ReservationStatus.ACTIVE, false, true, buyer);
        assertEquals(1, reservations.findCurrentHolds(vehicle.getId(), time.today()).size());
        assertThrows(SalesSchedulingException.class, () -> service.create(form()));
    }

    @Test
    void databaseChangeAfterLoadingOptionsIsRechecked() {
        assertTrue(service.vehicles(buyer.getId()).stream().anyMatch(o -> vehicle.getId().equals(o.getId())));
        Buyer other = new Buyer(); other.setFullName("Other test buyer"); other.setPhone("0903456789"); em.persist(other);
        em.find(Consignment.class, consignment.getId()).setStatus(ConsignmentStatus.RESERVED);
        hold(ReservationStatus.ACTIVE, false, false, other);
        assertThrows(SalesSchedulingException.class, () -> service.create(form()));
    }
}
