package daikon.split;

import java.util.*;
import jtb.syntaxtree.*;
import jtb.ParseException;
import jtb.visitor.*;
import daikon.tools.jtb.*;

/**
 * TokenReplacer is a jtb syntax tree visitor that replaces a given set of
 * tokens that are names of a variable with another set of tokens.  Note that
 * it only replaces tokens that represent name of variables and have no prefixes.
 * For example the with the expression "i + this.i + someClass.i + i(i)" where
 * i is to be replaced with j, the following expression would be produced:
 * "j + this.i + someClass.i + i(j)".
 */
class TokenReplacer extends DepthFirstVisitor {

  /** The old variable names that should be replaced by the new variable names. */
  private List<String> oldVars;

  /** The new variable names with which the old variable names should be replaced. */
  private List<String> newVars;

  // newVars and oldVars must be order s.t. ith element of newVars is the
  // the replacement for the ith element of oldVars.

  /** the last token visited. */
  private NodeToken lastToken;

  /** the token visited before lastToken. */
  private NodeToken twoTokensAgo;

  /**
   * Creates a new TokenReplacer with ith element of oldVars being
   * replaced with ith element of new newVars.
   * @param oldVars the variable names, as Strings, that should
   *  be replaced by newVars.
   * @param newVars the variable names, as Strings, that oldVars
   *  are replaced with.
   */
  private TokenReplacer(List<String> oldVars, List<String> newVars) {
    super();
    this.oldVars = oldVars;
    this.newVars = newVars;
  }

  /**
   * Replaces all the instances of an element oldVars in expression with
   * the corresponding element of newVars.  The ith element of oldVars
   * will be replaced by the ith element of newVars.
   * @param expression a segment of valid java code in which instances of
   *  oldVars should be replaced by the corresponding element of newVars.
   * @param oldVars the variable names, as Strings, that should
   *  be replaced by newVars.
   * @param newVars the variable names, as Strings, that oldVars
   *  are replaced with.
   * @return expression with all instances of an element of oldVars replaced
   *  by the corresponding element of newVars.
   */
  public static String replaceTokens(String expression, List<String> oldVars, List<String> newVars)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    TokenReplacer tokenReplacer = new TokenReplacer(oldVars, newVars);
    root.accept(tokenReplacer);
    tokenReplacer.replaceLastToken();
    return Ast.format(root);
  }

  /**
   * Replaces lastToken if needed.
   */
  private void replaceLastToken() {
    if (Visitors.isIdentifier(lastToken) &&
        (twoTokensAgo == null || (! Visitors.isDot(twoTokensAgo)))) {
      for (int i = 0; i < oldVars.size(); i++) {
        if (lastToken.tokenImage.equals(oldVars.get(i))) {
          lastToken.tokenImage = newVars.get(i);
          break;
        }
      }
    }
  }

  /**
   * This method should not be directly used by users of this class;
   * however, it must be public to full-fill the Visitor interface.
   * If lastToken.tokenImage is a complete variable name and an
   * element of  oldVars, lastToken.tokenImage is replaced by the
   * corresponding element of newVars.  The beginColumn and endColumn
   * of all NodeTokens are set to -1 to ensure that Ast printing exceptions
   * are not thrown from the lengths of tokens changing.
   */
  public void visit(NodeToken n) {
    if (Visitors.isLParen(n) &&
        lastToken != null &&
        Visitors.isIdentifier(lastToken) &&
        (twoTokensAgo == null || (! Visitors.isDot(twoTokensAgo)))) {
      for (int i = 0; i < oldVars.size(); i++) {
        if (lastToken.tokenImage.equals(oldVars.get(i))) {
          lastToken.tokenImage = newVars.get(i);
          break;
        }
      }
    }
    n.beginColumn = -1;
    n.endColumn = -1;
    twoTokensAgo = lastToken;
    lastToken = n;
    super.visit(n);
  }


}
