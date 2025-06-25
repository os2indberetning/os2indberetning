package dk.digitalidentity.indberetning.model.entity;

import dk.digitalidentity.indberetning.model.entity.enums.ApiType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "api_access")
@Setter
@Getter
public class ApiAccess {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private long id;
	
	@Column
	private String apiKey;
	
	@Column
	@Enumerated(EnumType.STRING)
	private ApiType type;
	
	@Column
	private boolean disabled;
	
}
