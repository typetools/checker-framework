// Tests for JDBC types resource leak detection
// Test case for issue 6354:
// https://github.com/typetools/checker-framework/issues/6354

import java.sql.*;

class JdbcResourceLeak {

    // ========== ResultSet Tests ==========

    void resultSetNotClosed(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT 1");
        // :: error: (required.method.not.called)
    }

    void resultSetClosed(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT 1");
        rs.close();
    }

    // ========== Statement Tests ==========

    void statementNotClosed(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        // :: error: (required.method.not.called)
    }

    void statementClosed(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.close();
    }

    // ========== PreparedStatement Tests ==========

    void preparedStatementNotClosed(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT ?");
        // :: error: (required.method.not.called)
    }

    void preparedStatementClosed(Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT ?");
        ps.close();
    }

    // ========== Nested Resources ==========

    void nestedBothClosed(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1");
        rs.close();
        stmt.close();
    }

    void nestedStatementNotClosed(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1");
        rs.close();
        // :: error: (required.method.not.called)
    }

    void nestedResultSetNotClosed(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1");
        stmt.close();
        // :: error: (required.method.not.called)
    }

}
