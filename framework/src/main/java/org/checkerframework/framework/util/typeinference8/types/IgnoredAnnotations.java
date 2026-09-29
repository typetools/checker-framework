package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext.ReplacedTypes;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/**
 * Utility methods for comparing types some of whose annotations are ignored.
 *
 * <p>Two kinds of position in a type have annotations that are ignored. Both kinds of annotations
 * come from a formula about a use of an inference variable that has an explicit primary annotation,
 * such as {@code @Nullable T}. That annotation overrides the variable's, so they say nothing about
 * the variable's annotations. The two kinds are:
 *
 * <ul>
 *   <li>the root of a type whose {@link AbstractType#ignoreAnnotations} is true: its primary
 *       annotations, including those of its bounds if it is a type variable or a wildcard, but not
 *       annotations nested in it, such as those of its type arguments; and
 *   <li>an ignored substitution: a position in a {@link ProperType} where an instantiation that
 *       ignores annotations was substituted. The type's {@link ProperType#getOrigin() origin} has a
 *       use of the variable there.
 * </ul>
 *
 * The rest of each type is compared as usual.
 */
final class IgnoredAnnotations {

  /** Do not instantiate. */
  private IgnoredAnnotations() {
    throw new AssertionError("Class IgnoredAnnotations cannot be instantiated.");
  }

  /**
   * Returns the annotated types of {@code sub} and {@code sup}, in that order, for checking that
   * {@code sub} is a subtype of {@code sup}. The root of a type that ignores annotations has bottom
   * annotations if it is {@code sub}, and top annotations if it is {@code sup}, so that it
   * satisfies the check. An ignored substitution has the annotations of the corresponding position
   * of the other type. If neither type has an ignored position, the result holds the types' own
   * annotated types; otherwise it holds copies.
   *
   * @param sub the potential subtype
   * @param sup the potential supertype
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return the annotated types of {@code sub} and {@code sup}, with ignored positions replaced
   */
  static ReplacedTypes replaceIgnoredForSubtype(
      AbstractType sub, AbstractType sup, QualifierHierarchy qh, Types types) {
    boolean mayHaveIgnoredSubstitutions = hasOrigin(sub) || hasOrigin(sup);
    if (!sub.ignoreAnnotations && !sup.ignoreAnnotations && !mayHaveIgnoredSubstitutions) {
      return new ReplacedTypes(sub.getAnnotatedType(), sup.getAnnotatedType());
    }
    AbstractType alignedSub = sub;
    if (mayHaveIgnoredSubstitutions) {
      // Compare the type arguments of the same class, so that corresponding positions line up.
      alignedSub = alignedToClass(sub, sup.getJavaType(), types);
    }
    AnnotatedTypeMirror subATM = alignedSub.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror supATM = sup.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredSubstitutions) {
      replaceIgnoredSubstitutions(
          subATM,
          supATM,
          ignoredSubstitutions(alignedSub, subATM, qh),
          ignoredSubstitutions(sup, supATM, qh));
    }
    if (sub.ignoreAnnotations) {
      subATM.replaceAnnotations(qh.getBottomAnnotations());
    }
    if (sup.ignoreAnnotations) {
      supATM.replaceAnnotations(qh.getTopAnnotations());
    }
    return new ReplacedTypes(subATM, supATM);
  }

  /**
   * Returns the annotated types of {@code type1} and {@code type2}, in that order, for checking
   * that the two, which have the same Java type, are equal. An ignored position of one type has the
   * annotations of the corresponding position of the other type. If neither type has an ignored
   * position, the result holds the types' own annotated types; otherwise it holds copies.
   *
   * @param type1 a type
   * @param type2 a type with the same Java type as {@code type1}
   * @param qh the qualifier hierarchy
   * @return the annotated types of {@code type1} and {@code type2}, with ignored positions replaced
   */
  static ReplacedTypes replaceIgnoredForEquality(
      AbstractType type1, AbstractType type2, QualifierHierarchy qh) {
    boolean mayHaveIgnoredSubstitutions = hasOrigin(type1) || hasOrigin(type2);
    if (!type1.ignoreAnnotations && !type2.ignoreAnnotations && !mayHaveIgnoredSubstitutions) {
      return new ReplacedTypes(type1.getAnnotatedType(), type2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = type1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = type2.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredSubstitutions) {
      replaceIgnoredSubstitutions(
          atm1, atm2, ignoredSubstitutions(type1, atm1, qh), ignoredSubstitutions(type2, atm2, qh));
    }
    if (type1.ignoreAnnotations) {
      copyRootAnnotations(atm2, atm1);
    } else if (type2.ignoreAnnotations) {
      copyRootAnnotations(atm1, atm2);
    }
    return new ReplacedTypes(atm1, atm2);
  }

  /**
   * Returns the annotated types of {@code type1} and {@code type2}, in the order given, for
   * combining them into their least upper bound, whose Java type is {@code lubType}. If either type
   * has ignored substitutions, then both are viewed as the class of {@code lubType}, and an ignored
   * substitution of one has the annotations of the corresponding position of the other, so that it
   * contributes nothing to the result. The roots are not replaced. If nothing is replaced, the
   * result holds the types' own annotated types; otherwise it holds copies.
   *
   * @param type1 a type
   * @param type2 a type
   * @param lubType the Java type of the least upper bound of {@code type1} and {@code type2}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return the annotated types of {@code type1} and {@code type2}, with ignored substitutions
   *     replaced
   */
  static ReplacedTypes replaceIgnoredForCombining(
      AbstractType type1,
      AbstractType type2,
      TypeMirror lubType,
      QualifierHierarchy qh,
      Types types) {
    if (!hasOrigin(type1) && !hasOrigin(type2)) {
      return new ReplacedTypes(type1.getAnnotatedType(), type2.getAnnotatedType());
    }
    AbstractType aligned1 = alignedToClass(type1, lubType, types);
    AbstractType aligned2 = alignedToClass(type2, lubType, types);
    if (!types.isSameType(
        types.erasure(aligned1.getJavaType()), types.erasure(aligned2.getJavaType()))) {
      return new ReplacedTypes(type1.getAnnotatedType(), type2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = aligned1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = aligned2.getAnnotatedType().deepCopy();
    replaceIgnoredSubstitutions(
        atm1,
        atm2,
        ignoredSubstitutions(aligned1, atm1, qh),
        ignoredSubstitutions(aligned2, atm2, qh));
    return new ReplacedTypes(atm1, atm2);
  }

  /**
   * Returns {@code type} viewed as the class of {@code classType}, if both are declared types of
   * different classes and that view exists; otherwise, returns {@code type}.
   *
   * @param type a type
   * @param classType a supertype of {@code type}
   * @param types the type utilities
   * @return {@code type} viewed as the class of {@code classType}, or {@code type}
   */
  private static AbstractType alignedToClass(AbstractType type, TypeMirror classType, Types types) {
    if (type.getTypeKind() != TypeKind.DECLARED
        || classType.getKind() != TypeKind.DECLARED
        || types.isSameType(types.erasure(type.getJavaType()), types.erasure(classType))) {
      return type;
    }
    AbstractType asSuper = type.asSuper(classType);
    return asSuper != null && asSuper.isProper() ? asSuper : type;
  }

  /**
   * Returns true if comparing {@code type1} and {@code type2} is decided by their roots alone,
   * because at least one of them ignores its root annotations and neither has any other position
   * with annotations. Then the comparison succeeds without looking at the types; see {@link
   * #replaceIgnoredForSubtype} and {@link #replaceIgnoredForEquality}.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if the comparison of {@code type1} and {@code type2} is decided by an ignored root
   */
  static boolean decidedByIgnoredRoot(AbstractType type1, AbstractType type2) {
    return (type1.ignoreAnnotations || type2.ignoreAnnotations)
        && !hasOrigin(type1)
        && !hasOrigin(type2)
        && hasOnlyRoot(type1.getAnnotatedType())
        && hasOnlyRoot(type2.getAnnotatedType());
  }

  /**
   * Returns true if {@code type} has no annotated position other than its root. A type variable is
   * not considered, because the bounds of its declaration can have type arguments.
   *
   * @param type an annotated type
   * @return true if {@code type} has no annotated position other than its root
   */
  private static boolean hasOnlyRoot(AnnotatedTypeMirror type) {
    switch (type.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType declared = (AnnotatedDeclaredType) type;
        AnnotatedDeclaredType enclosing = declared.getEnclosingType();
        return declared.getTypeArguments().isEmpty()
            && (enclosing == null || hasOnlyRoot(enclosing));
      }
      case NULL -> {
        return true;
      }
      default -> {
        return type.getKind().isPrimitive();
      }
    }
  }

  /**
   * Returns true if {@code type} is a proper type with an origin, and so may have ignored
   * substitutions.
   *
   * @param type a type
   * @return true if {@code type} is a proper type with an origin
   */
  private static boolean hasOrigin(AbstractType type) {
    return type instanceof ProperType properType && properType.getOrigin() != null;
  }

  /**
   * Returns the ignored substitutions of {@code atm}, which is {@code type}'s annotated type or a
   * deep copy of it. The result is compared by identity.
   *
   * @param type a type
   * @param atm the annotated type of {@code type}, or a deep copy of it
   * @param qh the qualifier hierarchy
   * @return the ignored substitutions of {@code atm}
   */
  private static Set<AnnotatedTypeMirror> ignoredSubstitutions(
      AbstractType type, AnnotatedTypeMirror atm, QualifierHierarchy qh) {
    if (!(type instanceof ProperType properType)) {
      return Collections.emptySet();
    }
    InferenceType origin = properType.getOrigin();
    if (origin == null) {
      return Collections.emptySet();
    }
    Set<AnnotatedTypeMirror> result = Collections.newSetFromMap(new IdentityHashMap<>());
    collectIgnoredSubstitutions(
        origin.getAnnotatedType(), atm, origin.getMap(), qh, new HashSet<>(), result);
    return result;
  }

  /**
   * Adds to {@code result} each ignored substitution of {@code atm}, which is {@code origin} with
   * instantiations substituted. A position is an ignored substitution if it corresponds to a use,
   * in {@code origin}, of a variable whose instantiation ignores annotations, where the use's own
   * annotations do not cover every qualifier hierarchy (see {@link
   * UseOfVariable#applyInstantiations}), or if it is an ignored substitution of the instantiation
   * that was substituted there.
   *
   * @param origin a type that mentions inference variables
   * @param atm {@code origin} with instantiations substituted
   * @param map the inference variables of {@code origin}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned; an F-bounded variable's
   *     instantiation can mention the variable itself
   * @param result the set to which to add ignored substitutions
   */
  private static void collectIgnoredSubstitutions(
      AnnotatedTypeMirror origin,
      AnnotatedTypeMirror atm,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Set<AnnotatedTypeMirror> result) {
    if (origin.getKind() == TypeKind.TYPEVAR) {
      Variable variable = map.get(origin.getUnderlyingType());
      if (variable != null) {
        ProperType instantiation = variable.getInstantiation();
        if (instantiation == null) {
          return;
        }
        if (instantiation.ignoreAnnotations
            && nonPolymorphicCount(origin, qh) < qh.getTopAnnotations().size()) {
          result.add(atm);
        }
        InferenceType instantiationOrigin = instantiation.getOrigin();
        if (instantiationOrigin != null && visiting.add(variable)) {
          collectIgnoredSubstitutions(
              instantiationOrigin.getAnnotatedType(),
              atm,
              instantiationOrigin.getMap(),
              qh,
              visiting,
              result);
          visiting.remove(variable);
        }
      }
      return;
    }
    if (origin.getKind() != atm.getKind()) {
      return;
    }
    switch (origin.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType originDT = (AnnotatedDeclaredType) origin;
        AnnotatedDeclaredType atmDT = (AnnotatedDeclaredType) atm;
        collectPairwise(
            originDT.getTypeArguments(), atmDT.getTypeArguments(), map, qh, visiting, result);
        AnnotatedDeclaredType originEnclosing = originDT.getEnclosingType();
        AnnotatedDeclaredType atmEnclosing = atmDT.getEnclosingType();
        if (originEnclosing != null && atmEnclosing != null) {
          collectIgnoredSubstitutions(originEnclosing, atmEnclosing, map, qh, visiting, result);
        }
      }
      case ARRAY ->
          collectIgnoredSubstitutions(
              ((AnnotatedArrayType) origin).getComponentType(),
              ((AnnotatedArrayType) atm).getComponentType(),
              map,
              qh,
              visiting,
              result);
      case WILDCARD -> {
        AnnotatedWildcardType originWT = (AnnotatedWildcardType) origin;
        AnnotatedWildcardType atmWT = (AnnotatedWildcardType) atm;
        collectIgnoredSubstitutions(
            originWT.getExtendsBound(), atmWT.getExtendsBound(), map, qh, visiting, result);
        collectIgnoredSubstitutions(
            originWT.getSuperBound(), atmWT.getSuperBound(), map, qh, visiting, result);
      }
      case INTERSECTION ->
          collectPairwise(
              ((AnnotatedIntersectionType) origin).getBounds(),
              ((AnnotatedIntersectionType) atm).getBounds(),
              map,
              qh,
              visiting,
              result);
      default -> {}
    }
  }

  /**
   * Calls {@link #collectIgnoredSubstitutions} on corresponding elements of the two lists, if they
   * have the same length.
   *
   * @param origins types that mention inference variables
   * @param atms {@code origins} with instantiations substituted
   * @param map the inference variables of {@code origins}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param result the set to which to add ignored substitutions
   */
  private static void collectPairwise(
      List<? extends AnnotatedTypeMirror> origins,
      List<? extends AnnotatedTypeMirror> atms,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Set<AnnotatedTypeMirror> result) {
    if (origins.size() != atms.size()) {
      return;
    }
    Iterator<? extends AnnotatedTypeMirror> atmIter = atms.iterator();
    for (AnnotatedTypeMirror origin : origins) {
      collectIgnoredSubstitutions(origin, atmIter.next(), map, qh, visiting, result);
    }
  }

  /**
   * Returns the number of primary annotations of {@code type} that are not polymorphic.
   *
   * @param type a type
   * @param qh the qualifier hierarchy
   * @return the number of primary annotations of {@code type} that are not polymorphic
   */
  private static int nonPolymorphicCount(AnnotatedTypeMirror type, QualifierHierarchy qh) {
    int count = 0;
    for (AnnotationMirror anno : type.getPrimaryAnnotations()) {
      if (!qh.isPolymorphicQualifier(anno)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Gives each ignored substitution of {@code atm1} the annotations of the corresponding position
   * of {@code atm2}, and vice versa. Positions correspond only where the two types have the same
   * structure; elsewhere, nothing is replaced.
   *
   * @param atm1 a type; side-effected by this method
   * @param atm2 a type; side-effected by this method
   * @param ignored1 the ignored substitutions of {@code atm1}
   * @param ignored2 the ignored substitutions of {@code atm2}
   */
  private static void replaceIgnoredSubstitutions(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      Set<AnnotatedTypeMirror> ignored1,
      Set<AnnotatedTypeMirror> ignored2) {
    if (ignored1.contains(atm1)) {
      copyRootAnnotations(boundOf(atm2), atm1);
      return;
    }
    if (ignored2.contains(atm2)) {
      copyRootAnnotations(boundOf(atm1), atm2);
      return;
    }
    if (atm1.getKind() != atm2.getKind()) {
      // A type argument is compared with a wildcard's bound; see #boundOf.
      if (atm1.getKind() == TypeKind.WILDCARD) {
        replaceIgnoredSubstitutions(boundOf(atm1), atm2, ignored1, ignored2);
      } else if (atm2.getKind() == TypeKind.WILDCARD) {
        replaceIgnoredSubstitutions(atm1, boundOf(atm2), ignored1, ignored2);
      }
      return;
    }
    switch (atm1.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType dt1 = (AnnotatedDeclaredType) atm1;
        AnnotatedDeclaredType dt2 = (AnnotatedDeclaredType) atm2;
        List<AnnotatedTypeMirror> args1 = dt1.getTypeArguments();
        List<AnnotatedTypeMirror> args2 = dt2.getTypeArguments();
        if (args1.size() == args2.size()) {
          for (int i = 0; i < args1.size(); i++) {
            replaceIgnoredSubstitutions(args1.get(i), args2.get(i), ignored1, ignored2);
          }
        }
        AnnotatedDeclaredType enclosing1 = dt1.getEnclosingType();
        AnnotatedDeclaredType enclosing2 = dt2.getEnclosingType();
        if (enclosing1 != null && enclosing2 != null) {
          replaceIgnoredSubstitutions(enclosing1, enclosing2, ignored1, ignored2);
        }
      }
      case ARRAY ->
          replaceIgnoredSubstitutions(
              ((AnnotatedArrayType) atm1).getComponentType(),
              ((AnnotatedArrayType) atm2).getComponentType(),
              ignored1,
              ignored2);
      case WILDCARD -> {
        AnnotatedWildcardType wt1 = (AnnotatedWildcardType) atm1;
        AnnotatedWildcardType wt2 = (AnnotatedWildcardType) atm2;
        replaceIgnoredSubstitutions(
            wt1.getExtendsBound(), wt2.getExtendsBound(), ignored1, ignored2);
        replaceIgnoredSubstitutions(wt1.getSuperBound(), wt2.getSuperBound(), ignored1, ignored2);
      }
      default -> {}
    }
  }

  /**
   * Returns the bound of {@code type} that a type argument is compared with, if {@code type} is a
   * wildcard: its lower bound if it has one, otherwise its upper bound. Returns any other type
   * itself.
   *
   * @param type a type
   * @return the bound of {@code type} that a type argument is compared with, or {@code type}
   */
  private static AnnotatedTypeMirror boundOf(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedWildcardType wildcard) {
      return wildcard.getSuperBound().getKind() == TypeKind.NULL
          ? wildcard.getExtendsBound()
          : wildcard.getSuperBound();
    }
    return type;
  }

  /**
   * Gives {@code target} the root annotations of {@code source}. If {@code source} is a use of a
   * type variable without a primary annotation, then its annotations are those of its bounds, so
   * {@code target}, if it is also a type variable, gets those bounds' annotations and no primary
   * annotation.
   *
   * @param source the type whose root annotations to copy
   * @param target the type whose root annotations to replace; side-effected by this method
   */
  static void copyRootAnnotations(AnnotatedTypeMirror source, AnnotatedTypeMirror target) {
    if (source.getPrimaryAnnotations().isEmpty()
        && source instanceof AnnotatedTypeVariable sourceTV
        && target instanceof AnnotatedTypeVariable targetTV) {
      targetTV.clearPrimaryAnnotations();
      targetTV.getUpperBound().replaceAnnotations(sourceTV.getUpperBound().getPrimaryAnnotations());
      targetTV.getLowerBound().replaceAnnotations(sourceTV.getLowerBound().getPrimaryAnnotations());
    } else {
      target.replaceAnnotations(source.getAnnotations());
    }
  }
}
