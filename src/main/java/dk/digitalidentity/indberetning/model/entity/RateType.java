package dk.digitalidentity.indberetning.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rate_types")
public class RateType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "rateType", orphanRemoval = true)
    private Set<Rate> rates = new LinkedHashSet<>();

    @Column(name = "pay_type")
    private Integer payType;

    @Column(name = "sequential_number")
    private Integer sequentialNumber;

    @Column(name = "prime")
    private boolean prime;

}
