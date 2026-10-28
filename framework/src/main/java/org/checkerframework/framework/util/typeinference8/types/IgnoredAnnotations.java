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
 * <p>A <em>position</em> in a type is the type itself or a type nested in it at any depth. Two
 * kinds of position in a type have annotations that are ignored, because they come from a formula
 * about a use of an inference variable that has an explicit primary annotation, such as
 * {@code @Nullable T}, and so say nothing about the variable's annotations:
 *
 * <ul>
 *   <li>the primary annotations of a type whose {@link AbstractType#ignoreAnnotations} is true,
 *       including those of its bounds if it is a type variable or a wildcard, but not annotations
 *       nested in it, such as those of its type arguments; and
 *   <li>an <em>ignored substitution</em>: a position in a {@link ProperType} where {@link
 *       AbstractType#applyInstantiations} replaced a use of an inference variable by the variable's
 *       instantiation, and that instantiation ignores annotations. The type's {@link
 *       ProperType#getOrigin() origin} still has the use of the variable there.
 * </ul>
 *
 * <p>Before two types are compared for subtyping or equality, or combined by a least upper bound or
 * greatest lower bound, the methods of this class replace the annotations at their ignored
 * positions with annotations that cannot affect the result: for example, bottom for a subtype and
 * top for a supertype, or the annotations at the corresponding position of the other type. The rest
 * of each type is then compared or combined as usual.
 */
final class IgnoredAnnotations {

  /** Do not instantiate. */
  private IgnoredAnnotations() {
    throw new AssertionError("Class IgnoredAnnotations cannot be instantiated.");
  }

  /**
   * Returns the annotated types of {@code sub} and {@code sup} with their ignored positions
   * replaced. A type that ignores annotations has bottom primary annotations if it is {@code sub},
   * and top primary annotations if it is {@code sup}. An ignored substitution has the annotations
   * of the corresponding position of the other type. If neither type has an ignored position, the
   * result holds the types' own annotated types; otherwise it holds copies.
   *
   * @param sub the potential subtype
   * @param sup the potential supertype
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return the annotated types of {@code sub} and {@code sup}, with ignored positions replaced
   */
  static ReplacedTypes replaceIgnoredForSubtype(
      AbstractType sub, AbstractType sup, QualifierHierarchy qh, Types types) {
    AbstractType upToDateSub = upToDate(sub);
    AbstractType upToDateSup = upToDate(sup);
    boolean mayHaveIgnoredSubstitutions =
        upToDateSub.getOrigin() != null || upToDateSup.getOrigin() != null;
    if (!upToDateSub.ignoreAnnotations
        && !upToDateSup.ignoreAnnotations
        && !mayHaveIgnoredSubstitutions) {
      return new ReplacedTypes(upToDateSub.getAnnotatedType(), upToDateSup.getAnnotatedType());
    }
    AbstractType alignedSub = upToDateSub;
    if (mayHaveIgnoredSubstitutions) {
      // Compare the type arguments of the same class, so that corresponding positions line up.
      alignedSub = viewedAsClass(upToDateSub, upToDateSup.getJavaType(), types);
    }
    AnnotatedTypeMirror subATM = alignedSub.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror supATM = upToDateSup.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredSubstitutions) {
      replaceIgnoredSubstitutions(
          subATM,
          supATM,
          ignoredSubstitutions(alignedSub, subATM, qh),
          ignoredSubstitutions(upToDateSup, supATM, qh));
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
   * with their ignored positions replaced. An ignored position of one type has the annotations of
   * the corresponding position of the other type. If neither type has an ignored position, the
   * result holds the types' own annotated types; otherwise it holds copies.
   *
   * @param type1 a type
   * @param type2 a type with the same Java type as {@code type1}
   * @param qh the qualifier hierarchy
   * @return the annotated types of {@code type1} and {@code type2}, with ignored positions replaced
   */
  static ReplacedTypes replaceIgnoredForEquality(
      AbstractType type1, AbstractType type2, QualifierHierarchy qh) {
    AbstractType upToDate1 = upToDate(type1);
    AbstractType upToDate2 = upToDate(type2);
    boolean mayHaveIgnoredSubstitutions =
        upToDate1.getOrigin() != null || upToDate2.getOrigin() != null;
    if (!upToDate1.ignoreAnnotations
        && !upToDate2.ignoreAnnotations
        && !mayHaveIgnoredSubstitutions) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
    }
    AnnotatedTypeMirror atm1 = upToDate1.getAnnotatedType().deepCopy();
    AnnotatedTypeMirror atm2 = upToDate2.getAnnotatedType().deepCopy();
    if (mayHaveIgnoredSubstitutions) {
      replaceIgnoredSubstitutions(
          atm1,
          atm2,
          ignoredSubstitutions(upToDate1, atm1, qh),
          ignoredSubstitutions(upToDate2, atm2, qh));
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
   * ignored substitution of one type are replaced by those at the corresponding position of the
   * other. If either type has ignored substitutions, then both are first viewed as the class of
   * {@code resultType}. Primary annotations are not replaced. If nothing is replaced, the result
   * holds the types' own annotated types; otherwise it holds copies.
   *
   * @param type1 a type
   * @param type2 a type
   * @param resultType the Java type of the least upper bound or greatest lower bound of {@code
   *     type1} and {@code type2}
   * @param qh the qualifier hierarchy
   * @param types the type utilities
   * @return the annotated types of {@code type1} and {@code type2}, with ignored substitutions
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
    AbstractType aligned1 = viewedAsClass(upToDate1, resultType, types);
    AbstractType aligned2 = viewedAsClass(upToDate2, resultType, types);
    if (!types.isSameType(
        types.erasure(aligned1.getJavaType()), types.erasure(aligned2.getJavaType()))) {
      return new ReplacedTypes(upToDate1.getAnnotatedType(), upToDate2.getAnnotatedType());
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
   * Returns {@code type} brought up to date: if it is a proper type with an origin (see {@link
   * ProperType#getOrigin}), the result of substituting the current instantiations of the variables
   * that its origin mentions; otherwise, {@code type} itself. The ignored substitutions of a type
   * are found from those instantiations, so its annotated type must be made from them too.
   *
   * @param type a type
   * @return {@code type} brought up to date
   */
  private static AbstractType upToDate(AbstractType type) {
    return type.getOrigin() != null ? type.applyInstantiations() : type;
  }

  /**
   * Returns the supertype of {@code type} whose class is that of {@code classType} (see {@link
   * AbstractType#asSuper}); for example, {@code ArrayList<String>} viewed as class {@code List} is
   * {@code List<String>}. Returns {@code type} itself if either is not a declared type, if both are
   * of the same class, or if {@code type} has no such supertype that is a proper type.
   *
   * @param type a type
   * @param classType a type
   * @param types the type utilities
   * @return the supertype of {@code type} whose class is that of {@code classType}, or {@code type}
   */
  private static AbstractType viewedAsClass(AbstractType type, TypeMirror classType, Types types) {
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
   * at any depth (see {@link #hasNoTypeArguments}), and at least one of them ignores its primary
   * annotations.
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
   * Returns the ignored substitutions of {@code atm}, which is {@code type}'s annotated type or a
   * deep copy of it. The result is an identity set.
   *
   * @param type a type
   * @param atm the annotated type of {@code type}, or a deep copy of it
   * @param qh the qualifier hierarchy
   * @return the ignored substitutions of {@code atm}
   */
  private static Set<AnnotatedTypeMirror> ignoredSubstitutions(
      AbstractType type, AnnotatedTypeMirror atm, QualifierHierarchy qh) {
    InferenceType origin = type.getOrigin();
    if (origin == null) {
      return Collections.emptySet();
    }
    Set<AnnotatedTypeMirror> result = Collections.newSetFromMap(new IdentityHashMap<>());
    collectIgnoredSubstitutions(
        origin.getAnnotatedType(), atm, origin.getMap(), qh, new HashSet<>(), result);
    return result;
  }

  /**
   * Adds to {@code result} each ignored substitution of {@code substituted}, which is {@code
   * origin} with instantiations substituted. A position of {@code substituted} is an ignored
   * substitution if the corresponding position of {@code origin} is a use of a variable such that
   * either:
   *
   * <ul>
   *   <li>the variable's instantiation ignores annotations, and the use's non-polymorphic primary
   *       annotations do not cover every qualifier hierarchy (see {@link
   *       UseOfVariable#applyInstantiations}), or
   *   <li>the position is an ignored substitution of the variable's instantiation.
   * </ul>
   *
   * @param origin a type that mentions inference variables
   * @param substituted {@code origin} with instantiations substituted
   * @param map the inference variables of {@code origin}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param result the set to which to add ignored substitutions
   */
  private static void collectIgnoredSubstitutions(
      AnnotatedTypeMirror origin,
      AnnotatedTypeMirror substituted,
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
          result.add(substituted);
        }
        InferenceType instantiationOrigin = instantiation.getOrigin();
        // An F-bounded variable's instantiation can mention the variable itself.
        if (instantiationOrigin != null && visiting.add(variable)) {
          collectIgnoredSubstitutions(
              instantiationOrigin.getAnnotatedType(),
              substituted,
              instantiationOrigin.getMap(),
              qh,
              visiting,
              result);
          visiting.remove(variable);
        }
      }
      return;
    }
    if (origin.getKind() != substituted.getKind()) {
      return;
    }
    switch (origin.getKind()) {
      case DECLARED -> {
        AnnotatedDeclaredType originDT = (AnnotatedDeclaredType) origin;
        AnnotatedDeclaredType substitutedDT = (AnnotatedDeclaredType) substituted;
        collectPairwise(
            originDT.getTypeArguments(),
            substitutedDT.getTypeArguments(),
            map,
            qh,
            visiting,
            result);
        AnnotatedDeclaredType originEnclosing = originDT.getEnclosingType();
        AnnotatedDeclaredType substitutedEnclosing = substitutedDT.getEnclosingType();
        if (originEnclosing != null && substitutedEnclosing != null) {
          collectIgnoredSubstitutions(
              originEnclosing, substitutedEnclosing, map, qh, visiting, result);
        }
      }
      case ARRAY ->
          collectIgnoredSubstitutions(
              ((AnnotatedArrayType) origin).getComponentType(),
              ((AnnotatedArrayType) substituted).getComponentType(),
              map,
              qh,
              visiting,
              result);
      case WILDCARD -> {
        AnnotatedWildcardType originWT = (AnnotatedWildcardType) origin;
        AnnotatedWildcardType substitutedWT = (AnnotatedWildcardType) substituted;
        collectIgnoredSubstitutions(
            originWT.getExtendsBound(), substitutedWT.getExtendsBound(), map, qh, visiting, result);
        collectIgnoredSubstitutions(
            originWT.getSuperBound(), substitutedWT.getSuperBound(), map, qh, visiting, result);
      }
      case INTERSECTION ->
          collectPairwise(
              ((AnnotatedIntersectionType) origin).getBounds(),
              ((AnnotatedIntersectionType) substituted).getBounds(),
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
   * @param substitutedTypes {@code origins} with instantiations substituted
   * @param map the inference variables of {@code origins}
   * @param qh the qualifier hierarchy
   * @param visiting the variables whose instantiations are being scanned
   * @param result the set to which to add ignored substitutions
   */
  private static void collectPairwise(
      List<? extends AnnotatedTypeMirror> origins,
      List<? extends AnnotatedTypeMirror> substitutedTypes,
      Theta map,
      QualifierHierarchy qh,
      Set<Variable> visiting,
      Set<AnnotatedTypeMirror> result) {
    if (origins.size() != substitutedTypes.size()) {
      return;
    }
    Iterator<? extends AnnotatedTypeMirror> substitutedIter = substitutedTypes.iterator();
    for (AnnotatedTypeMirror origin : origins) {
      collectIgnoredSubstitutions(origin, substitutedIter.next(), map, qh, visiting, result);
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
      copyPrimaryAnnotations(boundOf(atm2), atm1);
      return;
    }
    if (ignored2.contains(atm2)) {
      copyPrimaryAnnotations(boundOf(atm1), atm2);
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
   * Returns the lower bound of {@code type} if it is a wildcard that has one, its upper bound if it
   * is a wildcard that does not, and {@code type} itself otherwise.
   *
   * @param type a type
   * @return the lower or upper bound of {@code type} if it is a wildcard, otherwise {@code type}
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
   * Gives {@code target} the primary annotations of {@code source}. If both are type variables and
   * {@code source} has no primary annotation, then {@code target}'s primary annotations are removed
   * and its bounds get the primary annotations of {@code source}'s bounds.
   *
   * @param source the type whose primary annotations to copy
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
