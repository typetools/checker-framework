package org.checkerframework.dataflow.expression;

import java.util.Objects;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A ClassName represents either a class literal or the occurrence of a class as part of a static
 * field access or static method invocation.
 */
public class ClassName extends JavaExpression {
  /** The string representation of the raw type of this. */
  private final String typeString;

  /**
   * Creates a new ClassName object for the given type.
   *
   * @param type the type for this ClassName: declared, primitive, type variable, an array of one of
   *     them, or void
   */
  public ClassName(TypeMirror type) {
    super(type);
    TypeMirror baseType;
    TypeKind baseKind = type.getKind();
    if (baseKind != TypeKind.ARRAY) {
      baseType = type;
    } else {
      baseType = TypesUtils.getInnermostComponentType((ArrayType) type);
      baseKind = baseType.getKind();
    }
    // Primitives, arrays, and void are permitted because ClassName can represent class literals.
    // (An alternate design would use a different JavaExpression for class literals.)
    // TYPEVAR and arrays are permitted for static method invocations.
    if (!(baseKind == TypeKind.DECLARED
        || baseKind.isPrimitive()
        || baseKind == TypeKind.VOID
        || baseKind == TypeKind.TYPEVAR)) {
      throw new Error(type + " is " + type.getKind());
    }
    String typeString = type.toString();
    if (typeString.endsWith(">")) {
      typeString = typeString.substring(0, typeString.indexOf("<"));
    }
    this.typeString = typeString;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof ClassName)) {
      return false;
    }
    ClassName other = (ClassName) obj;
    return typeString.equals(other.typeString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(typeString);
  }

  @Override
  public String toString() {
    return typeString + ".class";
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
    return true;
  }

  @Override
  public boolean syntacticEquals(JavaExpression je) {
    if (!(je instanceof ClassName)) {
      return false;
    }
    ClassName other = (ClassName) je;
    return typeString.equals(other.typeString);
  }

  @Override
  public boolean containsSyntacticEqualJavaExpression(JavaExpression other) {
    return this.syntacticEquals(other);
  }

  @Override
  public boolean containsModifiableAliasOf(Store<?> store, JavaExpression other) {
    return false; // not modifiable
  }

  @Override
  public <R, P> R accept(JavaExpressionVisitor<R, P> visitor, P p) {
    return visitor.visitClassName(this, p);
  }
}
