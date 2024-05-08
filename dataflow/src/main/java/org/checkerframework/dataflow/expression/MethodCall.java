package org.checkerframework.dataflow.expression;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.javacutil.AnnotationProvider;

/** A call to a @Deterministic method. */
public class MethodCall extends JavaExpression {

  /** The method being called. */
  protected final ExecutableElement method;

  /** The receiver argument. */
  protected final JavaExpression receiver;

  /** The arguments. */
  protected final List<JavaExpression> arguments;

  /**
   * Creates a new MethodCall.
   *
   * @param type the type of the method call
   * @param method the method being called
   * @param receiver the receiver argument
   * @param arguments the arguments
   */
  public MethodCall(
      TypeMirror type,
      ExecutableElement method,
      JavaExpression receiver,
      List<JavaExpression> arguments) {
    super(type);
    this.receiver = receiver;
    this.arguments = arguments;
    this.method = method;
  }

  /**
   * Returns the ExecutableElement for the method call.
   *
   * @return the ExecutableElement for the method call
   */
  public ExecutableElement getElement() {
    return method;
  }

  /**
   * Returns the method call receiver (for inspection only - do not modify).
   *
   * @return the method call receiver (for inspection only - do not modify)
   */
  public JavaExpression getReceiver() {
    return receiver;
  }

  /**
   * Returns the method call arguments (for inspection only - do not modify any of the arguments).
   *
   * @return the method call arguments (for inspection only - do not modify any of the arguments)
   */
  public List<JavaExpression> getArguments() {
    return Collections.unmodifiableList(arguments);
  }

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {

    if (getClass() == clazz) {
      return (T) this;
    }

    T result = receiver.containedOfClass(clazz);
    if (result != null) {
      return result;
    }
    for (JavaExpression p : arguments) {
      result = p.containedOfClass(clazz);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return (PurityUtils.isDeterministic(provider, method) || provider.isDeterministic(method))
        && listIsDeterministic(arguments, provider);
  }

  @Override
  public boolean isAssignableByOtherCode() {
    // TODO: The following comment is no longer accurate.  It should be removed and the
    // implementation changed.
    // There is no need to check that the method is deterministic, because a MethodCall is
    // only created for deterministic methods.
    return receiver.isModifiableByOtherCode()
        || arguments.stream().anyMatch(JavaExpression::isModifiableByOtherCode);
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return isAssignableByOtherCode();
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof MethodCall)) {
      return false;
    }
    MethodCall other = (MethodCall) je;
    return method.equals(other.method)
        && this.receiver.syntacticEquals(other.receiver)
        && JavaExpression.syntacticEqualsList(this.arguments, other.arguments);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return syntacticEquals(other)
        || receiver.containsSyntacticEqualJavaExpression(other)
        || JavaExpression.listContainsSyntacticEqualJavaExpression(arguments, other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    if (receiver.containsModifiableAliasOf(store, other)) {
      return true;
    }
    for (JavaExpression p : arguments) {
      if (p.containsModifiableAliasOf(store, other)) {
        return true;
      }
    }
    return false; // the method call itself is not modifiable
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MethodCall)) {
      return false;
    }
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      return false;
    }
    MethodCall other = (MethodCall) obj;
    return method.equals(other.method)
        && receiver.equals(other.receiver)
        && arguments.equals(other.arguments);
  }

  @Override
  public int hashCode() {
    if (method.getKind() == ElementKind.CONSTRUCTOR) {
      return System.identityHashCode(this);
    }
    return Objects.hash(method, receiver, arguments);
  }

  @Override
  public String toString() {
    StringBuilder preParen = new StringBuilder();
    if (receiver instanceof ClassName) {
      preParen.append(receiver.getType());
    } else {
      preParen.append(receiver);
    }
    preParen.append(".");
    String methodName = method.getSimpleName().toString();
    preParen.append(methodName);
    preParen.append("(");
    StringJoiner result = new StringJoiner(", ", preParen, ")");
    for (JavaExpression argument : arguments) {
      result.add(argument.toString());
    }
    return result.toString();
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitMethodCall(this, p);
  }
}
