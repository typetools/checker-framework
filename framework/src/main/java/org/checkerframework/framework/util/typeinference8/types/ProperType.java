package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A type that does not contain any inference variables. */
public class ProperType extends AbstractType {

  /** The annotated type mirror. */
  private final AnnotatedTypeMirror type;

  /** The Java type. */
  private final TypeMirror properType;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  private final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param properType the java type
   * @param context the context
   */
  public ProperType(
      AnnotatedTypeMirror type, TypeMirror properType, Java8InferenceContext context) {
    this(type, properType, AnnotationMirrorMap.emptyMap(), context, false);
  }

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param properType the java type
   * @param context the context
   * @param ignoreAnnotations whether the annotations on this type should be ignored
   */
  public ProperType(
      AnnotatedTypeMirror type,
      TypeMirror properType,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    this(type, properType, AnnotationMirrorMap.emptyMap(), context, ignoreAnnotations);
  }

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param properType the java type
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations whether the annotations on this type should be ignored
   */
  public ProperType(
      AnnotatedTypeMirror type,
      TypeMirror properType,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    super(context, ignoreAnnotations);
    this.properType = properType;
    this.type = type;
    this.qualifierVars = qualifierVars;
    verifyTypeKinds(type, properType);
  }

  /**
   * Creates a proper type from the type of the expression.
   *
   * @param tree an expression tree
   * @param context the context
   */
  public ProperType(ExpressionTree tree, Java8InferenceContext context) {
    super(context, false);
    this.type = context.typeFactory.getAnnotatedType(tree);
    this.properType = type.getUnderlyingType();
    this.qualifierVars = AnnotationMirrorMap.emptyMap();
    verifyTypeKinds(type, properType);
  }

  /**
   * Creates a proper type from the type of the variable.
   *
   * @param varTree a variable tree
   * @param context the context
   */
  public ProperType(VariableTree varTree, Java8InferenceContext context) {
    super(context, false);
    this.type = context.typeFactory.getAnnotatedType(varTree);
    this.properType = TreeUtils.typeOf(varTree);
    this.qualifierVars = AnnotationMirrorMap.emptyMap();
    verifyTypeKinds(type, properType);
  }

  /**
   * Asserts that the underlying type of {@code atm} is the same kind as {@code typeMirror}.
   *
   * @param atm annotated type mirror
   * @param typeMirror java type
   */
  private static void verifyTypeKinds(AnnotatedTypeMirror atm, TypeMirror typeMirror) {
    assert typeMirror != null && typeMirror.getKind() != TypeKind.VOID && atm != null;

    if (typeMirror.getKind() != atm.getKind()) {
      //      throw new BugInCF("type: %s annotated type: %s", typeMirror,
      // atm.getUnderlyingType());
    }
  }

  @Override
  public Kind getKind() {
    return Kind.PROPER;
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type, boolean ignoreAnnotations) {
    return new ProperType(atm, type, qualifierVars, context, ignoreAnnotations);
  }

  /**
   * If this is a primitive type, then the proper type corresponding to its wrapper is returned.
   * Otherwise, this object is return.
   *
   * @return the proper type that is the wrapper type for this type or this if no such wrapper
   *     exists
   */
  public ProperType boxType() {
    if (properType.getKind().isPrimitive()) {
      return new ProperType(
          typeFactory.getBoxedType((AnnotatedPrimitiveType) getAnnotatedType()),
          context.types.boxedClass((Type) properType).asType(),
          context,
          ignoreAnnotations);
    }
    return this;
  }

  /**
   * Is {@code this} a subtype of {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} is a subtype of {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isSubType(ProperType superType) {
    TypeMirror subJavaType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    // The TypeMirror for a captured type variables may have inference variables that have not
    // been substituted with their instantiation, so use the AnnotatedTypeMirror to get the erased
    // type.
    TypeMirror subErasedJavaType = this.getErased().getJavaType();
    TypeMirror superErasedJavaType = superType.getErased().getJavaType();

    if (context.typeFactory.types.isAssignable(subJavaType, superJavaType)
        || context.typeFactory.types.isAssignable(subErasedJavaType, superErasedJavaType)) {
      if (ignoreAnnotations || superType.ignoreAnnotations) {
        return ConstraintSet.TRUE;
      }
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return ConstraintSet.TRUE_ANNO_FAIL;
      }
    } else {
      return ConstraintSet.FALSE;
    }
  }

  /**
   * Is {@code this} an unchecked subtype of {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} is an unchecked subtype of {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isSubTypeUnchecked(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isSubtypeUnchecked((Type) subType, (Type) superJavaType)) {
      if (ignoreAnnotations || superType.ignoreAnnotations) {
        return ConstraintSet.TRUE;
      }
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return ConstraintSet.TRUE_ANNO_FAIL;
      }
    } else {
      return ConstraintSet.FALSE;
    }
  }

  /**
   * Is {@code this} assignable to {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} assignable to {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isAssignable(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isAssignable((Type) subType, (Type) superJavaType)) {
      if (ignoreAnnotations || superType.ignoreAnnotations) {
        return ConstraintSet.TRUE;
      }
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return ConstraintSet.TRUE_ANNO_FAIL;
      }
    } else {
      return ConstraintSet.FALSE;
    }
  }

  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProperType otherProperType = (ProperType) o;

    if (!type.equals(otherProperType.type)) {
      return false;
    }
    if (properType.getKind() == TypeKind.TYPEVAR) {
      if (otherProperType.properType.getKind() == TypeKind.TYPEVAR) {
        return TypesUtils.areSame(
            (TypeVariable) properType, (TypeVariable) otherProperType.properType);
      }
      return false;
    }
    return properType == otherProperType.properType // faster
        || context.env.getTypeUtils().isSameType(properType, otherProperType.properType); // slower
  }

  @Override
  public int hashCode() {
    int result = properType.toString().hashCode();
    result = 31 * result + Kind.PROPER.hashCode();
    return result;
  }

  @Override
  public TypeMirror getJavaType() {
    return type.getUnderlyingType();
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType() {
    return type;
  }

  @Override
  public boolean isObject() {
    return TypesUtils.isObject(properType);
  }

  @Override
  public Collection<Variable> getInferenceVariables() {
    return Collections.emptyList();
  }

  @Override
  public AbstractType applyInstantiations() {
    return this;
  }

  @Override
  public Set<AbstractQualifier> getQualifiers() {
    return AbstractQualifier.create(
        getAnnotatedType().getPrimaryAnnotations(), qualifierVars, context);
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
