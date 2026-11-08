package com.vraj.scheduler.service;

import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.CoverageGap;
import com.vraj.scheduler.model.CoverageRequirement;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a week's schedule with a greedy algorithm: walk the coverage slots in time order and give
 * each one to the eligible employees with the fewest hours so far. It is pure Java; {@link
 * SchedulingService} handles loading and saving.
 */
public class ScheduleGenerator {
    public record Result(List<Shift> shifts, List<CoverageGap> gaps) {}

    private final RuleValidator validator;

    public ScheduleGenerator(RuleValidator validator) {
        this.validator = validator;
    }

    /**
     * @param weekStart the Monday of the week to schedule
     * @param surroundingShifts already-saved shifts next to this week, so the rest rule holds across
     *     week boundaries
     */
    public Result generate(
            LocalDate weekStart,
            List<Employee> employees,
            List<Availability> availability,
            List<CoverageRequirement> requirements,
            List<Shift> surroundingShifts) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("Week must start on a Monday: " + weekStart);
        }

        List<CoverageRequirement> slots = new ArrayList<>(requirements);
        // Longest slot first when two start together: it may also cover the shorter one.
        slots.sort(
                Comparator.comparingInt(CoverageRequirement::getDayOfWeek)
                        .thenComparing(CoverageRequirement::getStart)
                        .thenComparing(CoverageRequirement::getEnd, Comparator.reverseOrder()));

        List<Shift> planned = new ArrayList<>();
        List<Shift> allShifts = new ArrayList<>(surroundingShifts);
        Map<Integer, Long> minutesByEmployee = new HashMap<>();
        List<CoverageGap> gaps = new ArrayList<>();

        for (CoverageRequirement slot : slots) {
            LocalDate date = weekStart.plusDays(slot.getDayOfWeek() - 1L);
            int assigned = countCovering(planned, date, slot);

            List<Employee> candidates =
                    employees.stream()
                            .filter(
                                    e ->
                                            validator.canAssign(
                                                    e,
                                                    availability,
                                                    date,
                                                    slot.getStart(),
                                                    slot.getEnd(),
                                                    allShifts))
                            .sorted(
                                    Comparator.<Employee>comparingLong(
                                                    e -> minutesByEmployee.getOrDefault(e.getId(), 0L))
                                            .thenComparingInt(Employee::getId))
                            .toList();

            for (Employee employee : candidates) {
                if (assigned >= slot.getMinStaff()) {
                    break;
                }
                Shift shift = new Shift(employee.getId(), date, slot.getStart(), slot.getEnd());
                planned.add(shift);
                allShifts.add(shift);
                minutesByEmployee.merge(employee.getId(), shift.getDurationMinutes(), Long::sum);
                assigned++;
            }

            if (assigned < slot.getMinStaff()) {
                gaps.add(
                        new CoverageGap(
                                date, slot.getStart(), slot.getEnd(), slot.getMinStaff(), assigned));
            }
        }
        return new Result(planned, gaps);
    }

    /** Staff already working the entire slot, for example from an overlapping longer slot. */
    private static int countCovering(List<Shift> shifts, LocalDate date, CoverageRequirement slot) {
        return (int)
                shifts.stream()
                        .filter(
                                s ->
                                        s.getDate().equals(date)
                                                && !s.getStart().isAfter(slot.getStart())
                                                && !s.getEnd().isBefore(slot.getEnd()))
                        .count();
    }
}
