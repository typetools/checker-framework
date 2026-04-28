// Tests for RowSet types resource leak detection
// Test case for issue 6354:
// https://github.com/typetools/checker-framework/issues/6354

import java.sql.SQLException;
import javax.sql.rowset.*;

class RowSetResourceLeak {

    // ========== JdbcRowSet Tests ==========

    void jdbcRowSetNotClosed(JdbcRowSet jrs) throws SQLException {
        // :: error: (required.method.not.called)
    }

    void jdbcRowSetClosed(JdbcRowSet jrs) throws SQLException {
        jrs.close();
    }


    // ========== CachedRowSet Tests ==========

    void cachedRowSetNotClosed(CachedRowSet crs) throws SQLException {
        // :: error: (required.method.not.called)
    }

    void cachedRowSetClosed(CachedRowSet crs) throws SQLException {
        crs.close();
    }


    // ========== FilteredRowSet Tests ==========

    void filteredRowSetNotClosed(FilteredRowSet frs) throws SQLException {
        // :: error: (required.method.not.called)
    }

    void filteredRowSetClosed(FilteredRowSet frs) throws SQLException {
        frs.close();
    }


    // ========== WebRowSet Tests ==========

    void webRowSetNotClosed(WebRowSet wrs) throws SQLException {
        // :: error: (required.method.not.called)
    }

    void webRowSetClosed(WebRowSet wrs) throws SQLException {
        wrs.close();
    }


    // ========== JoinRowSet Tests ==========

    void joinRowSetNotClosed(JoinRowSet jrs) throws SQLException {
        // :: error: (required.method.not.called)
    }

    void joinRowSetClosed(JoinRowSet jrs) throws SQLException {
        jrs.close();
    }
}
