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
@Table(name = "license_plates")
public class LicensePlate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(nullable = false, length = 10)
    private String registrationNumber;

    @Column
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    @JsonIgnore
    private Person person;

    @Column(nullable = false)
    private boolean prime;
}