package com.vraj.scheduler.service;

import com.vraj.scheduler.db.Database;
import com.vraj.scheduler.model.CoverageGap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Reports written as SQL queries against the saved schedule. */
public class ReportService {
    public record EmployeeHours(String name, double hours, int maxHours) {}

    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    /** Hours per employee for the week, most first. Unscheduled employees appear with 0 hours. */
    public List<EmployeeHours> hoursPerEmployee(LocalDate weekStart) throws SQLException {
        String sql =
                """
                SELECT e.name, e.max_weekly_hours,
                       COALESCE(SUM(DATEDIFF('MINUTE', s.start_time, s.end_time)), 0) / 60.0 AS hours
                FROM employee e
                LEFT JOIN shift s
                  ON s.employee_id = e.id AND s.shift_date >= ? AND s.shift_date < ?
                GROUP BY e.id, e.name, e.max_weekly_hours
                ORDER BY hours DESC, e.name
                """;
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, weekStart);
            statement.setObject(2, weekStart.plusDays(7));
            try (ResultSet rows = statement.executeQuery()) {
                List<EmployeeHours> report = new ArrayList<>();
                while (rows.next()) {
                    report.add(
                            new EmployeeHours(
                                    rows.getString("name"),
                                    rows.getDouble("hours"),
                                    rows.getInt("max_weekly_hours")));
                }
                return report;
            }
        }
    }

    /**
     * Coverage slots short of their minimum staff for the week. Deriving this from saved shifts
     * keeps it correct after a restart and means a stale copy can never disagree with the schedule.
     */
    public List<CoverageGap> coverageGaps(LocalDate weekStart) throws SQLException {
        String sql =
                """
                SELECT r.day_of_week, r.start_time, r.end_time, r.min_staff, COUNT(s.id) AS assigned
                FROM coverage_requirement r
                LEFT JOIN shift s
                  ON s.shift_date >= ? AND s.shift_date < ?
                 AND ISO_DAY_OF_WEEK(s.shift_date) = r.day_of_week
                 AND s.start_time <= r.start_time
                 AND s.end_time >= r.end_time
                GROUP BY r.id, r.day_of_week, r.start_time, r.end_time, r.min_staff
                HAVING COUNT(s.id) < r.min_staff
                ORDER BY r.day_of_week, r.start_time
                """;
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, weekStart);
            statement.setObject(2, weekStart.plusDays(7));
            try (ResultSet rows = statement.executeQuery()) {
                List<CoverageGap> gaps = new ArrayList<>();
                while (rows.next()) {
                    gaps.add(
                            new CoverageGap(
                                    weekStart.plusDays(rows.getInt("day_of_week") - 1L),
                                    rows.getObject("start_time", LocalTime.class),
                                    rows.getObject("end_time", LocalTime.class),
                                    rows.getInt("min_staff"),
                                    rows.getInt("assigned")));
                }
                return gaps;
            }
        }
    }
}
