package com.vraj.scheduler.db;

import com.vraj.scheduler.model.Shift;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ShiftDao {
    private final Database database;

    public ShiftDao(Database database) {
        this.database = database;
    }

    /** Returns shifts with dates from {@code from} (inclusive) to {@code to} (exclusive). */
    public List<Shift> findBetween(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                "SELECT id, employee_id, shift_date, start_time, end_time FROM shift"
                        + " WHERE shift_date >= ? AND shift_date < ?"
                        + " ORDER BY shift_date, start_time, employee_id";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, from);
            statement.setObject(2, to);
            try (ResultSet rows = statement.executeQuery()) {
                List<Shift> shifts = new ArrayList<>();
                while (rows.next()) {
                    shifts.add(
                            new Shift(
                                    rows.getInt("id"),
                                    rows.getInt("employee_id"),
                                    rows.getObject("shift_date", LocalDate.class),
                                    rows.getObject("start_time", LocalTime.class),
                                    rows.getObject("end_time", LocalTime.class)));
                }
                return shifts;
            }
        }
    }

    /**
     * Replaces every shift in the week starting on {@code weekStart} with {@code shifts}. It runs in
     * one transaction, so a failure leaves the previous schedule untouched.
     */
    public void replaceWeek(LocalDate weekStart, List<Shift> shifts) throws SQLException {
        String delete = "DELETE FROM shift WHERE shift_date >= ? AND shift_date < ?";
        String insert =
                "INSERT INTO shift (employee_id, shift_date, start_time, end_time)"
                        + " VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(delete);
                    PreparedStatement insertStatement = connection.prepareStatement(insert)) {
                deleteStatement.setObject(1, weekStart);
                deleteStatement.setObject(2, weekStart.plusDays(7));
                deleteStatement.executeUpdate();
                for (Shift shift : shifts) {
                    insertStatement.setInt(1, shift.getEmployeeId());
                    insertStatement.setObject(2, shift.getDate());
                    insertStatement.setObject(3, shift.getStart());
                    insertStatement.setObject(4, shift.getEnd());
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
