package daikon.split;

import java.util.*;

/**
 * ReplaceStatementMap is a immutable ADT for holding ReplaceStatements
 * that need to be looked-up by their method names.
 */
class ReplaceStatementMap {

  /**
   * Contains the ReplaceStatements of this, indexed by their names
   */
  private Map<String,ReplaceStatement> map;

  /**
   * Creates a new instance of ReplaceStatementMap of the replaceStatements
   * of replaceStatements.
   * @param replaceStatements a list of ReplaceStatements that specifies the
   *  the ReplaceStatements of this.
   */
  ReplaceStatementMap(List<ReplaceStatement> replaceStatements) {
    map = new HashMap<String,ReplaceStatement>();
    for (ReplaceStatement replaceStatement : replaceStatements) {
      map.put(replaceStatement.getName(), replaceStatement);
    }
  }

  /**
   * Returns the ReplaceStatement whose name is name.  If none can be
   * found, then name is repetitively shortened by eliminating each of its
   * leading prefixes in turn.  If once all prefixes are removed, still
   * no matching ReplaceStatements can be found, null is returned.
   * (A prefix is defined as a segment of name which is  terminated by
   * a "dot".)  For example if this contains entries with names "c", "b.c"
   * and "x.y.z", "get(c)" would return "c", "get(a.b.c)" would return "b.c"
   * and "get(y.z)" would return null.
   * @param name the name of the ReplaceStatement desired.
   */
  public ReplaceStatement get(String name) {
    ReplaceStatement rs = map.get(name);
    if (rs != null) {
      return rs;
    }
    int index = name.indexOf('.');
    while (index != -1) {
      rs = map.get(name.substring(index + 1));
      if (rs != null) {
        return rs;
      }
      index = name.indexOf('.', index + 1);
    }
    return null;
  }

  /** For debugging only. **/
  public String toString() {
    return map.toString();
  }

}
