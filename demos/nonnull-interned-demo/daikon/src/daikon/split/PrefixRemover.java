package daikon.split;

import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.tools.jtb.*;
import jtb.ParseException;

/**
 * PrefixRemover is a visitor for a JTB syntax tree that removes all instances
 * of some prefix. For example if "prefix" is the prefix then "prefix.x" would
 * go to "x" and "y.prefix.x" would go to "y.x" .
 * However, "prefix()" would be unaffected.  Finally, the prefix cannot be a
 * Java reserved word such as "this".
 */
class PrefixRemover extends DepthFirstVisitor {
  // Note: the instances of "prefix" are not really being removed; instead,
  // the token images are being set to the empty string so when view as a
  // string they no longer exist.  However, the nodes are still in the jtb
  // tree.

  /** The prefix being removed. */
  private String prefix;

  /** The last token visited by this. */
  private NodeToken lastToken;

  private int columnshift = 0;
  private int columnshiftline = -1;
 // column shifting only applies to a single line, then is turned off again.
 // States for the variables:
 // columnshift == 0, columnshiftline == -1:
 //    no column shifting needed
 // columnshift != 0, columnshiftline != -1:
 //    column shifting being needed, applies only to specified line

  /**
   * Creates a new instance of PrefixRemover to remove prefix.
   * @param prefix the prefix that should be removed by this.
   */
  private PrefixRemover(String prefix) {
    super();
    this.prefix = prefix;
  }

  /**
   * Removes prefix from prefix locations in expression.
   * @param expression valid segment of java code from which prefix
   *  should be removed.
   * @param  prefix the prefix that should be removed.
   *  Prefix can not be a java reserved word.
   * @return expression with instances of prefix
   *    removed from prefix locations.
   */
  public static String removePrefix(String expression, String prefix)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    PrefixRemover remover = new PrefixRemover(prefix);
    root.accept(remover);
    return Ast.format(root);
  }

  /**
   * This method should not be directly used by users of this class.
   * Replaces the token image with "" if it is prefix or
   * a "." following prefix.
   */
  public void visit(NodeToken n) {
    if (Visitors.isDot(n) &&
        Visitors.isIdentifier(lastToken) &&
        lastToken.tokenImage.equals(prefix)) {
      columnshift = columnshift - lastToken.tokenImage.length();
      lastToken.tokenImage = "";
      n.tokenImage = "";
      lastToken.endColumn = lastToken.beginColumn;
      n.endColumn = n.beginColumn;
      columnshiftline = n.endLine;
    }
    if (n.beginLine == columnshiftline) {
      n.beginColumn = n.beginColumn + columnshift;
      n.endColumn = n.endColumn + columnshift;
    } else {
      columnshift = 0;
      columnshiftline = -1;
    }
    lastToken = n;
  }

}
