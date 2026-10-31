package org.checkerframework.dataflow.expression;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.plumelib.javacparse.JavacParse;
import org.plumelib.util.StringsP;

/**
 * Helper methods to parse a string that represents a restricted Java expression.
 *
 * @checker_framework.manual #java-expressions-as-arguments Writing Java expressions as annotation
 *     arguments
 * @checker_framework.manual #dependent-types Annotations whose argument is a Java expression
 *     (dependent type annotations)
 */
public final class JavaExpressionParseUtil {

  /** Do not instantiate. */
  private JavaExpressionParseUtil() {
    throw new Error("Do not instantiate");
  }

  /** Regular expression for a formal parameter use. */
  static final String PARAMETER_REGEX = "#([1-9][0-9]*)";

  /** Pattern for a formal parameter use in "#2" syntax. */
  static final Pattern PARAMETER_PATTERN = Pattern.compile("#(\\d+)");

  /**
   * Parsable replacement for formal parameter references. It is parsable because it is a Java
   * identifier.
   */
  static final String PARAMETER_PREFIX = "_param_";

  /** The length of {@link #PARAMETER_PREFIX}. */
  static final int PARAMETER_PREFIX_LENGTH = PARAMETER_PREFIX.length();

  /** The replacement for a formal parameter in "#2" syntax. */
  static final String PARAMETER_REPLACEMENT = PARAMETER_PREFIX + "$1";

  /**
   * Parses a string to a {@link JavaExpression}.
   *
   * @param expression the string expression to parse
   * @param enclosingType type of the class that encloses the JavaExpression
   * @param thisReference the JavaExpression to which to parse "this", or null if "this" should not
   *     appear in the expression; not relevant to qualified "SomeClass.this" or
   *     "package.SomeClass.this"
   * @param parameters list of JavaExpressions to which to parse formal parameter references such as
   *     "#2", or null if formal parameter references should not appear in the expression
   * @param localVarPath if non-null, the expression is parsed as if it were written at this
   *     location; affects only parsing of local variables
   * @param pathToCompilationUnit required to use the underlying Javac API
   * @param env the processing environment
   * @return {@code expression} as a {@code JavaExpression}
   * @throws JavaExpressionParseException if the string cannot be parsed
   */
  public static JavaExpression parse(
      String expression,
      TypeMirror enclosingType,
      @Nullable ThisReference thisReference,
      @Nullable List<FormalParameter> parameters,
      @Nullable TreePath localVarPath,
      TreePath pathToCompilationUnit,
      ProcessingEnvironment env)
      throws JavaExpressionParseException {

    String expressionWithParameterNames =
        StringsP.replaceAll(expression, PARAMETER_PATTERN, PARAMETER_REPLACEMENT);
    ExpressionTree exprTree;
    try {
      exprTree = JavacParse.parseExpression(expressionWithParameterNames);
    } catch (IllegalArgumentException e) {
      @SuppressWarnings("nullness:assignment") // presently always non-null; could change in future
      @NonNull String msg = e.getMessage();
      throw JavaExpressionParseException.construct(expression, msg);
    }

    JavaExpression result =
        ExpressionTreeToJavaExpressionVisitor.convert(
            exprTree,
            enclosingType,
            thisReference,
            parameters,
            localVarPath,
            pathToCompilationUnit,
            env);

    if (result instanceof ClassName && !expression.endsWith(".class")) {
      throw JavaExpressionParseException.construct(
          expression,
          String.format(
              "a class name cannot terminate a Java expression string, where result=%s [%s]",
              result, result.getClass()));
    }
    return result;
  }

  /**
   * If {@code s} is exactly a formal parameter, return its 1-based index. Returns -1 otherwise.
   *
   * @param s a Java expression
   * @return the 1-based index of the formal parameter that {@code s} represents, or -1
   */
  public static int parameterIndex(String s) {
    Matcher matcher = PARAMETER_PATTERN.matcher(s);
    if (matcher.matches()) {
      @SuppressWarnings(
          "nullness:assignment") // group 1 is non-null due to the structure of the regex
      @NonNull String group1 = matcher.group(1);
      return Integer.parseInt(group1);
    }
    return -1;
  }

  /**
   * Returns every simple name in {@code expression} that might denote a local variable. That is,
   * every identifier other than a method name or a name selected from a qualifier: in {@code
   * a.b.m(c)}, the result is {"a", "c"}. The result may include names that denote classes or
   * fields; it is an over-approximation of the local variables that {@code expression} mentions.
   * Formal parameter references in "#2" syntax are not included.
   *
   * @param expression a Java expression
   * @return the simple names in {@code expression} that might denote a local variable, or null if
   *     {@code expression} cannot be parsed
   */
  public static @Nullable Set<String> possibleLocalVariableNames(String expression) {
    String expressionWithParameterNames =
        StringsP.replaceAll(expression, PARAMETER_PATTERN, PARAMETER_REPLACEMENT);
    ExpressionTree exprTree;
    try {
      exprTree = JavacParse.parseExpression(expressionWithParameterNames);
    } catch (IllegalArgumentException e) {
      return null;
    }
    Set<String> result = new HashSet<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void p) {
        String name = tree.getName().toString();
        if (!name.startsWith(PARAMETER_PREFIX)) {
          result.add(name);
        }
        return null;
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
        // Do not scan an unqualified method name, which is an IdentifierTree.
        if (tree.getMethodSelect() instanceof MemberSelectTree methodSelect) {
          scan(methodSelect.getExpression(), p);
        }
        scan(tree.getTypeArguments(), p);
        scan(tree.getArguments(), p);
        return null;
      }
    }.scan(exprTree, null);
    return result;
  }
}
