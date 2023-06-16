package org.checkerframework.framework.util.typeinference.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/**
 * Infers type arguments by using the Least Upper Bound computation on the supertype relationships
 * in a constraint map.
 */
public class SupertypesSolver {

  /**
   * Infers type arguments using supertype constraints.
   *
   * @param remainingTargets targets for which we still need to infer a value
   * @param constraintMap the set of constraints for all targets
   * @return a mapping from target to inferred type. Note this class always infers concrete types
   *     and will not infer that the target is equivalent to another target.
   */
  public InferenceResult solveFromSupertypes(
      Set<TypeVariable> remainingTargets,
      ConstraintMap constraintMap,
      AnnotatedTypeFactory typeFactory) {
    // infer a type for all targets that have supertype constraints
    Lubs lubs = targetToTypeLubs(remainingTargets, constraintMap, typeFactory);

    // add the lub types to the outgoing solution
    InferenceResult solution = new InferenceResult();
    for (TypeVariable target : remainingTargets) {
      AnnotatedTypeMirror lub = lubs.getType(target);
      AnnotationMirrorMap<AnnotationMirror> lubAnnos = lubs.getPrimaries(target);

      // we may have a partial solution present in the equality constraints, override
      // any annotations found in the lub with annotations from the equality constraints
      final InferredValue inferred;
      if (lub != null) {
        inferred = mergeLubTypeWithEqualities(target, lub, constraintMap, typeFactory);
      } else if (lubAnnos != null) {
        inferred = mergeLubAnnosWithEqualities(target, lubAnnos, constraintMap, typeFactory);
      } else {
        inferred = null;
      }

      if (inferred != null) {
        solution.put(target, inferred);
      }
    }

    return solution;
  }

  /**
   * We previously found a type that is equal to target but not in all hierarchies. Use the primary
   * annotations from the lub type to fill in the missing annotations in this type. Use that type as
   * the inferred argument.
   *
   * <p>If we failed to infer any annotation for a given hierarchy, either previously from
   * equalities or from the lub, return null.
   */
  protected InferredType mergeLubTypeWithEqualities(
      TypeVariable target,
      AnnotatedTypeMirror lub,
      ConstraintMap constraintMap,
      AnnotatedTypeFactory typeFactory) {
    Equalities equalities = constraintMap.getConstraints(target).equalities;
    AnnotationMirrorSet tops =
        new AnnotationMirrorSet(typeFactory.getQualifierHierarchy().getTopAnnotations());

    if (!equalities.types.isEmpty()) {
      // there should be only one equality type if any at this point
      Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> eqEntry =
          equalities.types.entrySet().iterator().next();
      AnnotatedTypeMirror equalityType = eqEntry.getKey();
      AnnotationMirrorSet equalityAnnos = eqEntry.getValue();

      boolean failed = false;
      for (AnnotationMirror top : tops) {
        if (!equalityAnnos.contains(top)) {
          AnnotationMirror lubAnno = lub.getPrimaryAnnotationInHierarchy(top);
          if (lubAnno == null) {
            // If the LUB and the Equality were the SAME typevar, and the lub was
            // unannotated then "NO ANNOTATION" is the correct choice.
            if (lub.getKind() == TypeKind.TYPEVAR
                && typeFactory.types.isSameType(
                    equalityType.getUnderlyingType(), lub.getUnderlyingType())) {
              equalityAnnos.add(top);
            } else {
              failed = true;
            }

          } else {
            equalityType.replaceAnnotation(lubAnno);
            equalityAnnos.add(top);
          }
        }
      }

      if (!failed) {
        return new InferredType(equalityType);
      }
    }

    return new InferredType(lub);
  }

  /**
   * We previously found a type that is equal to target but not in all hierarchies. Use the primary
   * annotations from the lub annos to fill in the missing annotations in this type. Use that type
   * as the inferred argument.
   *
   * <p>If we failed to infer any annotation for a given hierarchy, either previously from
   * equalities or from the lub, return null.
   */
  protected InferredType mergeLubAnnosWithEqualities(
      TypeVariable target,
      AnnotationMirrorMap<AnnotationMirror> lubAnnos,
      ConstraintMap constraintMap,
      AnnotatedTypeFactory typeFactory) {
    Equalities equalities = constraintMap.getConstraints(target).equalities;
    AnnotationMirrorSet tops =
        new AnnotationMirrorSet(typeFactory.getQualifierHierarchy().getTopAnnotations());

    if (!equalities.types.isEmpty()) {
      // there should be only equality type if any at this point
      Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> eqEntry =
          equalities.types.entrySet().iterator().next();
      AnnotatedTypeMirror equalityType = eqEntry.getKey();
      AnnotationMirrorSet equalityAnnos = eqEntry.getValue();

      boolean failed = false;
      for (AnnotationMirror top : tops) {
        if (!equalityAnnos.contains(top)) {
          AnnotationMirror lubAnno = lubAnnos.get(top);
          if (lubAnno == null) {
            failed = true;

          } else {
            equalityType.replaceAnnotation(lubAnno);
            equalityAnnos.add(top);
          }
        }
      }

      if (!failed) {
        return new InferredType(equalityType);
      }
    }

    return null;
  }

  /** Holds the least upper bounds for every target type parameter. */
  static class Lubs {
    public final Map<TypeVariable, AnnotatedTypeMirror> types = new LinkedHashMap<>();
    public final Map<TypeVariable, AnnotationMirrorMap<AnnotationMirror>> primaries =
        new LinkedHashMap<>();

    public void addPrimaries(TypeVariable target, AnnotationMirrorMap<AnnotationMirror> primaries) {
      this.primaries.put(target, new AnnotationMirrorMap<>(primaries));
    }

    public void addType(TypeVariable target, AnnotatedTypeMirror type) {
      types.put(target, type);
    }

    public AnnotationMirrorMap<AnnotationMirror> getPrimaries(TypeVariable target) {
      return primaries.get(target);
    }

    public AnnotatedTypeMirror getType(TypeVariable target) {
      return types.get(target);
    }
  }

  /**
   * For each target, lub all of the types/annotations in its supertypes constraints and return the
   * lubs.
   *
   * @param remainingTargets targets that do not already have an inferred type argument
   * @param constraintMap the set of constraints for all targets
   * @return the lub determined for each target that has at least 1 supertype constraint
   */
  private Lubs targetToTypeLubs(
      Set<TypeVariable> remainingTargets,
      ConstraintMap constraintMap,
      AnnotatedTypeFactory typeFactory) {
    QualifierHierarchy qualHierarchy = typeFactory.getQualifierHierarchy();
    AnnotationMirrorSet tops = new AnnotationMirrorSet(qualHierarchy.getTopAnnotations());

    Lubs solution = new Lubs();

    AnnotationMirrorMap<AnnotationMirror> lubOfPrimaries = new AnnotationMirrorMap<>();

    List<TypeVariable> targetsSupertypesLast = new ArrayList<>(remainingTargets);

    Types types = typeFactory.getProcessingEnv().getTypeUtils();
    // If we have two type variables <A, A extends B> order them B then A
    // this is required because we will use the fact that A must be above B
    // when determining the LUB of A
    Collections.sort(
        targetsSupertypesLast,
        (o1, o2) -> {
          if (types.isSubtype(o1, o2)) {
            return -1;
          } else if (types.isSubtype(o2, o1)) {
            return 1;
          }
          return 0;
        });

    for (TypeVariable target : targetsSupertypesLast) {
      TargetConstraints targetRecord = constraintMap.getConstraints(target);
      AnnotationMirrorMap<AnnotationMirrorSet> subtypeAnnos = targetRecord.supertypes.primaries;
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> subtypesOfTarget =
          targetRecord.supertypes.types;

      // If this target is a supertype of other targets and those targets have already been
      // lubbed add that LUB to the list of lubs for this target (as it must be above this
      // target).
      propagatePreviousLubs(targetRecord, solution, subtypesOfTarget);

      // lub all the primary annotations and put them in lubOfPrimaries
      lubPrimaries(lubOfPrimaries, subtypeAnnos, tops, qualHierarchy);
      solution.addPrimaries(target, lubOfPrimaries);

      if (!subtypesOfTarget.isEmpty()) {
        AnnotatedTypeMirror lub = leastUpperBound(target, typeFactory, subtypesOfTarget);
        AnnotationMirrorSet effectiveLubAnnos =
            new AnnotationMirrorSet(lub.getEffectiveAnnotations());

        for (AnnotationMirror lubAnno : effectiveLubAnnos) {
          AnnotationMirror hierarchy = qualHierarchy.getTopAnnotation(lubAnno);
          AnnotationMirror primaryLub = lubOfPrimaries.get(hierarchy);

          if (primaryLub != null) {
            if (qualHierarchy.isSubtype(lubAnno, primaryLub)
                && !AnnotationUtils.areSame(lubAnno, primaryLub)) {
              lub.replaceAnnotation(primaryLub);
            }
          }
        }

        solution.addType(target, lub);
      }
    }
    return solution;
  }

  /**
   * If the target corresponding to targetRecord must be a supertype of another target for which we
   * have already determined a lub, add that target's lub to this list.
   */
  protected static void propagatePreviousLubs(
      TargetConstraints targetRecord,
      Lubs solution,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> subtypesOfTarget) {

    for (Map.Entry<TypeVariable, AnnotationMirrorSet> supertypeTarget :
        targetRecord.supertypes.targets.entrySet()) {
      AnnotatedTypeMirror supertargetLub = solution.getType(supertypeTarget.getKey());
      if (supertargetLub != null) {
        AnnotationMirrorSet supertargetTypeAnnos = subtypesOfTarget.get(supertargetLub);
        if (supertargetTypeAnnos != null) {
          // there is already an equivalent type in the list of subtypes, just add
          // any hierarchies that are not in its list but are in the supertarget's list
          supertargetTypeAnnos.addAll(supertypeTarget.getValue());
        } else {
          subtypesOfTarget.put(supertargetLub, supertypeTarget.getValue());
        }
      }
    }
  }

  /**
   * For each qualifier hierarchy in tops, take the lub of the annos in subtypeAnnos that correspond
   * to that hierarchy place the lub in lubOfPrimaries.
   */
  protected static void lubPrimaries(
      AnnotationMirrorMap<AnnotationMirror> lubOfPrimaries,
      AnnotationMirrorMap<AnnotationMirrorSet> subtypeAnnos,
      AnnotationMirrorSet tops,
      QualifierHierarchy qualHierarchy) {

    lubOfPrimaries.clear();
    for (AnnotationMirror top : tops) {
      AnnotationMirrorSet annosInHierarchy = subtypeAnnos.get(top);
      if (annosInHierarchy != null && !annosInHierarchy.isEmpty()) {
        lubOfPrimaries.put(top, leastUpperBound(annosInHierarchy, qualHierarchy));
      } else {
        // If there are no annotations for this hierarchy, add bottom.  This happens
        // when the only constraint for a type variable is a use that is annotated in
        // this hierarchy. Calls to the method below have this property.
        // <T> void method(@NonNull T t) {}
        lubOfPrimaries.put(top, qualHierarchy.getBottomAnnotation(top));
      }
    }
  }

  /**
   * For each type in typeToHierarchies, if that type does not have a corresponding annotation for a
   * given hierarchy replace it with the corresponding value in lowerBoundAnnos.
   */
  public static AnnotatedTypeMirror groundMissingHierarchies(
      Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> typeToHierarchies,
      AnnotationMirrorMap<AnnotationMirror> lowerBoundAnnos) {
    AnnotationMirrorSet presentHierarchies = typeToHierarchies.getValue();
    AnnotationMirrorSet missingAnnos = new AnnotationMirrorSet();
    for (AnnotationMirror top : lowerBoundAnnos.keySet()) {
      if (!presentHierarchies.contains(top)) {
        missingAnnos.add(lowerBoundAnnos.get(top));
      }
    }

    if (!missingAnnos.isEmpty()) {
      AnnotatedTypeMirror copy = typeToHierarchies.getKey().deepCopy();
      copy.replaceAnnotations(missingAnnos);

      return copy;
    }

    return typeToHierarchies.getKey();
  }

  /**
   * Successively calls least upper bound on the elements of types. Unlike
   * AnnotatedTypes.leastUpperBound, this method will box primitives if necessary
   */
  public static AnnotatedTypeMirror leastUpperBound(
      TypeVariable target,
      AnnotatedTypeFactory typeFactory,
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> types) {

    QualifierHierarchy qualHierarchy = typeFactory.getQualifierHierarchy();
    AnnotatedTypeVariable targetsDeclaredType =
        (AnnotatedTypeVariable) typeFactory.getAnnotatedType(target.asElement());
    AnnotationMirrorMap<AnnotationMirror> lowerBoundAnnos =
        TypeArgInferenceUtil.createHierarchyMap(
            new AnnotationMirrorSet(targetsDeclaredType.getLowerBound().getEffectiveAnnotations()),
            qualHierarchy);

    Iterator<Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet>> typesIter =
        types.entrySet().iterator();
    if (!typesIter.hasNext()) {
      throw new BugInCF("Calling LUB on empty list.");
    }

    // If a constraint implies that a type parameter Ti is a supertype of an annotated type mirror
    // Ai but only in a subset of all qualifier hierarchies then for all other qualifier hierarchies
    // replace the primary annotation on Ai with the lowest possible annotation (ensuring that it
    // won't be the LUB unless there are no other constraints, or all other constraints imply the
    // bottom annotation is the LUB). Note: Even if we choose bottom as the lub here, the assignment
    // context may raise this annotation.
    Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> head = typesIter.next();

    AnnotatedTypeMirror lubType = groundMissingHierarchies(head, lowerBoundAnnos);
    AnnotatedTypeMirror nextType = null;
    while (typesIter.hasNext()) {
      nextType = groundMissingHierarchies(typesIter.next(), lowerBoundAnnos);

      if (lubType.getKind().isPrimitive()) {
        if (!nextType.getKind().isPrimitive()) {
          lubType = typeFactory.getBoxedType((AnnotatedPrimitiveType) lubType);
        }
      } else if (nextType.getKind().isPrimitive()) {
        if (!lubType.getKind().isPrimitive()) {
          nextType = typeFactory.getBoxedType((AnnotatedPrimitiveType) nextType);
        }
      }
      lubType = AnnotatedTypes.leastUpperBound(typeFactory, lubType, nextType);
    }

    return lubType;
  }

  /**
   * Returns the lub of all the annotations in annos.
   *
   * @param annos a set of annotations in the same annotation hierarchy
   * @param qualHierarchy the qualifier hierarchy that contains each annotation
   * @return the lub of all the annotations in annos
   */
  private static AnnotationMirror leastUpperBound(
      Iterable<? extends AnnotationMirror> annos, QualifierHierarchy qualHierarchy) {
    Iterator<? extends AnnotationMirror> annoIter = annos.iterator();
    AnnotationMirror lub = annoIter.next();

    while (annoIter.hasNext()) {
      lub = qualHierarchy.leastUpperBound(lub, annoIter.next());
    }

    return lub;
  }
}
