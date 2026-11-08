package com.vraj.scheduler.db;

import com.vraj.scheduler.model.Availability;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AvailabilityDao {
    private final Database database;

    public AvailabilityDao(Database database) {
        this.database = database;
    }

    public void add(Availability availability) throws SQLException {
        String sql =
                "INSERT INTO availability (employee_id, day_of_week, start_time, end_time)"
                        + " VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, availability.getEmployeeId());
            statement.setInt(2, availability.getDayOfWeek());
            statement.setObject(3, availability.getStart());
            statement.setObject(4, availability.getEnd());
            statement.executeUpdate();
        }
    }

    /** Removes the employee's windows for one day of the week. */
    public void clearDay(int employeeId, int dayOfWeek) throws SQLException {
        String sql = "DELETE FROM availability WHERE employee_id = ? AND day_of_week = ?";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setInt(2, dayOfWeek);
            statement.executeUpdate();
        }
    }

    public List<Availability> findAll() throws SQLException {
        String sql =
                "SELECT id, employee_id, day_of_week, start_time, end_time FROM availability"
                        + " ORDER BY employee_id, day_of_week, start_time";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Availability> windows = new ArrayList<>();
            while (rows.next()) {
                windows.add(
                        new Availability(
                                rows.getInt("id"),
                                rows.getInt("employee_id"),
                                rows.getInt("day_of_week"),
                                rows.getObject("start_time", LocalTime.class),
                                rows.getObject("end_time", LocalTime.class)));
            }
            return windows;
        }
    }
}
