package dk.digitalidentity.indberetning.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "personal_route_address_mapping")
public class PersonalRouteAddressMapping {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="id")
	private long id;

	@Column(nullable = false)
	private boolean waypoint;

	@Column(nullable = false)
	private boolean startPoint;

	@Column(nullable = false)
	private boolean endPoint;

	@Column(nullable = false)
	private int pointNumber;

	@ManyToOne(optional = false)
	@JoinColumn(name = "personal_route_id", nullable = false)
	private PersonalRoute personalRoute;

	@OneToOne(optional = false, orphanRemoval = true)
	@JoinColumn(name = "address_id", nullable = false)
	private Address address;

	public PersonalRouteAddressMapping(int pointNumber, PersonalRoute personalRoute, Address address) {
		this.pointNumber = pointNumber;
		this.personalRoute = personalRoute;
		this.address = address;
		this.waypoint = true;
	}
}
