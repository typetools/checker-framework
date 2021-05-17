package org.checkerframework.dataflow.expression;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

public class ThisReference extends JavaExpression {
  /** The printed representation of the type of this. */
  String typeToString;

  public ThisReference(TypeMirror type) {
    super(type);
    typeToString = type.toString();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof ThisReference
        // TypeMirrors should be compared using Types.isSameType(), but no Types instance is
        // available here.
        && typeToString.equals(((ThisReference) obj).typeToString);
  }

  @Override
  public int hashCode() {
    return typeToString.hashCode();
  }

  @Override
  public String toString() {
    // TODO: This should include the type.
    return "this";
  }

  @Override
  public boolean containsOfClass(Class<? extends JavaExpression> clazz) {
    return getClass() == clazz;
  }

  @Override
  public boolean isDeterministic(AnnotationProvider provider) {
    return true;
  }

  @Override
  public boolean isUnassignableByOtherCode() {
    return true;
  }

  @Override
  public boolean isUnmodifiableByOtherCode() {
    return TypesUtils.isImmutableTypeInJdk(type);
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    return this.equals(je);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return false; // 'this' is not modifiable
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitThisReference(this, p);
  }
}
