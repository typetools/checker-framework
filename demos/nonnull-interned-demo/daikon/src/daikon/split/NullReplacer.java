package daikon.split;

import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.tools.jtb.*;
import jtb.ParseException;

/**
 * NullReplacer is a jtb syntax tree visitor that replaces all instances
 * of "null" with "0" in a given expression. Note: "null" is
 * only referring to the java reserved word "null" not to any instances
 * of the string "null".
 */
class NullReplacer extends DepthFirstVisitor {


  private int columnshift = 0;
  private int columnshiftline = -1;
  // column shifting only applies to a single line, then is turned off again.
  // States for the variables:
  // columnshift == 0, columnshiftline == -1:
  //    no column shifting needed
  // columnshift != 0, columnshiftline != -1:
  //    column shifting being needed, applies only to specified line

  /** Blocks public constructor. */
  private NullReplacer() {
    super();
  }

  /**
   * Replaces all instance of "null" with "0".
   * @param expression a valid java expression.
   * @return expression with all instances of null replaced by
   *  instances of "0".
   * @throws ParseException if expression is not a valid java expression.
   */
  public static String replaceNull(String expression)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    NullReplacer replacer = new NullReplacer();
    root.accept(replacer);
    return Ast.format(root);
  }

  /**
   * Replaces all instance of "null" with "0" in the JTB syntax tree rooted
   * at root..
   * @param root a JTB syntax tree.
   */
  public static void replaceNull(Node root) {
    NullReplacer replacer = new NullReplacer();
    root.accept(replacer);
  }

  /**
   * This method should not be directly used by user of this class;
   * however it must be public to full-fill the visitor interface.
   * If n represents null then it is replaced by "0".
   */
  public void visit(NodeToken n) {
    if (n.beginLine == columnshiftline) {
      n.beginColumn = n.beginColumn - columnshift;
    } else {
      columnshift = 0;
      columnshiftline = -1;
    }
    if (Visitors.isNull(n)) {
      columnshift = columnshift + 3;
      n.tokenImage = "0";
      columnshiftline = n.beginLine;
      n.kind = Visitors.STRING_LITERAL;
    }
    n.endColumn = n.endColumn + columnshift;
  }

}
