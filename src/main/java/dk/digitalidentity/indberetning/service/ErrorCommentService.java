package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ErrorCommentDao;
import dk.digitalidentity.indberetning.model.entity.ErrorComment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorCommentService {
    private final ErrorCommentDao errorCommentDao;

    public ErrorComment save(ErrorComment errorComment) { return errorCommentDao.save(errorComment); }
}
