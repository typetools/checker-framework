package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.EqualsMethod;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AbstractAtmComboVisitor;
import org.checkerframework.framework.util.AtmCombo;
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
  public Boolean defaultAction(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {

    return super.defaultAction(type1, type2, p);
  }

  /**
   * Called for every combination that isn't specifically handled.
   *
   * @return error message explaining the two types' classes are not the same
   */
  @Override
  public String defaultErrorMessage(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void p) {
    return super.defaultErrorMessage(type1, type2, p)
        + System.lineSeparator()
        + "  visitHistory = "
        + visitHistory;
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
  private boolean areEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
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
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror top) {
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

  /**
   * Return true if type1 and type2 have the same set of annotations.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if type1 and type2 have the same set of annotations
   */
  protected boolean arePrimaryAnnosEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    if (currentTop != null) {
      AnnotationMirror anno1 = type1.getPrimaryAnnotationInHierarchy(currentTop);
      AnnotationMirror anno2 = type2.getPrimaryAnnotationInHierarchy(currentTop);
      TypeMirror typeMirror1 = type1.underlyingType;
      TypeMirror typeMirror2 = type2.underlyingType;
      QualifierHierarchy qh = type1.atypeFactory.getQualifierHierarchy();
      return qh.isSubtypeShallow(anno1, typeMirror1, anno2, typeMirror2)
          && qh.isSubtypeShallow(anno2, typeMirror2, anno1, typeMirror1);
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
      Collection<? extends AnnotatedTypeMirror> types1,
      Collection<? extends AnnotatedTypeMirror> types2) {
    if (types1.size() != types2.size()) {
      throw new BugInCF(
          "Mismatching collection sizes:%n    types 1: %s (%d)%n    types 2: %s (%d)",
          StringsPlume.join("; ", types1),
          types1.size(),
          StringsPlume.join("; ", types2),
          types2.size());
    }

    Iterator<? extends AnnotatedTypeMirror> types1Iter = types1.iterator();
    Iterator<? extends AnnotatedTypeMirror> types2Iter = types2.iterator();
    while (types1Iter.hasNext()) {
      AnnotatedTypeMirror type1 = types1Iter.next();
      AnnotatedTypeMirror type2 = types2Iter.next();
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
  protected boolean checkOrAreEqual(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    Boolean result = areEqual(type1, type2);
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
  public Boolean visitArray_Array(AnnotatedArrayType type1, AnnotatedArrayType type2, Void p) {
    if (!arePrimaryAnnosEqual(type1, type2)) {
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
      AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, Void p) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    // TODO: same class/interface is not enforced. Why?

    if (!arePrimaryAnnosEqual(type1, type2)) {
      return false;
    }
    // Prevent infinite recursion e.g. in Issue1587b
    visitHistory.put(type1, type2, currentTop, true);

    List<AnnotatedTypeMirror> type1Args = type1.getTypeArguments();
    List<AnnotatedTypeMirror> type2Args = type2.getTypeArguments();

    // Capture the types because the wildcards are only not equal if they are provably distinct.
    // Provably distinct is computed using the captured and erased upper bounds of wildcards.
    // See JLS 4.5.1. Type Arguments of Parameterized Types.
    AnnotatedTypeFactory atypeFactory = type1.atypeFactory;
    AnnotatedDeclaredType capturedType1 =
        (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(type1);
    AnnotatedDeclaredType capturedType2 =
        (AnnotatedDeclaredType) atypeFactory.applyCaptureConversion(type2);
    visitHistory.put(capturedType1, capturedType2, currentTop, true);

    List<AnnotatedTypeMirror> capturedType1Args = capturedType1.getTypeArguments();
    List<AnnotatedTypeMirror> capturedType2Args = capturedType2.getTypeArguments();
    boolean result = true;
    for (int i = 0; i < type1.getTypeArguments().size(); i++) {
      AnnotatedTypeMirror type1Arg = type1Args.get(i);
      AnnotatedTypeMirror type2Arg = type2Args.get(i);
      Boolean pastResultTA = visitHistory.get(type1Arg, type2Arg, currentTop);
      if (pastResultTA != null) {
        result = pastResultTA;
      } else {
        if (type1Arg.getKind() != TypeKind.WILDCARD || type2Arg.getKind() != TypeKind.WILDCARD) {
          result = areEqual(type1Arg, type2Arg);
        } else {
          AnnotatedWildcardType wildcardType1 = (AnnotatedWildcardType) type1Arg;
          AnnotatedWildcardType wildcardType2 = (AnnotatedWildcardType) type2Arg;
          if (type1.atypeFactory.ignoreRawTypeArguments
              && (wildcardType1.isTypeArgOfRawType() || wildcardType2.isTypeArgOfRawType())) {
            result = true;
          } else {
            AnnotatedTypeMirror capturedType1Arg = capturedType1Args.get(i);
            AnnotatedTypeMirror capturedType2Arg = capturedType2Args.get(i);
            result = areEqual(capturedType1Arg.getErased(), capturedType2Arg.getErased());
          }
        }
      }
      if (!result) {
        break;
      }
    }

    visitHistory.put(capturedType1, capturedType2, currentTop, result);
    visitHistory.put(type1, type2, currentTop, result);
    return result;
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
      AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, Void p) {
    if (!arePrimaryAnnosEqual(type1, type2)) {
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
      AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, Void p) {
    return arePrimaryAnnosEqual(type1, type2);
  }

  @Override
  public Boolean visitNull_Null(AnnotatedNullType type1, AnnotatedNullType type2, Void unused) {
    return arePrimaryAnnosEqual(type1, type2);
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
      AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, Void p) {
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
      AnnotatedWildcardType type1, AnnotatedWildcardType type2, Void p) {
    Boolean pastResult = visitHistory.get(type1, type2, currentTop);
    if (pastResult != null) {
      return pastResult;
    }

    if (type1.atypeFactory.ignoreRawTypeArguments
        && (type1.isTypeArgOfRawType() || type2.isTypeArgOfRawType())) {
      return true;
    }

    Boolean result =
        areEqual(type1.getExtendsBound(), type2.getExtendsBound())
            && areEqual(type1.getSuperBound(), type2.getSuperBound());
    visitHistory.put(type1, type2, currentTop, result);
    return result;
  }

  // Since we don't do a boxing conversion between primitive and declared types, in some cases
  // we must compare primitives with their boxed counterparts.
  @Override
  public Boolean visitDeclared_Primitive(
      AnnotatedDeclaredType type1, AnnotatedPrimitiveType type2, Void p) {
    if (!TypesUtils.isBoxOf(type1.getUnderlyingType(), type2.getUnderlyingType())) {
      throw new BugInCF(defaultErrorMessage(type1, type2, p));
    }

    return arePrimaryAnnosEqual(type1, type2);
  }

  @Override
  public Boolean visitPrimitive_Declared(
      AnnotatedPrimitiveType type1, AnnotatedDeclaredType type2, Void p) {
    if (!TypesUtils.isBoxOf(type2.getUnderlyingType(), type1.getUnderlyingType())) {
      throw new BugInCF(defaultErrorMessage(type1, type2, p));
    }

    return arePrimaryAnnosEqual(type1, type2);
  }

  @Override
  public Boolean visitTypevar_Declared(
      AnnotatedTypeVariable type1, AnnotatedDeclaredType type2, Void unused) {
    // This case should not happen, but sometimes type argument inference incorrectly infers a
    // captured type, when javac does not. See Issue6755.java.
    if (TypesUtils.isCapturedTypeVariable(type1.underlyingType)) {
      if (type1.getLowerBound().getKind() != TypeKind.NULL) {
        return visit(type1.getLowerBound(), type2, unused);
      }
      return visit(type1.getUpperBound(), type2, unused);
    }
    return super.visitTypevar_Declared(type1, type2, unused);
  }

  @Override
  public Boolean visitDeclared_Typevar(
      AnnotatedDeclaredType type1, AnnotatedTypeVariable type2, Void unused) {
    // This case should not happen, but sometimes type argument inference incorrectly infers a
    // captured type, when javac does not. See Issue6755.java.
    if (TypesUtils.isCapturedTypeVariable(type2.underlyingType)) {
      if (type2.getLowerBound().getKind() != TypeKind.NULL) {
        return visit(type1, type2.getLowerBound(), unused);
      }
      return visit(type1, type2.getUpperBound(), unused);
    }
    return super.visitDeclared_Typevar(type1, type2, unused);
  }
}
