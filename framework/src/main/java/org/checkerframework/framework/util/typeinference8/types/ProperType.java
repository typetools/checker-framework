package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/** A type that does not contain any inference variables. */
public class ProperType extends AbstractType {

  /**
   * The annotated type. Its underlying type is the Java type of this proper type; that is, {@link
   * #getJavaType()} returns {@code type.getUnderlyingType()}.
   */
  private final AnnotatedTypeMirror type;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  private final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param context the context
   */
  public ProperType(AnnotatedTypeMirror type, Java8InferenceContext context) {
    this(type, AnnotationMirrorMap.emptyMap(), context, false);
  }

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   */
  public ProperType(
      AnnotatedTypeMirror type, Java8InferenceContext context, boolean ignoreAnnotations) {
    this(type, AnnotationMirrorMap.emptyMap(), context, ignoreAnnotations);
  }

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   */
  public ProperType(
      AnnotatedTypeMirror type,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    super(context, ignoreAnnotations);
    this.type = type;
    this.qualifierVars = qualifierVars;
    verifyType();
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
    this.qualifierVars = AnnotationMirrorMap.emptyMap();
    verifyType();
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
    this.qualifierVars = AnnotationMirrorMap.emptyMap();
    verifyType();
  }

  /** Asserts that this type is not void, which a proper type cannot represent. */
  private void verifyType() {
    assert type.getKind() != TypeKind.VOID : "ProperType created for void type: " + type;
  }

  @Override
  public Kind getKind() {
    return Kind.PROPER;
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@code type} is ignored, because the Java type of a proper type is the underlying type of
   * its annotated type. (Callers such as {@link AbstractType#getErased()} do not always pass {@code
   * atm.getUnderlyingType()} as {@code type}.)
   */
  @Override
  public AbstractType create(AnnotatedTypeMirror atm, boolean ignoreAnnotations) {
    return new ProperType(atm, qualifierVars, context, ignoreAnnotations);
  }

  /**
   * If this is a primitive type, then the proper type corresponding to its wrapper is returned.
   * Otherwise, this object is return.
   *
   * @return the proper type that is the wrapper type for this type or this if no such wrapper
   *     exists
   */
  public ProperType boxType() {
    if (getJavaType().getKind().isPrimitive()) {
      return new ProperType(
          typeFactory.getBoxedType((AnnotatedPrimitiveType) getAnnotatedType()),
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
      return checkAnnotationSubtype(superType);
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
      return checkAnnotationSubtype(superType);
    } else {
      return ConstraintSet.FALSE;
    }
  }

  /**
   * Is {@code this} assignable to {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} is assignable to {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isAssignable(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isAssignable((Type) subType, (Type) superJavaType)) {
      return checkAnnotationSubtype(superType);
    } else {
      return ConstraintSet.FALSE;
    }
  }

  /**
   * Checks whether the annotations of {@code this} are the same as those of {@code other}, assuming
   * that their underlying Java types have already been found to be the same. If either type is
   * marked as having annotations that should be ignored, then the annotations are not compared.
   *
   * <p>Neither type may be an uncaptured wildcard: the type hierarchy compares a wildcard's {@code
   * extends} bound against the other type, which for a lower-bounded wildcard is not the bound that
   * holds its qualifiers.
   *
   * <p>The type hierarchy compares a polymorphic qualifier as though it were concrete, so it
   * reports a conflict with every qualifier that the polymorphic qualifier could be instantiated
   * to; see {@link AbstractQualifier#isUnsolvedPolymorphic} for why inference cannot solve for the
   * qualifier instead. If the comparison fails, it is therefore retried on copies in which each
   * polymorphic qualifier has been replaced by the qualifier it is compared against; see {@link
   * Java8InferenceContext#replacePolymorphicQualifiers}. The retry suppresses only the positions
   * and qualifier hierarchies that a polymorphic qualifier occupies, so a conflict at another type
   * argument, or one in another qualifier hierarchy, is still reported.
   *
   * @param other the type to compare against
   * @return {@link ConstraintSet#TRUE} if the annotations are ignored or if the annotations of
   *     {@code this} are the same as those of {@code other}; otherwise {@link
   *     ConstraintSet#TRUE_ANNO_FAIL}
   */
  public ConstraintSet checkAnnotationEquality(ProperType other) {
    if (ignoreAnnotations || other.ignoreAnnotations) {
      return ConstraintSet.TRUE;
    }
    AnnotatedTypeMirror thisATM = getAnnotatedType();
    AnnotatedTypeMirror otherATM = other.getAnnotatedType();
    // Compare using the type hierarchy in both directions rather than AnnotatedTypeMirror#equals,
    // which requires the underlying types to be the same object.
    if (typeFactory.getTypeHierarchy().isSubtype(thisATM, otherATM)
        && typeFactory.getTypeHierarchy().isSubtype(otherATM, thisATM)) {
      return ConstraintSet.TRUE;
    }
    // Scan for a polymorphic qualifier only now: when the annotations match, the result is the
    // same either way, and the scan is the more expensive of the two tests.
    if (context.hasPolymorphicQualifier(thisATM) || context.hasPolymorphicQualifier(otherATM)) {
      IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> replaced =
          context.replacePolymorphicQualifiers(thisATM, otherATM);
      if (typeFactory.getTypeHierarchy().isSubtype(replaced.first, replaced.second)
          && typeFactory.getTypeHierarchy().isSubtype(replaced.second, replaced.first)) {
        return ConstraintSet.TRUE;
      }
    }
    return ConstraintSet.TRUE_ANNO_FAIL;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProperType that = (ProperType) o;
    if (!sameInferenceProblem(that)) {
      return false;
    }
    // Two types with different qualifierVars have different qualifiers, as getQualifiers() shows.
    if (!qualifierVars.equals(that.qualifierVars)) {
      return false;
    }

    // Comparing the annotated types also compares the Java types: AnnotatedTypeMirror#equals
    // requires the underlying types to be the same object, and the Java type of a proper type is
    // the underlying type of its annotated type.
    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    // This is Objects.hash() expanded, to avoid allocating an array and boxing.  This method is
    // hot: inference puts these types in hash sets and rebuilds those sets repeatedly.
    int result = 31 + inferenceProblemHashCode();
    result = 31 * result + Objects.hashCode(qualifierVars);
    result = 31 * result + Objects.hashCode(type);
    return 31 * result + Kind.PROPER.hashCode();
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
    return TypesUtils.isObject(getJavaType());
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
