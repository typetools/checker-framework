package org.checkerframework.framework.type;

import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.Covariant;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Default implementation of TypeHierarchy that implements the JLS specification with minor
 * deviations as outlined by the Checker Framework manual. Changes to the JLS include forbidding
 * covariant array types, raw types, and allowing covariant type arguments depending on various
 * options passed to DefaultTypeHierarchy.
 *
 * <p>Subtyping rules of the JLS can be found in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.10">section 4.10,
 * "Subtyping"</a>.
 *
 * <p>Note: The visit methods of this class must be public but it is intended to be used through a
 * TypeHierarchy interface reference which will only allow isSubtype to be called. Clients should
 * not call the visit methods.
 */
public class DefaultTypeHierarchy extends AbstractAtmComboVisitor<Boolean, Void>
    implements TypeHierarchy {
  // used for processingEnvironment when needed
  protected final BaseTypeChecker checker;

  protected final QualifierHierarchy qualifierHierarchy;
  protected final StructuralEqualityComparer equalityComparer;

  protected final boolean ignoreRawTypes;
  protected final boolean invariantArrayComponents;

  // TODO: Incorporate feedback from David/Suzanne
  // IMPORTANT_NOTE:
  //
  // For MultigraphQualifierHierarchies, we check the subtyping relationship of each annotation
  // hierarchy individually.  This is done because when comparing a pair of type variables,
  // sometimes you need to traverse and compare the bounds of two type variables.  Other times it
  // is incorrect to compare the bounds.  These two cases can occur simultaneously when comparing
  // two hierarchies at once.  In this case, comparing both hierarchies simultaneously will lead
  // to an error.  More detail is given below.
  //
  // Recall, type variables may or may not have a primary annotation for each individual
  // hierarchy.  When comparing
  // two type variables for a specific hierarchy we have five possible cases:
  //      case 1:  only one of the type variables has a primary annotation
  //      case 2a: both type variables have primary annotations and they are uses of the same type
  //               parameter
  //      case 2b: both type variables have primary annotations and they are uses of different
  //               type parameters
  //      case 3a: neither type variable has a primary annotation and they are uses of the same
  //               type parameter
  //      case 3b: neither type variable has a primary annotation and they are uses of different
  //               type parameters
  //
  // Case 1, 2b, and 3b require us to traverse both type variables bounds to ensure that the
  // subtype's upper bound is a subtype of the supertype's lower bound. Cases 2a requires only
  // that we check that the primary annotation on the subtype is a subtype of the primary
  // annotation on the supertype.  In case 3a, we can just return true, since two
  // non-primary-annotated uses of the same type parameter are equivalent.  In this case it would
  // be an error to check the bounds because the check would only return true when the bounds are
  // exact but it should always return true.
  //
  // A problem occurs when, one hierarchy matches cases 1, 2b, or 3b and the other matches 3a.  In
  // the first set of cases we MUST check the type variables' bounds.  In case 3a we MUST NOT
  // check the bounds.  e.g.
  //
  // Suppose I have a hierarchy with two tops @A1 and @B1.  Let @A0 <: @A1 and @B0 <: @B1.
  //  @A1 T t1;  T t2;
  //  t1 = t2;
  //
  // To typecheck "t1 = t2;" in the hierarchy topped by @A1, we need to descend into the bounds of
  // t1 and t2 (where t1's bounds will be overridden by @A1).  However, for hierarchy B we need
  // only recognize that since neither variable has a primary annotation, the types are equivalent
  // and no traversal is needed.  If we tried to do these two actions simultaneously, in every
  // visit and isSubtype call, we would have to check to see that the @B hierarchy has been
  // handled and ignore those annotations.
  //
  // Solutions:
  // We could handle this problem by keeping track of which hierarchies have already been taken
  // care of.  We could then check each hierarchy before making comparisons.  But this would lead
  // to complicated plumbing that would be hard to understand.
  // The chosen solution is to only check one hierarchy at a time.  One trade-off to this approach
  // is that we have to re-traverse the types for each hierarchy being checked.
  //
  // The field currentTop identifies the hierarchy for which the types are currently being checked.
  // Final note: all annotation comparisons are done via isPrimarySubtype, isBottom, and
  // isAnnoSubtype in order to ensure that we first get the annotations in the hierarchy of
  // currentTop before passing annotations to qualifierHierarchy.
  /** The top annotation of the hierarchy currently being checked. */
  protected AnnotationMirror currentTop;

  /** Stores the result of isSubtype, if that result is true. */
  protected final SubtypeVisitHistory isSubtypeVisitHistory;

  /**
   * Stores the result of {@link #areEqualInHierarchy(AnnotatedTypeMirror, AnnotatedTypeMirror)} for
   * type arguments. Prevents infinite recursion on types that refer to themselves. (Stores both
   * true and false results.)
   */
  protected final StructuralEqualityVisitHistory areEqualVisitHistory;

  /** The Covariant.value field/element. */
  final ExecutableElement covariantValueElement;

  /**
   * Creates a DefaultTypeHierarchy.
   *
   * @param checker the type-checker that is associated with this
   * @param qualifierHierarchy the qualiifer hierarchy that is associated with this
   * @param ignoreRawTypes whether to ignore raw types
   * @param invariantArrayComponents whether to make array subtyping invariant with respect to array
   *     component types
   */
  public DefaultTypeHierarchy(
      final BaseTypeChecker checker,
      final QualifierHierarchy qualifierHierarchy,
      boolean ignoreRawTypes,
      boolean invariantArrayComponents) {
    this.checker = checker;
    this.qualifierHierarchy = qualifierHierarchy;
    this.isSubtypeVisitHistory = new SubtypeVisitHistory();
    this.areEqualVisitHistory = new StructuralEqualityVisitHistory();
    this.equalityComparer = createEqualityComparer();

    this.ignoreRawTypes = ignoreRawTypes;
    this.invariantArrayComponents = invariantArrayComponents;

    covariantValueElement =
        TreeUtils.getMethod(Covariant.class, "value", 0, checker.getProcessingEnvironment());
  }

  /**
   * Create the equality comparer.
   *
   * @return the equality comparer
   */
  protected StructuralEqualityComparer createEqualityComparer() {
    return new StructuralEqualityComparer(areEqualVisitHistory);
  }

  /**
   * Returns true if subtype {@literal <:} supertype.
   *
   * <p>This implementation iterates over all top annotations and invokes {@link
   * #isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror, AnnotationMirror)}. Most type systems
   * should not override this method, but instead override {@link #isSubtype(AnnotatedTypeMirror,
   * AnnotatedTypeMirror, AnnotationMirror)} or some of the {@code visitXXX} methods.
   *
   * @param subtype expected subtype
   * @param supertype expected supertype
   * @return true if subtype is a subtype of supertype or equal to it
   */
  @Override
  public boolean isSubtype(final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
    for (final AnnotationMirror top : qualifierHierarchy.getTopAnnotations()) {
      if (!isSubtype(subtype, supertype, top)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true if {@code subtype <: supertype}, but only for the hierarchy of which {@code top}
   * is the top.
   *
   * @param subtype expected subtype
   * @param supertype expected supertype
   * @param top the top of the hierarchy for which we want to make a comparison
   * @return true if {@code subtype} is a subtype of, or equal to, {@code supertype} in the
   *     qualifier hierarchy whose top is {@code top}
   */
  protected boolean isSubtype(
      final AnnotatedTypeMirror subtype,
      final AnnotatedTypeMirror supertype,
      final AnnotationMirror top) {
    assert top != null;
    currentTop = top;
    return AtmCombo.accept(subtype, supertype, null, this);
  }

  /**
   * Returns error message for the case when two types shouldn't be compared.
   *
   * @return error message for the case when two types shouldn't be compared
   */
  @Override
  protected String defaultErrorMessage(
      final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype, final Void p) {
    return "Incomparable types ("
        + subtype
        + ", "
        + supertype
        + ") visitHistory = "
        + isSubtypeVisitHistory;
  }

  /**
   * Compare the primary annotations of {@code subtype} and {@code supertype}. Neither type can be
   * missing annotations.
   *
   * @param subtype a type that might be a subtype (with respect to primary annotations)
   * @param supertype a type that might be a supertype (with respect to primary annotations)
   * @return true if the primary annotation on subtype {@literal <:} primary annotation on supertype
   *     for the current top.
   */
  protected boolean isPrimarySubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
    final AnnotationMirror subtypeAnno = subtype.getAnnotationInHierarchy(currentTop);
    final AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
    if (checker.getTypeFactory().hasQualifierParameterInHierarchy(supertype, currentTop)
        && checker.getTypeFactory().hasQualifierParameterInHierarchy(subtype, currentTop)) {
      // If the types have a class qualifier parameter, the qualifiers must be equivalent.
      return qualifierHierarchy.isSubtype(subtypeAnno, supertypeAnno)
          && qualifierHierarchy.isSubtype(supertypeAnno, subtypeAnno);
    }

    return qualifierHierarchy.isSubtype(subtypeAnno, supertypeAnno);
  }

  /**
   * Like {@link #isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)}, but uses a cache to prevent
   * infinite recursion on recursive types.
   *
   * @param subtype a type that may be a subtype
   * @param supertype a type that may be a supertype
   * @return true if subtype {@literal <:} supertype
   */
  protected boolean isSubtypeCaching(
      final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype) {
    if (isSubtypeVisitHistory.contains(subtype, supertype, currentTop)) {
      // visitHistory only contains pairs in a subtype relationship.
      return true;
    }

    boolean result = isSubtype(subtype, supertype, currentTop);
    // The call to put has no effect if result is false.
    isSubtypeVisitHistory.put(subtype, supertype, currentTop, result);
    return result;
  }

  /**
   * Are all the types in {@code subtypes} a subtype of {@code supertype}?
   *
   * <p>The underlying type mirrors of {@code subtypes} must be subtypes of the underlying type
   * mirror of {@code supertype}.
   */
  protected boolean areAllSubtypes(
      final Iterable<? extends AnnotatedTypeMirror> subtypes, final AnnotatedTypeMirror supertype) {
    for (final AnnotatedTypeMirror subtype : subtypes) {
      if (!isSubtype(subtype, supertype, currentTop)) {
        return false;
      }
    }

    return true;
  }

  protected boolean areEqualInHierarchy(
      final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
    return equalityComparer.areEqualInHierarchy(type1, type2, currentTop);
  }

  /**
   * Represents a wildcard or captured wildcard. Use this to avoid the need for special-case code
   * for {@link AnnotatedWildcardType} and {@link AnnotatedTypeVariable}.
   */
  protected static class BoundType {

    /** Lower bound. */
    protected final AnnotatedTypeMirror lower;

    /** Upper bound. */
    protected final AnnotatedTypeMirror upper;

    /**
     * Whether this has an explicit lower bound that is not the null type; in other words, whether
     * the source code syntax of this contains "super". Because BoundTypes can represent captured
     * types, if a bound type has an explicit lower bound, its upper bound may be {@code Object} or
     * any other type that is a supertype of the lower bound.
     */
    protected final boolean hasExplicitLowerBound;

    /**
     * Creates a bound type.
     *
     * @param type a wildcard or a captured wildcard
     */
    protected BoundType(AnnotatedTypeMirror type) {
      if (type.getKind() == TypeKind.WILDCARD) {
        AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) type;
        this.lower = wildcardType.getSuperBound();
        this.upper = wildcardType.getExtendsBound();
      } else if (TypesUtils.isCaptured(type.getUnderlyingType())) {
        AnnotatedTypeVariable typeVariable = (AnnotatedTypeVariable) type;
        this.lower = typeVariable.getLowerBound();
        this.upper = typeVariable.getUpperBound();
      } else {
        throw new BugInCF("Unexpected: %s", type);
      }
      this.hasExplicitLowerBound = lower.getKind() != TypeKind.NULL;
    }

    /**
     * Returns true if {@code type} is a wildcard or captured wildcard.
     *
     * @param type type to check
     * @return true if {@code type} is a wildcard or captured wildcard
     */
    protected static boolean isBoundType(AnnotatedTypeMirror type) {
      return type.getKind() == TypeKind.WILDCARD || TypesUtils.isCaptured(type.getUnderlyingType());
    }

    @Override
    public String toString() {
      return "[ extends " + upper + " super " + lower + ']';
    }
  }

  /**
   * Returns true if {@code outside} contains {@code inside}, that is, if the set of types denoted
   * by {@code outside} is a superset of or equal to the set of types denoted by {@code inside}.
   *
   * <p>Containment is described in <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-4.html#jls-4.5.1">JLS section
   * 4.5.1 "Type Arguments of Parameterized Types"</a>. A declared type is considered a supertype of
   * another declared type only if all of the type arguments of the declared type "contain" the
   * corresponding type arguments of the subtype.
   *
   * <p>The containment algorithm implemented here is slightly different that what is presented in
   * the JLS. The Checker Framework checks that method arguments are subtype of the method
   * parameters that have been viewpoint-adapted to the call site. Java does not do this check;
   * instead, it checks if an applicable method exists. By checking that method arguments are
   * subtypes of viewpoint-adapted parameters, the Checker Framework gives better error messages.
   * However, viewpoint-adapting parameters leads to types that Java does not account for in the
   * containment algorithm, namely wildcards with upper or lower bounds that are captured types. In
   * these cases, our algorithm calls containment recursively on the captured type bound. (Note, it
   * must recur rather than call isSubtype because the inside type may be in between the bounds of
   * the upper or lower bound. For example: outside: ? extends ? extends Object inside: String)
   *
   * @param inside a possibly-contained type
   * @param outside a possibly-containing type
   * @param canBeCovariant whether or not type arguments are allowed to be covariant
   * @return true if inside is contained by outside, or if canBeCovariant == true and {@code inside
   *     <: outside}
   */
  protected boolean isContainedBy(
      AnnotatedTypeMirror inside, AnnotatedTypeMirror outside, boolean canBeCovariant) {

    if (ignoreUninferredTypeArgument(inside) || ignoreUninferredTypeArgument(outside)) {
      areEqualVisitHistory.put(inside, outside, currentTop, true);
      return true;
    }
    if (BoundType.isBoundType(outside)) {
      Boolean previousResult = areEqualVisitHistory.get(inside, outside, currentTop);
      if (previousResult != null) {
        return previousResult;
      }
      areEqualVisitHistory.put(inside, outside, currentTop, true);
      boolean result = isContainedByBoundType(inside, new BoundType(outside), canBeCovariant);
      areEqualVisitHistory.put(inside, outside, currentTop, result);
      return result;
    }
    if (canBeCovariant) {
      return isSubtype(inside, outside, currentTop);
    }
    return areEqualInHierarchy(inside, outside);
  }

  /**
   * Returns true if {@code outside} contains {@code inside}, that is, if the set of types denoted
   * by {@code outside} is a superset of or equal to the set of types denoted by {@code inside}. See
   * {@link #isContainedBy(AnnotatedTypeMirror, AnnotatedTypeMirror, boolean)} for a full
   * explanation.
   *
   * <p>Roughly, the algorithm works as follows (assuming {@code canBeCovariant} is false):
   *
   * <ul>
   *   <li>If inside is a bound type, recur on the explicit bound.
   *   <li>If one of outside's bounds is itself a bound type, recur on that bound.
   *   <li>Otherwise, return {@code outside.lower <: inside && inside <: outside.upper}.
   * </ul>
   *
   * If {@code canBeCovariant} is true, then the checks on lower bounds return true.
   *
   * @param inside a possibly-contained type
   * @param outside a possibly-containing type
   * @param canBeCovariant whether or not type arguments are allowed to be covariant
   * @return true if inside is contained by outside, or if canBeCovariant == true and {@code inside
   *     <: outside}
   */
  protected boolean isContainedByBoundType(
      AnnotatedTypeMirror inside, BoundType outside, boolean canBeCovariant) {
    try {
      if (canBeCovariant) {
        if (outside.hasExplicitLowerBound) {
          return isSubtype(outside.lower, inside);
        } else {
          return isSubtype(inside, outside.upper);
        }
      }
      return isSubtype(outside.lower, inside) && isSubtype(inside, outside.upper);
    } catch (Throwable ex) {
      return false;
    }
  }

  /**
   * Returns true if {@code type} is an uninferred type argument and if the checker should not issue
   * warnings about uninferred type arguments.
   *
   * @param type type to check
   * @return true if {@code type} is an uninferred type argument and if the checker should not issue
   *     warnings about uninferred type arguments
   */
  private boolean ignoreUninferredTypeArgument(AnnotatedTypeMirror type) {
    return type.atypeFactory.ignoreUninferredTypeArguments
        && type.getKind() == TypeKind.WILDCARD
        && ((AnnotatedWildcardType) type).isUninferredTypeArgument();
  }

  // ------------------------------------------------------------------------
  // The rest of this file is the visitor methods.  It is a lot of methods, one for each
  // combination of types.

  // ------------------------------------------------------------------------
  // Arrays as subtypes

  @Override
  public Boolean visitArray_Array(
      AnnotatedArrayType subtype, AnnotatedArrayType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype)
        && (invariantArrayComponents
            ? areEqualInHierarchy(subtype.getComponentType(), supertype.getComponentType())
            : isSubtype(subtype.getComponentType(), supertype.getComponentType(), currentTop));
  }

  @Override
  public Boolean visitArray_Declared(
      AnnotatedArrayType subtype, AnnotatedDeclaredType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitArray_Null(AnnotatedArrayType subtype, AnnotatedNullType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitArray_Intersection(
      AnnotatedArrayType subtype, AnnotatedIntersectionType supertype, Void p) {
    return isSubtype(
        AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype),
        supertype,
        currentTop);
  }

  @Override
  public Boolean visitArray_Wildcard(
      AnnotatedArrayType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcardSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitArray_Typevar(
      AnnotatedArrayType subtype, AnnotatedTypeVariable superType, Void p) {
    return visitTypevarSupertype(subtype, superType);
  }

  // ------------------------------------------------------------------------
  // Declared as subtype
  @Override
  public Boolean visitDeclared_Array(
      AnnotatedDeclaredType subtype, AnnotatedArrayType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Declared(
      AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, Void p) {
    if (!isPrimarySubtype(subtype, supertype)) {
      return false;
    }
    AnnotatedTypeFactory factory = subtype.atypeFactory;
    if (factory.ignoreUninferredTypeArguments
        && (factory.containsUninferredTypeArguments(subtype)
            || factory.containsUninferredTypeArguments(supertype))) {
      // Calling castedAsSuper may cause the uninferredTypeArguments to be lost. So, just
      // return true here.
      return true;
    }
    AnnotatedDeclaredType subtypeAsSuper =
        AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);

    if (isSubtypeVisitHistory.contains(subtypeAsSuper, supertype, currentTop)) {
      return true;
    }

    final boolean result =
        visitTypeArgs(subtypeAsSuper, supertype, subtype.wasRaw(), supertype.wasRaw());
    isSubtypeVisitHistory.put(subtypeAsSuper, supertype, currentTop, result);

    return result;
  }

  /**
   * A helper class for visitDeclared_Declared. There are subtypes of DefaultTypeHierarchy that need
   * to customize the handling of type arguments. This method provides a convenient extension point.
   *
   * @param subtype a possible subtype
   * @param supertype a possible supertype
   * @param subtypeRaw whether {@code subtype} is a raw type
   * @param supertypeRaw whether {@code supertype} is a raw type
   * @return the result of visiting type args
   */
  protected boolean visitTypeArgs(
      AnnotatedDeclaredType subtype,
      AnnotatedDeclaredType supertype,
      final boolean subtypeRaw,
      final boolean supertypeRaw) {

    final boolean ignoreTypeArgs = ignoreRawTypes && (subtypeRaw || supertypeRaw);

    if (ignoreTypeArgs) {
      return true;
    }

    final List<? extends AnnotatedTypeMirror> subtypeTypeArgs = subtype.getTypeArguments();
    final List<? extends AnnotatedTypeMirror> supertypeTypeArgs = supertype.getTypeArguments();

    if (subtypeTypeArgs.size() != supertypeTypeArgs.size()) {
      return false;
    }
    if (subtypeTypeArgs.isEmpty()) {
      return true;
    }

    final TypeElement supertypeElem = (TypeElement) supertype.getUnderlyingType().asElement();
    AnnotationMirror covariantAnno =
        supertype.atypeFactory.getDeclAnnotation(supertypeElem, Covariant.class);

    List<Integer> covariantArgIndexes =
        (covariantAnno == null)
            ? null
            : AnnotationUtils.getElementValueArray(
                covariantAnno, covariantValueElement, Integer.class);

    // JLS: 4.10.2. Subtyping among Class and Interface Types
    // 3th  set of bullets
    if (isContainedMany(subtype.getTypeArguments(), supertypeTypeArgs, covariantArgIndexes)) {
      return true;
    }
    // 4th
    AnnotatedDeclaredType capturedSubtype =
        (AnnotatedDeclaredType) subtype.atypeFactory.applyCaptureConversion(subtype);
    return isContainedMany(
        capturedSubtype.getTypeArguments(), supertypeTypeArgs, covariantArgIndexes);
  }

  private boolean isContainedMany(
      List<? extends AnnotatedTypeMirror> subtypeTypeArgs,
      List<? extends AnnotatedTypeMirror> supertypeTypeArgs,
      List<Integer> covariantArgIndexes) {
    for (int i = 0; i < supertypeTypeArgs.size(); i++) {
      AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
      AnnotatedTypeMirror subTypeArg = subtypeTypeArgs.get(i);
      boolean covariant = covariantArgIndexes != null && covariantArgIndexes.contains(i);

      boolean result = isContainedBy(subTypeArg, superTypeArg, covariant);

      if (!result) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Boolean visitDeclared_Intersection(
      AnnotatedDeclaredType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitIntersectionSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Null(
      AnnotatedDeclaredType subtype, AnnotatedNullType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Primitive(
      AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, Void p) {
    // We do an asSuper first because in some cases unboxing implies a more specific annotation
    // e.g. @UnknownInterned Integer => @Interned int  because primitives are always interned
    final AnnotatedPrimitiveType subAsSuper =
        AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);
    if (subAsSuper == null) {
      return isPrimarySubtype(subtype, supertype);
    }
    return isPrimarySubtype(subAsSuper, supertype);
  }

  @Override
  public Boolean visitDeclared_Typevar(
      AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitTypevarSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Union(
      AnnotatedDeclaredType subtype, AnnotatedUnionType supertype, Void p) {
    Types types = checker.getTypeUtils();
    for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
      if (TypesUtils.isErasedSubtype(
              subtype.getUnderlyingType(), supertypeAltern.getUnderlyingType(), types)
          && isSubtype(subtype, supertypeAltern, currentTop)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitDeclared_Wildcard(
      AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcardSupertype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // Intersection as subtype
  @Override
  public Boolean visitIntersection_Declared(
      AnnotatedIntersectionType subtype, AnnotatedDeclaredType supertype, Void p) {
    return visitIntersectionSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitIntersection_Primitive(
      AnnotatedIntersectionType subtype, AnnotatedPrimitiveType supertype, Void p) {
    for (AnnotatedTypeMirror subtypeBound : subtype.getBounds()) {
      if (TypesUtils.isBoxedPrimitive(subtypeBound.getUnderlyingType())
          && isSubtype(subtypeBound, supertype, currentTop)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitIntersection_Intersection(
      AnnotatedIntersectionType subtype, AnnotatedIntersectionType supertype, Void p) {
    Types types = checker.getTypeUtils();
    for (AnnotatedTypeMirror subBound : subtype.getBounds()) {
      for (AnnotatedTypeMirror superBound : supertype.getBounds()) {
        if (TypesUtils.isErasedSubtype(
                subBound.getUnderlyingType(), superBound.getUnderlyingType(), types)
            && !isSubtype(subBound, superBound, currentTop)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public Boolean visitIntersection_Null(
      AnnotatedIntersectionType subtype, AnnotatedNullType supertype, Void p) {
    // this can occur through capture conversion/comparing bounds
    for (AnnotatedTypeMirror bound : subtype.getBounds()) {
      if (isPrimarySubtype(bound, supertype)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitIntersection_Typevar(
      AnnotatedIntersectionType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitIntersectionSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitIntersection_Wildcard(
      AnnotatedIntersectionType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitIntersectionSubtype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // Null as subtype
  @Override
  public Boolean visitNull_Array(AnnotatedNullType subtype, AnnotatedArrayType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Declared(
      AnnotatedNullType subtype, AnnotatedDeclaredType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Typevar(
      AnnotatedNullType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitTypevarSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Wildcard(
      AnnotatedNullType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcardSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Null(AnnotatedNullType subtype, AnnotatedNullType supertype, Void p) {
    // this can occur when comparing typevar lower bounds since they are usually null types
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Union(AnnotatedNullType subtype, AnnotatedUnionType supertype, Void p) {
    for (AnnotatedDeclaredType supertypeAltern : supertype.getAlternatives()) {
      if (isSubtype(subtype, supertypeAltern, currentTop)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Boolean visitNull_Intersection(
      AnnotatedNullType subtype, AnnotatedIntersectionType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Primitive(
      AnnotatedNullType subtype, AnnotatedPrimitiveType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // Primitive as subtype
  @Override
  public Boolean visitPrimitive_Declared(
      AnnotatedPrimitiveType subtype, AnnotatedDeclaredType supertype, Void p) {
    // see comment in visitDeclared_Primitive
    final AnnotatedDeclaredType subAsSuper =
        AnnotatedTypes.castedAsSuper(subtype.atypeFactory, subtype, supertype);
    if (subAsSuper == null) {
      return isPrimarySubtype(subtype, supertype);
    }
    return isPrimarySubtype(subAsSuper, supertype);
  }

  @Override
  public Boolean visitPrimitive_Primitive(
      AnnotatedPrimitiveType subtype, AnnotatedPrimitiveType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitPrimitive_Intersection(
      AnnotatedPrimitiveType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitIntersectionSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitPrimitive_Typevar(
      AnnotatedPrimitiveType subtype, AnnotatedTypeVariable supertype, Void p) {
    return AtmCombo.accept(subtype, supertype.getUpperBound(), null, this);
  }

  @Override
  public Boolean visitPrimitive_Wildcard(
      AnnotatedPrimitiveType subtype, AnnotatedWildcardType supertype, Void p) {
    if (supertype.atypeFactory.ignoreUninferredTypeArguments
        && supertype.isUninferredTypeArgument()) {
      return true;
    }
    // this can occur when passing a primitive to a method on a raw type (see test
    // checker/tests/nullness/RawAndPrimitive.java).  This can also occur because we don't box
    // primitives when we should and don't capture convert.
    return isPrimarySubtype(subtype, supertype.getSuperBound());
  }

  // ------------------------------------------------------------------------
  // Union as subtype
  @Override
  public Boolean visitUnion_Declared(
      AnnotatedUnionType subtype, AnnotatedDeclaredType supertype, Void p) {
    return visitUnionSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Intersection(
      AnnotatedUnionType subtype, AnnotatedIntersectionType supertype, Void p) {
    // For example:
    // <T extends Throwable & Cloneable> void method(T param) {}
    // ...
    // catch (Exception1 | Exception2 union) { // Assuming Exception1 and Exception2 implement
    // Cloneable
    //   method(union);
    // This case happens when checking that the inferred type argument is a subtype of the
    // declared type argument of method.
    // See org.checkerframework.common.basetype.BaseTypeVisitor#checkTypeArguments
    return visitUnionSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Union(
      AnnotatedUnionType subtype, AnnotatedUnionType supertype, Void p) {
    // For example:
    // <T> void method(T param) {}
    // ...
    // catch (Exception1 | Exception2 union) {
    //   method(union);
    // This case happens when checking the arguments to method after type variable substitution
    return visitUnionSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Wildcard(
      AnnotatedUnionType subtype, AnnotatedWildcardType supertype, Void p) {
    // For example:
    // } catch (RuntimeException | IOException e) {
    //     ArrayList<? super Exception> lWildcard = new ArrayList<>();
    //     lWildcard.add(e);

    return visitWildcardSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Typevar(
      AnnotatedUnionType subtype, AnnotatedTypeVariable supertype, Void p) {
    // For example:
    // } catch (RuntimeException | IOException e) {
    //     ArrayList<? super Exception> lWildcard = new ArrayList<>();
    //     lWildcard.add(e);

    return visitTypevarSupertype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // typevar as subtype
  @Override
  public Boolean visitTypevar_Declared(
      AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, Void p) {
    return visitTypevarSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Intersection(
      AnnotatedTypeVariable subtype, AnnotatedIntersectionType supertype, Void p) {
    // this can happen when checking type param bounds
    return visitIntersectionSupertype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Primitive(
      AnnotatedTypeVariable subtype, AnnotatedPrimitiveType supertype, Void p) {
    return visitTypevarSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Array(
      AnnotatedTypeVariable subtype, AnnotatedArrayType supertype, Void p) {
    // This happens when the type variable is a captured wildcard.
    return visitTypevarSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Typevar(
      AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, Void p) {

    if (AnnotatedTypes.haveSameDeclaration(checker.getTypeUtils(), subtype, supertype)) {
      // subtype and supertype are uses of the same type parameter
      boolean subtypeHasAnno = subtype.getAnnotationInHierarchy(currentTop) != null;
      boolean supertypeHasAnno = supertype.getAnnotationInHierarchy(currentTop) != null;

      if (subtypeHasAnno && supertypeHasAnno) {
        // if both have primary annotations then you can just check the primary annotations
        // as the bounds are the same
        return isPrimarySubtype(subtype, supertype);

      } else if (!subtypeHasAnno && !supertypeHasAnno && areEqualInHierarchy(subtype, supertype)) {
        // two unannotated uses of the same type parameter are of the same type
        return true;
      } else if (subtypeHasAnno) {
        Set<AnnotationMirror> superLBs =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualifierHierarchy, supertype);
        AnnotationMirror superLB =
            qualifierHierarchy.findAnnotationInHierarchy(superLBs, currentTop);
        return qualifierHierarchy.isSubtype(subtype.getAnnotationInHierarchy(currentTop), superLB);
      } else if (supertypeHasAnno) {
        return qualifierHierarchy.isSubtype(
            subtype.getEffectiveAnnotationInHierarchy(currentTop),
            supertype.getAnnotationInHierarchy(currentTop));

      } else if (subtype.getUpperBound().getKind() == TypeKind.INTERSECTION) {
        // This case happens when a type has an intersection bound.  e.g.,
        // T extends A & B
        //
        // And one use of the type has an annotation and the other does not. e.g.,
        // @X T xt = ...;  T t = ..;
        // xt = t;
        //
        return visit(subtype.getUpperBound(), supertype.getLowerBound(), null);
      }
    }

    if (AnnotatedTypes.areCorrespondingTypeVariables(
        checker.getProcessingEnvironment().getElementUtils(), subtype, supertype)) {
      if (areEqualInHierarchy(subtype, supertype)) {
        return true;
      }
    }

    if (TypesUtils.isCaptured(subtype.getUnderlyingType())
        && TypesUtils.isCaptured(supertype.getUnderlyingType())) {
      return isContainedByBoundType(subtype, new BoundType(supertype), false);
    }

    if (supertype.getLowerBound().getKind() != TypeKind.NULL) {
      return visit(subtype, supertype.getLowerBound(), p);
    }
    // check that the upper bound of the subtype is below the lower bound of the supertype
    return visitTypevarSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Null(
      AnnotatedTypeVariable subtype, AnnotatedNullType supertype, Void p) {
    return visitTypevarSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Wildcard(
      AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcardSupertype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // wildcard as subtype

  @Override
  public Boolean visitWildcard_Array(
      AnnotatedWildcardType subtype, AnnotatedArrayType supertype, Void p) {
    return visitWildcardSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Declared(
      AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, Void p) {
    if (subtype.isUninferredTypeArgument()) {
      if (subtype.atypeFactory.ignoreUninferredTypeArguments) {
        return true;
      } else if (supertype.getTypeArguments().isEmpty()) {
        // visitWildcardSubtype doesn't check uninferred type arguments, because the
        // underlying Java types may not be in the correct relationship.  But, if the
        // declared type does not have type arguments, then checking primary annotations is
        // sufficient.
        // For example, if the wildcard is ? extends @Nullable Object and the supertype is
        // @Nullable String, then it is safe to return true. However if the supertype is
        // @NullableList<@NonNull String> then it's not possible to decide if it is a
        // subtype of the wildcard.
        AnnotationMirror subtypeAnno = subtype.getEffectiveAnnotationInHierarchy(currentTop);
        AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
        return qualifierHierarchy.isSubtype(subtypeAnno, supertypeAnno);
      }
    }
    return visitWildcardSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Intersection(
      AnnotatedWildcardType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitWildcardSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Primitive(
      AnnotatedWildcardType subtype, AnnotatedPrimitiveType supertype, Void p) {
    if (subtype.isUninferredTypeArgument()) {
      AnnotationMirror subtypeAnno = subtype.getEffectiveAnnotationInHierarchy(currentTop);
      AnnotationMirror supertypeAnno = supertype.getAnnotationInHierarchy(currentTop);
      return qualifierHierarchy.isSubtype(subtypeAnno, supertypeAnno);
    }
    return visitWildcardSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Typevar(
      AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitWildcardSubtype(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Wildcard(
      AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcardSubtype(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // These "visit" methods are utility methods that aren't part of the visit interface
  // but that handle cases that more than one visit method shares in common.

  /**
   * An intersection is a supertype if all of its bounds are a supertype of subtype.
   *
   * @param subtype the possible subtype
   * @param supertype the possible supertype
   * @return true {@code subtype} is a subtype of {@code supertype}
   */
  protected boolean visitIntersectionSupertype(
      AnnotatedTypeMirror subtype, AnnotatedIntersectionType supertype) {
    if (isSubtypeVisitHistory.contains(subtype, supertype, currentTop)) {
      return true;
    }
    boolean result = true;
    for (AnnotatedTypeMirror bound : supertype.getBounds()) {
      // Only call isSubtype if the Java type is actually a subtype; otherwise,
      // only check primary qualifiers.
      if (TypesUtils.isErasedSubtype(
              subtype.getUnderlyingType(), bound.getUnderlyingType(), subtype.atypeFactory.types)
          && !isSubtype(subtype, bound, currentTop)) {
        result = false;
        break;
      }
    }
    isSubtypeVisitHistory.put(subtype, supertype, currentTop, result);
    return result;
  }

  /**
   * An intersection is a subtype if one of its bounds is a subtype of {@code supertype}.
   *
   * @param subtype an intersection type
   * @param supertype an annotated type
   * @return whether {@code subtype} is a subtype of {@code supertype}
   */
  protected boolean visitIntersectionSubtype(
      AnnotatedIntersectionType subtype, AnnotatedTypeMirror supertype) {
    Types types = checker.getTypeUtils();
    // The primary annotations of the bounds should already be the same as the annotations on
    // the intersection type.
    for (AnnotatedTypeMirror subtypeBound : subtype.getBounds()) {
      if (TypesUtils.isErasedSubtype(
              subtypeBound.getUnderlyingType(), supertype.getUnderlyingType(), types)
          && isSubtype(subtypeBound, supertype, currentTop)) {
        return true;
      }
    }
    return false;
  }

  /**
   * A type variable is a supertype if its lower bound is above subtype.
   *
   * @param subtype a type that might be a subtype
   * @param supertype a type that might be a supertype
   * @return true if {@code subtype} is a subtype of {@code supertype}
   */
  protected boolean visitTypevarSupertype(
      AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype) {
    return isSubtypeCaching(subtype, supertype.getLowerBound());
  }

  /**
   * A type variable is a subtype if its upper bounds is below the supertype. Note: When comparing
   * two type variables this method and visitTypevarSupertype will combine to isValid the subtypes
   * upper bound against the supertypes lower bound.
   */
  protected boolean visitTypevarSubtype(
      AnnotatedTypeVariable subtype, AnnotatedTypeMirror supertype) {
    AnnotatedTypeMirror upperBound = subtype.getUpperBound();
    if (TypesUtils.isBoxedPrimitive(upperBound.getUnderlyingType())
        && supertype instanceof AnnotatedPrimitiveType) {
      upperBound = supertype.atypeFactory.getUnboxedType((AnnotatedDeclaredType) upperBound);
    }
    if (supertype.getKind() == TypeKind.DECLARED
        && TypesUtils.getTypeElement(supertype.getUnderlyingType()).getKind()
            == ElementKind.INTERFACE) {
      // Make sure the upper bound is no wildcard or type variable
      while (upperBound.getKind() == TypeKind.TYPEVAR
          || upperBound.getKind() == TypeKind.WILDCARD) {
        if (upperBound.getKind() == TypeKind.TYPEVAR) {
          upperBound = ((AnnotatedTypeVariable) upperBound).getUpperBound();
        }
        if (upperBound.getKind() == TypeKind.WILDCARD) {
          upperBound = ((AnnotatedWildcardType) upperBound).getExtendsBound();
        }
      }
      // If the supertype is an interface, only compare the primary annotations.
      // The actual type argument could implement the interface and the bound of
      // the type variable must not implement the interface.
      if (upperBound.getKind() == TypeKind.INTERSECTION) {
        Types types = checker.getTypeUtils();
        for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) upperBound).getBounds()) {
          // Make sure the upper bound is no wildcard or type variable
          while (bound.getKind() == TypeKind.TYPEVAR || bound.getKind() == TypeKind.WILDCARD) {
            if (bound.getKind() == TypeKind.TYPEVAR) {
              bound = ((AnnotatedTypeVariable) bound).getUpperBound();
            }
            if (bound.getKind() == TypeKind.WILDCARD) {
              bound = ((AnnotatedWildcardType) bound).getExtendsBound();
            }
          }
          if (TypesUtils.isErasedSubtype(
                  bound.getUnderlyingType(), supertype.getUnderlyingType(), types)
              && isPrimarySubtype(bound, supertype)) {
            return true;
          }
        }
        return false;
      }
    }
    return isSubtypeCaching(upperBound, supertype);
  }

  /** A union type is a subtype if ALL of its alternatives are subtypes of supertype. */
  protected boolean visitUnionSubtype(AnnotatedUnionType subtype, AnnotatedTypeMirror supertype) {
    return areAllSubtypes(subtype.getAlternatives(), supertype);
  }

  protected boolean visitWildcardSupertype(
      AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype) {
    if (supertype.isUninferredTypeArgument()) { // TODO: REMOVE WHEN WE FIX TYPE ARG INFERENCE
      // Can't call isSubtype because underlying Java types won't be subtypes.
      return supertype.atypeFactory.ignoreUninferredTypeArguments;
    }
    return isSubtype(subtype, supertype.getSuperBound(), currentTop);
  }

  protected boolean visitWildcardSubtype(
      AnnotatedWildcardType subtype, AnnotatedTypeMirror supertype) {
    if (subtype.isUninferredTypeArgument()) {
      return subtype.atypeFactory.ignoreUninferredTypeArguments;
    }
    TypeMirror superTypeMirror = supertype.getUnderlyingType();
    if (supertype.getKind() == TypeKind.TYPEVAR) {
      TypeVariable atv = (TypeVariable) supertype.getUnderlyingType();
      if (TypesUtils.isCaptured(atv)) {
        superTypeMirror = TypesUtils.getCapturedWildcard(atv);
      }
    }

    if (superTypeMirror.getKind() == TypeKind.WILDCARD) {
      // This can happen at a method invocation where a type variable in the method
      // declaration is substituted with a wildcard.
      // For example:
      // <T> void method(Gen<T> t) {}
      // Gen<?> x;
      // method(x); // this method is called when checking this method call
      // And also when checking lambdas

      boolean subtypeHasAnno = subtype.getAnnotationInHierarchy(currentTop) != null;
      boolean supertypeHasAnno = supertype.getAnnotationInHierarchy(currentTop) != null;

      if (subtypeHasAnno && supertypeHasAnno) {
        // if both have primary annotations then just check the primary annotations
        // as the bounds are the same
        return isPrimarySubtype(subtype, supertype);

      } else if (!subtypeHasAnno && !supertypeHasAnno && areEqualInHierarchy(subtype, supertype)) {
        // TODO: wildcard capture conversion
        // TODO: can this be removed?
        // Two unannotated uses of wildcard types are the same type
        return true;
      }
    }

    return isSubtype(subtype.getExtendsBound(), supertype, currentTop);
  }
}
