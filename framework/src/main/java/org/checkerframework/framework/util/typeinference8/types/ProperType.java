package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext.ReplacedTypes;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.TypesUtils;

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
   * The type that this type was made from by substituting instantiations, if at least one of them
   * ignores annotations (see {@link AbstractType#ignoreAnnotations}); otherwise, null.
   *
   * <p>A position where such an instantiation was substituted is an ignored substitution: its
   * annotations are ignored, because they say nothing about the variable's annotations. But {@link
   * #type} cannot record which positions those are. The origin can: it still has a use of the
   * variable at each such position. So this type computes its type arguments and its other parts
   * from its origin (see {@link #partFromOrigin}), and a part at such a position ignores
   * annotations, as {@link UseOfVariable#applyInstantiations} makes it. And when the variable's
   * instantiation changes, as it does when one that ignores annotations is replaced, {@link
   * #applyInstantiations} substitutes the new one.
   */
  private final @Nullable InferenceType origin;

  /**
   * If {@link #origin} is non-null, the value of {@link #instantiationStamp} for it when this type
   * was last found to be up to date; otherwise, 0. It is not part of this type's value: it only
   * saves {@link #applyInstantiations} from substituting again when nothing has changed.
   */
  private long originStamp;

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
    this(type, qualifierVars, context, ignoreAnnotations, null);
  }

  /**
   * Creates a proper type.
   *
   * @param type the annotated type
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   * @param origin the type that {@code type} was made from by substituting instantiations, at least
   *     one of which ignores annotations, or null; see {@link #origin}
   */
  ProperType(
      AnnotatedTypeMirror type,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations,
      @Nullable InferenceType origin) {
    super(context, ignoreAnnotations);
    this.type = type;
    this.qualifierVars = qualifierVars;
    this.origin = origin;
    this.originStamp = origin == null ? 0 : instantiationStamp(origin);
    verifyType(this.type);
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
    this.origin = null;
    this.originStamp = 0;
    verifyType(this.type);
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
    this.origin = null;
    this.originStamp = 0;
    verifyType(this.type);
  }

  /**
   * Returns a number that changes whenever the instantiation of a variable that {@code type}
   * mentions changes.
   *
   * @param type a type
   * @return a number that changes whenever the instantiation of a variable that {@code type}
   *     mentions changes
   */
  private static long instantiationStamp(AbstractType type) {
    long stamp = 0;
    for (Variable variable : type.getInferenceVariables()) {
      stamp += variable.getBounds().getInstantiationChanges();
    }
    return stamp;
  }

  /**
   * Returns a part of this type, computed from {@link #origin} if this type has one, so that the
   * part knows which of its positions are ignored substitutions. If this type has no origin, or the
   * part of the origin is not a proper type once instantiations are applied, then the part is
   * computed from {@link #type} instead.
   *
   * <p>If {@code sharesRoot} is true, then the root of the part is the root of this type, or it has
   * this type's root annotations, so the part computed from the origin gets the root annotations
   * and {@link #ignoreAnnotations} of the part computed from {@link #type}; see {@link
   * #withRootOf}.
   *
   * @param part computes the part of a type
   * @param plain computes the part of this type from {@link #type}
   * @param sharesRoot true if the root of the part is determined by the root of this type
   * @return the part of this type
   */
  private @Nullable AbstractType partFromOrigin(
      Function<AbstractType, @Nullable AbstractType> part,
      Supplier<@Nullable AbstractType> plain,
      boolean sharesRoot) {
    if (origin != null) {
      AbstractType derived = part.apply(origin);
      if (derived != null) {
        AbstractType result = derived.applyInstantiations();
        if (result.isProper()) {
          return sharesRoot ? withRootOf((ProperType) result, plain.get()) : result;
        }
      }
    }
    return plain.get();
  }

  /**
   * Returns {@code derived}, a part of this type computed from {@link #origin}, with the root
   * annotations and {@link #ignoreAnnotations} of {@code plainPart}, the same part computed from
   * {@link #type}. The origin's root annotations are not this type's: they may have been changed
   * since this type was made, for example by {@link UseOfVariable#applyInstantiations} or {@link
   * #withAnnotatedType}.
   *
   * @param derived a part of this type computed from {@link #origin}
   * @param plainPart the same part of this type computed from {@link #type}, or null
   * @return {@code derived}, with the root annotations of {@code plainPart}
   */
  private ProperType withRootOf(ProperType derived, @Nullable AbstractType plainPart) {
    if (plainPart == null) {
      return derived;
    }
    // `derived` may be an instantiation that is stored as a bound, so copy before mutating.
    AnnotatedTypeMirror atm = derived.getAnnotatedType().deepCopy();
    IgnoredAnnotations.copyRootAnnotations(plainPart.getAnnotatedType(), atm);
    return derived.withAnnotatedType(atm, plainPart.ignoreAnnotations);
  }

  /**
   * Returns parts of this type, computed from {@link #origin} if this type has one; see {@link
   * #partFromOrigin}. If any part of the origin is not a proper type once instantiations are
   * applied, then all the parts are computed from {@link #type} instead.
   *
   * <p>If {@code sharesRoot} is true, then each part gets the root annotations of the corresponding
   * part computed from {@link #type}, as in {@link #partFromOrigin}.
   *
   * @param parts computes the parts of a type
   * @param plain computes the parts of this type from {@link #type}
   * @param sharesRoot true if the root of each part is determined by the root of this type
   * @return the parts of this type
   */
  private @Nullable List<AbstractType> partsFromOrigin(
      Function<AbstractType, @Nullable List<AbstractType>> parts,
      Supplier<@Nullable List<AbstractType>> plain,
      boolean sharesRoot) {
    if (origin != null) {
      List<AbstractType> derived = parts.apply(origin);
      if (derived != null) {
        List<AbstractType> result = new ArrayList<>(derived.size());
        for (AbstractType t : derived) {
          AbstractType r = t.applyInstantiations();
          if (!r.isProper()) {
            return plain.get();
          }
          result.add(r);
        }
        if (sharesRoot) {
          List<AbstractType> plainParts = plain.get();
          if (plainParts != null && plainParts.size() == result.size()) {
            for (int i = 0; i < result.size(); i++) {
              result.set(i, withRootOf((ProperType) result.get(i), plainParts.get(i)));
            }
          }
        }
        return result;
      }
    }
    return plain.get();
  }

  /**
   * Returns true if this type is a wildcard or an intersection type with a primary annotation. Then
   * its bounds have its primary annotations, as {@code AnnotatedWildcardType} and {@code
   * AnnotatedIntersectionType} ensure.
   *
   * @return true if the bounds of this type have its primary annotations
   */
  private boolean boundsShareRoot() {
    return !type.getPrimaryAnnotations().isEmpty();
  }

  @Override
  public @Nullable List<AbstractType> getTypeArguments() {
    return partsFromOrigin(AbstractType::getTypeArguments, super::getTypeArguments, false);
  }

  @Override
  public @Nullable AbstractType getEnclosingType() {
    return partFromOrigin(AbstractType::getEnclosingType, super::getEnclosingType, false);
  }

  @Override
  public @Nullable AbstractType getComponentType() {
    return partFromOrigin(AbstractType::getComponentType, super::getComponentType, false);
  }

  @Override
  public @Nullable AbstractType asSuper(TypeMirror superType) {
    return partFromOrigin(t -> t.asSuper(superType), () -> super.asSuper(superType), true);
  }

  @Override
  public @Nullable List<AbstractType> getFunctionTypeParameterTypes() {
    return partsFromOrigin(
        AbstractType::getFunctionTypeParameterTypes, super::getFunctionTypeParameterTypes, false);
  }

  @Override
  public @Nullable AbstractType getFunctionTypeReturnType() {
    return partFromOrigin(
        AbstractType::getFunctionTypeReturnType, super::getFunctionTypeReturnType, false);
  }

  @Override
  public AbstractType getWildcardLowerBound() {
    AbstractType result =
        partFromOrigin(
            AbstractType::getWildcardLowerBound, super::getWildcardLowerBound, boundsShareRoot());
    assert result != null : "@AssumeAssertion(nullness): getWildcardLowerBound is non-null";
    return result;
  }

  @Override
  public AbstractType getWildcardUpperBound() {
    AbstractType result =
        partFromOrigin(
            AbstractType::getWildcardUpperBound, super::getWildcardUpperBound, boundsShareRoot());
    assert result != null : "@AssumeAssertion(nullness): getWildcardUpperBound is non-null";
    return result;
  }

  @Override
  public List<AbstractType> getIntersectionBounds() {
    List<AbstractType> result =
        partsFromOrigin(
            AbstractType::getIntersectionBounds, super::getIntersectionBounds, boundsShareRoot());
    assert result != null : "@AssumeAssertion(nullness): getIntersectionBounds is non-null";
    return result;
  }

  /**
   * Asserts that {@code type} is not void, which a proper type cannot represent.
   *
   * @param type the annotated type of a proper type
   */
  private static void verifyType(AnnotatedTypeMirror type) {
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
   * that their underlying Java types have already been found to be the same. The annotations of a
   * position that ignores annotations are not compared; see {@link
   * IgnoredAnnotations#replaceIgnoredForEquality}.
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
   * @return {@link ConstraintSet#TRUE} if the annotations of {@code this} are the same as those of
   *     {@code other}, apart from those that are ignored; otherwise {@link
   *     ConstraintSet#TRUE_ANNO_FAIL}
   */
  public ConstraintSet checkAnnotationEquality(ProperType other) {
    if (IgnoredAnnotations.decidedByIgnoredRoot(this, other)) {
      return ConstraintSet.TRUE;
    }
    ReplacedTypes compared =
        IgnoredAnnotations.replaceIgnoredForEquality(
            this, other, typeFactory.getQualifierHierarchy());
    AnnotatedTypeMirror thisATM = compared.type1();
    AnnotatedTypeMirror otherATM = compared.type2();
    // Compare using the type hierarchy in both directions rather than AnnotatedTypeMirror#equals,
    // which requires the underlying types to be the same object.
    if (typeFactory.getTypeHierarchy().isSubtype(thisATM, otherATM)
        && typeFactory.getTypeHierarchy().isSubtype(otherATM, thisATM)) {
      return ConstraintSet.TRUE;
    }
    // Scan for a polymorphic qualifier only now: when the annotations match, the result is the
    // same either way, and the scan is the more expensive of the two tests.
    if (context.hasPolymorphicQualifier(thisATM) || context.hasPolymorphicQualifier(otherATM)) {
      ReplacedTypes replaced = context.replacePolymorphicQualifiers(thisATM, otherATM);
      if (typeFactory.getTypeHierarchy().isSubtype(replaced.type1(), replaced.type2())
          && typeFactory.getTypeHierarchy().isSubtype(replaced.type2(), replaced.type1())) {
        return ConstraintSet.TRUE;
      }
    }
    return ConstraintSet.TRUE_ANNO_FAIL;
  }

  @Override
  public boolean equals(@Nullable Object o) {
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
    // A type with an origin has positions whose annotations are ignored, so it is not
    // interchangeable with one that has the same annotations and no origin.
    if ((origin == null) != (that.origin == null)) {
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

  /**
   * {@inheritDoc}
   *
   * <p>A proper type has no inference variables, so this returns this type, unless it has an {@link
   * #origin} and the instantiation of a variable that the origin mentions has changed since this
   * type was made. Then it returns the origin with the current instantiations applied.
   */
  @Override
  public AbstractType applyInstantiations() {
    if (origin == null || instantiationStamp(origin) == originStamp) {
      return this;
    }
    AbstractType result = origin.applyInstantiations();
    if (!result.isProper()) {
      return this;
    }
    ProperType properResult = (ProperType) result;
    // `origin` has no ignored substitution at its root, so the root annotations of this type are
    // still right, and they may differ from those of `origin` if resolution set them; see
    // Resolution#lubOfLowerBounds.  `result` is a new type, so its annotated type may be mutated.
    AnnotatedTypeMirror resultATM = properResult.getAnnotatedType();
    IgnoredAnnotations.copyRootAnnotations(type, resultATM);
    if (properResult.origin != null && isSameAnnotatedType(resultATM, type)) {
      // Nothing that this type depends on has changed.  Returning this type, rather than an
      // equal new one, tells the caller so; otherwise, two variables whose instantiations mention
      // each other would make each other change forever.
      originStamp = instantiationStamp(origin);
      return this;
    }
    return new ProperType(
        resultATM, qualifierVars, context, ignoreAnnotations, properResult.origin);
  }

  /**
   * Returns the type that this type was made from by substituting instantiations, if at least one
   * of them ignores annotations; otherwise, null. See {@link #origin}.
   *
   * @return the type that this type was made from, or null
   */
  @Nullable InferenceType getOrigin() {
    return origin;
  }

  /**
   * Returns true if {@code a} and {@code b} are the same annotated type. {@link
   * AnnotatedTypeMirror#equals} is not used, because it requires the underlying types to be the
   * same object, and substituting instantiations makes new ones.
   *
   * @param a an annotated type
   * @param b an annotated type
   * @return true if {@code a} and {@code b} are the same annotated type
   */
  private boolean isSameAnnotatedType(AnnotatedTypeMirror a, AnnotatedTypeMirror b) {
    return context.types.isSameType((Type) a.getUnderlyingType(), (Type) b.getUnderlyingType())
        && typeFactory.getTypeHierarchy().isSubtype(a, b)
        && typeFactory.getTypeHierarchy().isSubtype(b, a);
  }

  /**
   * Returns a copy of this type, whose annotated type is a deep copy of this type's and may be
   * mutated. Unlike {@link #create}, the copy has this type's origin; see {@link #origin}. So only
   * the root annotations of the copy may be changed, because the origin gives the positions of the
   * rest.
   *
   * @return a copy of this type
   */
  public ProperType copy() {
    return withAnnotatedType(type.deepCopy(), ignoreAnnotations);
  }

  /**
   * Returns a type like this one, with this type's origin, but whose annotated type is {@code atm}
   * and whose {@link #ignoreAnnotations} is {@code ignoreAnnotations}. {@code atm} must be this
   * type's annotated type, or a copy of it, with only its root annotations changed, because the
   * origin gives the positions of the rest; see {@link #origin}. The result is up to date exactly
   * when this type is.
   *
   * @param atm this type's annotated type, or a copy of it, with only its root annotations changed
   * @param ignoreAnnotations true if the root annotations of {@code atm} should be ignored
   * @return a type like this one, whose annotated type is {@code atm}
   */
  ProperType withAnnotatedType(AnnotatedTypeMirror atm, boolean ignoreAnnotations) {
    ProperType result = new ProperType(atm, qualifierVars, context, ignoreAnnotations, origin);
    result.originStamp = originStamp;
    return result;
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
