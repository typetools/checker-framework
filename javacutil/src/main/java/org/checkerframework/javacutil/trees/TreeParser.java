package org.checkerframework.javacutil.trees;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;
import java.util.StringTokenizer;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * A utility class for parsing Java expression snippets, and converting them to proper Javac AST
 * nodes.
 *
 * <p>This is useful for parsing {@code EnsuresNonNull*}, and {@code KeyFor} values.
 *
 * <p>Currently, it handles five tree types only:
 *
 * <ul>
 *   <li>Identifier tree (e.g. {@code id})
 *   <li>Literal tree (e.g. 2, 3)
 *   <li>Method invocation tree (e.g. {@code method(2, 3)})
 *   <li>Member select tree (e.g. {@code Class.field}, {@code instance.method()})
 *   <li>Array access tree (e.g. {@code array[id]})
 * </ul>
 *
 * Notable limitation: Doesn't handle spaces, or non-method-argument parentheses.
 *
 * <p>It's implemented via a Recursive-Descent parser.
 */
public class TreeParser {
  /** Valid delimiters. */
  private static final String DELIMS = ".[](),";

  /** A sentinel value. */
  private static final String SENTINEL = "";

  /** The TreeMaker instance. */
  private final TreeMaker maker;

  /** The names instance. */
  private final Names names;

  /**
   * Creates a TreeParser.
   *
   * @param env the processing environment
   */
  public TreeParser(ProcessingEnvironment env) {
    Context context = ((JavacProcessingEnvironment) env).getContext();
    maker = TreeMaker.instance(context);
    names = Names.instance(context);
  }

  /**
   * Parses the snippet in the string as an internal Javac AST expression node.
   *
   * @param s the Java snippet
   * @return the AST corresponding to the snippet
   */
  public ExpressionTree parseTree(String s) {
    StringTokenizer tokenizer = new StringTokenizer(s, DELIMS, true);
    String token = tokenizer.nextToken();

    try {
      return parseExpression(tokenizer, token).expression();
    } catch (Exception e) {
      throw new ParseError(e);
    }
  }

  /**
   * Returns the next token from the tokenizer, or the {@code SENTINEL} if none is available.
   *
   * @param tokenizer the tokenizer to read from
   * @return the next token, or {@code SENTINEL}
   */
  private String nextToken(StringTokenizer tokenizer) {
    return tokenizer.hasMoreTokens() ? tokenizer.nextToken() : SENTINEL;
  }

  /**
   * Returns the parsed expression tree for the given token.
   *
   * @param token a token that is a literal or an identifier
   * @return the parsed expression tree for the token
   */
  private JCExpression fromToken(String token) {
    // Optimization
    if ("true".equals(token)) {
      return maker.Literal(true);
    } else if ("false".equals(token)) {
      return maker.Literal(false);
    }

    if (Character.isLetter(token.charAt(0))) {
      return maker.Ident(names.fromString(token));
    }

    Object value;
    try {
      value = Integer.valueOf(token);
    } catch (Exception e2) {
      try {
        value = Double.valueOf(token);
      } catch (Exception ef) {
        throw new Error("Can't parse as integer or double: " + token);
      }
    }
    return maker.Literal(value);
  }

  /**
   * A parsed expression and the token that follows it.
   *
   * @param expression the parsed expression
   * @param nextToken the token that follows the expression
   */
  private record ExpressionAndNextToken(JCExpression expression, String nextToken) {}

  /**
   * Parses an expression.
   *
   * @param tokenizer the tokenizer
   * @param token the first token
   * @return a parsed expression and the next token
   */
  private ExpressionAndNextToken parseExpression(StringTokenizer tokenizer, String token) {
    JCExpression tree = fromToken(token);

    while (tokenizer.hasMoreTokens()) {
      String delim = nextToken(tokenizer);
      token = delim;
      if (".".equals(delim)) {
        token = nextToken(tokenizer);
        tree = maker.Select(tree, names.fromString(token));
      } else if ("(".equals(delim)) {
        token = nextToken(tokenizer);
        ListBuffer<JCExpression> args = new ListBuffer<>();
        while (!")".equals(token)) {
          ExpressionAndNextToken p = parseExpression(tokenizer, token);
          JCExpression arg = p.expression();
          token = p.nextToken();
          args.append(arg);
          if (",".equals(token)) {
            token = nextToken(tokenizer);
          }
        }
        // For now, handle empty args only
        assert ")".equals(token) : "Unexpected token: " + token;
        tree = maker.Apply(List.nil(), tree, args.toList());
      } else if ("[".equals(token)) {
        token = nextToken(tokenizer);
        ExpressionAndNextToken p = parseExpression(tokenizer, token);
        JCExpression index = p.expression();
        token = p.nextToken();
        assert "]".equals(token) : "Unexpected token: " + token;
        tree = maker.Indexed(tree, index);
      } else {
        return new ExpressionAndNextToken(tree, token);
      }
      assert tokenizer != null : "@AssumeAssertion(nullness): side effects";
    }

    return new ExpressionAndNextToken(tree, token);
  }

  /** An internal error. */
  private static class ParseError extends RuntimeException {
    /** The serial version UID. */
    private static final long serialVersionUID = 1887754619522101929L;

    /**
     * Creates a ParseError.
     *
     * @param cause the underlying exception
     */
    ParseError(Throwable cause) {
      super(cause);
    }
  }
}
