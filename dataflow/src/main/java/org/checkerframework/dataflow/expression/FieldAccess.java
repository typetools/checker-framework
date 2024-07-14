package org.checkerframework.dataflow.expression;

import com.sun.tools.javac.code.Symbol;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A FieldAccess represents a field access. It does not represent a class literal such as {@code
 * SomeClass.class} or {@code int[].class}.
 */
public class FieldAccess extends JavaExpression {
  /** The receiver of the field access. */
  protected final JavaExpression receiver;

  /** The field being accessed. */
  protected final VariableElement field;

  /**
   * Returns the receiver.
   *
   * @return the receiver
   */
  public JavaExpression getReceiver() {
    return receiver;
  }

  /**
   * Returns the field.
   *
   * @return the field
   */
  public VariableElement getField() {
    return field;
  }

  /**
   * Create a {@code FieldAccess}.
   *
   * @param receiver receiver of the field access
   * @param node the FieldAccessNode
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
      BugInCF e =
          new BugInCF(
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
    if (!fa.getField().equals(getField())) {
      return false;
    }

    if (fa.getReceiver().equals(getReceiver())) {
      return true;
    }

    return (fa.getReceiver() instanceof SuperReference || fa.getReceiver() instanceof ThisReference)
        && (this.getReceiver() instanceof SuperReference
            || this.getReceiver() instanceof ThisReference);
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
    String receiverString =
        (receiver instanceof ClassName) ? receiver.getType().toString() : receiver.toString();
    if (Node.disambiguateOwner) {
      return receiverString + "." + field + "{owner=" + ((Symbol) field).owner + "}";
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

  @SuppressWarnings("unchecked") // generic cast
  @Override
  public <T extends JavaExpression> @Nullable T containedOfClass(Class<T> clazz) {
    if (getClass() == clazz) {
      return (T) this;
    }
    return receiver.containedOfClass(clazz);
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return receiver.isDeterministic(provider);
  }

  @Override
  public boolean isAssignableByOtherCode() {
    return !isFinal() || getReceiver().isAssignableByOtherCode();
  }

  @Override
  public boolean isModifiableByOtherCode() {
    return isAssignableByOtherCode() || !TypesUtils.isImmutableTypeInJdk(getReceiver().type);
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitFieldAccess(this, p);
  }
}
