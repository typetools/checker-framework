package org.checkerframework.framework.util.typeinference8.types;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
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
 * Utility methods for operating on types while ignoring some of their annotations.
 *
 * <p>In a constraint where one type is a use of an inference variable with an explicit primary
 * annotation, the primary annotations in that constraint should not influence the primary
 * annotations of the variable's instantiation. To ensure this, some types are marked as ignoring
 * annotations (see {@link AbstractType#ignoreAnnotations}).
 *
 * <p>A <em>position</em> in a type is the type itself or a type nested in it at any depth. Two
 * kinds of annotations are ignored:
 *
 * <ul>
 *   <li>the primary annotations of a type whose {@link AbstractType#ignoreAnnotations} is true,
 *       including those of its bounds if it is a type variable or a wildcard, and
 *   <li>the annotations at an <em>ignored position</em>: a position in a {@link ProperType} where
 *       {@link AbstractType#applyInstantiations} replaced a use of an inference variable by the
 *       variable's instantiation, and that instantiation ignores annotations. The type's {@link
 *       ProperType#getOrigin() origin} is the type before instantiations were applied; it has a use
 *       of an inference variable at each ignored position.
 * </ul>
 *
 * <p>Before two types are compared for subtyping or equality, or combined by a least upper bound or
 * greatest lower bound, the methods of this class replace their ignored annotations with
 * annotations that cannot affect the result: for example, bottom for a subtype and top for a
 * supertype, or the annotations at the corresponding position of the other type. The rest of each
 * type is then compared or combined as usual.
 */
final class IgnoredAnnotations {

  /** Do not instantiate. */
  private IgnoredAnnotations() {
    throw new AssertionError("Class IgnoredAnnotations cannot be instantiated.");
  }

  /**
   * Returns the annotated types of {@code sub} and {@code sup} with their ignored annotations
   * replaced. If the primary annotations of {@code sub} are ignored, then they are replaced with
   * bottom annotations. If the primary annotations of {@code sup} are ignored, then they are
   * replaced with top annotations. Ignored annotations elsewhere in either type are replaced with
   * the annotations at the corresponding position in the other type. If neither type has ignored
   * annotations, the result holds the types' own annotated types; otherwise it holds copies.
   *
   * @param sub a potential subtype
   * @param sup the potential supertype
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return a pair of annotated types: {@code sub} and {@code sup}, with ignored annotations
   *     replaced
   */
  static ReplacedTypes replaceIgnoredForSubtype(
      AbstractType sub, AbstractType sup, QualifierHierarchy qh, Types types) {
    AbstractType upToDateSub = upToDate(sub);
    AbstractType upToDateSup = upToDate(sup);
    boolean mayHaveIgnoredPositions =
        upToDateSub.getOrigin() != null || upToDateSup.getOrigin() != null;
    if (!upToDateSub.ignoreAnnotations
        && !upToDateSup.ignoreAnnotations
        && !mayHaveIgnoredPositions) {
      return new ReplacedTypes(upToDateSub.getAnnotatedType(), upToDateSup.getAnnotatedType());
    }
    AbstractType subAsSuper = upToDateSub;
    if (mayHaveIgnoredPositions) {
      // Compare the type arguments of the same class, so that corresponding positions line up.
      subAsSuper = asSuper(upToDateSub, upToDateSup.getJavaType(), types);
    }
    AnnotatedTypeMirror subATM = subAsSuper.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror supATM = upToDateSup.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredPositions) {
      replaceIgnoredPositions(
          subATM,
          supATM,
          ignoredPositions(subAsSuper, subATM, qh),
          ignoredPositions(upToDateSup, supATM, qh));
    }
    if (upToDateSub.ignoreAnnotations) {
      subATM.replaceAnnotations(qh.getBottomAnnotations());
    }
    if (upToDateSup.ignoreAnnotations) {
      supATM.replaceAnnotations(qh.getTopAnnotations());
    }
    return new ReplacedTypes(subATM, supATM);
  }

  /**
   * Returns the annotated types of {@code type1} and {@code type2}, which have the same Java type,
   * with their ignored annotations replaced. Each ignored annotation of one type is replaced by the
   * annotation at the corresponding position of the other type. If neither type has ignored
   * annotations, the result holds the types' own annotated types; otherwise it holds modified
   * copies.
   *
   * @param type1 a type
   * @param type2 a type with the same Java type as {@code type1}
   * @param qh the qualifier hierarchy
   * @return a pair of annotated types: {@code type1} and {@code type2}, with ignored annotations
   *     replaced from the other type
   */
  static ReplacedTypes replaceIgnoredForEquality(
      AbstractType type1, AbstractType type2, QualifierHierarchy qh) {
    AbstractType upToDate1 = upToDate(type1);
    AbstractType upToDate2 = upToDate(type2);
    boolean mayHaveIgnoredPositions =
        upToDate1.getOrigin() != null || upToDate2.getOrigin() != null;
    if (!upToDate1.ignoreAnnotations && !upToDate2.ignoreAnnotations && !mayHaveIgnoredPositions) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = upToDate1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = upToDate2.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredPositions) {
      replaceIgnoredPositions(
          atm1, atm2, ignoredPositions(upToDate1, atm1, qh), ignoredPositions(upToDate2, atm2, qh));
    }
    if (upToDate1.ignoreAnnotations) {
      copyPrimaryAnnotations(atm2, atm1);
    } else if (upToDate2.ignoreAnnotations) {
      copyPrimaryAnnotations(atm1, atm2);
    }
    return new ReplacedTypes(atm1, atm2);
  }

  /**
   * Returns the annotated types of {@code type1} and {@code type2}, where the annotations at each
   * ignored position of one type are replaced by those at the corresponding position of the other.
   * If either type has ignored positions, then both are first replaced by their supertypes whose
   * class is that of {@code resultType} (see {@link #asSuper}). If they are still of different
   * classes, then nothing is replaced. Otherwise, positions correspond only where the two types
   * have the same structure (see {@link #replaceIgnoredPositions}). Primary annotations are not
   * replaced. If nothing is replaced, the result holds the types' own annotated types; otherwise it
   * holds copies.
   *
   * @param type1 a type
   * @param type2 a type
   * @param resultType the Java type of the least upper bound or greatest lower bound of {@code
   *     type1} and {@code type2}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return a pair of annotated types: {@code type1} and {@code type2}, with ignored positions
   *     replaced
   */
  static ReplacedTypes replaceIgnoredForLubOrGlb(
      AbstractType type1,
      AbstractType type2,
      TypeMirror resultType,
      QualifierHierarchy qh,
      Types types) {
    AbstractType upToDate1 = upToDate(type1);
    AbstractType upToDate2 = upToDate(type2);
    if (upToDate1.getOrigin() == null && upToDate2.getOrigin() == null) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
    }
    AbstractType asSuper1 = asSuper(upToDate1, resultType, types);
    AbstractType asSuper2 = asSuper(upToDate2, resultType, types);
    if (!types.isSameType(
        types.erasure(asSuper1.getJavaType()), types.erasure(asSuper2.getJavaType()))) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = asSuper1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = asSuper2.getAnnotatedType().deepCopy();
    replaceIgnoredPositions(
        atm1, atm2, ignoredPositions(asSuper1, atm1, qh), ignoredPositions(asSuper2, atm2, qh));
    return new ReplacedTypes(atm1, atm2);
  }

  /**
   * Returns true if no ignored position of {@code type} corresponds to a position of {@code other}
   * that is not an ignored position. Positions correspond as in {@link #replaceIgnoredForLubOrGlb},
   * which gives each such ignored position of {@code type} the annotations of that position of
   * {@code other}, except that if {@code other} is a type variable, then the positions of its upper
   * bound are used.
   *
   * @param type a type
   * @param other a type
   * @param resultType the Java type of the least upper bound or greatest lower bound of {@code
   *     type} and {@code other}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return true if every ignored position of {@code type} that corresponds to a position of {@code
   *     other} corresponds to an ignored position of {@code other}
   */
  static boolean ignoredPositionsStayIgnored(
      AbstractType type,
      AbstractType other,
      TypeMirror resultType,
      QualifierHierarchy qh,
      Types types) {
    AbstractType upToDate = upToDate(type);
    if (upToDate.getOrigin() == null) {
      return true;
    }
    AbstractType typeAsSuper = asSuper(upToDate, resultType, types);
    // The least upper bound or greatest lower bound of a type variable is computed from its upper
    // bound, so compare the positions of that bound.
    AbstractType otherAsSuper =
        asSuper(upperBoundIfTypeVariable(upToDate(other)), resultType, types);
    if (!types.isSameType(
        types.erasure(typeAsSuper.getJavaType()), types.erasure(otherAsSuper.getJavaType()))) {
      return true;
    }
    AnnotatedTypeMirror atm = typeAsSuper.getAnnotatedType();
    AnnotatedTypeMirror otherATM = otherAsSuper.getAnnotatedType();
    Set<AnnotatedTypeMirror> ignored = ignoredPositions(typeAsSuper, atm, qh);
    Set<AnnotatedTypeMirror> otherIgnored = ignoredPositions(otherAsSuper, otherATM, qh);
    return allCorrespondingPositions(
        atm,
        otherATM,
        (position, otherPosition) ->
            !ignored.contains(position) || otherIgnored.contains(otherPosition));
  }

  /**
   * Returns the upper bound of {@code type} if it is a type variable, following the bounds of type
   * variables whose upper bound is a type variable; otherwise, returns {@code type} itself.
   *
   * @param type a type
   * @return the upper bound of {@code type} if it is a type variable, otherwise {@code type}
   */
  private static AbstractType upperBoundIfTypeVariable(AbstractType type) {
    AbstractType result = type;
    while (result.getTypeKind() == TypeKind.TYPEVAR
        && result.getAnnotatedType() instanceof AnnotatedTypeVariable) {
      result = result.getTypeVarUpperBound();
    }
    return result;
  }

  /**
   * Returns {@code type} brought up to date: if it is a proper type with an origin (see {@link
   * ProperType#getOrigin}), the result of applying the current instantiations of the variables that
   * its origin mentions to its origin; otherwise, {@code type} itself. The ignored positions of a
   * type are found from those instantiations, so its annotated type must be made from them too.
   *
   * @param type a type
   * @return {@code type} brought up to date
   */
  private static AbstractType upToDate(AbstractType type) {
    return type.getOrigin() != null ? type.applyInstantiations() : type;
  }

  /**
   * Returns the supertype of {@code type} whose class is that of {@code classType} (see {@link
   * AbstractType#asSuper}); for example, the supertype of {@code ArrayList<String>} whose class is
   * {@code List} is {@code List<String>}. Returns {@code type} itself if either is not a declared
   * type, if both are of the same class, or if {@code type} has no such supertype that is a proper
   * type.
   *
   * @param type a type
   * @param classType a type
   * @param types the type utilities
   * @return the supertype of {@code type} whose class is that of {@code classType}, or {@code type}
   */
  private static AbstractType asSuper(AbstractType type, TypeMirror classType, Types types) {
    if (type.getTypeKind() != TypeKind.DECLARED
        || classType.getKind() != TypeKind.DECLARED
        || types.isSameType(types.erasure(type.getJavaType()), types.erasure(classType))) {
      return type;
    }
    AbstractType asSuper = type.asSuper(classType);
    return asSuper != null && asSuper.isProper() ? asSuper : type;
  }

  /**
   * Returns true if comparing the annotations of {@code type1} and {@code type2} would compare
   * nothing: neither has an origin (see {@link ProperType#getOrigin}), neither has type arguments
   * at any depth (see {@link #hasNoTypeArguments}), and at least one of the types ignores its
   * primary annotations.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if comparing the annotations of {@code type1} and {@code type2} would compare
   *     nothing
   */
  static boolean noAnnotationsToCompare(AbstractType type1, AbstractType type2) {
    return (type1.ignoreAnnotations || type2.ignoreAnnotations)
        && type1.getOrigin() == null
        && type2.getOrigin() == null
        && hasNoTypeArguments(type1.getAnnotatedType())
        && hasNoTypeArguments(type2.getAnnotatedType());
  }

  /**
   * Returns true if {@code type} is a primitive type, the null type, or a declared type with no
   * type arguments whose enclosing type, if any, also satisfies this condition. Returns false for
   * every other kind of type, such as an array, a wildcard, or a type variable, any of which can
   * contain a type that has type arguments.
   *
   * @param type an annotated type
   * @return true if {@code type} has no type arguments at any depth
   */
  private static boolean hasNoTypeArguments(AnnotatedTypeMirror type) {
    switch (type.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType declared = (AnnotatedDeclaredType) type;
        AnnotatedDeclaredType enclosing = declared.getEnclosingType();
        return declared.getTypeArguments().isEmpty()
            && (enclosing == null || hasNoTypeArguments(enclosing));
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
   * Returns the ignored positions of {@code atm}.
   *
   * @param type a type
   * @param atm an annotated type with the same structure as {@code type}'s annotated type, such as
   *     a deep copy of it; not modified
   * @param qh the qualifier hierarchy
   * @return the types within {@code atm} that are at its ignored positions, as an identity set,
   *     because distinct positions can hold equal types
   */
  private static Set<AnnotatedTypeMirror> ignoredPositions(
      AbstractType type, AnnotatedTypeMirror atm, QualifierHierarchy qh) {
    InferenceType origin = type.getOrigin();
    if (origin == null) {
      return Collections.emptySet();
    }
    Set<AnnotatedTypeMirror> result = Collections.newSetFromMap(new IdentityHashMap<>());
    collectIgnoredPositions(
        origin.getAnnotatedType(), atm, origin.getMap(), qh, new HashSet<>(), result);
    return result;
  }

  /**
   * Adds to {@code result} each ignored position of {@code applied}. A position of {@code applied}
   * is an ignored position if the corresponding position of {@code origin} is a use of a variable
   * such that either:
   *
   * <ul>
   *   <li>the variable's instantiation ignores annotations, and the use's non-polymorphic primary
   *       annotations do not cover every qualifier hierarchy (see {@link
   *       UseOfVariable#applyInstantiations}), or
   *   <li>the position is an ignored position of the variable's instantiation.
   * </ul>
   *
   * @param origin a type that mentions inference variables
   * @param applied {@code origin} with instantiations applied
   * @param map the inference variables of {@code origin}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param result the set to which to add ignored positions
   */
  private static void collectIgnoredPositions(
      AnnotatedTypeMirror origin,
      AnnotatedTypeMirror applied,
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
            && nonPolymorphicPrimaryAnnotationCount(origin, qh) < qh.getTopAnnotations().size()) {
          result.add(applied);
        }
        InferenceType instantiationOrigin = instantiation.getOrigin();
        // An F-bounded variable's instantiation can mention the variable itself.
        if (instantiationOrigin != null && visiting.add(variable)) {
          collectIgnoredPositions(
              instantiationOrigin.getAnnotatedType(),
              applied,
              instantiationOrigin.getMap(),
              qh,
              visiting,
              result);
          visiting.remove(variable);
        }
      }
      return;
    }
    if (origin.getKind() != applied.getKind()) {
      return;
    }
    switch (origin.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType originDT = (AnnotatedDeclaredType) origin;
        AnnotatedDeclaredType appliedDT = (AnnotatedDeclaredType) applied;
        collectPairwise(
            originDT.getTypeArguments(), appliedDT.getTypeArguments(), map, qh, visiting, result);
        AnnotatedDeclaredType originEnclosing = originDT.getEnclosingType();
        AnnotatedDeclaredType appliedEnclosing = appliedDT.getEnclosingType();
        if (originEnclosing != null && appliedEnclosing != null) {
          collectIgnoredPositions(originEnclosing, appliedEnclosing, map, qh, visiting, result);
        }
      }
      case ARRAY ->
          collectIgnoredPositions(
              ((AnnotatedArrayType) origin).getComponentType(),
              ((AnnotatedArrayType) applied).getComponentType(),
              map,
              qh,
              visiting,
              result);
      case WILDCARD -> {
        AnnotatedWildcardType originWT = (AnnotatedWildcardType) origin;
        AnnotatedWildcardType appliedWT = (AnnotatedWildcardType) applied;
        collectIgnoredPositions(
            originWT.getExtendsBound(), appliedWT.getExtendsBound(), map, qh, visiting, result);
        collectIgnoredPositions(
            originWT.getSuperBound(), appliedWT.getSuperBound(), map, qh, visiting, result);
      }
      case INTERSECTION ->
          collectPairwise(
              ((AnnotatedIntersectionType) origin).getBounds(),
              ((AnnotatedIntersectionType) applied).getBounds(),
              map,
              qh,
              visiting,
              result);
      default -> {}
    }
  }

  /**
   * Calls {@link #collectIgnoredPositions} on corresponding elements of the two lists, if they have
   * the same length. Otherwise, does nothing.
   *
   * @param origins types that mention inference variables
   * @param appliedTypes {@code origins} with instantiations applied
   * @param map the inference variables of {@code origins}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param result the set to which to add ignored positions
   */
  private static void collectPairwise(
      List<? extends AnnotatedTypeMirror> origins,
      List<? extends AnnotatedTypeMirror> appliedTypes,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Set<AnnotatedTypeMirror> result) {
    if (origins.size() != appliedTypes.size()) {
      return;
    }
    Iterator<? extends AnnotatedTypeMirror> appliedIter = appliedTypes.iterator();
    for (AnnotatedTypeMirror origin : origins) {
      collectIgnoredPositions(origin, appliedIter.next(), map, qh, visiting, result);
    }
  }

  /**
   * Returns the number of primary annotations of {@code type} that are not polymorphic.
   *
   * @param type a type
   * @param qh the qualifier hierarchy
   * @return the number of primary annotations of {@code type} that are not polymorphic
   */
  private static int nonPolymorphicPrimaryAnnotationCount(
      AnnotatedTypeMirror type, QualifierHierarchy qh) {
    int count = 0;
    for (AnnotationMirror anno : type.getPrimaryAnnotations()) {
      if (!qh.isPolymorphicQualifier(anno)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Gives each ignored position of {@code atm1} the annotations of the corresponding position of
   * {@code atm2}, and vice versa. If corresponding positions of both types are ignored positions,
   * then the one in {@code atm1} gets the annotations of the one in {@code atm2}, so the two have
   * the same annotations. Positions correspond only where the two types have the same structure;
   * elsewhere, nothing is replaced.
   *
   * @param atm1 a type; side-effected by this method
   * @param atm2 a type; side-effected by this method
   * @param ignored1 the ignored positions of {@code atm1}
   * @param ignored2 the ignored positions of {@code atm2}
   */
  private static void replaceIgnoredPositions(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      Set<AnnotatedTypeMirror> ignored1,
      Set<AnnotatedTypeMirror> ignored2) {
    allCorrespondingPositions(
        atm1,
        atm2,
        (position1, position2) -> {
          if (ignored1.contains(position1)) {
            copyPrimaryAnnotations(boundOf(position2), position1);
          } else if (ignored2.contains(position2)) {
            copyPrimaryAnnotations(boundOf(position1), position2);
          }
          return true;
        });
  }

  /**
   * Returns true if {@code test} holds for each pair of corresponding positions of {@code atm1} and
   * {@code atm2}, starting with {@code atm1} and {@code atm2} themselves. A position is tested
   * before the positions nested in it, and no position is tested after {@code test} returns false.
   * Positions correspond only where the two types have the same structure, except that a wildcard
   * corresponds both to the type at the other position and, through its bound (see {@link
   * #boundOf}), to the positions nested in that type.
   *
   * @param atm1 a type
   * @param atm2 a type
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @return true if {@code test} holds for each pair of corresponding positions
   */
  private static boolean allCorrespondingPositions(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test) {
    if (!test.test(atm1, atm2)) {
      return false;
    }
    if (atm1.getKind() != atm2.getKind()) {
      // A type argument is compared with a wildcard's bound; see #boundOf.
      if (atm1.getKind() == TypeKind.WILDCARD) {
        return allCorrespondingPositions(boundOf(atm1), atm2, test);
      } else if (atm2.getKind() == TypeKind.WILDCARD) {
        return allCorrespondingPositions(atm1, boundOf(atm2), test);
      }
      return true;
    }
    switch (atm1.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType dt1 = (AnnotatedDeclaredType) atm1;
        AnnotatedDeclaredType dt2 = (AnnotatedDeclaredType) atm2;
        if (!allCorrespondingPositionsPairwise(
            dt1.getTypeArguments(), dt2.getTypeArguments(), test)) {
          return false;
        }
        AnnotatedDeclaredType enclosing1 = dt1.getEnclosingType();
        AnnotatedDeclaredType enclosing2 = dt2.getEnclosingType();
        return enclosing1 == null
            || enclosing2 == null
            || allCorrespondingPositions(enclosing1, enclosing2, test);
      }
      case ARRAY -> {
        return allCorrespondingPositions(
            ((AnnotatedArrayType) atm1).getComponentType(),
            ((AnnotatedArrayType) atm2).getComponentType(),
            test);
      }
      case WILDCARD -> {
        AnnotatedWildcardType wt1 = (AnnotatedWildcardType) atm1;
        AnnotatedWildcardType wt2 = (AnnotatedWildcardType) atm2;
        return allCorrespondingPositions(wt1.getExtendsBound(), wt2.getExtendsBound(), test)
            && allCorrespondingPositions(wt1.getSuperBound(), wt2.getSuperBound(), test);
      }
      case INTERSECTION -> {
        return allCorrespondingPositionsPairwise(
            ((AnnotatedIntersectionType) atm1).getBounds(),
            ((AnnotatedIntersectionType) atm2).getBounds(),
            test);
      }
      default -> {
        return true;
      }
    }
  }

  /**
   * Returns true if {@link #allCorrespondingPositions} holds for each pair of corresponding
   * elements of the two lists. If the lists have different lengths, no positions correspond, so
   * returns true without testing any.
   *
   * @param types1 a list of types
   * @param types2 a list of types
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @return true if {@link #allCorrespondingPositions} holds for each pair of corresponding
   *     elements
   */
  private static boolean allCorrespondingPositionsPairwise(
      List<? extends AnnotatedTypeMirror> types1,
      List<? extends AnnotatedTypeMirror> types2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test) {
    if (types1.size() != types2.size()) {
      return true;
    }
    Iterator<? extends AnnotatedTypeMirror> iter2 = types2.iterator();
    for (AnnotatedTypeMirror type1 : types1) {
      if (!allCorrespondingPositions(type1, iter2.next(), test)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the lower bound of {@code type} if it is a wildcard that has a lower bound, its upper
   * bound if it is a wildcard that does not have a lower bound, and {@code type} itself otherwise.
   *
   * @param type a type
   * @return the lower or upper bound of {@code type} if it is a wildcard, otherwise {@code type}
   */
  private static AnnotatedTypeMirror boundOf(AnnotatedTypeMirror type) {
    if (type instanceof AnnotatedWildcardType wildcard) {
      AnnotatedTypeMirror superBound = wildcard.getSuperBound();
      return superBound.getKind() == TypeKind.NULL ? wildcard.getExtendsBound() : superBound;
    }
    return type;
  }

  /**
   * Gives {@code target} the primary annotations of {@code source}. If both are type variables and
   * {@code source} has no primary annotation, then {@code target}'s primary annotations are removed
   * and its bounds get the primary annotations of {@code source}'s bounds.
   *
   * @param source a type whose primary annotations to copy
   * @param target the type whose primary annotations to replace; side-effected by this method
   */
  static void copyPrimaryAnnotations(AnnotatedTypeMirror source, AnnotatedTypeMirror target) {
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
