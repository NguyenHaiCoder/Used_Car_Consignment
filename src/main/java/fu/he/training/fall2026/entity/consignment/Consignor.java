package fu.he.training.fall2026.entity.consignment;

import fu.he.training.fall2026.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "consignors")
public class Consignor extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(nullable = false, length = 20)
    private String idNumber;

    @Column(length = 500)
    private String address;

    @Column(nullable = false, length = 120)
    private String bank;

    @Column(nullable = false, length = 40)
    private String accountNumber;

    @Column(nullable = false, length = 160)
    private String accountName;

    @Column(nullable = false)
    private boolean consentStored;
}
