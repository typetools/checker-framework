package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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
 *   <li>the annotations at an <em>ignored position</em>: a position in a {@link ProperType} that
 *       {@link AbstractType#applyInstantiations} created, at which it replaced a use of an
 *       inference variable by the variable's instantiation, and either:
 *       <ul>
 *         <li>that instantiation ignores annotations, and the use does not have a non-polymorphic
 *             primary annotation in every qualifier hierarchy (if it does, then those annotations
 *             replace the instantiation's; see {@link UseOfVariable#applyInstantiations}), or
 *         <li>the corresponding position of that instantiation is an ignored position of it.
 *       </ul>
 *       The type's {@link ProperType#getOrigin() origin} is the type before instantiations were
 *       applied; it has a use of an inference variable at each ignored position.
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
 * supertype, or the annotations at the corresponding position of the other type. (An exception: a
 * position that both types ignore is not replaced before a least upper bound or greatest lower
 * bound, so that the result there combines the annotations of both types.) The rest of each type is
 * then compared or combined as usual.
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
      replaceIgnoredPositions(
          subATM,
          supATM,
          ignoredPositions(subAsSuper, subATM, qh),
          ignoredPositions(upToDateSup, supATM, qh),
          true);
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
   * with their ignored annotations replaced.
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
          atm1,
          atm2,
          ignoredPositions(upToDate1, atm1, qh),
          ignoredPositions(upToDate2, atm2, qh),
          true);
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
   *       the annotations of the corresponding position of the other, unless that position is
   *       ignored too (see {@link #replaceIgnoredPositions}), except that a type variable is
   *       returned itself, with no annotations replaced. A position that both types ignore keeps
   *       its annotations in each type, so that the least upper bound or greatest lower bound
   *       combines them.
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
    replaceIgnoredPositions(
        atm1,
        atm2,
        ignoredPositions(asSuper1, atm1, qh),
        ignoredPositions(asSuper2, atm2, qh),
        false);
    // A type variable is returned itself, because the least upper bound or greatest lower bound
    // needs it rather than its upper bound.  TODO: Replace the ignored positions within the upper
    // bound of a type variable too.
    return new ReplacedTypes(
        bound1 == upToDate1 ? atm1 : upToDate1.getAnnotatedType(),
        bound2 == upToDate2 ? atm2 : upToDate2.getAnnotatedType());
  }

  /**
   * Returns true if no ignored position of {@code type} corresponds to a position of {@code other}
   * that is not an ignored position. Positions correspond as in {@link #replaceIgnoredForLubOrGlb},
   * which gives each such ignored position of {@code type} the annotations of that position of
   * {@code other}, except that if {@code other} is a type variable, then the positions of its upper
   * bound are used.
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
    return allCorrespondingPositionsMatch(
        atm,
        otherATM,
        (position, otherPosition) ->
            !ignored.contains(position) || otherIgnored.contains(otherPosition));
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
    while (result.getTypeKind() == TypeKind.TYPEVAR
        && result.getAnnotatedType() instanceof AnnotatedTypeVariable) {
      result = result.getTypeVarUpperBound();
    }
    return result;
  }

  /**
   * Returns {@code type} brought up to date. If {@code type} is a proper type with an origin (see
   * {@link ProperType#getOrigin}), the result is its origin with the current instantiations applied
   * and with the primary annotations of {@code type}; otherwise, it is {@code type} itself. The
   * result is {@code type} itself if none of those instantiations has changed since {@code type}
   * was made, or if a variable that its origin contains no longer has an instantiation. Otherwise,
   * the result can differ from {@code type} at positions whose instantiation changed. The ignored
   * positions of a type are found from the current instantiations, so its annotated type must be
   * made from them too.
   *
   * @param type a type
   * @return {@code type} brought up to date
   */
  private static AbstractType upToDate(AbstractType type) {
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
   * Returns the ignored positions of {@code atm}, as an identity set, because distinct positions
   * can hold equal types.
   *
   * @param type a type
   * @param atm an annotated type with the same structure as {@code type}'s annotated type, such as
   *     a deep copy of it; not modified
   * @param qh the qualifier hierarchy
   * @return the types within {@code atm} that are at its ignored positions
   */
  private static Set<AnnotatedTypeMirror> ignoredPositions(
      AbstractType type, AnnotatedTypeMirror atm, QualifierHierarchy qh) {
    InferenceType origin = type.getOrigin();
    if (origin == null) {
      return Collections.emptySet();
    }
    Set<AnnotatedTypeMirror> result = Collections.newSetFromMap(new IdentityHashMap<>());
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
   * Adds to {@code result} each ignored position (see {@link IgnoredAnnotations}) of {@code
   * applied}. Ignored positions are found where the corresponding position of {@code origin} is a
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
   * @param result the set to which to add ignored positions
   */
  private static void collectIgnoredPositions(
      AnnotatedTypeMirror origin,
      AnnotatedTypeMirror applied,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Set<AnnotatedTypeMirror> result) {
    if (origin.getKind() == TypeKind.TYPEVAR) {
      Variable variable = map.get(origin.getUnderlyingType());
      if (variable != null) {
        ProperType instantiation = variable.getInstantiation();
        if (instantiation == null) {
          return;
        }
        // TODO: If the use has a non-polymorphic primary annotation in some qualifier hierarchies
        // but not all, then the whole position is ignored, including the annotations that the use
        // wrote explicitly.  Recording the ignored hierarchies at each position would let those
        // annotations be compared.  AbstractType#ignoreAnnotations is likewise all-or-nothing; see
        // ProperType#withPrimaryAnnotationsReplaced.
        if (instantiation.ignoreAnnotations
            && nonPolymorphicPrimaryAnnotationCount(origin, qh) < qh.getTopAnnotations().size()) {
          result.add(applied);
        }
        InferenceType instantiationOrigin = instantiation.getOrigin();
        // An F-bounded variable's instantiation can contain the variable itself.
        if (instantiationOrigin != null && visiting.add(variable)) {
          collectIgnoredPositions(
              instantiationOrigin.getAnnotatedType(),
              applied,
              instantiationOrigin.getMap(),
              qh,
              visiting,
              visitedWildcards,
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
   * Calls {@link #collectIgnoredPositions} on corresponding elements of the two lists, which have
   * the same length because {@code appliedTypes} was made from {@code origins} by applying
   * instantiations.
   *
   * @param origins types that may contain uses of the inference variables in {@code map}
   * @param appliedTypes {@code origins} with the current instantiations of those variables applied
   * @param map the inference variables of {@code origins}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param visitedWildcards the pairs of wildcards that have been scanned (see {@link #addPair})
   * @param result the set to which to add ignored positions
   */
  private static void collectPairwise(
      List<? extends AnnotatedTypeMirror> origins,
      List<? extends AnnotatedTypeMirror> appliedTypes,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards,
      Set<AnnotatedTypeMirror> result) {
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
   * Gives each ignored position of {@code atm1} or {@code atm2} the annotations of the
   * corresponding position of the other type.
   *
   * <ul>
   *   <li>If a position of {@code atm1} is ignored and the corresponding position of {@code atm2}
   *       is not, the position of {@code atm1} gets the annotations of the position of {@code
   *       atm2}, and vice versa.
   *   <li>If both positions are ignored and {@code replaceWhenBothIgnored} is true, the position of
   *       {@code atm1} gets the annotations of the position of {@code atm2}, so that the two
   *       positions have the same annotations. If {@code replaceWhenBothIgnored} is false, neither
   *       position is changed.
   * </ul>
   *
   * <p>If the position that annotations are copied from is a wildcard, the annotations of its bound
   * are copied (see {@link #boundOf}).
   *
   * <p>Positions correspond only where the two types have the same structure; elsewhere, nothing is
   * replaced.
   *
   * @param atm1 a type; side-effected by this method
   * @param atm2 a type; side-effected by this method
   * @param ignored1 the ignored positions of {@code atm1}
   * @param ignored2 the ignored positions of {@code atm2}
   * @param replaceWhenBothIgnored true if a position that is ignored in both types gets the
   *     annotations of {@code atm2}'s position. That is right for a subtype or equality check, which
   *     the annotations at such a position must not make fail. It is wrong for a least upper bound
   *     or greatest lower bound, whose result at such a position must combine the annotations of
   *     both types, so that it does not depend on the order of the types (see {@link
   *     InferenceFactory#lub(Set)}).
   */
  private static void replaceIgnoredPositions(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      Set<AnnotatedTypeMirror> ignored1,
      Set<AnnotatedTypeMirror> ignored2,
      boolean replaceWhenBothIgnored) {
    allCorrespondingPositionsMatch(
        atm1,
        atm2,
        (position1, position2) -> {
          boolean isIgnored1 = ignored1.contains(position1);
          boolean isIgnored2 = ignored2.contains(position2);
          // If both are ignored and !replaceWhenBothIgnored, neither is changed.
          if (isIgnored1 && (!isIgnored2 || replaceWhenBothIgnored)) {
            copyPrimaryAnnotations(boundOf(position2), position1);
          } else if (isIgnored2 && !isIgnored1) {
            copyPrimaryAnnotations(boundOf(position1), position2);
          }
          return true;
        });
  }

  /**
   * Returns true if {@code test} holds for each pair of corresponding positions of {@code atm1} and
   * {@code atm2}, starting with {@code atm1} and {@code atm2} themselves. A position is tested
   * before the positions nested in it, and no position is tested after {@code test} returns false.
   * Positions correspond only where the two types have the same structure, with one exception:
   * where one type has a wildcard and the other does not, the wildcard and its bound (see {@link
   * #boundOf}) both correspond to the other type, so the positions nested in the bound correspond
   * to those nested in the other type. For example, in {@code List<? extends Box<@A String>>} and
   * {@code List<Box<@B String>>}, both {@code ? extends Box<@A String>} and {@code Box<@A String>}
   * correspond to {@code Box<@B String>}, and {@code @A String} corresponds to {@code @B String}.
   *
   * <p>Where both types have declared types of different classes, their type arguments correspond
   * by the supertype relation between the classes; see {@link
   * #allCorrespondingTypeArgumentsMatch}.
   *
   * @param atm1 a type
   * @param atm2 a type
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @return true if {@code test} holds for each pair of corresponding positions
   */
  private static boolean allCorrespondingPositionsMatch(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test) {
    return allCorrespondingPositionsMatch(atm1, atm2, test, new IdentityHashMap<>());
  }

  /**
   * Returns true if {@code test} holds for each pair of corresponding positions of {@code atm1} and
   * {@code atm2}; see {@link #allCorrespondingPositionsMatch(AnnotatedTypeMirror,
   * AnnotatedTypeMirror, BiPredicate)}. A pair of positions at least one of which is a wildcard is
   * tested at most once.
   *
   * @param atm1 a type
   * @param atm2 a type
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @return true if {@code test} holds for each pair of corresponding positions
   */
  private static boolean allCorrespondingPositionsMatch(
      AnnotatedTypeMirror atm1,
      AnnotatedTypeMirror atm2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards) {
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
        return allCorrespondingPositionsMatch(boundOf(atm1), atm2, test, visitedWildcards);
      } else if (atm2.getKind() == TypeKind.WILDCARD) {
        return allCorrespondingPositionsMatch(atm1, boundOf(atm2), test, visitedWildcards);
      }
      return true;
    }
    switch (atm1.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType dt1 = (AnnotatedDeclaredType) atm1;
        AnnotatedDeclaredType dt2 = (AnnotatedDeclaredType) atm2;
        TypeElement element1 = (TypeElement) dt1.getUnderlyingType().asElement();
        TypeElement element2 = (TypeElement) dt2.getUnderlyingType().asElement();
        if (!element1.equals(element2)) {
          return allCorrespondingTypeArgumentsMatch(
              dt1, element1, dt2, element2, test, visitedWildcards);
        }
        if (!allCorrespondingPositionsMatchPairwise(
            dt1.getTypeArguments(), dt2.getTypeArguments(), test, visitedWildcards)) {
          return false;
        }
        AnnotatedDeclaredType enclosing1 = dt1.getEnclosingType();
        AnnotatedDeclaredType enclosing2 = dt2.getEnclosingType();
        return enclosing1 == null
            || enclosing2 == null
            || allCorrespondingPositionsMatch(enclosing1, enclosing2, test, visitedWildcards);
      }
      case ARRAY -> {
        return allCorrespondingPositionsMatch(
            ((AnnotatedArrayType) atm1).getComponentType(),
            ((AnnotatedArrayType) atm2).getComponentType(),
            test,
            visitedWildcards);
      }
      case WILDCARD -> {
        AnnotatedWildcardType wt1 = (AnnotatedWildcardType) atm1;
        AnnotatedWildcardType wt2 = (AnnotatedWildcardType) atm2;
        return allCorrespondingPositionsMatch(
                wt1.getExtendsBound(), wt2.getExtendsBound(), test, visitedWildcards)
            && allCorrespondingPositionsMatch(
                wt1.getSuperBound(), wt2.getSuperBound(), test, visitedWildcards);
      }
      case INTERSECTION -> {
        return allCorrespondingPositionsMatchPairwise(
            ((AnnotatedIntersectionType) atm1).getBounds(),
            ((AnnotatedIntersectionType) atm2).getBounds(),
            test,
            visitedWildcards);
      }
      default -> {
        return true;
      }
    }
  }

  /**
   * Returns true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   * type arguments of {@code dt1} and {@code dt2}, which are declared types of different classes.
   * Such types can be nested within types of the same class, as in {@code Box<? extends
   * Swap<String, Integer>>} and {@code Box<? extends Pair<Integer, String>>}.
   *
   * <p>Type arguments correspond by the supertype relation between the two classes, not by index.
   * For example, if {@code Swap<A, B>} extends {@code Pair<B, A>}, then the first type argument of
   * {@code Swap} corresponds to the second type argument of {@code Pair}, and vice versa. A type
   * argument of the supertype that is not just a type parameter of the subtype, such as {@code
   * String} in {@code StringPair<B> extends Pair<String, B>}, corresponds to no type argument of
   * the subtype. If neither class is a subclass of the other, or either type is raw, then no type
   * arguments correspond. Enclosing types never correspond.
   *
   * @param dt1 a declared type
   * @param element1 the class of {@code dt1}
   * @param dt2 a declared type whose class is not that of {@code dt1}
   * @param element2 the class of {@code dt2}
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @return true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   *     type arguments
   */
  private static boolean allCorrespondingTypeArgumentsMatch(
      AnnotatedDeclaredType dt1,
      TypeElement element1,
      AnnotatedDeclaredType dt2,
      TypeElement element2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards) {
    List<? extends AnnotatedTypeMirror> args1 = dt1.getTypeArguments();
    List<? extends AnnotatedTypeMirror> args2 = dt2.getTypeArguments();
    if (args1.size() != element1.getTypeParameters().size()
        || args2.size() != element2.getTypeParameters().size()) {
      // A raw type.
      return true;
    }
    int[] indicesInDt1 = typeParameterCorrespondence(element1, element2);
    if (indicesInDt1 != null) {
      // `element1` is a subclass of `element2`.
      for (int i2 = 0; i2 < indicesInDt1.length; i2++) {
        int i1 = indicesInDt1[i2];
        if (i1 != -1
            && !allCorrespondingPositionsMatch(
                args1.get(i1), args2.get(i2), test, visitedWildcards)) {
          return false;
        }
      }
      return true;
    }
    int[] indicesInDt2 = typeParameterCorrespondence(element2, element1);
    if (indicesInDt2 != null) {
      // `element2` is a subclass of `element1`.
      for (int i1 = 0; i1 < indicesInDt2.length; i1++) {
        int i2 = indicesInDt2[i1];
        if (i2 != -1
            && !allCorrespondingPositionsMatch(
                args1.get(i1), args2.get(i2), test, visitedWildcards)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * If {@code superElement} is a proper superclass or superinterface of {@code subElement}, returns
   * an array that has an element for each type parameter of {@code superElement}: the index of the
   * type parameter of {@code subElement} that it is instantiated to in the supertype of {@code
   * subElement}, or -1 if it is instantiated to some other type. Otherwise, returns null. For
   * example, if {@code Swap<A, B>} extends {@code Pair<B, A>}, then {@code
   * typeParameterCorrespondence(Swap, Pair)} is {@code [1, 0]}.
   *
   * @param subElement a class or interface
   * @param superElement a class or interface other than {@code subElement}
   * @return the index of the type parameter of {@code subElement} that each type parameter of
   *     {@code superElement} corresponds to, or null if {@code superElement} is not a supertype of
   *     {@code subElement}
   */
  private static int @Nullable [] typeParameterCorrespondence(
      TypeElement subElement, TypeElement superElement) {
    int[] identity = new int[subElement.getTypeParameters().size()];
    for (int i = 0; i < identity.length; i++) {
      identity[i] = i;
    }
    return typeParameterCorrespondence(subElement, identity, superElement, new HashSet<>());
  }

  /**
   * Helper for {@link #typeParameterCorrespondence(TypeElement, TypeElement)}, which searches the
   * supertypes of {@code current} for {@code superElement}.
   *
   * @param current a supertype of the original subclass, or the subclass itself
   * @param indices for each type parameter of {@code current}, the index of the type parameter of
   *     the original subclass that it corresponds to, or -1
   * @param superElement the class or interface to search for
   * @param visited the classes and interfaces that have been searched. Java forbids inheriting a
   *     class or interface with two different parameterizations, so each needs to be searched only
   *     once.
   * @return the correspondence for {@code superElement}, or null if it is not a supertype of {@code
   *     current}
   */
  private static int @Nullable [] typeParameterCorrespondence(
      TypeElement current, int[] indices, TypeElement superElement, Set<TypeElement> visited) {
    if (current.equals(superElement)) {
      return indices;
    }
    if (!visited.add(current)) {
      return null;
    }
    List<TypeMirror> supertypes = new ArrayList<>(current.getInterfaces());
    supertypes.add(current.getSuperclass());
    List<? extends TypeParameterElement> currentParams = current.getTypeParameters();
    for (TypeMirror supertype : supertypes) {
      if (supertype.getKind() != TypeKind.DECLARED) {
        // The superclass of Object or of an interface is NoType.
        continue;
      }
      DeclaredType declaredSupertype = (DeclaredType) supertype;
      TypeElement supertypeElement = (TypeElement) declaredSupertype.asElement();
      List<? extends TypeMirror> supertypeArgs = declaredSupertype.getTypeArguments();
      int[] supertypeIndices = new int[supertypeElement.getTypeParameters().size()];
      Arrays.fill(supertypeIndices, -1);
      // If the supertype is raw, then no type parameter of it corresponds.
      if (supertypeArgs.size() == supertypeIndices.length) {
        for (int i = 0; i < supertypeIndices.length; i++) {
          if (supertypeArgs.get(i) instanceof TypeVariable typeVar) {
            int index = currentParams.indexOf(typeVar.asElement());
            if (index != -1) {
              supertypeIndices[i] = indices[index];
            }
          }
        }
      }
      int[] result =
          typeParameterCorrespondence(supertypeElement, supertypeIndices, superElement, visited);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   * elements of the two lists. If the lists have different lengths, no positions correspond, so
   * returns true without testing any. That is not an error: the lists can be the type arguments of
   * a raw type and of a parameterized type of the same class, or the bounds of intersection types
   * that have different numbers of bounds.
   *
   * @param types1 a list of types
   * @param types2 a list of types
   * @param test the test to apply to each pair of corresponding positions; it may side-effect them
   * @param visitedWildcards the pairs of positions, at least one of which is a wildcard, that have
   *     been tested (see {@link #addPair})
   * @return true if {@link #allCorrespondingPositionsMatch} holds for each pair of corresponding
   *     elements
   */
  private static boolean allCorrespondingPositionsMatchPairwise(
      List<? extends AnnotatedTypeMirror> types1,
      List<? extends AnnotatedTypeMirror> types2,
      BiPredicate<AnnotatedTypeMirror, AnnotatedTypeMirror> test,
      Map<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visitedWildcards) {
    if (types1.size() != types2.size()) {
      return true;
    }
    Iterator<? extends AnnotatedTypeMirror> iter2 = types2.iterator();
    for (AnnotatedTypeMirror type1 : types1) {
      if (!allCorrespondingPositionsMatch(type1, iter2.next(), test, visitedWildcards)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds the pair of {@code atm1} and {@code atm2} to {@code visited}, which maps a type to the
   * types with which it has been paired. Types are compared by identity, because distinct positions
   * can hold equal types.
   *
   * @param visited the pairs visited so far; side-effected by this method
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
