package dk.digitalidentity.indberetning.service;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.indberetning.model.dao.ReportDao;
import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportDaoTransactionService {
	private final ReportDao reportDao;
	
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Report> saveAllWithTransaction(Collection<Report> reports) {
        return reportDao.saveAll(reports);
    }
}
