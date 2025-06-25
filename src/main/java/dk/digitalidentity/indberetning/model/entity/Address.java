package dk.digitalidentity.indberetning.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.digitalidentity.indberetning.model.entity.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "street_name", nullable = false)
    private String streetName;

    @Column(name = "street_number", nullable = false)
    private String streetNumber;

    @Column(name = "zip_code", nullable = false)
    private int zipCode;

    @Column(name = "town", nullable = false)
    private String town;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "description")
    private String description;

    @Column(name = "is_dirty")
    private boolean isDirty;

    @Column(name = "dirty_string")
    private String dirtyString;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AddressType type;
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_route_id")
    private PersonalRoute personalRoute;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orgunit_id")
    private OrgUnit orgUnit;

    @Column(name = "standard_address")
    private boolean standardAddress;

    @Column(name = "primary_address")
    private boolean primary;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deviating_address")
    private Address deviatingAddress;

    @Column(name = "home_to_work_distance_override_deviation", nullable = true)
    private double homeToWorkDistanceOverrideDeviation;

    @Column
    @CreationTimestamp
    private LocalDateTime createdTimestamp;

    @Column
    private int coordinateFetchTries;

    public String getAddressString() {
        return getStreetName() + " " + getStreetNumber() + ", " + getZipCode() + " " + getTown();
    }

    public String getDirtyDescription() {
        if(description != null) { return description; }
        if(orgUnit != null) { return orgUnit.getLongDescription(); }
        if(person != null) { return person.getName() + "'s adresse"; }
        return "";
    }

    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getAddressString()).append(" (");

        String desc = getDescription();
        boolean descriptionPresent = StringUtils.hasLength(desc);
        if (descriptionPresent) {
            sb.append(desc);
        }

        AddressType addressType = getType();
        boolean showTypeDescription = addressType == AddressType.HOME || addressType == AddressType.WORK;

        if (descriptionPresent && showTypeDescription) {
            sb.append("; ");
        }

        if (addressType == AddressType.HOME || addressType == AddressType.WORK) {
            sb.append(addressType.getDescription());
        }
        sb.append(")");

        return sb.toString();
    }

    public boolean washed() {
        return isDirty() && dirtyString != null && !dirtyString.isEmpty() && (!AddressType.WORK.equals(type) || latitude != 0 || longitude != 0);
    }

    public String getPrettyType() {
        return getType().getDescription();
    }
}
