package dk.digitalidentity.indberetning.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "personal_routes")
public class PersonalRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "description")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person personId;

    @Column(name = "route_geometry")
    private String routeGeometry;

    @Column(name = "start_address")
    private String start;

    @Column(name = "end_address")
    private String end;

    @OneToMany(mappedBy = "personalRoute")
    private List<PersonalRouteAddressMapping> personalRouteAddressMappings;

    public List<String> getAddressesAsStrings() {
        List<String> addressesAsStrings = new ArrayList<>();
        for (PersonalRouteAddressMapping mapping : personalRouteAddressMappings) {
            addressesAsStrings.add(mapping.getAddress().getAddressString());
        }
        return addressesAsStrings;
    }

    public List<String> getAddressesForWaypoints() {
        List<String> addressesForWaypoints = new ArrayList<>();
        for (PersonalRouteAddressMapping mapping : personalRouteAddressMappings) {
            if (!mapping.isStartPoint() && !mapping.isEndPoint()) {
                addressesForWaypoints.add(mapping.getAddress().getAddressString());
            }
        }
        return addressesForWaypoints;
    }
}
