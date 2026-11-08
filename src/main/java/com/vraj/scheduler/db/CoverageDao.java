package com.vraj.scheduler.db;

import com.vraj.scheduler.model.CoverageRequirement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CoverageDao {
    private final Database database;

    public CoverageDao(Database database) {
        this.database = database;
    }

    /** Adds the slot, or updates its minimum staff if the same day and times already exist. */
    public void save(CoverageRequirement requirement) throws SQLException {
        String sql =
                "MERGE INTO coverage_requirement (day_of_week, start_time, end_time, min_staff)"
                        + " KEY (day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requirement.getDayOfWeek());
            statement.setObject(2, requirement.getStart());
            statement.setObject(3, requirement.getEnd());
            statement.setInt(4, requirement.getMinStaff());
            statement.executeUpdate();
        }
    }

    public List<CoverageRequirement> findAll() throws SQLException {
        String sql =
                "SELECT id, day_of_week, start_time, end_time, min_staff FROM coverage_requirement"
                        + " ORDER BY day_of_week, start_time, end_time";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<CoverageRequirement> requirements = new ArrayList<>();
            while (rows.next()) {
                requirements.add(
                        new CoverageRequirement(
                                rows.getInt("id"),
                                rows.getInt("day_of_week"),
                                rows.getObject("start_time", LocalTime.class),
                                rows.getObject("end_time", LocalTime.class),
                                rows.getInt("min_staff")));
            }
            return requirements;
        }
    }
}
