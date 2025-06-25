package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ErrorResponseDao;
import dk.digitalidentity.indberetning.model.entity.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorResponseService {
    private final ErrorResponseDao errorResponseDao;

    public ErrorResponse save(ErrorResponse errorResponse) { return errorResponseDao.save(errorResponse); }
}
