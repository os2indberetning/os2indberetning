package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.ErrorLogDao;
import dk.digitalidentity.indberetning.model.entity.ErrorLog;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {
    private final ErrorLogDao errorLogDao;

    public void delete(long id) { errorLogDao.deleteById(id); }

    public List<ErrorLog> getAll() { return errorLogDao.findAll(); }

    public List<ErrorLog> getAllAssignedTo(Person person) { return errorLogDao.findByAssignedTo(person); }

    public List<ErrorLog> getAllByReport(Report report) { return errorLogDao.findByReport(report); }

    public List<ErrorLog> getAllUnassigned() { return errorLogDao.findByAssignedTo(null); }

    public List<ErrorLog> getAllUnsolved() { return errorLogDao.findBySolutionDateNull(); }

    public ErrorLog getById(long id) { return errorLogDao.findById(id).orElse(null); }

    public ErrorLog save(ErrorLog errorLog) { return errorLogDao.save(errorLog); }

    public List<ErrorLog> saveAll(List<ErrorLog> errorLogs) { return errorLogDao.saveAll(errorLogs); }
}
