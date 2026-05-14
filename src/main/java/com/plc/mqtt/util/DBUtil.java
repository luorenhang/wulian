package com.plc.mqtt.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBUtil {

    private static final Logger logger = Logger.getLogger(DBUtil.class.getName());

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/iiot";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "1234";

    private static final String INSERT_POINT_SQL = 
        "INSERT INTO plc_points (address, name, created_at) " +
        "VALUES (?, ?, ?) ON CONFLICT (address) DO NOTHING";

    private static final String UPDATE_CURRENT_STATUS_SQL = 
        "UPDATE plc_current_status SET status = ?, update_time = NOW() WHERE point_id = ?";

    private static final String INSERT_STATUS_LOG_SQL = 
        "INSERT INTO plc_status_log (point_id, old_status, new_status, change_time) " +
        "VALUES (?, ?, ?, NOW())";

    private static final String GET_POINT_ID_SQL = 
        "SELECT id FROM plc_points WHERE address = ?";

    private static final String GET_CURRENT_STATUS_SQL = 
        "SELECT status FROM plc_current_status WHERE point_id = ?";

    private static HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);
            config.setDriverClassName("org.postgresql.Driver");
            
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize connection pool: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() {
        if (dataSource == null) {
            logger.severe("Connection pool not initialized");
            return null;
        }
        try {
            Connection connection = dataSource.getConnection();
            logger.fine("Database connection obtained from pool");
            return connection;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get database connection. ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return null;
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.fine("Database connection closed");
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing database connection: " + e.getMessage(), e);
            }
        }
    }

    public static boolean initPoint(String pointName, String description) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            if (connection == null) {
                logger.severe("Cannot init point: connection is null");
                return false;
            }

            preparedStatement = connection.prepareStatement(INSERT_POINT_SQL);
            preparedStatement.setString(1, pointName);
            preparedStatement.setString(2, description);
            preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Point initialized successfully: " + pointName);
            } else {
                logger.fine("Point already exists: " + pointName);
            }
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to init point: " + pointName + ". ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return false;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing prepared statement: " + e.getMessage(), e);
                }
            }
            closeConnection(connection);
        }
    }

    public static int getPointId(String pointName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            if (connection == null) {
                logger.severe("Cannot get point ID: connection is null");
                return -1;
            }

            preparedStatement = connection.prepareStatement(GET_POINT_ID_SQL);
            preparedStatement.setString(1, pointName);

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                logger.warning("Point not found: " + pointName);
                return -1;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get point ID: " + pointName + ". ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return -1;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing result set: " + e.getMessage(), e);
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing prepared statement: " + e.getMessage(), e);
                }
            }
            closeConnection(connection);
        }
    }

    public static Boolean getCurrentStatus(int pointId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            if (connection == null) {
                logger.severe("Cannot get current status: connection is null");
                return null;
            }

            preparedStatement = connection.prepareStatement(GET_CURRENT_STATUS_SQL);
            preparedStatement.setInt(1, pointId);

            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getBoolean("status");
            } else {
                logger.warning("No current status found for point_id: " + pointId);
                return null;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to get current status for point_id: " + pointId + ". ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return null;
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing result set: " + e.getMessage(), e);
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing prepared statement: " + e.getMessage(), e);
                }
            }
            closeConnection(connection);
        }
    }

    public static boolean updateCurrentStatus(int pointId, boolean status) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            if (connection == null) {
                logger.severe("Cannot update current status: connection is null");
                return false;
            }

            preparedStatement = connection.prepareStatement(UPDATE_CURRENT_STATUS_SQL);
            preparedStatement.setBoolean(1, status);
            preparedStatement.setInt(2, pointId);

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.fine("Current status updated for point_id: " + pointId + ", status: " + status);
                return true;
            } else {
                logger.warning("Failed to update current status for point_id: " + pointId);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update current status for point_id: " + pointId + ". ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return false;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing prepared statement: " + e.getMessage(), e);
                }
            }
            closeConnection(connection);
        }
    }

    public static boolean insertStatusLog(int pointId, boolean oldStatus, boolean newStatus) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            if (connection == null) {
                logger.severe("Cannot insert status log: connection is null");
                return false;
            }

            preparedStatement = connection.prepareStatement(INSERT_STATUS_LOG_SQL);
            preparedStatement.setInt(1, pointId);
            preparedStatement.setBoolean(2, oldStatus);
            preparedStatement.setBoolean(3, newStatus);

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                logger.fine("Status log inserted for point_id: " + pointId + ", oldStatus: " + oldStatus + ", newStatus: " + newStatus);
                return true;
            } else {
                logger.warning("Failed to insert status log for point_id: " + pointId);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to insert status log for point_id: " + pointId + ". ErrorCode: " + e.getErrorCode() +
                    ", SQLState: " + e.getSQLState() + ", Message: " + e.getMessage(), e);
            return false;
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error closing prepared statement: " + e.getMessage(), e);
                }
            }
            closeConnection(connection);
        }
    }

    public static void main(String[] args) {
        logger.info("Testing DBUtil...");

        Connection connection = getConnection();
        if (connection != null) {
            logger.info("Connection test passed");
            closeConnection(connection);

            boolean initResult = initPoint("I0.2", "测试点");
            if (initResult) {
                logger.info("Init point test passed");
            } else {
                logger.severe("Init point test failed");
            }
        } else {
            logger.severe("Connection test failed");
        }
    }
}