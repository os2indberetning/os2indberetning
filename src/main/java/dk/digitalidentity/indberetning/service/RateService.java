package dk.digitalidentity.indberetning.service;

import dk.digitalidentity.indberetning.model.dao.RateDao;
import dk.digitalidentity.indberetning.model.entity.Employment;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Rate;
import dk.digitalidentity.indberetning.model.entity.RateType;
import dk.digitalidentity.indberetning.model.entity.Report;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RateService {

    private final RateDao rateDao;
	private final ReportService reportService;

    public Rate getByYearAndType(int year, RateType type) {
        return rateDao.findByActiveYearAndRateType(year, type).orElse(null);
    }

    public Rate getById(long id) {
        return rateDao.findById(id).orElse(null);
    }

    public List<Rate> getAll() {
        return rateDao.findAll();
    }

    public Rate save(Rate rate) {
        return rateDao.save(rate);
    }

    public void delete(long id) {
        rateDao.deleteById(id);
    }

    public List<Rate> getByActiveYear(int year) {
        return rateDao.findByActiveYear(year);
    }

	@NotNull
	public Optional<Rate> getPreSelectedRate(List<Rate> activeRates, Person currentUser, Employment employment) {
		// 1. Org unit default rate type
		if (employment.getOrgUnit().getDefaultRateType() != null) {
			RateType defaultType = employment.getOrgUnit().getDefaultRateType();
			Optional<Rate> match = activeRates.stream()
					.filter(r -> r.getRateType().getId() == defaultType.getId())
					.findAny();
			if (match.isPresent()) {
				return match;
			}
			// defaultRateType has no active rate for this year → fall through
		}

		// 2. Prime rate
		Optional<Rate> primeActiveRate = activeRates.stream()
				.filter(rate -> rate.getRateType().isPrime())
				.findAny();

		// 3. Last-used rate — match by ID first, name as legacy fallback
		if (primeActiveRate.isEmpty()) {
			Optional<Report> latestReport = reportService.findLatestReportByPerson(currentUser);
			if (latestReport.isPresent()) {
				Report last = latestReport.get();
				if (last.getKmRateTypeId() != null) {
					primeActiveRate = activeRates.stream()
							.filter(r -> Objects.equals(r.getRateType().getId(), last.getKmRateTypeId()))
							.findAny();
				}
				// Legacy fallback: km_rate_type_id not set yet
				if (primeActiveRate.isEmpty() && last.getKmRateType() != null) {
					primeActiveRate = activeRates.stream()
							.filter(r -> Objects.equals(r.getRateType().getName(), last.getKmRateType()))
							.findAny();
				}
			}
		}
		return primeActiveRate;
	}

}
