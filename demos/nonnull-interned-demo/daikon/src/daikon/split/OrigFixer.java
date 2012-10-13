package daikon.split;

import jtb.syntaxtree.*;
import jtb.visitor.*;
import daikon.tools.jtb.*;
import jtb.ParseException;

/**
 * OrigFixer is a visitor for a jtb syntax tree that replaces instances of
 * of "orig()" with "orig_".  For example, "orig(x) < y" would yield
 * "orig_x < y".
 */
class OrigFixer extends DepthFirstVisitor {

  /** True iff presently visiting the args of "orig()". */
  private boolean withinArgList = false;

  private boolean foundOrig = false;

  /** The last NodeToken visited. */
  private NodeToken lastToken;

  /** The token visited before lastToken. */
  private NodeToken twoTokensAgo;

  /** Blocks public constructor. */
  private OrigFixer() {
    super();
  }

  /**
   * Replaces all instance of "orig(variableName) with "orig_variableName"
   * in expression. In the case of multiple variable names appearing within
   * the argument of "orig()" all variable names are prefixed with "orig_".
   * For example, "orig(x + y > z - 3)" would yield,
   * "orig_x + orig_y > orig_z - 3".
   * @param expression a valid segment of java code in which "orig()" is
   *  being replaced.
   * @return condition with all instances of "orig()" replaced.
   * @throws ParseException if expression is not valid java code.
   */
  public static String fixOrig(String expression)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    OrigFixer fixer = new OrigFixer();
    root.accept(fixer);
    return Ast.format(root);
  }

  /**
   * This method should not be directly used by users of this class.
   * If n is an instance of "orig()" it is replaced.
   */
  public void visit(PrimaryExpression n) {
    if (isOrig(n)) {
      NodeToken origToken =  ((Name) n.f0.f0.choice).f0;
      origToken.tokenImage = "";
      NodeToken openParen =
        ((Arguments) ((PrimarySuffix) n.f1.elementAt(0)).f0.choice).f0;
      openParen.tokenImage = "";
      foundOrig = true;
      super.visit(n);

      // handle lastToken
      if (lastToken != null &&
          Visitors.isIdentifier(lastToken) &&
          (twoTokensAgo == null || (! Visitors.isDot(twoTokensAgo)))) {
        lastToken.tokenImage = "orig_" + lastToken.tokenImage;
      }
      foundOrig = false;
      NodeToken closeParen =
        ((Arguments) ((PrimarySuffix) n.f1.elementAt(0)).f0.choice).f2;
      closeParen.tokenImage = "";
    } else {
      super.visit(n);
    }
  }

  /**
   * This method should not be directly used by users of this class.
   * Marks whether this is presently visiting the arguments to an
   * instance of "orig()".
   */
  public void visit(Arguments n) {
    if (foundOrig) {
      withinArgList = true;
    } else {
      withinArgList = false;
    }
    super.visit(n);
    withinArgList = false;
  }

 /**
  * Returns in n if an instance of the method "orig".
  * @return true iff n is a instance of the method "orig".
  */
  private boolean isOrig(PrimaryExpression n) {
    return ((n.f0.f0.choice instanceof Name) &&
            (((Name) n.f0.f0.choice).f0.tokenImage.equals("orig")) &&
            (n.f1.size() > 0) &&
            (n.f1.elementAt(0) instanceof PrimarySuffix) &&
            (((PrimarySuffix) n.f1.elementAt(0)).f0.choice instanceof Arguments));
  }

  /**
   * This method should not be directly used by users of this class.
   * Updates n to be prefixed with "orig_" if needed.
   */
  public void visit(NodeToken n) {
    n.beginColumn = -1;
    n.endColumn = -1;
    if (withinArgList && isLastTokenVar(n)) {
      lastToken.tokenImage = "orig_" + lastToken.tokenImage;
    }
    twoTokensAgo = lastToken;
    lastToken = n;
  }

  /**
   * Returns if the the last token represents a
   * variable name.
   */
  private boolean isLastTokenVar(NodeToken n) {
    return (lastToken != null &&
            Visitors.isIdentifier(lastToken) &&
            (twoTokensAgo == null || (! Visitors.isDot(twoTokensAgo))) &&
            (! Visitors.isLParen(n)));
  }

}
