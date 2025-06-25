package dk.digitalidentity.indberetning.service;

import com.google.common.base.Stopwatch;
import dk.digitalidentity.indberetning.model.dao.SixtyDayRuleDao;
import dk.digitalidentity.indberetning.model.entity.GpsCoordinate;
import dk.digitalidentity.indberetning.model.entity.Person;
import dk.digitalidentity.indberetning.model.entity.Report;
import dk.digitalidentity.indberetning.model.entity.SixtyDayRule;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class SixtyDayRuleService {

	private final SixtyDayRuleDao sixtyDayRuleDao;
	private final ReportService reportService;

	public List<SixtyDayRule> findAll() {
		return sixtyDayRuleDao.findAll();
	}

	public List<SixtyDayRule> findByCountOver60() {
		return sixtyDayRuleDao.findByCountGreaterThanEqual(60);
	}

	public List<SixtyDayRule> findByPersonAndAddress(Person person, String address) {
		return sixtyDayRuleDao.findByPersonAndAddress(person, address);
	}

	public List<SixtyDayRule> findByDriveDateOver60DaysAgo() {
		return sixtyDayRuleDao.findByLastDriveDateBefore(LocalDate.now().minusDays(61));
	}

	public long deleteByDriveDateOver60DaysAgo() {
		return sixtyDayRuleDao.deleteByLastDriveDateBeforeAllIgnoreCase(LocalDate.now().minusDays(61));
	}

	public SixtyDayRule save(SixtyDayRule sixtyDayRule) {
		return sixtyDayRuleDao.save(sixtyDayRule);
	}

	public void processIndividualSixtyDayRule(Report report, Person person, List<GpsCoordinate> coords, LocalDate driveDate) {
		if (report.isEndsAtHome() || report.isStartsAtHome()) {
			List<SixtyDayRule> entryList = findByPersonAndAddress(person, coords.getLast().getAddress());
			if (entryList == null || entryList.isEmpty()) {
				SixtyDayRule newEntry = new SixtyDayRule();
				newEntry.setAddress(coords.get(coords.size()-1).getAddress());
				newEntry.setPerson(person);
				newEntry.setCount(1);
				newEntry.setLastDriveDate(driveDate);

				save(newEntry);
			}
			else {
				if (entryList.size() > 1) {
					log.error("There should never be more than one address record for sixty day rule per person (personId:{})", person.getId());
					return;
				}
				// Theoretically addresses are bound to person objects so they should be unique!
				SixtyDayRule entry = entryList.getFirst();
				// Increment the counter for number of times address has been visited,
				// flag if possible 60-day rule violation.
				entry.setCount(entry.getCount() + 1);

				if (!Objects.equals(report.isSixtyDaysRule(), entry.getCount() > 60)) {
					report.setFlaggedSixtyDayRule(entry.getCount() > 60);
					reportService.save(report);
				}

				// Override last drive date if it's newer than the currently set one.
				LocalDate lastDriveDate = entry.getLastDriveDate();
				if (driveDate.isAfter(lastDriveDate)) {
					entry.setLastDriveDate(driveDate);
				}

				save(entry);
			}
		}
	}

	public void check60Days() {
		List<SixtyDayRule> list = findByCountOver60();
		if (!list.isEmpty()) {
			log.info("Found: " + list.size() + " Address entries that exceeded 60 visits. Validating rule.");
			Stopwatch stopwatch = Stopwatch.createStarted();

			for (SixtyDayRule sixtyDayRule : list) {
				validate60DaysRule(sixtyDayRule);
			}

			stopwatch.stop();
			log.info("60-day rule processed. Took: " + stopwatch.toString());
		}
		else {
			log.debug("No entries found that exceed the max 60 days...");
		}
	}

	//Validation of the 60 days rule, if over we flag all reports afterwards and automatically do the same when creating reports
	@Transactional
	public void validate60DaysRule(SixtyDayRule entry) {
		List<Report> reportList = reportService.getByPersonAndAddressAndDriveDateBetween(entry.getAddress(), LocalDate.now().minusMonths(12), LocalDate.now(), entry.getPerson());
		entry.setCount(reportList.size());
		if (entry.getCount() > 60) {
			for (int i = 61; i < reportList.size(); i++) {
				Report report = reportList.get(i);
				report.setFlaggedSixtyDayRule(true);
			}
			reportService.saveAll(reportList);
		}
		save(entry);
	}

	//If a person has not worked/reported in the last 60 days then we reset. check skat.dk
	@Transactional
	public void checkAllFor60DaysReset() {
		long deleted = deleteByDriveDateOver60DaysAgo();
		if (deleted > 0) {
			log.info("Reset " + deleted + " Address entries with a last drive date over 60 days ago");
		}
		else {
			log.debug("No entries found that require resetting their 60 days counter");
		}
	}
}
