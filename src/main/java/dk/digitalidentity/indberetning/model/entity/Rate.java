package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rates")
public class Rate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(nullable = false, name="rate_per_km")
    private int ratePerKm;

    @Column(nullable = false, name="active_year")
    private int activeYear;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rate_type_id", nullable = false)
    @JsonIgnore
    private RateType rateType;

}
