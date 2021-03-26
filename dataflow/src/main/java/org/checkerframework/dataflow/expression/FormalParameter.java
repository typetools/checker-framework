package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;

/**
 * A formal parameter, represented by its 1-based index.
 *
 * <p>{@link LocalVariable} represents a formal parameter expressed using its name.
 */
public class FormalParameter extends JavaExpression {

  /** The 1-based index. */
  protected final int index;

  /** The element for this formal parameter. */
  protected final VariableElement element;

  /**
   * Creates a FormalParameter.
   *
   * @param index the 1-based index
   * @param element the element for the formal parameter
   */
  public FormalParameter(int index, VariableElement element) {
    super(ElementUtils.getType(element));
    this.index = index;
    this.element = element;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof FormalParameter)) {
      return false;
    }

    FormalParameter other = (FormalParameter) obj;
    return this.index == other.index && LocalVariable.sameElement(this.element, other.element);
  }

  /**
   * Returns the 1-based index of this formal parameter.
   *
   * @return the 1-based index of this formal parameter
   */
  public int getIndex() {
    return index;
  }

  /**
   * Returns the element for this variable.
   *
   * @return the element for this variable
   */
  public VariableElement getElement() {
    return element;
  }

  @Override
  public int hashCode() {
    VarSymbol vs = (VarSymbol) element;
    return Objects.hash(
        index,
        vs.name.toString(),
        TypeAnnotationUtils.unannotatedType(vs.type).toString(),
        vs.owner.toString());
  }

  @Override
  public String toString() {
    return "#" + index;
  }

  @Override
  public String toStringDebug() {
    return super.toStringDebug()
        + " [element="
        + element
        + ", owner="
        + ((VarSymbol) element).owner
        + "]";
  }

  @Override
  public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
    return getClass() == clazz;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof FormalParameter)) {
      return false;
    }
    FormalParameter other = (FormalParameter) je;
    return index == other.index;
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return syntacticEquals(other);
  }

  @Override
  public boolean isUnassignableByOtherCode() {
    return true;
  }

  @Override
  public boolean isUnmodifiableByOtherCode() {
    return true;
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return true;
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitFormalParameter(this, p);
  }
}
