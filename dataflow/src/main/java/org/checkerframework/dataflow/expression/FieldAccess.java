package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.code.Symbol;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

public class FieldAccess extends JavaExpression {
  protected final JavaExpression receiver;
  protected final VariableElement field;

  public JavaExpression getReceiver() {
    return receiver;
  }

  public VariableElement getField() {
    return field;
  }

  /**
   * Create a {@code FieldAccess}.
   *
   * @param receiver receiver of the field access
   * @param node FieldAccessNode
   */
  public FieldAccess(JavaExpression receiver, FieldAccessNode node) {
    this(receiver, node.getType(), node.getElement());
  }

  /**
   * Create a {@code FieldAccess}.
   *
   * @param receiver receiver of the field access
   * @param fieldElement element of the field
   */
  public FieldAccess(JavaExpression receiver, VariableElement fieldElement) {
    this(receiver, fieldElement.asType(), fieldElement);
  }

  /**
   * Create a {@code FieldAccess}.
   *
   * @param receiver receiver of the field access
   * @param type type of the field
   * @param fieldElement element of the field
   */
  public FieldAccess(JavaExpression receiver, TypeMirror type, VariableElement fieldElement) {
    super(type);
    this.receiver = receiver;
    this.field = fieldElement;
    String fieldName = fieldElement.toString();
    if (fieldName.equals("class") || fieldName.equals("this")) {
      Error e =
          new Error(
              String.format(
                  "bad field name \"%s\" in new FieldAccess(%s, %s, %s)%n",
                  fieldName, receiver, type, fieldElement));
      e.printStackTrace(System.out);
      e.printStackTrace(System.err);
      throw e;
    }
  }

  public boolean isFinal() {
    return ElementUtils.isFinal(field);
  }

  public boolean isStatic() {
    return ElementUtils.isStatic(field);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof FieldAccess)) {
      return false;
    }
    FieldAccess fa = (FieldAccess) obj;
    return fa.getField().equals(getField()) && fa.getReceiver().equals(getReceiver());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getField(), getReceiver());
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof FieldAccess)) {
      return false;
    }
    FieldAccess other = (FieldAccess) je;
    return this.receiver.syntacticEquals(other.receiver) && this.field.equals(other.field);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return syntacticEquals(other) || receiver.containsSyntacticEqualJavaExpression(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return super.containsModifiableAliasOf(store, other)
        || receiver.containsModifiableAliasOf(store, other);
  }

  @Override
  public String toString() {
    if (receiver instanceof ClassName) {
      return receiver.getType() + "." + field;
    } else {
      return receiver + "." + field;
    }
  }

  @Override
  public String toStringDebug() {
    return String.format(
        "FieldAccess(type=%s, receiver=%s, field=%s [%s] [%s] owner=%s)",
        type,
        receiver.toStringDebug(),
        field,
        field.getClass().getSimpleName(),
        System.identityHashCode(field),
        ((Symbol) field).owner);
  }

  @Override
  public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
    return getClass() == clazz || receiver.containsOfClass(clazz);
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return receiver.isDeterministic(provider);
  }

  @Override
  public boolean isUnassignableByOtherCode() {
    return isFinal() && getReceiver().isUnassignableByOtherCode();
  }

  @Override
  public boolean isUnmodifiableByOtherCode() {
    return isUnassignableByOtherCode() && TypesUtils.isImmutableTypeInJdk(getReceiver().type);
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitFieldAccess(this, p);
  }
}
