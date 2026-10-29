package fu.he.training.fall2026.repository;

import fu.he.training.fall2026.entity.consignment.Consignment;
import fu.he.training.fall2026.entity.consignment.ConsignmentAgreement;
import fu.he.training.fall2026.entity.consignment.TransferCase;
import fu.he.training.fall2026.entity.consignment.VehicleSaleNews;
import fu.he.training.fall2026.entity.enums.AgreementStatus;
import fu.he.training.fall2026.entity.enums.ContractStatus;
import fu.he.training.fall2026.entity.enums.ListingStatus;
import fu.he.training.fall2026.entity.sales.SalesContract;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-side queries WP05 (payments, settlement, handover) needs over records owned by other modules:
 * sales contracts (WP04), consignments and agreements (WP02), listings (WP03) and transfer cases (WP06).
 */
public interface SaleLookupRepository extends Repository<SalesContract, Long> {

    @Query("""
            select sc from SalesContract sc join fetch sc.reservation r join fetch r.buyer join fetch r.vehicle
            where sc.id = :id
            """)
    Optional<SalesContract> findContract(@Param("id") Long id);

    /** Contracts in the given statuses that are not closed yet, newest first. */
    @Query("""
            select sc from SalesContract sc join fetch sc.reservation r join fetch r.buyer join fetch r.vehicle
            where sc.status in :statuses and sc.saleClosed = false order by sc.createdAt desc
            """)
    List<SalesContract> findOpenContracts(@Param("statuses") Collection<ContractStatus> statuses);

    /** Contracts in the given statuses that have no settlement yet, newest first. */
    @Query("""
            select sc from SalesContract sc join fetch sc.reservation r join fetch r.buyer join fetch r.vehicle
            where sc.status in :statuses
              and not exists (select s.id from Settlement s where s.salesContract = sc)
            order by sc.createdAt desc
            """)
    List<SalesContract> findContractsWithoutSettlement(@Param("statuses") Collection<ContractStatus> statuses);

    /** Consignments of the vehicle, newest first; the first one is the consignment being sold. */
    @Query("select c from Consignment c join fetch c.consignor where c.vehicle.id = :vehicleId order by c.createdAt desc")
    List<Consignment> findConsignmentsOfVehicle(@Param("vehicleId") Long vehicleId);

    /** Agreements of the consignment in the given status, highest version first. */
    @Query("select a from ConsignmentAgreement a where a.consignment.id = :consignmentId and a.status = :status order by a.versionNo desc")
    List<ConsignmentAgreement> findAgreements(
            @Param("consignmentId") Long consignmentId, @Param("status") AgreementStatus status);

    @Query("select t from TransferCase t where t.salesContract.id = :contractId")
    Optional<TransferCase> findTransferCase(@Param("contractId") Long contractId);

    @Query("select n from VehicleSaleNews n where n.consignment.id = :consignmentId and n.status in :statuses")
    List<VehicleSaleNews> findListings(
            @Param("consignmentId") Long consignmentId, @Param("statuses") Collection<ListingStatus> statuses);
}
