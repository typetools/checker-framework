package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.AnnotatedFor;
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
import org.checkerframework.javacutil.AnnotationMirrorSet;
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
 * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-4.10">section 4.10,
 * "Subtyping"</a>.
 *
 * <p>Note: The visit methods of this class must be public but it is intended to be used through a
 * TypeHierarchy interface reference which will only allow isSubtype to be called. Clients should
 * not call the visit methods.
 *
 * <p>The visit methods return true if the first argument is a subtype of the second argument.
 */
public class DefaultTypeHierarchy extends AbstractAtmComboVisitor<Boolean, Void>
    implements TypeHierarchy {
  /**
   * The type-checker that is associated with this.
   *
   * <p>Used for processingEnvironment when needed.
   */
  protected final BaseTypeChecker checker;

  /** The qualifier hierarchy that is associated with this. */
  protected final QualifierHierarchy qualHierarchy;

  /** The equality comparer. */
  protected final StructuralEqualityComparer equalityComparer;

  /** Whether to ignore raw types. */
  protected final boolean ignoreRawTypes;

  /** Whether to make array subtyping invariant with respect to array component types. */
  protected final boolean invariantArrayComponents;

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
   * @param qualHierarchy the qualifier hierarchy that is associated with this
   * @param ignoreRawTypes whether to ignore raw types
   * @param invariantArrayComponents whether to make array subtyping invariant with respect to array
   *     component types
   */
  public DefaultTypeHierarchy(
      BaseTypeChecker checker,
      QualifierHierarchy qualHierarchy,
      boolean ignoreRawTypes,
      boolean invariantArrayComponents) {
    this.checker = checker;
    this.qualHierarchy = qualHierarchy;
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
  public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
    for (AnnotationMirror top : qualHierarchy.getTopAnnotations()) {
      if (!isSubtype(subtype, supertype, top)) {
        return false;
      }
    }

    return true;
  }

  /** A set of annotations and a {@link TypeMirror}. */
  @AnnotatedFor("nullness")
  private static class ShallowType {

    /** A set of annotations. */
    AnnotationMirrorSet annos;

    /** A TypeMirror. */
    TypeMirror typeMirror;

    /**
     * Creates a {@code ShallowType}.
     *
     * @param annos a set of annotations
     * @param typeMirror a type mirror
     */
    private ShallowType(AnnotationMirrorSet annos, TypeMirror typeMirror) {
      this.annos = annos;
      this.typeMirror = typeMirror;
    }

    /**
     * Creates a {@code ShallowType} from {@code type}: the annotations are the effective
     * annotations on {@code type} and the type mirror is the underlying type of {@code type}.
     *
     * @param type an annotated type to convert to a {@code ShallowType}
     * @return a shallow type created from {@code type}
     */
    @SuppressWarnings("nullness") // AnnotatedTypeMirror isn't annotated for nullness.
    public static ShallowType create(AnnotatedTypeMirror type) {
      AnnotatedTypeMirror erasedType = type.getErased();
      TypeMirror typeMirror =
          erasedType.getKind() == type.getKind()
              ? type.getUnderlyingType()
              : erasedType.getUnderlyingType();
      // The effective annotations are the primary annotations on the erased type.
      return new ShallowType(erasedType.getPrimaryAnnotations(), typeMirror);
    }
  }

  @Override
  public boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
    ShallowType subShallowType = ShallowType.create(subtype);
    ShallowType superShallowType = ShallowType.create(supertype);
    return qualHierarchy.isSubtypeShallow(
        subShallowType.annos,
        subShallowType.typeMirror,
        superShallowType.annos,
        superShallowType.typeMirror);
  }

  @Override
  public boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror hierarchy) {
    ShallowType subShallowType = ShallowType.create(subtype);
    ShallowType superShallowType = ShallowType.create(supertype);
    return qualHierarchy.isSubtypeShallow(
        qualHierarchy.findAnnotationInSameHierarchy(subShallowType.annos, hierarchy),
        subShallowType.typeMirror,
        qualHierarchy.findAnnotationInSameHierarchy(superShallowType.annos, hierarchy),
        superShallowType.typeMirror);
  }

  @Override
  public boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, Collection<? extends AnnotationMirror> superQualifiers) {
    ShallowType subShallowType = ShallowType.create(subtype);
    return qualHierarchy.isSubtypeShallow(
        subShallowType.annos, superQualifiers, subShallowType.typeMirror);
  }

  @Override
  public boolean isSubtypeShallowEffective(
      Collection<? extends AnnotationMirror> subQualifiers, AnnotatedTypeMirror supertype) {
    ShallowType superShallowType = ShallowType.create(supertype);
    return qualHierarchy.isSubtypeShallow(
        subQualifiers, superShallowType.annos, superShallowType.typeMirror);
  }

  @Override
  public boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, AnnotationMirror superQualifier) {
    ShallowType subShallowType = ShallowType.create(subtype);
    return qualHierarchy.isSubtypeShallow(
        qualHierarchy.findAnnotationInSameHierarchy(subShallowType.annos, superQualifier),
        superQualifier,
        subShallowType.typeMirror);
  }

  @Override
  public boolean isSubtypeShallowEffective(
      AnnotationMirror subQualifier, AnnotatedTypeMirror supertype) {
    ShallowType superShallowType = ShallowType.create(supertype);
    return qualHierarchy.isSubtypeShallow(
        subQualifier,
        qualHierarchy.findAnnotationInSameHierarchy(superShallowType.annos, subQualifier),
        superShallowType.typeMirror);
  }

  /**
   * Returns true if {@code subtype <: supertype}, but only for the hierarchy of which {@code top}
   * is the top.
   *
   * @param subtype expected subtype
   * @param supertype expected supertype
   * @param top the top of the hierarchy for which we want to make a comparison
   * @return true if {@code subtype} is a subtype of or equal to {@code supertype}, in the qualifier
   *     hierarchy whose top is {@code top}
   */
  protected boolean isSubtype(
      AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top) {
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
  public String defaultErrorMessage(
      AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, Void p) {
    return super.defaultErrorMessage(subtype, supertype, p)
        + System.lineSeparator()
        + "  visitHistory = "
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
    TypeMirror subTM = subtype.getUnderlyingType();
    TypeMirror superTM = supertype.getUnderlyingType();

    AnnotationMirror subtypeAnno = subtype.getPrimaryAnnotationInHierarchy(currentTop);
    AnnotationMirror supertypeAnno = supertype.getPrimaryAnnotationInHierarchy(currentTop);
    if (checker.getTypeFactory().hasQualifierParameterInHierarchy(supertype, currentTop)
        && checker.getTypeFactory().hasQualifierParameterInHierarchy(subtype, currentTop)) {
      // If the types have a class qualifier parameter, the qualifiers must be equivalent.
      return qualHierarchy.isSubtypeShallow(subtypeAnno, subTM, supertypeAnno, superTM)
          && qualHierarchy.isSubtypeShallow(supertypeAnno, superTM, subtypeAnno, subTM);
    }

    return qualHierarchy.isSubtypeShallow(subtypeAnno, subTM, supertypeAnno, superTM);
  }

  /**
   * Like {@link #isSubtype(AnnotatedTypeMirror, AnnotatedTypeMirror)}, but uses a cache to prevent
   * infinite recursion on recursive types.
   *
   * @param subtype a type that may be a subtype
   * @param supertype a type that may be a supertype
   * @return true if subtype {@literal <:} supertype
   */
  protected boolean isSubtypeCaching(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype) {
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
      Iterable<? extends AnnotatedTypeMirror> subtypes, AnnotatedTypeMirror supertype) {
    for (AnnotatedTypeMirror subtype : subtypes) {
      if (!isSubtype(subtype, supertype, currentTop)) {
        return false;
      }
    }

    return true;
  }

  protected boolean areEqualInHierarchy(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    return equalityComparer.areEqualInHierarchy(type1, type2, currentTop);
  }

  /**
   * Returns true if {@code outside} contains {@code inside}, that is, if the set of types denoted
   * by {@code outside} is a superset of, or equal to, the set of types denoted by {@code inside}.
   *
   * <p>Containment is described in <a
   * href="https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-4.5.1">JLS section
   * 4.5.1 "Type Arguments of Parameterized Types"</a>.
   *
   * <p>As described in <a
   * href=https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html#jls-4.10.2>JLS section
   * 4.10.2 Subtyping among Class and Interface Types</a>, a declared type S is considered a
   * supertype of another declared type T only if all of S's type arguments "contain" the
   * corresponding type arguments of the subtype T.
   *
   * @param inside a possibly-contained type; its underlying type is contained by {@code outside}'s
   *     underlying type
   * @param outside a possibly-containing type; its underlying type contains {@code inside}'s
   *     underlying type
   * @param canBeCovariant whether or not type arguments are allowed to be covariant
   * @return true if inside is contained by outside, or if canBeCovariant == true and {@code inside
   *     <: outside}
   */
  protected boolean isContainedBy(
      AnnotatedTypeMirror inside, AnnotatedTypeMirror outside, boolean canBeCovariant) {
    Boolean previousResult = areEqualVisitHistory.get(inside, outside, currentTop);
    if (previousResult != null) {
      return previousResult;
    }

    if (shouldIgnoreRawTypeArgs(inside) || shouldIgnoreRawTypeArgs(outside)) {
      areEqualVisitHistory.put(inside, outside, currentTop, true);
      return true;
    }

    if (outside.getKind() == TypeKind.WILDCARD) {
      // This is all cases except bullet 6, "T <= T".
      AnnotatedWildcardType outsideWildcard = (AnnotatedWildcardType) outside;

      // Add a placeholder in case of recursion, to prevent infinite regress.
      areEqualVisitHistory.put(inside, outside, currentTop, true);
      boolean result =
          isContainedWithinBounds(
              inside,
              outsideWildcard.getSuperBound(),
              outsideWildcard.getExtendsBound(),
              canBeCovariant);
      areEqualVisitHistory.put(inside, outside, currentTop, result);
      return result;
    } else if (TypesUtils.isCapturedTypeVariable(outside.getUnderlyingType())) {
      // Sometimes the wildcard has been captured too early, so treat the captured type
      // variable as wildcard.
      // This is all cases except bullet 6, "T <= T".
      AnnotatedTypeVariable outsideTypeVar = (AnnotatedTypeVariable) outside;

      // Add a placeholder in case of recursion, to prevent infinite regress.
      areEqualVisitHistory.put(inside, outside, currentTop, true);
      boolean result =
          isContainedWithinBounds(
              inside,
              outsideTypeVar.getLowerBound(),
              outsideTypeVar.getUpperBound(),
              canBeCovariant);
      areEqualVisitHistory.put(inside, outside, currentTop, result);
      if (result) {
        return true;
      }
      areEqualVisitHistory.remove(inside, outsideTypeVar, currentTop);
    }

    // The remainder of the method is bullet 6, "T <= T".
    if (canBeCovariant) {
      return isSubtype(inside, outside, currentTop);
    }

    try {
      return areEqualInHierarchy(inside, outside);
    } catch (Exception e) {
      // Ignore exception and try capturing.
      // See https://github.com/typetools/checker-framework/issues/6867.
      // https://bugs.openjdk.org/browse/JDK-8054309
    }
    AnnotatedTypeMirror capturedOutside = outside.atypeFactory.applyCaptureConversion(outside);
    previousResult = areEqualVisitHistory.get(inside, capturedOutside, currentTop);
    if (previousResult != null) {
      return previousResult;
    }
    return areEqualInHierarchy(inside, capturedOutside);
  }

  /**
   * Let {@code outside} be {@code ? super outsideLower extends outsideUpper}. Returns true if
   * {@code outside} contains {@code inside}, that is, if the set of types denoted by {@code
   * outside} is a superset of, or equal to, the set of types denoted by {@code inside}.
   *
   * <p>This method is a helper method for {@link #isContainedBy(AnnotatedTypeMirror,
   * AnnotatedTypeMirror, boolean)}.
   *
   * @param inside a possibly-contained type
   * @param outsideLower the lower bound of the possibly-containing type
   * @param outsideUpper the upper bound of the possibly-containing type
   * @param canBeCovariant whether or not type arguments are allowed to be covariant
   * @return true if inside is contained by outside, or if canBeCovariant == true and {@code inside
   *     <: outside}
   */
  protected boolean isContainedWithinBounds(
      AnnotatedTypeMirror inside,
      AnnotatedTypeMirror outsideLower,
      AnnotatedTypeMirror outsideUpper,
      boolean canBeCovariant) {
    try {
      if (canBeCovariant) {
        if (outsideLower.getKind() == TypeKind.NULL) {
          return isSubtype(inside, outsideUpper);
        } else {
          return isSubtype(outsideLower, inside);
        }
      }
      // If inside is a wildcard, then isSubtype(outsideLower, inside) calls
      // isSubtype(outsideLower, inside.getLowerBound()) and isSubtype(inside, outsideUpper)
      // calls isSubtype(inside.getUpperBound(), outsideUpper). This is slightly different
      // from the algorithm in the JLS.  Only one of the Java type bounds can be specified,
      // but there can be annotations on both the upper and lower bound of a wildcard.
      return isSubtype(outsideLower, inside) && isSubtype(inside, outsideUpper);
    } catch (Throwable ex) {
      // Work around:
      // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8265255
      if (ex.getMessage().contains("AsSuperVisitor")) {
        return false;
      }
      throw ex;
    }
  }

  /**
   * Returns true if {@code type} is a type argument from a raw type and if the checker should not
   * issue warnings about such type arguments.
   *
   * @param type type to check
   * @return true if {@code type} is a type argument from a raw type and if the checker should not
   *     issue warnings about such type arguments.
   */
  private boolean shouldIgnoreRawTypeArgs(AnnotatedTypeMirror type) {
    return this.ignoreRawTypes && AnnotatedTypes.isTypeArgOfRawType(type);
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
    return visitType_Wildcard(subtype, supertype);
  }

  @Override
  public Boolean visitArray_Typevar(
      AnnotatedArrayType subtype, AnnotatedTypeVariable superType, Void p) {
    return visitType_Typevar(subtype, superType);
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

    if (isSubtypeVisitHistory.contains(subtype, supertype, currentTop)) {
      return true;
    }

    boolean result =
        visitTypeArgs(
            subtype, supertype, subtype.isUnderlyingTypeRaw(), supertype.isUnderlyingTypeRaw());
    isSubtypeVisitHistory.put(subtype, supertype, currentTop, result);

    return result;
  }

  /**
   * Returns true if the type arguments in {@code supertype} contain the type arguments in {@code
   * subtype} and false otherwise. See {@link #isContainedBy} for an explanation of containment.
   *
   * @param subtype a possible subtype
   * @param supertype a possible supertype
   * @param subtypeRaw whether {@code subtype} is a raw type
   * @param supertypeRaw whether {@code supertype} is a raw type
   * @return true if the type arguments in {@code supertype} contain the type arguments in {@code
   *     subtype} and false otherwise
   */
  protected boolean visitTypeArgs(
      AnnotatedDeclaredType subtype,
      AnnotatedDeclaredType supertype,
      boolean subtypeRaw,
      boolean supertypeRaw) {
    AnnotatedTypeFactory typeFactory = subtype.atypeFactory;

    // JLS 11: 4.10.2. Subtyping among Class and Interface Types
    // 4th paragraph, bullet 1.
    AnnotatedDeclaredType subtypeAsSuper =
        AnnotatedTypes.castedAsSuper(typeFactory, subtype, supertype);

    if (ignoreRawTypes && (subtypeRaw || supertypeRaw)) {
      return true;
    }

    List<? extends AnnotatedTypeMirror> subtypeTypeArgs = subtypeAsSuper.getTypeArguments();
    List<? extends AnnotatedTypeMirror> supertypeTypeArgs = supertype.getTypeArguments();

    if (subtypeTypeArgs.size() != supertypeTypeArgs.size()) {
      throw new BugInCF("Type arguments are not the same size: %s %s", subtypeAsSuper, supertype);
    }
    // This method, `visitTypeArgs`, is called even if `subtype` doesn't have type arguments.
    if (subtypeTypeArgs.isEmpty()) {
      return true;
    }

    List<Integer> covariantArgIndexes = getCovariantArgIndexes(supertype);

    // JLS 11: 4.10.2. Subtyping among Class and Interface Types
    // 4th paragraph, bullet 2
    try {
      if (isContainedMany(
          subtypeAsSuper.getTypeArguments(), supertypeTypeArgs, covariantArgIndexes)) {
        return true;
      }
    } catch (Exception e) {
      // Some types need to be captured first, so ignore crashes.
      for (int i = 0; i < supertypeTypeArgs.size(); i++) {
        areEqualVisitHistory.remove(
            subtypeAsSuper.getTypeArguments().get(i), supertypeTypeArgs.get(i), currentTop);
      }
    }
    // 5th paragraph:
    // Instead of calling isSubtype with the captured type, just check for containment.
    AnnotatedDeclaredType capturedSubtype =
        (AnnotatedDeclaredType) typeFactory.applyCaptureConversion(subtype);
    AnnotatedDeclaredType capturedSubtypeAsSuper =
        AnnotatedTypes.castedAsSuper(typeFactory, capturedSubtype, supertype);
    return isContainedMany(
        capturedSubtypeAsSuper.getTypeArguments(), supertypeTypeArgs, covariantArgIndexes);
  }

  @Override
  public List<Integer> getCovariantArgIndexes(AnnotatedDeclaredType type) {
    TypeElement supertypeElem = (TypeElement) type.getUnderlyingType().asElement();
    AnnotationMirror covariantAnno =
        type.atypeFactory.getDeclAnnotation(supertypeElem, Covariant.class);
    if (covariantAnno == null) {
      return Collections.emptyList();
    }

    return AnnotationUtils.getElementValueArray(
        covariantAnno, covariantValueElement, Integer.class);
  }

  /**
   * Calls {@link #isContainedBy(AnnotatedTypeMirror, AnnotatedTypeMirror, boolean)} on the two
   * lists of type arguments. Returns true if every type argument in {@code supertypeTypeArgs}
   * contains the type argument at the same index in {@code subtypeTypeArgs}.
   *
   * @param subtypeTypeArgs subtype arguments
   * @param supertypeTypeArgs supertype arguments
   * @param covariantArgIndexes indexes into the type arguments list which correspond to the type
   *     arguments that are marked @{@link Covariant}.
   * @return whether {@code supertypeTypeArgs} contain {@code subtypeTypeArgs}
   */
  protected boolean isContainedMany(
      List<? extends AnnotatedTypeMirror> subtypeTypeArgs,
      List<? extends AnnotatedTypeMirror> supertypeTypeArgs,
      List<Integer> covariantArgIndexes) {
    for (int i = 0; i < supertypeTypeArgs.size(); i++) {
      AnnotatedTypeMirror superTypeArg = supertypeTypeArgs.get(i);
      AnnotatedTypeMirror subTypeArg = subtypeTypeArgs.get(i);
      boolean covariant = covariantArgIndexes.contains(i);
      if (!isContainedBy(subTypeArg, superTypeArg, covariant)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Boolean visitDeclared_Intersection(
      AnnotatedDeclaredType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitType_Intersection(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Null(
      AnnotatedDeclaredType subtype, AnnotatedNullType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitDeclared_Primitive(
      AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, Void p) {
    AnnotatedTypeMirror unboxedType;
    try {
      unboxedType = subtype.atypeFactory.getUnboxedType(subtype);
    } catch (IllegalArgumentException ex) {
      throw new BugInCF(
          "DefaultTypeHierarchy: subtype isn't a boxed type: subtype: %s superType: %s",
          subtype, supertype);
    }
    return isPrimarySubtype(unboxedType, supertype);
  }

  @Override
  public Boolean visitDeclared_Typevar(
      AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitType_Typevar(subtype, supertype);
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
    return visitType_Wildcard(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // Intersection as subtype
  @Override
  public Boolean visitIntersection_Declared(
      AnnotatedIntersectionType subtype, AnnotatedDeclaredType supertype, Void p) {
    return visitIntersection_Type(subtype, supertype);
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
    return visitIntersection_Type(subtype, supertype);
  }

  @Override
  public Boolean visitIntersection_Wildcard(
      AnnotatedIntersectionType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitIntersection_Type(subtype, supertype);
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
    return visitType_Typevar(subtype, supertype);
  }

  @Override
  public Boolean visitNull_Wildcard(
      AnnotatedNullType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitType_Wildcard(subtype, supertype);
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
    AnnotatedTypeFactory atypeFactory = subtype.atypeFactory;
    Types types = atypeFactory.types;
    AnnotatedPrimitiveType narrowedType = subtype;
    if (TypesUtils.isBoxedPrimitive(supertype.getUnderlyingType())) {
      TypeMirror unboxedSuper = types.unboxedType(supertype.getUnderlyingType());
      if (unboxedSuper.getKind() != subtype.getKind()
          && TypesUtils.canBeNarrowingPrimitiveConversion(unboxedSuper, types)) {
        narrowedType = atypeFactory.getNarrowedPrimitive(subtype, unboxedSuper);
      }
    }
    AnnotatedTypeMirror boxedSubtype = atypeFactory.getBoxedType(narrowedType);
    return isPrimarySubtype(boxedSubtype, supertype);
  }

  @Override
  public Boolean visitPrimitive_Primitive(
      AnnotatedPrimitiveType subtype, AnnotatedPrimitiveType supertype, Void p) {
    return isPrimarySubtype(subtype, supertype);
  }

  @Override
  public Boolean visitPrimitive_Intersection(
      AnnotatedPrimitiveType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitType_Intersection(subtype, supertype);
  }

  @Override
  public Boolean visitPrimitive_Typevar(
      AnnotatedPrimitiveType subtype, AnnotatedTypeVariable supertype, Void p) {
    return AtmCombo.accept(subtype, supertype.getUpperBound(), null, this);
  }

  @Override
  public Boolean visitPrimitive_Wildcard(
      AnnotatedPrimitiveType subtype, AnnotatedWildcardType supertype, Void p) {
    if (shouldIgnoreRawTypeArgs(supertype)) {
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
    return visitUnion_Type(subtype, supertype);
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
    return visitUnion_Type(subtype, supertype);
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
    return visitUnion_Type(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Wildcard(
      AnnotatedUnionType subtype, AnnotatedWildcardType supertype, Void p) {
    // For example:
    // } catch (RuntimeException | IOException e) {
    //     ArrayList<? super Exception> lWildcard = new ArrayList<>();
    //     lWildcard.add(e);

    return visitType_Wildcard(subtype, supertype);
  }

  @Override
  public Boolean visitUnion_Typevar(
      AnnotatedUnionType subtype, AnnotatedTypeVariable supertype, Void p) {
    // For example:
    // } catch (RuntimeException | IOException e) {
    //     ArrayList<? super Exception> lWildcard = new ArrayList<>();
    //     lWildcard.add(e);

    return visitType_Typevar(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // typevar as subtype
  @Override
  public Boolean visitTypevar_Declared(
      AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, Void p) {
    return visitTypevar_Type(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Intersection(
      AnnotatedTypeVariable subtype, AnnotatedIntersectionType supertype, Void p) {
    // this can happen when checking type param bounds
    return visitType_Intersection(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Primitive(
      AnnotatedTypeVariable subtype, AnnotatedPrimitiveType supertype, Void p) {
    return visitTypevar_Type(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Array(
      AnnotatedTypeVariable subtype, AnnotatedArrayType supertype, Void p) {
    // This happens when the type variable is a captured wildcard.
    return visitTypevar_Type(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Typevar(
      AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, Void p) {

    TypeMirror subTM = subtype.getUnderlyingType();
    TypeMirror superTM = supertype.getUnderlyingType();

    if (AnnotatedTypes.haveSameDeclaration(checker.getTypeUtils(), subtype, supertype)) {
      // The underlying types of subtype and supertype are uses of the same type parameter,
      // but they
      // may have different primary annotations.
      boolean subtypeHasAnno = subtype.getPrimaryAnnotationInHierarchy(currentTop) != null;
      boolean supertypeHasAnno = supertype.getPrimaryAnnotationInHierarchy(currentTop) != null;

      if (subtypeHasAnno && supertypeHasAnno) {
        // If both have primary annotations then just check the primary annotations
        // as the bounds are the same.
        return isPrimarySubtype(subtype, supertype);

      } else if (!subtypeHasAnno && !supertypeHasAnno) {
        // two unannotated uses of the same type parameter are of the same type
        return areEqualInHierarchy(subtype, supertype);
      } else if (subtypeHasAnno && !supertypeHasAnno) {
        // This is the case "@A T <: T" where T is a type variable.
        AnnotationMirrorSet superLBs =
            AnnotatedTypes.findEffectiveLowerBoundAnnotations(qualHierarchy, supertype);
        AnnotationMirror superLB = qualHierarchy.findAnnotationInHierarchy(superLBs, currentTop);
        return qualHierarchy.isSubtypeShallow(
            subtype.getPrimaryAnnotationInHierarchy(currentTop), subTM, superLB, superTM);
      } else if (!subtypeHasAnno && supertypeHasAnno) {
        // This is the case "T <: @A T" where T is a type variable.
        return qualHierarchy.isSubtypeShallow(
            subtype.getEffectiveAnnotationInHierarchy(currentTop),
            subTM,
            supertype.getPrimaryAnnotationInHierarchy(currentTop),
            superTM);
      }
    }

    if (AnnotatedTypes.areCorrespondingTypeVariables(
        checker.getProcessingEnvironment().getElementUtils(), subtype, supertype)) {
      if (areEqualInHierarchy(subtype, supertype)) {
        return true;
      }
    }

    if (TypesUtils.isCapturedTypeVariable(subTM) && TypesUtils.isCapturedTypeVariable(superTM)) {
      // This case happens when the captured type variables should be the same type, but
      // aren't because type argument inference isn't implemented correctly.
      if (isContainedWithinBounds(
          subtype, supertype.getLowerBound(), supertype.getUpperBound(), false)) {
        return true;
      }
    }

    if (supertype.getLowerBound().getKind() != TypeKind.NULL) {
      return visit(subtype, supertype.getLowerBound(), p);
    }
    // check that the upper bound of the subtype is below the lower bound of the supertype
    return visitTypevar_Type(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Null(
      AnnotatedTypeVariable subtype, AnnotatedNullType supertype, Void p) {
    return visitTypevar_Type(subtype, supertype);
  }

  @Override
  public Boolean visitTypevar_Wildcard(
      AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, Void p) {
    return visitType_Wildcard(subtype, supertype);
  }

  // ------------------------------------------------------------------------
  // wildcard as subtype

  @Override
  public Boolean visitWildcard_Array(
      AnnotatedWildcardType subtype, AnnotatedArrayType supertype, Void p) {
    return visitWildcard_Type(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Declared(
      AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, Void p) {
    if (subtype.isTypeArgOfRawType()) {
      if (ignoreRawTypes) {
        return true;
      } else if (supertype.getTypeArguments().isEmpty()) {
        // visitWildcard_Type doesn't check type arguments from raw types, because the
        // underlying Java types may not be in the correct relationship.  But, if the
        // declared type does not have type arguments, then checking primary annotations is
        // sufficient.
        // For example, if the wildcard is ? extends @Nullable Object and the supertype is
        // @Nullable String, then it is safe to return true. However if the supertype is
        // @NullableList<@NonNull String> then it's not possible to decide if it is a
        // subtype of the wildcard.
        return isSubtypeShallowEffective(subtype, supertype, currentTop);
      }
    }
    return visitWildcard_Type(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Intersection(
      AnnotatedWildcardType subtype, AnnotatedIntersectionType supertype, Void p) {
    return visitWildcard_Type(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Primitive(
      AnnotatedWildcardType subtype, AnnotatedPrimitiveType supertype, Void p) {
    if (subtype.isTypeArgOfRawType()) {
      return isSubtypeShallowEffective(subtype, supertype, currentTop);
    }
    return visitWildcard_Type(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Typevar(
      AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, Void p) {
    return visitWildcard_Type(subtype, supertype);
  }

  @Override
  public Boolean visitWildcard_Wildcard(
      AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, Void p) {
    return visitWildcard_Type(subtype, supertype);
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
  protected boolean visitType_Intersection(
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
  protected boolean visitIntersection_Type(
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
  protected boolean visitType_Typevar(
      AnnotatedTypeMirror subtype, AnnotatedTypeVariable supertype) {
    return isSubtypeCaching(subtype, supertype.getLowerBound());
  }

  /**
   * A type variable is a subtype if its upper bound is below the supertype.
   *
   * @param subtype a type that might be a subtype
   * @param supertype a type that might be a supertype
   * @return true if {@code subtype} is a subtype of {@code supertype}
   */
  protected boolean visitTypevar_Type(
      AnnotatedTypeVariable subtype, AnnotatedTypeMirror supertype) {
    AnnotatedTypeMirror subtypeUpperBound = subtype.getUpperBound();
    if (TypesUtils.isBoxedPrimitive(subtypeUpperBound.getUnderlyingType())
        && supertype instanceof AnnotatedPrimitiveType) {
      subtypeUpperBound =
          subtype.atypeFactory.getUnboxedType((AnnotatedDeclaredType) subtypeUpperBound);
    }
    if (supertype.getKind() == TypeKind.DECLARED
        && TypesUtils.getTypeElement(supertype.getUnderlyingType()).getKind()
            == ElementKind.INTERFACE) {
      // The supertype is an interface.
      subtypeUpperBound = getNonWildcardOrTypeVarUpperBound(subtypeUpperBound);
      if (subtypeUpperBound.getKind() == TypeKind.INTERSECTION) {
        // Only compare the primary annotations.
        Types types = checker.getTypeUtils();
        for (AnnotatedTypeMirror bound :
            ((AnnotatedIntersectionType) subtypeUpperBound).getBounds()) {
          // Make sure the upper bound is no wildcard or type variable.
          bound = getNonWildcardOrTypeVarUpperBound(bound);
          if (TypesUtils.isErasedSubtype(
                  bound.getUnderlyingType(), supertype.getUnderlyingType(), types)
              && isPrimarySubtype(bound, supertype)) {
            return true;
          }
        }
        return false;
      }
    }
    try {
      return isSubtypeCaching(subtypeUpperBound, supertype);
    } catch (BugInCF e) {
      if (TypesUtils.isCapturedTypeVariable(subtype.underlyingType)) {
        // The upper bound of captured type variable may be computed incorrectly by javac.
        // javac computes the upper bound as a declared type, when it should be an
        // intersection type.
        // (This is a bug in the GLB algorithm; see
        // https://bugs.openjdk.org/browse/JDK-8039222)
        // In this case, the upperbound is not a subtype of `supertype` and the Checker
        // Framework crashes. So catch that crash and just return false.
        // TODO: catch the problem more locally.
        return false;
      }
      throw e;
    }
  }

  /**
   * If {@code type} is a type variable or wildcard recur on its upper bound until an upper bound is
   * found that is neither a type variable nor a wildcard.
   *
   * @param type the type
   * @return if {@code type} is a type variable or wildcard, recur on its upper bound until an upper
   *     bound is found that is neither a type variable nor a wildcard. Otherwise, return {@code
   *     type} itself.
   */
  private AnnotatedTypeMirror getNonWildcardOrTypeVarUpperBound(AnnotatedTypeMirror type) {
    while (type.getKind() == TypeKind.TYPEVAR || type.getKind() == TypeKind.WILDCARD) {
      if (type.getKind() == TypeKind.TYPEVAR) {
        type = ((AnnotatedTypeVariable) type).getUpperBound();
      }
      if (type.getKind() == TypeKind.WILDCARD) {
        type = ((AnnotatedWildcardType) type).getExtendsBound();
      }
    }
    return type;
  }

  /**
   * A union type is a subtype if ALL of its alternatives are subtypes of supertype.
   *
   * @param subtype the potential subtype to check
   * @param supertype the supertype to check
   * @return whether all the alternatives of subtype are subtypes of supertype
   */
  protected boolean visitUnion_Type(AnnotatedUnionType subtype, AnnotatedTypeMirror supertype) {
    return areAllSubtypes(subtype.getAlternatives(), supertype);
  }

  /**
   * Check a wildcard type's relation against a subtype.
   *
   * @param subtype the potential subtype to check
   * @param supertype the wildcard supertype to check
   * @return whether the subtype is a subtype of the supertype's super bound
   */
  protected boolean visitType_Wildcard(
      AnnotatedTypeMirror subtype, AnnotatedWildcardType supertype) {
    if (supertype.isTypeArgOfRawType()) {
      return ignoreRawTypes;
    }
    return isSubtype(subtype, supertype.getSuperBound(), currentTop);
  }

  /**
   * Check a wildcard type's relation against a supertype.
   *
   * @param subtype the potential wildcard subtype to check
   * @param supertype the supertype to check
   * @return whether the subtype's extends bound is a subtype of the supertype
   */
  protected boolean visitWildcard_Type(
      AnnotatedWildcardType subtype, AnnotatedTypeMirror supertype) {
    if (subtype.isTypeArgOfRawType()) {
      return ignoreRawTypes;
    }

    if (supertype.getKind() == TypeKind.WILDCARD) {
      // This can happen at a method invocation where a type variable in the method
      // declaration is substituted with a wildcard.
      // For example:
      //   <T> void method(Gen<T> t) {}
      //   Gen<?> x;
      //   method(x);
      // visitWildcard_Type is called when checking the method call `method(x)`,
      // and also when checking lambdas.

      boolean subtypeHasAnno = subtype.getPrimaryAnnotationInHierarchy(currentTop) != null;
      boolean supertypeHasAnno = supertype.getPrimaryAnnotationInHierarchy(currentTop) != null;

      if (subtypeHasAnno && supertypeHasAnno) {
        // If both have primary annotations then just check the primary annotations
        // as the bounds are the same.
        return isPrimarySubtype(subtype, supertype);

      } else if (!subtypeHasAnno && !supertypeHasAnno && areEqualInHierarchy(subtype, supertype)) {
        // Two unannotated uses of wildcard types are the same type
        return true;
      }
    }

    return isSubtype(subtype.getExtendsBound(), supertype, currentTop);
  }
}
