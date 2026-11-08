package com.vraj.scheduler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.CoverageGap;
import com.vraj.scheduler.model.CoverageRequirement;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScheduleGeneratorTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    private final ScheduleGenerator generator = new ScheduleGenerator(new RuleValidator());
    private final Employee ann = new Employee(1, "Ann", "Cashier", 40);
    private final Employee ben = new Employee(2, "Ben", "Cashier", 40);

    private static LocalTime at(int hour) {
        return LocalTime.of(hour, 0);
    }

    private static List<Availability> allWeek(Employee employee) {
        List<Availability> windows = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            windows.add(new Availability(employee.getId(), day, at(0), at(23)));
        }
        return windows;
    }

    private static List<Availability> allWeek(Employee first, Employee second) {
        List<Availability> windows = new ArrayList<>(allWeek(first));
        windows.addAll(allWeek(second));
        return windows;
    }

    @Test
    void fillsASlotWithoutRecordingAGap() {
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann, ben),
                        allWeek(ann, ben),
                        List.of(new CoverageRequirement(1, at(9), at(17), 2)),
                        List.of());

        assertEquals(2, result.shifts().size());
        assertTrue(result.gaps().isEmpty());
        assertTrue(result.shifts().stream().allMatch(s -> s.getDate().equals(MONDAY)));
    }

    @Test
    void placesEachSlotOnItsWeekday() {
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann),
                        allWeek(ann),
                        List.of(new CoverageRequirement(7, at(9), at(17), 1)),
                        List.of());

        assertEquals(MONDAY.plusDays(6), result.shifts().get(0).getDate());
    }

    @Test
    void recordsAGapWhenThereAreTooFewEligibleStaff() {
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann, ben),
                        allWeek(ann),
                        List.of(new CoverageRequirement(1, at(9), at(17), 2)),
                        List.of());

        assertEquals(1, result.shifts().size());
        assertEquals(
                List.of(new CoverageGap(MONDAY, at(9), at(17), 2, 1)), result.gaps());
        assertEquals(1, result.gaps().get(0).shortfall());
    }

    @Test
    void assignsTheEmployeeWithFewestHoursSoFar() {
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann, ben),
                        allWeek(ann, ben),
                        List.of(
                                new CoverageRequirement(1, at(9), at(17), 1),
                                new CoverageRequirement(2, at(9), at(17), 1),
                                new CoverageRequirement(3, at(9), at(17), 1),
                                new CoverageRequirement(4, at(9), at(17), 1)),
                        List.of());

        long annShifts = result.shifts().stream().filter(s -> s.getEmployeeId() == 1).count();
        long benShifts = result.shifts().stream().filter(s -> s.getEmployeeId() == 2).count();
        assertEquals(2, annShifts);
        assertEquals(2, benShifts);
    }

    @Test
    void neverExceedsAnEmployeesWeeklyHours() {
        Employee partTimer = new Employee(1, "Ann", "Cashier", 8);
        var result =
                generator.generate(
                        MONDAY,
                        List.of(partTimer),
                        allWeek(partTimer),
                        List.of(
                                new CoverageRequirement(1, at(9), at(17), 1),
                                new CoverageRequirement(2, at(9), at(17), 1)),
                        List.of());

        assertEquals(1, result.shifts().size());
        assertEquals(1, result.gaps().size());
        assertEquals(MONDAY.plusDays(1), result.gaps().get(0).date());
    }

    @Test
    void honorsRestAfterAShiftFromTheWeekBefore() {
        Shift sundayClose = new Shift(1, MONDAY.minusDays(1), at(16), at(23));
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann),
                        allWeek(ann),
                        List.of(new CoverageRequirement(1, at(6), at(12), 1)),
                        List.of(sundayClose));

        assertTrue(result.shifts().isEmpty());
        assertEquals(1, result.gaps().size());
    }

    @Test
    void doesNotAddStaffToASlotAlreadyCoveredByALongerOne() {
        var result =
                generator.generate(
                        MONDAY,
                        List.of(ann, ben),
                        allWeek(ann, ben),
                        List.of(
                                new CoverageRequirement(1, at(12), at(14), 1),
                                new CoverageRequirement(1, at(9), at(17), 1)),
                        List.of());

        assertEquals(1, result.shifts().size());
        assertEquals(at(9), result.shifts().get(0).getStart());
        assertTrue(result.gaps().isEmpty());
    }

    @Test
    void rejectsAWeekThatDoesNotStartOnMonday() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        generator.generate(
                                MONDAY.plusDays(1), List.of(), List.of(), List.of(), List.of()));
    }
}
