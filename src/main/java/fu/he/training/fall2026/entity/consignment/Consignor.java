package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consignors")
public class Consignor extends BaseEntity {

    @Column(nullable = false, length = 160)
    @Nationalized
    private String fullName;

    @Column(nullable = false, length = 30)
    @Nationalized
    private String phone;

    @Column(length = 180)
    @Nationalized
    private String email;

    @Column(nullable = false, length = 20)
    @Nationalized
    private String idNumber;

    @Column(length = 500)
    @Nationalized
    private String address;

    @Column(nullable = false, length = 120)
    @Nationalized
    private String bank;

    @Column(nullable = false, length = 40)
    @Nationalized
    private String accountNumber;

    @Column(nullable = false, length = 160)
    @Nationalized
    private String accountName;

    @Column(nullable = false)
    private boolean consentStored;
}
