package com.vraj.scheduler.db;

import com.vraj.scheduler.model.Employee;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDao {
    private final Database database;

    public EmployeeDao(Database database) {
        this.database = database;
    }

    /** Saves the employee and returns a copy carrying its generated id. */
    public Employee add(Employee employee) throws SQLException {
        String sql = "INSERT INTO employee (name, role, max_weekly_hours) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, employee.getName());
            statement.setString(2, employee.getRole());
            statement.setInt(3, employee.getMaxWeeklyHours());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new Employee(
                        keys.getInt(1),
                        employee.getName(),
                        employee.getRole(),
                        employee.getMaxWeeklyHours());
            }
        }
    }

    public List<Employee> findAll() throws SQLException {
        String sql = "SELECT id, name, role, max_weekly_hours FROM employee ORDER BY id";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            List<Employee> employees = new ArrayList<>();
            while (rows.next()) {
                employees.add(fromRow(rows));
            }
            return employees;
        }
    }

    public Optional<Employee> findById(int id) throws SQLException {
        String sql = "SELECT id, name, role, max_weekly_hours FROM employee WHERE id = ?";
        try (Connection connection = database.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(fromRow(rows)) : Optional.empty();
            }
        }
    }

    private static Employee fromRow(ResultSet row) throws SQLException {
        return new Employee(
                row.getInt("id"),
                row.getString("name"),
                row.getString("role"),
                row.getInt("max_weekly_hours"));
    }
}
