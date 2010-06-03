package daikon.split;

import jtb.JavaParser;
import jtb.JavaParserConstants;
import jtb.ParseException;
import java.io.*;
import jtb.syntaxtree.*;
import jtb.visitor.*;

/**
 * This class consists solely of static methods that are useful when
 * working with jtb syntax tree visitors.
 */
class Visitors implements JavaParserConstants {
  private Visitors() { throw new Error("do not instantiate"); }

  /**
   * Returns the root of the JBT syntax tree for expression.
   * @param expression a valid java expression.
   * @throws ParseException if expression is not a valid java expression.
   */
  public static Node getJtbTree(String expression)
    throws ParseException {
    class ExpressionExtractor extends DepthFirstVisitor {
      private Node expressionNode;
      public void visit(VariableInitializer n) {
        expressionNode = n.f0;
      }
    }
    String expressionClass = "class c { bool b = " + expression + "; }";
    Reader input = new StringReader(expressionClass);
    JavaParser parser = new JavaParser(input);
    Node root = parser.CompilationUnit();
    ExpressionExtractor expressionExtractor = new ExpressionExtractor();
    root.accept(expressionExtractor);
    return expressionExtractor.expressionNode;
  }

  /**
   * Returns whether n represents the java reserved word "this".
   */
  public static boolean isThis(NodeToken n) {
    return n.kind == THIS;
  }

  /**
   * Returns whether n represents a left bracket, "[".
   */
  public static boolean isLBracket(NodeToken n) {
    return n.kind == LBRACKET;
  }

  /**
   * Returns whether n represents a dot, ".".
   */
  public static boolean isDot(NodeToken n) {
    return n.kind == DOT;
  }

  /**
   * Returns whether n represents a java identifier.
   */
  public static boolean isIdentifier(NodeToken n) {
    return n.kind == IDENTIFIER;
  }

  /**
   * Returns whether n represents a left parenthesis, "(".
   */
  public static boolean isLParen(NodeToken n) {
    return n.kind == LPAREN;
  }

  /**
   * Returns whether n represents the java reserved word "null".
   */
  public static boolean isNull(NodeToken n) {
    return n.kind == NULL;
  }

}
