package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.StringsPlume;

/**
 * A visitor used to compare two type mirrors for "structural" equality. Structural equality implies
 * that, for two objects, all fields are also structurally equal and for primitives their values are
 * equal.
 *
 * <p>See also DefaultTypeHierarchy, and SubtypeVisitHistory
 */
public class StructuralEqualityComparer extends AbstractAtmComboVisitor<Boolean, Void> {
  /** History saving the result of previous comparisons. */
  protected final StructuralEqualityVisitHistory visitHistory;

  // See org.checkerframework.framework.type.DefaultTypeHierarchy.currentTop
  private AnnotationMirror currentTop = null;

  /**
   * Create a StructuralEqualityComparer.
   *
   * @param typeargVisitHistory history saving the result of previous comparisons
   */
  public StructuralEqualityComparer(StructuralEqualityVisitHistory typeargVisitHistory) {
    this.visitHistory = typeargVisitHistory;
  }

  @Override
  protected Boolean defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {
    if (type1.getKind() == TypeKind.NULL || type2.getKind() == TypeKind.NULL) {
      // If one of the types is the NULL type, compare main qualifiers only.
      return arePrimeAnnosEqual(type1, type2);
    }

    if (type1.containsUninferredTypeArguments() || type2.containsUninferredTypeArguments()) {
      return type1.atypeFactory.ignoreUninferredTypeArguments;
    }

    return super.defaultAction(type1, type2, p);
  }

  /**
   * Called for every combination that isn't specifically handled.
   *
   * @return error message explaining the two types' classes are not the same
   */
  @Override
  protected String defaultErrorMessage(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {
    return StringsPlume.joinLines(
        "AnnotatedTypeMirrors aren't structurally equal.",
        "  type1 = " + type1.getClass().getSimpleName() + "( " + type1 + " )",
        "  type2 = " + type2.getClass().getSimpleName() + "( " + type2 + " )",
        "  visitHistory = " + visitHistory);
  }

  /**
   * Returns true if type1 and type2 are structurally equivalent. With one exception,
   * type1.getClass().equals(type2.getClass()) must be true. However, because the Checker Framework
   * sometimes "infers" Typevars to be Wildcards, we allow the combination Wildcard,Typevar. In this
   * case, the two types are "equal" if their bounds are.
   *
   * @param type1 the first AnnotatedTypeMirror to compare
   * @param type2 the second AnnotatedTypeMirror to compare
   * @return true if type1 and type2 are equal
   */
  @EqualsMethod
  private boolean areEqual(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
    if (type1 == type2) {
      return true;
    }
    assert currentTop != null;
    if (type1 == null || type2 == null) {
      return false;
    }
    return AtmCombo.accept(type1, type2, null, this);
  }

  public boolean areEqualInHierarchy(
      final AnnotatedTypeMirror type1,
      final AnnotatedTypeMirror type2,
      final AnnotationMirror top) {
    assert top != null;
    boolean areEqual;
    AnnotationMirror prevTop = currentTop;
    currentTop = top;
    try {
      areEqual = areEqual(type1, type2);
    } finally {
      currentTop = prevTop;
    }

    return areEqual;
  }

  /** Return true if type1 and type2 have the same set of annotations. */
  protected boolean arePrimeAnnosEqual(
      final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
    if (currentTop != null) {
      return AnnotationUtils.areSame(
          type1.getAnnotationInHierarchy(currentTop), type2.getAnnotationInHierarchy(currentTop));
    } else {
      throw new BugInCF("currentTop null");
    }
  }

  /**
   * Compare each type in types1 and types2 pairwise and return true if they are all equal. This
   * method throws an exceptions if types1.size() != types2.size()
   *
   * @return true if for each pair (t1 = types1.get(i); t2 = types2.get(i)), areEqual(t1,t2)
   */
  protected boolean areAllEqual(
      final Collection<? extends AnnotatedTypeMirror> types1,
      final Collection<? extends AnnotatedTypeMirror> types2) {
    if (types1.size() != types2.size()) {
      throw new BugInCF(
          "Mismatching collection sizes:%n    types 1: %s (%d)%n    types 2: %s (%d)",
          StringsPlume.join("; ", types1),
          types1.size(),
          StringsPlume.join("; ", types2),
          types2.size());
    }

    final Iterator<? extends AnnotatedTypeMirror> types1Iter = types1.iterator();
    final Iterator<? extends AnnotatedTypeMirror> types2Iter = types2.iterator();
    while (types1Iter.hasNext()) {
      final AnnotatedTypeMirror type1 = types1Iter.next();
      final AnnotatedTypeMirror type2 = types2Iter.next();
      if (!checkOrAreEqual(type1, type2)) {
        return false;
      }
    }

    return true;
  }

  /**
   * First check visitHistory to see if type1 and type2 have been compared once already. If so
   * return true; otherwise compare them and put them in visitHistory.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @return whether the two types are equal
   */
  protected boolean checkOrAreEqual(
      final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    final Boolean result = areEqual(type1, type2);
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  /**
   * Two arrays are equal if:
   *
   * <ol>
   *   <li>Their sets of primary annotations are equal, and
   *   <li>Their component types are equal
   * </ol>
   */
  @Override
  public Boolean visitArray_Array(
      final AnnotatedArrayType type1, final AnnotatedArrayType type2, final Void p) {
    if (!arePrimeAnnosEqual(type1, type2)) {
      return false;
    }

    return areEqual(type1.getComponentType(), type2.getComponentType());
  }

  /**
   * Two declared types are equal if:
   *
   * <ol>
   *   <li>The types are of the same class/interfaces
   *   <li>Their sets of primary annotations are equal
   *   <li>Their sets of type arguments are equal or one type is raw
   * </ol>
   */
  @Override
  public Boolean visitDeclared_Declared(
      final AnnotatedDeclaredType type1, final AnnotatedDeclaredType type2, final Void p) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    // TODO: same class/interface is not enforced. Why?

    if (!arePrimeAnnosEqual(type1, type2)) {
      return false;
    }

    // Prevent infinite recursion e.g. in Issue1587b
    visitHistory.put(type1, type2, currentTop, true);

    boolean result = visitTypeArgs(type1, type2);
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  /**
   * A helper class for visitDeclared_Declared. There are subtypes of DefaultTypeHierarchy that need
   * to customize the handling of type arguments. This method provides a convenient extension point.
   */
  protected boolean visitTypeArgs(
      final AnnotatedDeclaredType type1, final AnnotatedDeclaredType type2) {

    // TODO: ANYTHING WITH RAW TYPES? SHOULD WE HANDLE THEM LIKE DefaultTypeHierarchy, i.e. use
    // ignoreRawTypes
    final List<? extends AnnotatedTypeMirror> type1Args = type1.getTypeArguments();
    final List<? extends AnnotatedTypeMirror> type2Args = type2.getTypeArguments();

    if (type1Args.isEmpty() && type2Args.isEmpty()) {
      return true;
    }

    if (type1Args.size() == type2Args.size()) {
      return areAllEqual(type1Args, type2Args);
    } else {
      throw new BugInCF(
          "Mismatching type argument sizes:%n    type 1: %s (%d)%n    type 2: %s (%d)",
          type1, type1Args.size(), type2, type2Args.size());
    }
  }

  /**
   * Two intersection types are equal if:
   *
   * <ul>
   *   <li>Their sets of primary annotations are equal
   *   <li>Their sets of bounds (the types being intersected) are equal
   * </ul>
   */
  @Override
  public Boolean visitIntersection_Intersection(
      final AnnotatedIntersectionType type1, final AnnotatedIntersectionType type2, final Void p) {
    if (!arePrimeAnnosEqual(type1, type2)) {
      return false;
    }

    boolean result = areAllEqual(type1.getBounds(), type2.getBounds());
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  /**
   * Two primitive types are equal if:
   *
   * <ul>
   *   <li>Their sets of primary annotations are equal
   * </ul>
   */
  @Override
  public Boolean visitPrimitive_Primitive(
      final AnnotatedPrimitiveType type1, final AnnotatedPrimitiveType type2, final Void p) {
    return arePrimeAnnosEqual(type1, type2);
  }

  /**
   * Two type variables are equal if:
   *
   * <ul>
   *   <li>Their bounds are equal
   * </ul>
   *
   * Note: Primary annotations will be taken into account when the bounds are retrieved
   */
  @Override
  public Boolean visitTypevar_Typevar(
      final AnnotatedTypeVariable type1, final AnnotatedTypeVariable type2, final Void p) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    Boolean result =
        areEqual(type1.getUpperBound(), type2.getUpperBound())
            && areEqual(type1.getLowerBound(), type2.getLowerBound());
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  /**
   * Two wildcards are equal if:
   *
   * <ul>
   *   <li>Their bounds are equal
   * </ul>
   *
   * Note: Primary annotations will be taken into account when the bounds are retrieved
   */
  @Override
  public Boolean visitWildcard_Wildcard(
      final AnnotatedWildcardType type1, final AnnotatedWildcardType type2, final Void p) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    if (type1.atypeFactory.ignoreUninferredTypeArguments
        && (type1.isUninferredTypeArgument() || type2.isUninferredTypeArgument())) {
      return true;
    }

    Boolean result =
        areEqual(type1.getExtendsBound(), type2.getExtendsBound())
            && areEqual(type1.getSuperBound(), type2.getSuperBound());
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  // since we don't do a boxing conversion between primitive and declared types in some cases
  // we must compare primitives with their boxed counterparts
  @Override
  public Boolean visitDeclared_Primitive(
      AnnotatedDeclaredType type1, AnnotatedPrimitiveType type2, Void p) {
    if (!TypesUtils.isBoxOf(type1.getUnderlyingType(), type2.getUnderlyingType())) {
      defaultErrorMessage(type1, type2, p);
    }

    return arePrimeAnnosEqual(type1, type2);
  }

  @Override
  public Boolean visitPrimitive_Declared(
      AnnotatedPrimitiveType type1, AnnotatedDeclaredType type2, Void p) {
    if (!TypesUtils.isBoxOf(type2.getUnderlyingType(), type1.getUnderlyingType())) {
      defaultErrorMessage(type1, type2, p);
    }

    return arePrimeAnnosEqual(type1, type2);
  }
}
