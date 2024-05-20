package org.checkerframework.dataflow.expression;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A <a href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27">Java Lambda
 * expression</a>.
 */
public class Lambda extends JavaExpression {

  /** The formal parameters of this lambda expression. */
  protected List<LocalVariable> parameters;

  /**
   * The body of this lambda expression.
   *
   * <p>The <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-LambdaBody">body</a>
   * of a lambda expression can either be an <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-Expression">expression</a>
   * or <a
   * href="https://docs.oracle.com/javase/specs/jls/se21/html/jls-14.html#jls-Block">block</a>.
   */
  protected Tree body;

  /**
   * Creates a new Lambda expression.
   *
   * @param type the type of the lamdba expression, which is a functional interface type
   * @param parameters the formal parameters of the lambda expression
   * @param body the body of the lambda expression
   */
  public Lambda(TypeMirror type, List<LocalVariable> parameters, Tree body) {
    super(type);
    this.parameters = parameters;
    this.body = body;
  }

  /**
   * Returns the parameter(s) for this lambda expression.
   *
   * @return the parameter(s) for this lambda expression
   */
  @Pure
  public List<LocalVariable> getParameters() {
    return parameters;
  }

  /**
   * Returns the body for this lambda expression.
   *
   * @return the body for this lambda expression
   */
  @Pure
  public Tree getBody() {
    return body;
  }

  @SuppressWarnings("unchecked") // Generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    if (getClass() == clazz) {
      return (T) this;
    }
    T result = null;
    for (JavaExpression parameter : parameters) {
      result = parameter.containedOfClass(clazz);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Creates a list of {@link LocalVariable} from the parameters of the given {@link
   * LambdaExpressionTree}.
   *
   * @param lambdaTree a lambda expression tree
   * @return a list of {@link LocalVariable} from the parameters of the lambda expression tree
   */
  public static List<LocalVariable> createLambdaParameters(LambdaExpressionTree lambdaTree) {
    return lambdaTree.getParameters().stream()
        .map(TreeUtils::elementFromDeclaration)
        .map(LocalVariable::new)
        .collect(Collectors.toList());
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    // Conservative; cannot think of an easy way to determine whether the lambda is deterministic.
    // One way is via {@code PurityChecker.checkPurity}, but we do not have access to whether
    // certain flags are set or not.
    return false;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof Lambda)) {
      return false;
    }
    Lambda other = (Lambda) je;
    return body.equals(other.body)
        && JavaExpression.syntacticEqualsList(parameters, other.getParameters());
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return syntacticEquals(other)
        || JavaExpression.listContainsSyntacticEqualJavaExpression(parameters, other);
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitLambda(this, p);
  }

  @Override
  public String toString() {
    List<String> parameterNames =
        parameters.stream().map(JavaExpression::toString).collect(Collectors.toList());
    String commaSeparatedParameterNames = String.join(", ", parameterNames);
    return "(" + commaSeparatedParameterNames + ") -> " + body;
  }
}
