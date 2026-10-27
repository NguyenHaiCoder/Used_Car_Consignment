package fu.he.training.fall2026.entity.sales;

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
@Table(name = "buyers")
public class Buyer extends BaseEntity {
    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    @Nationalized
    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(length = 20)
    private String idNumber;

    @Nationalized
    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private boolean consentStored;
}
