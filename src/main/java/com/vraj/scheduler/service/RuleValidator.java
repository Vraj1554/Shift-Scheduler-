package com.vraj.scheduler.service;

import com.vraj.scheduler.model.Availability;
import com.vraj.scheduler.model.Employee;
import com.vraj.scheduler.model.Shift;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/** The labor rules, as pure Java with no database access so each one is easy to unit test. */
public class RuleValidator {
    public static final Duration MIN_REST = Duration.ofHours(8);

    /** Returns true if the proposed shift passes every rule for this employee. */
    public boolean canAssign(
            Employee employee,
            List<Availability> availability,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            List<Shift> existingShifts) {
        if (!start.isBefore(end)) {
            return false;
        }
        List<Availability> ownWindows =
                availability.stream().filter(a -> a.getEmployeeId() == employee.getId()).toList();
        List<Shift> ownShifts =
                existingShifts.stream().filter(s -> s.getEmployeeId() == employee.getId()).toList();
        return isWithinAvailability(ownWindows, date, start, end)
                && !hasOverlap(ownShifts, date, start, end)
                && !exceedsWeeklyHours(employee, ownShifts, date, start, end)
                && hasEnoughRest(ownShifts, date, start, end);
    }

    /** Rule 1: the whole shift must fit inside one availability window on that weekday. */
    public boolean isWithinAvailability(
            List<Availability> windows, LocalDate date, LocalTime start, LocalTime end) {
        int day = date.getDayOfWeek().getValue();
        return windows.stream()
                .anyMatch(
                        w ->
                                w.getDayOfWeek() == day
                                        && !start.isBefore(w.getStart())
                                        && !end.isAfter(w.getEnd()));
    }

    /** Rule 2: shifts may touch end to start, but not overlap. */
    public boolean hasOverlap(List<Shift> existing, LocalDate date, LocalTime start, LocalTime end) {
        return existing.stream()
                .anyMatch(
                        s ->
                                s.getDate().equals(date)
                                        && start.isBefore(s.getEnd())
                                        && end.isAfter(s.getStart()));
    }

    /** Rule 3: Monday-to-Sunday hours, including this shift, cannot exceed the employee's max. */
    public boolean exceedsWeeklyHours(
            Employee employee,
            List<Shift> existing,
            LocalDate date,
            LocalTime start,
            LocalTime end) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = monday.plusDays(7);
        long workedMinutes =
                existing.stream()
                        .filter(
                                s ->
                                        !s.getDate().isBefore(monday)
                                                && s.getDate().isBefore(nextMonday))
                        .mapToLong(Shift::getDurationMinutes)
                        .sum();
        long proposedMinutes = Duration.between(start, end).toMinutes();
        return workedMinutes + proposedMinutes > employee.getMaxWeeklyHours() * 60L;
    }

    /** Rule 4: at least 8 hours off before and after this shift, including across midnight. */
    public boolean hasEnoughRest(
            List<Shift> existing, LocalDate date, LocalTime start, LocalTime end) {
        LocalDateTime newStart = date.atTime(start);
        LocalDateTime newEnd = date.atTime(end);
        return existing.stream()
                .allMatch(
                        s -> {
                            Duration restBefore =
                                    Duration.between(s.getDate().atTime(s.getEnd()), newStart);
                            Duration restAfter =
                                    Duration.between(newEnd, s.getDate().atTime(s.getStart()));
                            return restBefore.compareTo(MIN_REST) >= 0
                                    || restAfter.compareTo(MIN_REST) >= 0;
                        });
    }
}
