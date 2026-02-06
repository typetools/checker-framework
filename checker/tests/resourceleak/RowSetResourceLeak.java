// Tests for RowSet types resource leak detection
// Test case for issue 6354:
// https://github.com/typetools/checker-framework/issues/6354

import javax.sql.rowset.*;
import com.sun.rowset.*;
import java.sql.*;

class RowSetResourceLeak {

    // ========== JdbcRowSet Tests ==========

    void jdbcRowSetNotClosed(ResultSet rs) throws SQLException {
        JdbcRowSet jrs = new JdbcRowSetImpl(rs);
        // :: error: (required.method.not.called)
    }

    void jdbcRowSetClosed(ResultSet rs) throws SQLException {
        JdbcRowSet jrs = new JdbcRowSetImpl(rs);
        jrs.close();
    }

    // ========== CachedRowSet Tests ==========

    void cachedRowSetNotClosed() throws SQLException {
        CachedRowSet crs = new CachedRowSetImpl();
        // :: error: (required.method.not.called)
    }

    void cachedRowSetClosed() throws SQLException {
        CachedRowSet crs = new CachedRowSetImpl();
        crs.close();
    }

    void cachedRowSetToResultSetNotClosed() throws SQLException {
        CachedRowSet crs = new CachedRowSetImpl();
        ResultSet rs = crs.toResultSet();
        crs.close();
      // :: error: (required.method.not.called)
    }

    void cachedRowSetToResultSetBothClosed() throws SQLException {
        CachedRowSet crs = new CachedRowSetImpl();
        ResultSet rs = crs.toResultSet();
        rs.close();
        crs.close();
    }

    // ========== FilteredRowSet Tests ==========

    void filteredRowSetNotClosed() throws SQLException {
        FilteredRowSet frs = new FilteredRowSetImpl();
        // :: error: (required.method.not.called)
    }

    void filteredRowSetClosed() throws SQLException {
        FilteredRowSet frs = new FilteredRowSetImpl();
        frs.close();
    }

    // ========== WebRowSet Tests ==========

    void webRowSetNotClosed() throws SQLException {
        WebRowSet wrs = new WebRowSetImpl();
        // :: error: (required.method.not.called)
    }

    void webRowSetClosed() throws SQLException {
        WebRowSet wrs = new WebRowSetImpl();
        wrs.close();
    }

    // ========== JoinRowSet Tests ==========

    void joinRowSetNotClosed() throws SQLException {
        JoinRowSet jrs = new JoinRowSetImpl();
        // :: error: (required.method.not.called)
    }

    void joinRowSetClosed() throws SQLException {
        JoinRowSet jrs = new JoinRowSetImpl();
        jrs.close();
    }
}
