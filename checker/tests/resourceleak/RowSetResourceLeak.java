// Tests for RowSet types resource leak detection
// Test case for issue 6354:
// https://github.com/typetools/checker-framework/issues/6354

import java.sql.SQLException;
import javax.sql.rowset.*;

class RowSetResourceLeak {

  // ========== JdbcRowSet Tests ==========

  void jdbcRowSetNotClosed() throws SQLException {
    JdbcRowSet jrs = RowSetProvider.newFactory().createJdbcRowSet();
    // :: error: [required.method.not.called]
  }

  void jdbcRowSetClosed() throws SQLException {
    JdbcRowSet jrs = RowSetProvider.newFactory().createJdbcRowSet();
    jrs.close();
  }

  // ========== CachedRowSet Tests ==========

  void cachedRowSetNotClosed() throws SQLException {
    CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
    // :: error: [required.method.not.called]
  }

  void cachedRowSetClosed() throws SQLException {
    CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
    crs.close();
  }

  // ========== FilteredRowSet Tests ==========

  void filteredRowSetNotClosed() throws SQLException {
    FilteredRowSet frs = RowSetProvider.newFactory().createFilteredRowSet();
    // :: error: [required.method.not.called]
  }

  void filteredRowSetClosed() throws SQLException {
    FilteredRowSet frs = RowSetProvider.newFactory().createFilteredRowSet();
    frs.close();
  }

  // ========== WebRowSet Tests ==========

  void webRowSetNotClosed() throws SQLException {
    WebRowSet wrs = RowSetProvider.newFactory().createWebRowSet();
    // :: error: [required.method.not.called]
  }

  void webRowSetClosed() throws SQLException {
    WebRowSet wrs = RowSetProvider.newFactory().createWebRowSet();
    wrs.close();
  }

  // ========== JoinRowSet Tests ==========

  void joinRowSetNotClosed() throws SQLException {
    JoinRowSet jrs = RowSetProvider.newFactory().createJoinRowSet();
    // :: error: [required.method.not.called]
  }

  void joinRowSetClosed() throws SQLException {
    JoinRowSet jrs = RowSetProvider.newFactory().createJoinRowSet();
    jrs.close();
  }
}
