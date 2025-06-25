package dk.digitalidentity.indberetning.model.dao;

import dk.digitalidentity.indberetning.model.entity.ErrorComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorCommentDao extends JpaRepository<ErrorComment, Long> {
}
