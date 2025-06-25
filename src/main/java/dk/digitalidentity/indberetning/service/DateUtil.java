package dk.digitalidentity.indberetning.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateUtil {

	private DateUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean dateBetweenInclusive(LocalDate date, LocalDate startDate, LocalDate endDate) {
		return dateTimeBetweenInclusive(date, startDate == null ? null : startDate.atStartOfDay(), endDate == null ? null : endDate.atTime(23, 59, 59));
	}

	public static boolean dateTimeBetweenInclusive(LocalDate date, LocalDateTime startDate, LocalDateTime endDate) {
		boolean inclusiveBegin = startDate == null || date.isEqual(startDate.toLocalDate()) || date.isAfter(startDate.toLocalDate());
		boolean inclusiveEnd =  endDate == null || date.isEqual(endDate.toLocalDate()) || date.isBefore(endDate.toLocalDate());

		return inclusiveBegin && inclusiveEnd;
	}
}
