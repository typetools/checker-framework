package daikon.split;

import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.tools.jtb.*;
import jtb.ParseException;

/**
 * ThisRemover is a visitor for a jtb syntax tree that removes all instances
 * of "this.". For example "this.x" would go to "x".
 */
class ThisRemover extends DepthFirstVisitor {
  // Note: the instances of "this." are not really being removed; instead,
  // the token images are being set to the empty string so when view as a
  // string they no longer exist.  However, the nodes are still in the jtb
  // tree.

  /** true iff the previous token was "this". */
  private boolean previousThis = false;

  private int columnshift = 0;
  private int columnshiftline = -1;
  // column shifting only applies to a single line, then is turned off again.
  // States for the variables:
  // columnshift == 0, columnshiftline == -1:
  //    no column shifting needed
  // columnshift != 0, columnshiftline != -1:
  //    column shifting being needed, applies only to specified line

  /** blocks public constructor. */
  private ThisRemover() {
    super();
  }

  /**
   * Removes "this." from prefix location in expression.
   * @param expression valid segment of java code from which "this."
   *  should be removed.
   * @return expression with instances of "this."
   *    removed from prefix locations.
   */
  public static String removeThisDot(String expression)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    ThisRemover remover = new ThisRemover();
    root.accept(remover);
    return Ast.format(root);
  }

  /**
   * This method should not be directly used by users of this class.
   * Replaces the token image with "" if it is "this" or
   * a "." following "this".
   */
  public void visit(NodeToken n) {
    if (n.beginLine == columnshiftline) {
      n.beginColumn = n.beginColumn + columnshift;
    }
    else {
      columnshift = 0;
      columnshiftline = -1;
    }
    if (Visitors.isThis(n)) {
      columnshift = columnshift - 4;
      n.tokenImage = "";
      columnshiftline = n.beginLine;
      previousThis = true;
    }
    else if (Visitors.isDot(n)) {
      if (previousThis) {
        columnshift = columnshift - 1;
        n.tokenImage = "";
        columnshiftline = n.beginLine;
      }
      previousThis = false;
    }
    else {
      previousThis = false;
    }
    n.endColumn = n.endColumn + columnshift;
  }

}
