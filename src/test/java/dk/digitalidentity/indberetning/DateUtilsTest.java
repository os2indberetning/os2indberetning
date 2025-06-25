package dk.digitalidentity.indberetning;

import dk.digitalidentity.indberetning.service.DateUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateUtilsTest {

    @Test
    void testDateWithinRange() {
        LocalDate date = LocalDate.of(2024, 4, 10);
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertTrue(DateUtil.dateBetweenInclusive(date, start, end));
    }

    @Test
    void testDateOnStartBoundary() {
        LocalDate date = LocalDate.of(2024, 4, 1);
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertTrue(DateUtil.dateBetweenInclusive(date, start, end));
    }

    @Test
    void testDateOnEndBoundary() {
        LocalDate date = LocalDate.of(2024, 4, 30);
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertTrue(DateUtil.dateBetweenInclusive(date, start, end));
    }

    @Test
    void testDateBeforeRange() {
        LocalDate date = LocalDate.of(2024, 3, 31);
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertFalse(DateUtil.dateBetweenInclusive(date, start, end));
    }

    @Test
    void testDateAfterRange() {
        LocalDate date = LocalDate.of(2024, 5, 1);
        LocalDate start = LocalDate.of(2024, 4, 1);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertFalse(DateUtil.dateBetweenInclusive(date, start, end));
    }

    @Test
    void testNullStartDate() {
        LocalDate date = LocalDate.of(2024, 4, 10);
        LocalDate end = LocalDate.of(2024, 4, 30);

        assertTrue(DateUtil.dateBetweenInclusive(date, null, end));
    }

    @Test
    void testNullEndDate() {
        LocalDate date = LocalDate.of(2024, 4, 10);
        LocalDate start = LocalDate.of(2024, 4, 1);

        assertTrue(DateUtil.dateBetweenInclusive(date, start, null));
    }

    @Test
    void testNullStartAndEndDate() {
        LocalDate date = LocalDate.of(2024, 4, 10);

        assertTrue(DateUtil.dateBetweenInclusive(date, null, null));
    }

    @Test
    void testDateTimeVariant() {
        LocalDate date = LocalDate.of(2024, 4, 10);
        LocalDateTime start = LocalDateTime.of(2024, 4, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2024, 4, 30, 23, 59);

        assertTrue(DateUtil.dateTimeBetweenInclusive(date, start, end));
    }

    @Test
    void testDateTimeOutsideRange() {
        LocalDate date = LocalDate.of(2024, 3, 31);
        LocalDateTime start = LocalDateTime.of(2024, 4, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 4, 30, 23, 59);

        assertFalse(DateUtil.dateTimeBetweenInclusive(date, start, end));
    }
}
