package org.checkerframework.dataflow.expression;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.javacparse.JavacParse;
import org.checkerframework.framework.util.javacparse.JavacParseResult;
import org.plumelib.util.StringsPlume;

/**
 * Helper methods to parse a string that represents a restricted Java expression.
 *
 * @checker_framework.manual #java-expressions-as-arguments Writing Java expressions as annotation
 *     arguments
 * @checker_framework.manual #dependent-types Annotations whose argument is a Java expression
 *     (dependent type annotations)
 */
public class JavaExpressionParseUtil {

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
   * <p>For most uses, clients should call one of the static methods in {@link
   * StringToJavaExpression} rather than calling this method directly.
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
        StringsPlume.replaceAll(expression, PARAMETER_PATTERN, PARAMETER_REPLACEMENT);
    ExpressionTree exprTree;
    try {
      JavacParseResult<ExpressionTree> jpr =
          JavacParse.parseExpression(expressionWithParameterNames);
      if (jpr.hasParseError()) {
        throw JavaExpressionParseException.construct(expression, jpr.getParseErrorMessages());
      }
      exprTree = jpr.getTree();
    } catch (IllegalArgumentException e) {
      throw JavaExpressionParseException.construct(expression, e.getMessage());
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
}
