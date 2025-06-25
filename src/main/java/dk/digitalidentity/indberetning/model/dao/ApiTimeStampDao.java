
package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.ApiTimeStamp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTimeStampDao extends JpaRepository<ApiTimeStamp, Long> {

}