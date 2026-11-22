package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext.ReplacedTypes;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.BugInCF;

/**
 * Utility methods for operating on types while ignoring some of their annotations.
 *
 * <p>In a constraint where one type is a use of an inference variable with an explicit primary
 * annotation, the primary annotations in that constraint should not influence the primary
 * annotations of the variable's instantiation. To ensure this, some types are marked as ignoring
 * annotations (see {@link AbstractType#ignoreAnnotations}).
 *
 * <p>A <em>position</em> in a type is a location in its tree of nested types: the type itself or,
 * at any depth, a type argument, an enclosing type, an array component type, a wildcard bound, or
 * an intersection bound. The bounds of a type variable are not positions in it. Code identifies a
 * position by the {@link AnnotatedTypeMirror} object at it, so a set of positions is an identity
 * set.
 *
 * <p>Two kinds of annotations are ignored:
 *
 * <ul>
 *   <li>the primary annotations of a type whose {@link AbstractType#ignoreAnnotations} is true,
 *       including those of its bounds if it is a type variable or a wildcard, and
 *   <li>the annotations at an <em>ignored position</em> of a {@link ProperType}. {@link
 *       AbstractType#applyInstantiations} makes a proper type by replacing each use of an inference
 *       variable with the variable's instantiation. A position in the result is ignored if either:
 *       <ul>
 *         <li>a use was replaced there, and the instantiation ignores annotations, or
 *         <li>it is within a replaced use, at an ignored position of the instantiation.
 *       </ul>
 *       The type's {@link ProperType#getOrigin() origin} is the type before instantiations were
 *       applied. The origin has a use of an inference variable at or enclosing each ignored
 *       position.
 *       <p>The annotations at an ignored position are ignored only in some qualifier hierarchies:
 *       those in which the use does not have a non-polymorphic primary annotation. In a hierarchy
 *       in which it does, that annotation replaces the instantiation's (see {@link
 *       UseOfVariable#applyInstantiations}), so it is not ignored. A position is an ignored
 *       position only if its annotations are ignored in at least one hierarchy.
 * </ul>
 *
 * <p>The <em>current instantiation</em> of an inference variable is its instantiation at this
 * moment (see {@link Variable#getInstantiation}). It can change during inference: for example, an
 * instantiation that ignores annotations can be replaced by one that does not. A type that was made
 * by applying instantiations is then out of date (see {@link #upToDate}).
 *
 * <p>Before two types are compared for subtyping or equality, or combined by a least upper bound or
 * greatest lower bound, the methods of this class replace their ignored annotations with
 * annotations that cannot affect the result: for example, bottom for a subtype and top for a
 * supertype, or the annotations at the corresponding position of the other type. (An exception: in
 * a hierarchy in which both types ignore the annotations at a position, that position is not
 * replaced before a least upper bound or greatest lower bound, so that the result there combines
 * the annotations of both types.) The rest of each type is then compared or combined as usual.
 */
final class IgnoredAnnotations {

  /** Do not instantiate. */
  private IgnoredAnnotations() {
    throw new AssertionError("Class IgnoredAnnotations cannot be instantiated.");
  }

  /**
   * Returns the annotated types of {@code sub} and {@code sup} with their ignored annotations
   * replaced.
   *
   * <ul>
   *   <li>If the primary annotations of {@code sub} are ignored, then they are replaced with bottom
   *       annotations.
   *   <li>If the primary annotations of {@code sup} are ignored, then they are replaced with top
   *       annotations.
   *   <li>Ignored annotations elsewhere in either type are replaced with the annotations at the
   *       corresponding position in the other type.
   * </ul>
   *
   * <p>If neither type has ignored annotations, the result holds the types' own annotated types;
   * otherwise it holds copies.
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
      replaceIgnoredWithCorresponding(
          subATM,
          supATM,
          ignoredPositions(subAsSuper, subATM, qh),
          ignoredPositions(upToDateSup, supATM, qh),
          false,
          qh,
          types);
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
   * Returns the annotated types of {@code type1} and {@code type2} with their ignored annotations
   * replaced.
   *
   * <ul>
   *   <li>Each ignored annotation of {@code type1} is replaced by the annotation at the
   *       corresponding position of {@code type2}, even if that annotation is ignored too.
   *   <li>Each other ignored annotation of {@code type2} is replaced by the annotation at the
   *       corresponding position of {@code type1}.
   * </ul>
   *
   * <p>Either way, the corresponding annotations are then the same, so ignored annotations cannot
   * make the types unequal.
   *
   * <p>If neither type has ignored annotations, the result holds the types' own annotated types;
   * otherwise it holds modified copies.
   *
   * @param type1 a type
   * @param type2 a type with the same Java type as {@code type1}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return a pair of annotated types: {@code type1} and {@code type2}, with ignored annotations
   *     replaced from the other type
   */
  static ReplacedTypes replaceIgnoredForEquality(
      AbstractType type1, AbstractType type2, QualifierHierarchy qh, Types types) {
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
      replaceIgnoredWithCorresponding(
          atm1,
          atm2,
          ignoredPositions(upToDate1, atm1, qh),
          ignoredPositions(upToDate2, atm2, qh),
          false,
          qh,
          types);
    }
    if (upToDate1.ignoreAnnotations) {
      copyPrimaryAnnotations(atm2, atm1);
    } else if (upToDate2.ignoreAnnotations) {
      copyPrimaryAnnotations(atm1, atm2);
    }
    return new ReplacedTypes(atm1, atm2);
  }

  /**
   * Returns the annotated types of {@code type1} and {@code type2}, with the annotations at their
   * ignored positions replaced. The steps are:
   *
   * <ol>
   *   <li>If neither type has an origin (see {@link ProperType#getOrigin}), then neither has
   *       ignored positions, so return the types' own annotated types.
   *   <li>Find the supertype whose class is that of {@code resultType} (see {@link #asSuper}) of
   *       each type, or of its upper bound if it is a type variable. If the two supertypes are of
   *       different classes, then their positions do not correspond, so return the types' own
   *       annotated types.
   *   <li>Otherwise, return copies of the two supertypes in which each ignored position of one gets
   *       the annotations of the corresponding position of the other, in each hierarchy in which
   *       the annotations of only one of the two positions are ignored (see {@link
   *       #replaceIgnoredWithCorresponding}), except that a type variable is returned itself, with
   *       no annotations replaced. In a hierarchy in which the annotations of both positions are
   *       ignored, both are kept, so that the least upper bound or greatest lower bound combines
   *       them and does not depend on the order of {@code type1} and {@code type2}.
   * </ol>
   *
   * <p>This method does not replace primary annotations that are ignored because of {@link
   * AbstractType#ignoreAnnotations}; the caller handles them.
   *
   * @param type1 a type
   * @param type2 a type
   * @param resultType the Java type of the least upper bound or greatest lower bound of {@code
   *     type1} and {@code type2}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return a pair of annotated types: {@code type1} and {@code type2}, or their supertypes whose
   *     class is that of {@code resultType}, with ignored positions replaced; a type variable is
   *     returned itself
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
    // The least upper bound or greatest lower bound of a type variable is computed from its upper
    // bound, so the positions of a type variable are those of its upper bound.
    AbstractType bound1 = upperBoundIfTypeVariable(upToDate1);
    AbstractType bound2 = upperBoundIfTypeVariable(upToDate2);
    AbstractType asSuper1 = asSuper(bound1, resultType, types);
    AbstractType asSuper2 = asSuper(bound2, resultType, types);
    if (!types.isSameType(
        types.erasure(asSuper1.getJavaType()), types.erasure(asSuper2.getJavaType()))) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = asSuper1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = asSuper2.getAnnotatedType().deepCopy();
    replaceIgnoredWithCorresponding(
        atm1,
        atm2,
        ignoredPositions(asSuper1, atm1, qh),
        ignoredPositions(asSuper2, atm2, qh),
        true,
        qh,
        types);
    // A type variable is returned itself, because the least upper bound or greatest lower bound
    // needs it rather than its upper bound.  TODO: Replace the ignored positions within the upper
    // bound of a type variable too.
    return new ReplacedTypes(
        isTypeVariable(upToDate1) ? upToDate1.getAnnotatedType() : atm1,
        isTypeVariable(upToDate2) ? upToDate2.getAnnotatedType() : atm2);
  }

  /**
   * Returns true if no ignored position of {@code type} corresponds to a position of {@code other}
   * at which the annotations of some hierarchy are not ignored, but are ignored at the position of
   * {@code type}. Positions correspond as in {@link #replaceIgnoredForLubOrGlb}, which gives each
   * such ignored position of {@code type} the annotations of that position of {@code other} in
   * those hierarchies, except that if {@code other} is a type variable, then the positions of its
   * upper bound are used.
   *
   * <p>This method is not commutative: it tests only the ignored positions of {@code type}, and
   * only {@code other} is replaced by its upper bound. For example, if {@code type} has an ignored
   * position and {@code other} has no origin, then {@code ignoredPositionsStayIgnored(type, other)}
   * may be false, but {@code ignoredPositionsStayIgnored(other, type)} is true.
   *
   * @param type a type
   * @param other a type
   * @param resultType the Java type of the least upper bound or greatest lower bound of {@code
   *     type} and {@code other}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return true if every ignored position of {@code type} that corresponds to a position of {@code
   *     other} corresponds to an ignored position of {@code other} whose annotations are ignored in
   *     at least the same hierarchies
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
    Map<AnnotatedTypeMirror, AnnotationMirrorSet> ignored = ignoredPositions(typeAsSuper, atm, qh);
    Map<AnnotatedTypeMirror, AnnotationMirrorSet> otherIgnored =
        ignoredPositions(otherAsSuper, otherATM, qh);
    return allCorrespondingPositionsMatch(
        atm,
        otherATM,
        (position, otherPosition) ->
            ignoredHierarchies(otherIgnored, otherPosition)
                .containsAll(ignoredHierarchies(ignored, position)),
        types);
  }

  /**
   * Returns true if the annotations of {@code type1} and {@code type2} cannot make an annotation
   * subtype or equality check between them fail (see {@link AbstractType#checkAnnotationSubtype}
   * and {@link ProperType#checkAnnotationEquality}). That is the case when the only annotations of
   * either type that are compared are its primary annotations, and at least one of the types
   * ignores its primary annotations. A type has annotations that are compared other than its
   * primary annotations if it has an origin (see {@link ProperType#getOrigin}) or if {@link
   * #onlyPrimaryAnnotationsAreCompared} is false for it.
   *
   * @param type1 a type
   * @param type2 a type
   * @return true if the annotations of {@code type1} and {@code type2} cannot make an annotation
   *     subtype or equality check between them fail
   */
  static boolean noAnnotationsToCompare(AbstractType type1, AbstractType type2) {
    return (type1.ignoreAnnotations || type2.ignoreAnnotations)
        && type1.getOrigin() == null
        && type2.getOrigin() == null
        && onlyPrimaryAnnotationsAreCompared(type1.getAnnotatedType())
        && onlyPrimaryAnnotationsAreCompared(type2.getAnnotatedType());
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

  // //////////////////////////////////////////////////////////////////////////
  // Private methods appear below; package-protected methods appear above
  //

  /**
   * Returns the upper bound of {@code type} if it is a type variable, following the bounds of type
   * variables whose upper bound is a type variable; otherwise, returns {@code type} itself.
   *
   * @param type a type
   * @return the upper bound of {@code type} if it is a type variable, otherwise {@code type}
   */
  private static AbstractType upperBoundIfTypeVariable(AbstractType type) {
    AbstractType result = type;
    while (isTypeVariable(result)) {
      result = result.getTypeVarUpperBound();
    }
    return result;
  }

  /**
   * Returns true if {@code type} is a type variable whose annotated type is an {@link
   * AnnotatedTypeVariable}.
   *
   * @param type a type
   * @return true if {@code type} is a type variable whose annotated type is an {@link
   *     AnnotatedTypeVariable}
   */
  private static boolean isTypeVariable(AbstractType type) {
    return type.getTypeKind() == TypeKind.TYPEVAR
        && type.getAnnotatedType() instanceof AnnotatedTypeVariable;
  }

  /**
   * Returns {@code type} brought up to date with the current instantiations.
   *
   * <ul>
   *   <li>If {@code type} has no origin (see {@link ProperType#getOrigin}), returns {@code type}.
   *   <li>Otherwise, returns its origin with the current instantiations applied and with the
   *       primary annotations of {@code type}. That is {@code type} itself if no instantiation of a
   *       variable that the origin contains has changed since {@code type} was made, or if one of
   *       those variables no longer has an instantiation.
   * </ul>
   *
   * @param type a type
   * @return {@code type} brought up to date
   */
  private static AbstractType upToDate(AbstractType type) {
    // The ignored positions of a type are found from the current instantiations, so its annotated
    // type must be made from them too.
    return type.getOrigin() != null ? type.applyInstantiations() : type;
  }

  /**
   * Returns the supertype of {@code type} whose class is that of {@code superType} (see {@link
   * AbstractType#asSuper}); for example, the supertype of {@code ArrayList<String>} whose class is
   * {@code List} is {@code List<String>}. Returns {@code type} itself if either is not a declared
   * type, if both are of the same class, or if {@code type} has no such supertype that is a proper
   * type.
   *
   * @param type a type
   * @param superType a type; need not be a supertype of {@code type}
   * @param types the type utilities
   * @return the supertype of {@code type} whose class is that of {@code superType}, or {@code type}
   */
  private static AbstractType asSuper(AbstractType type, TypeMirror superType, Types types) {
    if (type.getTypeKind() != TypeKind.DECLARED
        || superType.getKind() != TypeKind.DECLARED
        || types.isSameType(types.erasure(type.getJavaType()), types.erasure(superType))) {
      return type;
    }
    AbstractType asSuper = type.asSuper(superType);
    return asSuper != null && asSuper.isProper() ? asSuper : type;
  }

  /**
   * Returns true if a subtype or equality check of {@code type} compares only its primary
   * annotations. That is the case if {@code type} is a primitive type, the null type, or a declared
   * type with no type arguments whose enclosing type, if any, also satisfies this condition.
   *
   * <p>Returns false for every other kind of type. A check of an array compares the annotations of
   * its component type, even if the component type satisfies this condition, and a check of a
   * wildcard or a type variable compares the annotations of its bounds.
   *
   * @param type an annotated type
   * @return true if a subtype or equality check of {@code type} compares only its primary
   *     annotations
   */
  private static boolean onlyPrimaryAnnotationsAreCompared(AnnotatedTypeMirror type) {
    switch (type.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType declared = (AnnotatedDeclaredType) type;
        AnnotatedDeclaredType enclosing = declared.getEnclosingType();
        return declared.getTypeArguments().isEmpty()
            && (enclosing == null || onlyPrimaryAnnotationsAreCompared(enclosing));
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
   * Returns the ignored positions of {@code atm}, each mapped to the top annotations of the
   * hierarchies in which its annotations are ignored. The map is an identity map, because distinct
   * positions can hold equal types. Each set in it is non-empty.
   *
   * @param type a type
   * @param atm an annotated type with the same structure as {@code type}'s annotated type, such as
   *     a deep copy of it; not modified
   * @param qh the qualifier hierarchy
   * @return the types within {@code atm} that are at its ignored positions, each mapped to the top
   *     annotations of the hierarchies in which its annotations are ignored
   */
  private static Map<AnnotatedTypeMirror, AnnotationMirrorSet> ignoredPositions(
      AbstractType type, AnnotatedTypeMirror atm, QualifierHierarchy qh) {
    InferenceType origin = type.getOrigin();
    if (origin == null) {
      return Collections.emptyMap();
    }
    IdentityHashMap<AnnotatedTypeMirror, AnnotationMirrorSet> result = new IdentityHashMap<>();
    collectIgnoredPositions(
        origin.getAnnotatedType(),
        atm,
        origin.getMap(),
        qh,
        new HashSet<>(),
        new IdentityHashMap<>(),
        result);
    return result;
  }

  /**
   * Returns the top annotations of the hierarchies in which the annotations at {@code position} are
   * ignored, according to {@code ignored}.
   *
   * @param ignored ignored positions, as returned by {@link #ignoredPositions}
   * @param position a position
   * @return the top annotations of the hierarchies in which the annotations at {@code position} are
   *     ignored; empty if {@code position} is not an ignored position
   */
  private static AnnotationMirrorSet ignoredHierarchies(
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> ignored, AnnotatedTypeMirror position) {
    AnnotationMirrorSet result = ignored.get(position);
    return result != null ? result : AnnotationMirrorSet.emptySet();
  }

  /**
   * Adds to {@code result} each ignored position (see {@link IgnoredAnnotations}) of {@code
   * applied}, mapped to the top annotations of the hierarchies in which its annotations are
   * ignored. Ignored positions are found where the corresponding position of {@code origin} is a
   * use of an inference variable.
   *
   * @param origin a type that may contain uses of the inference variables in {@code map}
   * @param applied {@code origin} with the current instantiations of those variables applied, apart
   *     from primary annotations; it contains no inference variables. If one of those variables no
   *     longer has an instantiation, then no ignored positions are found within its uses.
   * @param map the inference variables of {@code origin}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param visitedWildcards the pairs of wildcards of {@code origin} and {@code applied} that have
   *     been scanned (see {@link #addPair})
   * @param result the map to which to add ignored positions
   */
  private static void collectIgnoredPositions(
      AnnotatedTypeMirror origin,
      AnnotatedTypeMirror applied,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> result) {
    if (origin.getKind() == TypeKind.TYPEVAR) {
      Variable variable = map.get(origin.getUnderlyingType());
      if (variable != null) {
        ProperType instantiation = variable.getInstantiation();
        if (instantiation == null) {
          return;
        }
        // In a hierarchy in which the use has a non-polymorphic primary annotation, that annotation
        // replaces the instantiation's, so the annotation at `applied` is not ignored.
        AnnotationMirrorSet unannotated = hierarchiesWithoutNonPolymorphicAnnotation(origin, qh);
        if (instantiation.ignoreAnnotations) {
          addIgnored(result, applied, unannotated);
        }
        InferenceType instantiationOrigin = instantiation.getOrigin();
        // An F-bounded variable's instantiation can contain the variable itself.
        if (instantiationOrigin != null && visiting.add(variable)) {
          IdentityHashMap<AnnotatedTypeMirror, AnnotationMirrorSet> nested =
              new IdentityHashMap<>();
          collectIgnoredPositions(
              instantiationOrigin.getAnnotatedType(),
              applied,
              instantiationOrigin.getMap(),
              qh,
              visiting,
              visitedWildcards,
              nested);
          visiting.remove(variable);
          for (Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> entry : nested.entrySet()) {
            AnnotationMirrorSet hierarchies = entry.getValue();
            @SuppressWarnings("interning:not.interned") // Checking for exact object.
            boolean isApplied = entry.getKey() == applied;
            if (isApplied) {
              // The use's primary annotations apply to `applied` itself, not to nested positions.
              hierarchies = new AnnotationMirrorSet(hierarchies);
              hierarchies.retainAll(unannotated);
            }
            addIgnored(result, entry.getKey(), hierarchies);
          }
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
            originDT.getTypeArguments(),
            appliedDT.getTypeArguments(),
            map,
            qh,
            visiting,
            visitedWildcards,
            result);
        AnnotatedDeclaredType originEnclosing = originDT.getEnclosingType();
        AnnotatedDeclaredType appliedEnclosing = appliedDT.getEnclosingType();
        if (originEnclosing != null && appliedEnclosing != null) {
          collectIgnoredPositions(
              originEnclosing, appliedEnclosing, map, qh, visiting, visitedWildcards, result);
        }
      }
      case ARRAY ->
          collectIgnoredPositions(
              ((AnnotatedArrayType) origin).getComponentType(),
              ((AnnotatedArrayType) applied).getComponentType(),
              map,
              qh,
              visiting,
              visitedWildcards,
              result);
      case WILDCARD -> {
        AnnotatedWildcardType originWT = (AnnotatedWildcardType) origin;
        AnnotatedWildcardType appliedWT = (AnnotatedWildcardType) applied;
        // The type argument of a raw type can be a wildcard that is reachable from its own bound
        // (see BoundsInitializer).  Every cycle that this method traverses passes through a
        // wildcard, so stopping at a repeated pair of wildcards ensures termination.
        if (!addPair(visitedWildcards, originWT, appliedWT)) {
          return;
        }
        collectIgnoredPositions(
            originWT.getExtendsBound(),
            appliedWT.getExtendsBound(),
            map,
            qh,
            visiting,
            visitedWildcards,
            result);
        collectIgnoredPositions(
            originWT.getSuperBound(),
            appliedWT.getSuperBound(),
            map,
            qh,
            visiting,
            visitedWildcards,
            result);
      }
      case INTERSECTION ->
          collectPairwise(
              ((AnnotatedIntersectionType) origin).getBounds(),
              ((AnnotatedIntersectionType) applied).getBounds(),
              map,
              qh,
              visiting,
              visitedWildcards,
              result);
      default -> {}
    }
  }

  /**
   * Records in {@code result} that the annotations at {@code position} are ignored in the
   * hierarchies of {@code hierarchies}, in addition to any hierarchies already recorded for it.
   * Does nothing if {@code hierarchies} is empty.
   *
   * @param result ignored positions; side-effected by this method
   * @param position a position
   * @param hierarchies top annotations of the hierarchies in which the annotations at {@code
   *     position} are ignored
   */
  private static void addIgnored(
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> result,
      AnnotatedTypeMirror position,
      AnnotationMirrorSet hierarchies) {
    if (!hierarchies.isEmpty()) {
      result.computeIfAbsent(position, k -> new AnnotationMirrorSet()).addAll(hierarchies);
    }
  }

  /**
   * Calls {@link #collectIgnoredPositions} on corresponding elements of the two lists. Throws
   * {@link BugInCF} if the lists have different lengths.
   *
   * @param origins types that may contain uses of the inference variables in {@code map}
   * @param appliedTypes {@code origins} with the current instantiations of those variables applied
   * @param map the inference variables of {@code origins}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param visitedWildcards the pairs of wildcards that have been scanned (see {@link #addPair})
   * @param result the map to which to add ignored positions
   */
  private static void collectPairwise(
      List<? extends AnnotatedTypeMirror> origins,
      List<? extends AnnotatedTypeMirror> appliedTypes,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> result) {
    if (origins.size() != appliedTypes.size()) {
      throw new BugInCF(
          "origins %s and appliedTypes %s have different lengths", origins, appliedTypes);
    }
    Iterator<? extends AnnotatedTypeMirror> appliedIter = appliedTypes.iterator();
    for (AnnotatedTypeMirror origin : origins) {
      collectIgnoredPositions(
          origin, appliedIter.next(), map, qh, visiting, visitedWildcards, result);
    }
  }

  /**
   * Returns the top annotations of the hierarchies in which {@code type} has no primary annotation
   * that is not polymorphic.
   *
   * @param type a type
   * @param qh the qualifier hierarchy
   * @return the top annotations of the hierarchies in which {@code type} has no non-polymorphic
   *     primary annotation
   */
  private static AnnotationMirrorSet hierarchiesWithoutNonPolymorphicAnnotation(
      AnnotatedTypeMirror type, QualifierHierarchy qh) {
    AnnotationMirrorSet result = new AnnotationMirrorSet();
    for (AnnotationMirror top : qh.getTopAnnotations()) {
      AnnotationMirror anno = qh.findAnnotationInHierarchy(type.getPrimaryAnnotations(), top);
      if (anno == null || qh.isPolymorphicQualifier(anno)) {
        result.add(top);
      }
    }
    return result;
  }

  /**
   * Gives {@code target} the primary annotations of {@code source} in the hierarchies of {@code
   * hierarchies}; its annotations in other hierarchies are unchanged. If {@code hierarchies}
   * contains every hierarchy, this is the same as {@link
   * #copyPrimaryAnnotations(AnnotatedTypeMirror, AnnotatedTypeMirror)}.
   *
   * @param source a type whose primary annotations to copy
   * @param target the type whose primary annotations to replace; side-effected by this method
   * @param hierarchies the top annotations of the hierarchies in which to copy annotations
   * @param qh the qualifier hierarchy
   */
  private static void copyPrimaryAnnotations(
      AnnotatedTypeMirror source,
      AnnotatedTypeMirror target,
      AnnotationMirrorSet hierarchies,
      QualifierHierarchy qh) {
    if (hierarchies.size() == qh.getTopAnnotations().size()) {
      copyPrimaryAnnotations(source, target);
      return;
    }
    for (AnnotationMirror top : hierarchies) {
      AnnotationMirror anno = qh.findAnnotationInHierarchy(source.getPrimaryAnnotations(), top);
      if (anno == null
          && source instanceof AnnotatedTypeVariable sourceTV
          && target instanceof AnnotatedTypeVariable targetTV) {
        // As in copyPrimaryAnnotations(source, target), give the bounds the annotations of the
        // bounds of `source`.
        targetTV.removePrimaryAnnotationInHierarchy(top);
        copyAnnotationInHierarchy(sourceTV.getUpperBound(), targetTV.getUpperBound(), top, qh);
        copyAnnotationInHierarchy(sourceTV.getLowerBound(), targetTV.getLowerBound(), top, qh);
      } else {
        copyAnnotationInHierarchy(source, target, top, qh);
      }
    }
  }

  /**
   * Gives {@code target} the effective annotation of {@code source} in the hierarchy of {@code
   * top}, if {@code source} has one.
   *
   * @param source a type whose annotation to copy
   * @param target the type whose annotation to replace; side-effected by this method
   * @param top the top annotation of the hierarchy in which to copy the annotation
   * @param qh the qualifier hierarchy
   */
  private static void copyAnnotationInHierarchy(
      AnnotatedTypeMirror source,
      AnnotatedTypeMirror target,
      AnnotationMirror top,
      QualifierHierarchy qh) {
    AnnotationMirror anno = qh.findAnnotationInHierarchy(source.getAnnotations(), top);
    if (anno != null) {
      target.replaceAnnotation(anno);
    }
  }

  /**
   * Gives each ignored position of {@code atm1} or {@code atm2} the annotations of the
   * corresponding position of the other type, in each hierarchy in which its annotations are
   * ignored. In each hierarchy:
   *
   * <ul>
   *   <li>If the annotation at a position of {@code atm1} is ignored, it gets the annotation of the
   *       corresponding position of {@code atm2}, even if that is ignored too, unless {@code
   *       keepBothIgnored} is true.
   *   <li>Otherwise, if the annotation at the corresponding position of {@code atm2} is ignored, it
   *       gets the annotation of the position of {@code atm1}.
   * </ul>
   *
   * <p>If the position that annotations are copied from is a wildcard, the annotations of its bound
   * are copied (see {@link #boundOf}).
   *
   * <p>Positions correspond only where the two types have the same structure; elsewhere, nothing is
   * replaced. See {@link #allCorrespondingPositionsMatch}.
   *
   * <p>A position can correspond to several positions of the other type: for example, given {@code
   * interface Repeat<A> extends Pair<A, A>}, the type argument of {@code Repeat<T>} corresponds to
   * both type arguments of {@code Pair<? extends @NonNull String, ? extends @Nullable String>}. If
   * such a position gets annotations from several distinct positions, the result does not depend on
   * their order; see {@link #combineCopies}. All annotations are read from the types before any is
   * replaced.
   *
   * @param atm1 a type; side-effected by this method
   * @param atm2 a type; side-effected by this method
   * @param ignored1 the ignored positions of {@code atm1}
   * @param ignored2 the ignored positions of {@code atm2}
   * @param keepBothIgnored if true, then in a hierarchy in which the annotations at both of two
   *     corresponding positions are ignored, neither is replaced. This is appropriate for a least
   *     upper bound or greatest lower bound, which should combine both annotations. If false, both
   *     positions get the annotation of {@code atm2}, which is appropriate for an equality or
   *     subtype check.
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   */
  private static void replaceIgnoredWithCorresponding(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> ignored1,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> ignored2,
      boolean keepBothIgnored,
      QualifierHierarchy qh,
      Types types) {
    // The copies into each position, collected before any is made so that every annotation that is
    // copied is read from the unmodified types.
    IdentityHashMap<AnnotatedTypeMirror, List<Copy>> copies = new IdentityHashMap<>();
    allCorrespondingPositionsMatch(
        atm1,
        atm2,
        (position1, position2) -> {
          AnnotationMirrorSet hierarchies1 = ignoredHierarchies(ignored1, position1);
          AnnotationMirrorSet hierarchies2 = ignoredHierarchies(ignored2, position2);
          if (hierarchies1.isEmpty() && hierarchies2.isEmpty()) {
            return true;
          }
          AnnotationMirrorSet into1 = new AnnotationMirrorSet(hierarchies1);
          if (keepBothIgnored) {
            into1.removeAll(hierarchies2);
          }
          AnnotationMirrorSet into2 = new AnnotationMirrorSet(hierarchies2);
          into2.removeAll(hierarchies1);
          if (!into1.isEmpty()) {
            copies
                .computeIfAbsent(position1, k -> new ArrayList<>())
                .add(new Copy(position2, into1));
          }
          if (!into2.isEmpty()) {
            copies
                .computeIfAbsent(position2, k -> new ArrayList<>())
                .add(new Copy(position1, into2));
          }
          return true;
        },
        types);
    for (Map.Entry<AnnotatedTypeMirror, List<Copy>> entry : copies.entrySet()) {
      combineCopies(entry.getKey(), entry.getValue(), qh);
    }
  }

  /**
   * A copy of annotations into an ignored position, from a corresponding position of the other
   * type.
   *
   * @param from the position whose annotations are copied; if it is a wildcard, the annotations of
   *     its bound are copied (see {@link #boundOf})
   * @param hierarchies the top annotations of the hierarchies in which to copy annotations
   */
  private record Copy(AnnotatedTypeMirror from, AnnotationMirrorSet hierarchies) {}

  /**
   * Gives {@code target} the annotations of the positions of {@code copies}, in their hierarchies.
   * The result does not depend on the order of {@code copies}.
   *
   * <p>In a hierarchy in which every copy is from the same type, {@code target} gets its primary
   * annotation as by {@link #copyPrimaryAnnotations(AnnotatedTypeMirror, AnnotatedTypeMirror,
   * AnnotationMirrorSet, QualifierHierarchy)}. Otherwise, the annotations that are copied are
   * combined so that {@code target} satisfies every position it is copied from, if possible:
   *
   * <ul>
   *   <li>a wildcard with a lower bound requires the annotation to be above that of its bound;
   *   <li>any other wildcard requires the annotation to be below that of its bound;
   *   <li>any other type requires the annotation to be that of the type.
   * </ul>
   *
   * <p>{@code target} gets the least upper bound of the annotations that it must be above, if there
   * are any; otherwise, the greatest lower bound of the annotations that it must be below. For
   * example, if the copies are from {@code ? extends @NonNull String} and {@code ?
   * extends @Nullable String}, then {@code target} gets {@code @NonNull}, which satisfies both.
   *
   * @param target an ignored position; side-effected by this method
   * @param copies the copies into {@code target}
   * @param qh the qualifier hierarchy
   */
  private static void combineCopies(
      AnnotatedTypeMirror target, List<Copy> copies, QualifierHierarchy qh) {
    if (copies.size() == 1) {
      Copy copy = copies.get(0);
      copyPrimaryAnnotations(boundOf(copy.from()), target, copy.hierarchies(), qh);
      return;
    }
    for (AnnotationMirror top : qh.getTopAnnotations()) {
      Set<AnnotatedTypeMirror> sources = Collections.newSetFromMap(new IdentityHashMap<>());
      @Nullable AnnotationMirror lower = null;
      @Nullable AnnotationMirror upper = null;
      for (Copy copy : copies) {
        if (!copy.hierarchies().contains(top)) {
          continue;
        }
        AnnotatedTypeMirror source = boundOf(copy.from());
        sources.add(source);
        AnnotationMirror anno = source.getAnnotationInHierarchy(top);
        if (anno == null) {
          continue;
        }
        boolean isWildcard = copy.from().getKind() == TypeKind.WILDCARD;
        boolean hasLowerBound =
            isWildcard
                && ((AnnotatedWildcardType) copy.from()).getSuperBound().getKind() != TypeKind.NULL;
        if (!isWildcard || hasLowerBound) {
          lower = lower == null ? anno : qh.leastUpperBoundQualifiersOnly(lower, anno);
        }
        if (!isWildcard || !hasLowerBound) {
          upper = upper == null ? anno : qh.greatestLowerBoundQualifiersOnly(upper, anno);
        }
      }
      if (sources.size() == 1) {
        copyPrimaryAnnotations(
            sources.iterator().next(), target, AnnotationMirrorSet.singleton(top), qh);
      } else if (lower != null) {
        target.replaceAnnotation(lower);
      } else if (upper != null) {
        target.replaceAnnotation(upper);
      }
    }
  }

  /**
   * Returns true if {@code test} holds for each pair of corresponding positions of {@code atm1} and
   * {@code atm2}, starting with {@code atm1} and {@code atm2} themselves. A position is tested
   * before the positions nested in it, and no position is tested after {@code test} returns false.
   * Positions correspond only where the two types have the same structure, with two exceptions:
   *
   * <ul>
   *   <li>Where one type has a wildcard and the other does not, the wildcard and its bound (see
   *       {@link #boundOf}) both correspond to the other type, so the positions nested in the bound
   *       correspond to those nested in the other type. For example, in {@code List<? extends
   *       Box<@A String>>} and {@code List<Box<@B String>>}, both {@code ? extends Box<@A String>}
   *       and {@code Box<@A String>} correspond to {@code Box<@B String>}, and {@code @A String}
   *       corresponds to {@code @B String}.
   *   <li>Where the two types are declared types of different classes, one a subclass of the other,
   *       a type argument of the subclass corresponds to a type argument of the superclass if the
   *       superclass's type argument is that type parameter of the subclass (see {@link
   *       #typeArgumentCorrespondence}). For example, given {@code class Flip<A, B> implements
   *       Pair<B, A>}, in {@code Flip<@A String, @B String>} and {@code Pair<@C String, @D
   *       String>}, {@code @A String} corresponds to {@code @D String}.
   * </ul>
   *
   * @param atm1 a type
   * @param atm2 a type
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param types the type utilities
   * @return true if {@code test} holds for each pair of corresponding positions
   */
  private static boolean allCorrespondingPositionsMatch(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Types types) {
    return allCorrespondingPositionsMatch(atm1, atm2, test, new IdentityHashMap<>(), types);
  }

  /**
   * Returns true if {@code test} holds for each pair of corresponding positions of {@code atm1} and
   * {@code atm2}; see {@link #allCorrespondingPositionsMatch(AnnotatedTypeMirror,
   * AnnotatedTypeMirror, BiPredicate, Types)}. A pair of positions at least one of which is a
   * wildcard is tested at most once.
   *
   * @param atm1 a type
   * @param atm2 a type
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @param types the type utilities
   * @return true if {@code test} holds for each pair of corresponding positions
   */
  private static boolean allCorrespondingPositionsMatch(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Types types) {
    // The type argument of a raw type can be a wildcard that is reachable from its own bound (see
    // BoundsInitializer).  Every cycle that this method traverses passes through a wildcard, so
    // stopping at a repeated pair that includes a wildcard ensures termination.  The pair was
    // already tested, so it does not make the result false.
    if ((atm1.getKind() == TypeKind.WILDCARD || atm2.getKind() == TypeKind.WILDCARD)
        && !addPair(visitedWildcards, atm1, atm2)) {
      return true;
    }
    if (!test.test(atm1, atm2)) {
      return false;
    }
    if (atm1.getKind() != atm2.getKind()) {
      // A type argument is compared with a wildcard's bound; see #boundOf.
      if (atm1.getKind() == TypeKind.WILDCARD) {
        return allCorrespondingPositionsMatch(boundOf(atm1), atm2, test, visitedWildcards, types);
      } else if (atm2.getKind() == TypeKind.WILDCARD) {
        return allCorrespondingPositionsMatch(atm1, boundOf(atm2), test, visitedWildcards, types);
      }
      return true;
    }
    switch (atm1.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType dt1 = (AnnotatedDeclaredType) atm1;
        AnnotatedDeclaredType dt2 = (AnnotatedDeclaredType) atm2;
        if (!types.isSameType(
            types.erasure(dt1.getUnderlyingType()), types.erasure(dt2.getUnderlyingType()))) {
          return alignedTypeArgumentsMatch(dt1, dt2, test, visitedWildcards, types);
        }
        if (!allCorrespondingPositionsMatchPairwise(
            dt1.getTypeArguments(), dt2.getTypeArguments(), test, visitedWildcards, types)) {
          return false;
        }
        AnnotatedDeclaredType enclosing1 = dt1.getEnclosingType();
        AnnotatedDeclaredType enclosing2 = dt2.getEnclosingType();
        return enclosing1 == null
            || enclosing2 == null
            || allCorrespondingPositionsMatch(
                enclosing1, enclosing2, test, visitedWildcards, types);
      }
      case ARRAY -> {
        return allCorrespondingPositionsMatch(
            ((AnnotatedArrayType) atm1).getComponentType(),
            ((AnnotatedArrayType) atm2).getComponentType(),
            test,
            visitedWildcards,
            types);
      }
      case WILDCARD -> {
        AnnotatedWildcardType wt1 = (AnnotatedWildcardType) atm1;
        AnnotatedWildcardType wt2 = (AnnotatedWildcardType) atm2;
        return allCorrespondingPositionsMatch(
                wt1.getExtendsBound(), wt2.getExtendsBound(), test, visitedWildcards, types)
            && allCorrespondingPositionsMatch(
                wt1.getSuperBound(), wt2.getSuperBound(), test, visitedWildcards, types);
      }
      case INTERSECTION -> {
        return allCorrespondingPositionsMatchPairwise(
            ((AnnotatedIntersectionType) atm1).getBounds(),
            ((AnnotatedIntersectionType) atm2).getBounds(),
            test,
            visitedWildcards,
            types);
      }
      default -> {
        return true;
      }
    }
  }

  /**
   * Returns true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   * type arguments of {@code dt1} and {@code dt2}, which are declared types of different classes.
   * If one class is a subclass of the other, then a type argument of the subclass corresponds to a
   * type argument of the superclass if the superclass's type argument, in the subclass's
   * declaration, is that type parameter of the subclass; see {@link #typeArgumentCorrespondence}.
   * Otherwise, no type arguments correspond. Enclosing types never correspond.
   *
   * @param dt1 a declared type
   * @param dt2 a declared type of a different class than {@code dt1}
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @param types the type utilities
   * @return true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   *     type arguments
   */
  private static boolean alignedTypeArgumentsMatch(
      AnnotatedDeclaredType dt1,
      AnnotatedDeclaredType dt2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Types types) {
    TypeElement element1 = (TypeElement) dt1.getUnderlyingType().asElement();
    TypeElement element2 = (TypeElement) dt2.getUnderlyingType().asElement();
    List<? extends AnnotatedTypeMirror> args1 = dt1.getTypeArguments();
    List<? extends AnnotatedTypeMirror> args2 = dt2.getTypeArguments();
    if (args1.size() != element1.getTypeParameters().size()
        || args2.size() != element2.getTypeParameters().size()) {
      // A raw type.
      return true;
    }
    int[] subIndexes = typeArgumentCorrespondence(element1, element2, types);
    boolean firstIsSubclass = subIndexes != null;
    if (subIndexes == null) {
      subIndexes = typeArgumentCorrespondence(element2, element1, types);
      if (subIndexes == null) {
        return true;
      }
    }
    for (int superIndex = 0; superIndex < subIndexes.length; superIndex++) {
      int subIndex = subIndexes[superIndex];
      if (subIndex == -1) {
        continue;
      }
      boolean matches =
          firstIsSubclass
              ? allCorrespondingPositionsMatch(
                  args1.get(subIndex), args2.get(superIndex), test, visitedWildcards, types)
              : allCorrespondingPositionsMatch(
                  args1.get(superIndex), args2.get(subIndex), test, visitedWildcards, types);
      if (!matches) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns, for each type parameter of {@code superclass}, the index of the type parameter of
   * {@code subclass} that is its type argument in the supertype of {@code subclass} whose class is
   * {@code superclass}, or -1 if that type argument is not a type parameter of {@code subclass}.
   * For example, given {@code class Flip<A, B> implements Pair<B, A>}, the result for {@code Flip}
   * and {@code Pair} is {@code [1, 0]}. Returns null if {@code superclass} is not a superclass or
   * superinterface of {@code subclass}, or if that supertype is raw.
   *
   * @param subclass a class
   * @param superclass a class
   * @param types the type utilities
   * @return the index in the type parameters of {@code subclass} of each type argument of the
   *     supertype of {@code subclass} whose class is {@code superclass}, or null
   */
  private static int @Nullable [] typeArgumentCorrespondence(
      TypeElement subclass, TypeElement superclass, Types types) {
    DeclaredType supertype = supertypeOfClass(subclass, superclass, types);
    if (supertype == null) {
      return null;
    }
    List<? extends TypeMirror> superArgs = supertype.getTypeArguments();
    if (superArgs.size() != superclass.getTypeParameters().size()) {
      return null;
    }
    List<? extends TypeParameterElement> subParams = subclass.getTypeParameters();
    int[] result = new int[superArgs.size()];
    for (int superIndex = 0; superIndex < superArgs.size(); superIndex++) {
      result[superIndex] = -1;
      TypeMirror superArg = superArgs.get(superIndex);
      if (superArg.getKind() != TypeKind.TYPEVAR) {
        continue;
      }
      for (int subIndex = 0; subIndex < subParams.size(); subIndex++) {
        if (types.isSameType(superArg, subParams.get(subIndex).asType())) {
          result[superIndex] = subIndex;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Returns the supertype of the generic declaration of {@code subclass}, such as {@code Pair<B,
   * A>} for {@code class Flip<A, B> implements Pair<B, A>}, whose class is {@code superclass}, or
   * null if there is none.
   *
   * @param subclass a class
   * @param superclass a class
   * @param types the type utilities
   * @return the supertype of {@code subclass}'s declaration whose class is {@code superclass}, or
   *     null
   */
  private static @Nullable DeclaredType supertypeOfClass(
      TypeElement subclass, TypeElement superclass, Types types) {
    TypeMirror superErasure = types.erasure(superclass.asType());
    Queue<TypeMirror> worklist = new ArrayDeque<>();
    worklist.add(subclass.asType());
    Set<TypeElement> visited = new HashSet<>();
    while (!worklist.isEmpty()) {
      TypeMirror type = worklist.remove();
      if (type.getKind() != TypeKind.DECLARED
          || !visited.add((TypeElement) ((DeclaredType) type).asElement())) {
        continue;
      }
      if (types.isSameType(types.erasure(type), superErasure)) {
        return (DeclaredType) type;
      }
      worklist.addAll(types.directSupertypes(type));
    }
    return null;
  }

  /**
   * Returns true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   * elements of the two lists. If the lists have different lengths, no positions correspond, so
   * returns true without testing any. That is not an error: the lists can be the type arguments of
   * declared types of the same class, one of them raw, or the bounds of intersection types that
   * have different numbers of bounds.
   *
   * @param types1 a list of types
   * @param types2 a list of types
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @param types the type utilities
   * @return true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   *     elements
   */
  private static boolean allCorrespondingPositionsMatchPairwise(
      List<? extends AnnotatedTypeMirror> types1,
      List<? extends AnnotatedTypeMirror> types2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Types types) {
    if (types1.size() != types2.size()) {
      return true;
    }
    Iterator<? extends AnnotatedTypeMirror> iter2 = types2.iterator();
    for (AnnotatedTypeMirror type1 : types1) {
      if (!allCorrespondingPositionsMatch(type1, iter2.next(), test, visitedWildcards, types)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds the pair of {@code atm1} and {@code atm2} to {@code visited}. Types are compared by
   * identity, because distinct positions can hold equal types.
   *
   * @param visited the pairs visited so far, as a map from a type to the types with which it has
   *     been paired; side-effected by this method
   * @param atm1 the first type of the pair
   * @param atm2 the second type of the pair
   * @return true if the pair was not already in {@code visited}
   */
  private static boolean addPair(
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visited,
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2) {
    return visited
        .computeIfAbsent(atm1, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
        .add(atm2);
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
}
