package com.vraj.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleValidatorTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate NEXT_MONDAY = MONDAY.plusDays(7);

    private final RuleValidator validator = new RuleValidator();
    private final Employee employee = new Employee(1, "Sam", "Cashier", 8);

    private static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    private static Availability mondayWindow(int startHour, int endHour) {
        return new Availability(1, 1, at(startHour), at(endHour));
    }

    // Rule 1: availability

    @Test
    void acceptsShiftInsideAvailability() {
        assertTrue(
                validator.isWithinAvailability(List.of(mondayWindow(8, 18)), MONDAY, at(9), at(17)));
    }

    @Test
    void acceptsShiftExactlyMatchingAvailability() {
        assertTrue(
                validator.isWithinAvailability(List.of(mondayWindow(9, 17)), MONDAY, at(9), at(17)));
    }

    @Test
    void rejectsShiftRunningPastAvailability() {
        assertFalse(
                validator.isWithinAvailability(List.of(mondayWindow(9, 17)), MONDAY, at(9), at(18)));
    }

    @Test
    void rejectsShiftOnADayWithoutAvailability() {
        assertFalse(
                validator.isWithinAvailability(List.of(mondayWindow(9, 17)), TUESDAY, at(9), at(17)));
    }

    @Test
    void rejectsShiftSpanningTwoSeparateWindows() {
        List<Availability> windows = List.of(mondayWindow(9, 12), mondayWindow(13, 17));
        assertFalse(validator.isWithinAvailability(windows, MONDAY, at(9), at(17)));
    }

    // Rule 2: overlap

    @Test
    void rejectsOverlappingShift() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(17)));
        assertTrue(validator.hasOverlap(existing, MONDAY, at(16), at(20)));
    }

    @Test
    void rejectsShiftContainedInsideAnother() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(17)));
        assertTrue(validator.hasOverlap(existing, MONDAY, at(11), at(12)));
    }

    @Test
    void shiftsThatOnlyTouchDoNotOverlap() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(17)));
        assertFalse(validator.hasOverlap(existing, MONDAY, at(17), at(20)));
    }

    @Test
    void sameTimesOnAnotherDayDoNotOverlap() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(17)));
        assertFalse(validator.hasOverlap(existing, TUESDAY, at(9), at(17)));
    }

    // Rule 3: weekly hours

    @Test
    void allowsReachingExactlyMaxHours() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(13)));
        assertFalse(validator.exceedsWeeklyHours(employee, existing, TUESDAY, at(9), at(13)));
    }

    @Test
    void rejectsGoingOverMaxHours() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(13)));
        assertTrue(validator.exceedsWeeklyHours(employee, existing, TUESDAY, at(9), at(14)));
    }

    @Test
    void hoursFromOtherWeeksDoNotCount() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(17)));
        assertFalse(validator.exceedsWeeklyHours(employee, existing, NEXT_MONDAY, at(9), at(17)));
    }

    // Rule 4: rest

    @Test
    void allowsExactlyEightHoursRest() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(14), at(22)));
        assertTrue(validator.hasEnoughRest(existing, TUESDAY, at(6), at(12)));
    }

    @Test
    void rejectsLessThanEightHoursRestAcrossMidnight() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(14), at(22)));
        assertFalse(validator.hasEnoughRest(existing, TUESDAY, LocalTime.of(5, 59), at(12)));
    }

    @Test
    void rejectsTooLittleRestBeforeALaterExistingShift() {
        List<Shift> existing = List.of(new Shift(1, TUESDAY, at(6), at(12)));
        assertFalse(validator.hasEnoughRest(existing, MONDAY, at(16), at(23)));
    }

    @Test
    void rejectsBackToBackShiftsOnTheSameDay() {
        List<Shift> existing = List.of(new Shift(1, MONDAY, at(9), at(13)));
        assertFalse(validator.hasEnoughRest(existing, MONDAY, at(13), at(17)));
    }

    // All rules together

    @Test
    void canAssignWhenEveryRulePasses() {
        assertTrue(
                validator.canAssign(
                        employee, List.of(mondayWindow(9, 17)), MONDAY, at(9), at(17), List.of()));
    }

    @Test
    void canAssignIgnoresOtherEmployeesRecords() {
        List<Shift> someoneElsesShift = List.of(new Shift(2, MONDAY, at(9), at(17)));
        Availability someoneElsesWindow = new Availability(2, 1, at(9), at(17));
        assertTrue(
                validator.canAssign(
                        employee,
                        List.of(mondayWindow(9, 17)),
                        MONDAY,
                        at(9),
                        at(17),
                        someoneElsesShift));
        assertFalse(
                validator.canAssign(
                        employee, List.of(someoneElsesWindow), MONDAY, at(9), at(17), List.of()));
    }

    @Test
    void canAssignRejectsAnEmptyTimeRange() {
        assertFalse(
                validator.canAssign(
                        employee, List.of(mondayWindow(9, 17)), MONDAY, at(12), at(12), List.of()));
    }
}
