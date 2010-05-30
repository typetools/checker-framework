package daikon.split;

import java.util.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;
import jtb.ParseException;

/**
 * TokenExtractor is a visitor for a jtb syntax tree that returns all the
 * tokens from a expression in the order they appear in the expression.
 * For example on "x > someMethod(i[3])" would yield an array of the
 * following elements: x, >, someMethod,(, i, [, 3, ], ).
 **/
class TokenExtractor extends DepthFirstVisitor {

  /** The tokens of expression. */
  private List<NodeToken> tokens = new ArrayList<NodeToken>();

  /** blocks public constructor. */
  private TokenExtractor() {
    super();
    }

  /**
   * Extracts all the tokens from expression.
   * @param expression a valid segment of java code from which expression
   *  should be extracted.
   * @throws ParseException when expression is not a valid segment of
   *    java code.
   * @return all the tokens of expression.
   */
  public static NodeToken[] extractTokens(String expression)
    throws ParseException {
    Node root = Visitors.getJtbTree(expression);
    TokenExtractor extractor = new TokenExtractor();
    root.accept(extractor);
    return extractor.tokens.toArray(new NodeToken[0]);
  }

  /**
   * Extracts all the tokens from expression whose jtb syntax tree is
   * rooted at root.
   * @param root a jtb syntax tree.
   * @return all the tokens in the tree rooted at root.
   */
  public static NodeToken[] extractTokens(Node root) {
    TokenExtractor extractor = new TokenExtractor();
    root.accept(extractor);
    return extractor.tokens.toArray(new NodeToken[0]);
  }

  /**
   * This method should not be used directly by users of this class.
   * If presently visiting expression, adds n to tokens.
   */
  public void visit(NodeToken n) {
      tokens.add(n);
      super.visit(n);
  }

}
