package org.checkerframework.framework.util.typeinference.solver;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredTarget;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.javacutil.BugInCF;

/**
 * EqualitiesSolver infers type arguments for targets using the equality constraints in
 * ConstraintMap. When a type is inferred, it rewrites the remaining equality/supertype constraints
 */
public class EqualitiesSolver {
  private boolean dirty = false;

  /**
   * For each target, if there is one or more equality constraints involving concrete types that
   * lets us infer a primary annotation in all qualifier hierarchies then infer a concrete type
   * argument. else if there is one or more equality constraints involving other targets that lets
   * us infer a primary annotation in all qualifier hierarchies then infer that type argument is the
   * other type argument
   *
   * <p>if we have inferred either a concrete type or another target as type argument rewrite all of
   * the constraints for the current target to instead use the inferred type/target
   *
   * <p>We do this iteratively until NO new inferred type argument is found
   *
   * @param targets the list of type parameters for which we are inferring type arguments
   * @param constraintMap the set of constraints over the set of targets
   * @return a Map from target to (inferred type or target)
   */
  public InferenceResult solveEqualities(
      Set<TypeVariable> targets, ConstraintMap constraintMap, AnnotatedTypeFactory typeFactory) {
    final InferenceResult solution = new InferenceResult();

    do {
      dirty = false;
      for (TypeVariable target : targets) {
        if (solution.containsKey(target)) {
          continue;
        }

        Equalities equalities = constraintMap.getConstraints(target).equalities;

        InferredValue inferred =
            mergeConstraints(target, equalities, solution, constraintMap, typeFactory);
        if (inferred != null) {
          if (inferred instanceof InferredType) {
            rewriteWithInferredType(target, ((InferredType) inferred).type, constraintMap);
          } else {
            rewriteWithInferredTarget(
                target, ((InferredTarget) inferred).target, constraintMap, typeFactory);
          }

          solution.put(target, inferred);
        }
      }

    } while (dirty);

    solution.resolveChainedTargets();

    return solution;
  }

  /**
   * Let Ti be a target type parameter. When we reach this method we have inferred an argument, Ai,
   * for Ti.
   *
   * <p>However, there still may be constraints of the form {@literal Ti = Tj}, {@literal Ti <: Tj},
   * {@literal Tj <: Ti} in the constraint map. In this case we need to replace Ti with the type.
   * That is, they become {@literal Ai = Tj}, {@literal Ai <: Tj}, and {@literal Tj <: Ai}
   *
   * <p>To do this, we find the TargetConstraints for Tj and add these constraints to the
   * appropriate map in TargetConstraints. We can then clear the constraints for the current target
   * since we have inferred a type.
   *
   * @param target the target for which we have inferred a concrete type argument
   * @param type the type inferred
   * @param constraints the constraints that are side-effected by this method
   */
  private void rewriteWithInferredType(
      final @FindDistinct TypeVariable target,
      final AnnotatedTypeMirror type,
      final ConstraintMap constraints) {

    final TargetConstraints targetRecord = constraints.getConstraints(target);
    final Map<TypeVariable, AnnotationMirrorSet> equivalentTargets =
        targetRecord.equalities.targets;
    // each target that was equivalent to this one needs to be equivalent in the same
    // hierarchies as the inferred type
    for (final Map.Entry<TypeVariable, AnnotationMirrorSet> eqEntry :
        equivalentTargets.entrySet()) {
      constraints.addTypeEqualities(eqEntry.getKey(), type, eqEntry.getValue());
    }

    for (TypeVariable otherTarget : constraints.getTargets()) {
      if (otherTarget != target) {
        final TargetConstraints record = constraints.getConstraints(otherTarget);

        // each target that was equivalent to this one needs to be equivalent in the same
        // hierarchies as the inferred type
        final AnnotationMirrorSet hierarchies = record.equalities.targets.get(target);
        if (hierarchies != null) {
          record.equalities.targets.remove(target);
          constraints.addTypeEqualities(otherTarget, type, hierarchies);
        }

        // otherTypes may have AnnotatedTypeVariables of type target, run substitution on
        // these with type
        Map<AnnotatedTypeMirror, AnnotationMirrorSet> toIterate =
            new LinkedHashMap<>(record.equalities.types);
        record.equalities.types.clear();
        for (AnnotatedTypeMirror otherType : toIterate.keySet()) {
          final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, type, otherType);
          final AnnotationMirrorSet otherHierarchies = toIterate.get(otherType);
          record.equalities.types.put(copy, otherHierarchies);
        }
      }
    }

    for (TypeVariable otherTarget : constraints.getTargets()) {
      if (otherTarget != target) {
        final TargetConstraints record = constraints.getConstraints(otherTarget);

        // each target that was equivalent to this one needs to be equivalent in the same
        // hierarchies as the inferred type
        final AnnotationMirrorSet hierarchies = record.supertypes.targets.get(target);
        if (hierarchies != null) {
          record.supertypes.targets.remove(target);
          constraints.addTypeEqualities(otherTarget, type, hierarchies);
        }

        // otherTypes may have AnnotatedTypeVariables of type target, run substitution on
        // these with type
        Map<AnnotatedTypeMirror, AnnotationMirrorSet> toIterate =
            new LinkedHashMap<>(record.supertypes.types);
        record.supertypes.types.clear();
        for (AnnotatedTypeMirror otherType : toIterate.keySet()) {
          final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, type, otherType);
          final AnnotationMirrorSet otherHierarchies = toIterate.get(otherType);
          record.supertypes.types.put(copy, otherHierarchies);
        }
      }
    }

    targetRecord.equalities.clear();
    targetRecord.supertypes.clear();
  }

  /**
   * Let Ti be a target type parameter. When we reach this method we have inferred that Ti has the
   * exact same argument as another target Tj
   *
   * <p>Therefore, we want to stop solving for Ti and instead wait till we solve for Tj and use that
   * result.
   *
   * <p>Let ATM be any annotated type mirror and Tk be a target type parameter where k != i and k !=
   * j Even though we've inferred Ti = Tj, there still may be constraints of the form Ti = ATM or
   * {@literal Ti <: Tk} These constraints are still useful for inferring a argument for Ti/Tj. So,
   * we replace Ti in these constraints with Tj and place those constraints in the TargetConstraints
   * object for Tj.
   *
   * <p>We then clear the constraints for Ti.
   *
   * @param target the target for which we know another target is exactly equal to this target
   * @param inferredTarget the other target inferred to be equal
   * @param constraints the constraints that are side-effected by this method
   * @param typeFactory type factory
   */
  private void rewriteWithInferredTarget(
      final @FindDistinct TypeVariable target,
      final @FindDistinct TypeVariable inferredTarget,
      final ConstraintMap constraints,
      final AnnotatedTypeFactory typeFactory) {
    final TargetConstraints targetRecord = constraints.getConstraints(target);
    final Map<AnnotatedTypeMirror, AnnotationMirrorSet> equivalentTypes =
        targetRecord.equalities.types;
    final Map<AnnotatedTypeMirror, AnnotationMirrorSet> supertypes = targetRecord.supertypes.types;

    // each type that was equivalent to this one needs to be equivalent in the same hierarchies
    // to the inferred target
    for (final Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> eqEntry :
        equivalentTypes.entrySet()) {
      constraints.addTypeEqualities(inferredTarget, eqEntry.getKey(), eqEntry.getValue());
    }

    for (final Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> superEntry :
        supertypes.entrySet()) {
      constraints.addTypeSupertype(inferredTarget, superEntry.getKey(), superEntry.getValue());
    }

    for (TypeVariable otherTarget : constraints.getTargets()) {
      if (otherTarget != target && otherTarget != inferredTarget) {
        final TargetConstraints record = constraints.getConstraints(otherTarget);

        // each target that was equivalent to this one needs to be equivalent in the same
        // hierarchies as the inferred target
        final AnnotationMirrorSet hierarchies = record.equalities.targets.get(target);
        if (hierarchies != null) {
          record.equalities.targets.remove(target);
          constraints.addTargetEquality(otherTarget, inferredTarget, hierarchies);
        }

        // otherTypes may have AnnotatedTypeVariables of type target, run substitution on
        // these with type
        Map<AnnotatedTypeMirror, AnnotationMirrorSet> toIterate =
            new LinkedHashMap<>(record.equalities.types);
        record.equalities.types.clear();
        for (AnnotatedTypeMirror otherType : toIterate.keySet()) {
          final AnnotatedTypeMirror copy =
              TypeArgInferenceUtil.substitute(
                  target, createAnnotatedTypeVar(target, typeFactory), otherType);
          final AnnotationMirrorSet otherHierarchies = toIterate.get(otherType);
          record.equalities.types.put(copy, otherHierarchies);
        }
      }
    }

    for (TypeVariable otherTarget : constraints.getTargets()) {
      if (otherTarget != target && otherTarget != inferredTarget) {
        final TargetConstraints record = constraints.getConstraints(otherTarget);

        final AnnotationMirrorSet hierarchies = record.supertypes.targets.get(target);
        if (hierarchies != null) {
          record.supertypes.targets.remove(target);
          constraints.addTargetSupertype(otherTarget, inferredTarget, hierarchies);
        }

        // otherTypes may have AnnotatedTypeVariables of type target, run substitution on
        // these with type
        Map<AnnotatedTypeMirror, AnnotationMirrorSet> toIterate =
            new LinkedHashMap<>(record.supertypes.types);
        record.supertypes.types.clear();
        for (AnnotatedTypeMirror otherType : toIterate.keySet()) {
          final AnnotatedTypeMirror copy =
              TypeArgInferenceUtil.substitute(
                  target, createAnnotatedTypeVar(target, typeFactory), otherType);
          final AnnotationMirrorSet otherHierarchies = toIterate.get(otherType);
          record.supertypes.types.put(copy, otherHierarchies);
        }
      }
    }

    targetRecord.equalities.clear();
    targetRecord.supertypes.clear();
  }

  /** Creates a declaration AnnotatedTypeVariable for TypeVariable. */
  private AnnotatedTypeVariable createAnnotatedTypeVar(
      final TypeVariable typeVariable, final AnnotatedTypeFactory typeFactory) {
    return (AnnotatedTypeVariable) typeFactory.getAnnotatedType(typeVariable.asElement());
  }

  /**
   * Returns a concrete type argument or null if there was not enough information to infer one.
   *
   * @param typesToHierarchies a mapping of (types &rarr; hierarchies) that indicate that the
   *     argument being inferred is equal to the types in each of the hierarchies
   * @param primaries a map (hierarchy &rarr; annotation in hierarchy) where the annotation in
   *     hierarchy is equal to the primary annotation on the argument being inferred
   * @param tops the set of top annotations in the qualifier hierarchy
   * @return a concrete type argument or null if there was not enough information to infer one
   */
  private InferredType mergeTypesAndPrimaries(
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> typesToHierarchies,
      AnnotationMirrorMap<AnnotationMirror> primaries,
      final AnnotationMirrorSet tops,
      AnnotatedTypeFactory typeFactory) {
    final AnnotationMirrorSet missingAnnos = new AnnotationMirrorSet(tops);

    Iterator<Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet>> entryIterator =
        typesToHierarchies.entrySet().iterator();
    if (!entryIterator.hasNext()) {
      throw new BugInCF("Merging a list of empty types.");
    }

    final Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> head = entryIterator.next();

    AnnotatedTypeMirror mergedType = head.getKey();
    missingAnnos.removeAll(head.getValue());

    // 1. if there are multiple equality constraints in a ConstraintMap then the types better
    // have the same underlying type or Javac will complain and we won't be here.  When building
    // ConstraintMaps constraints involving AnnotatedTypeMirrors that are exactly equal are
    // combined so there must be some difference between two types being merged here.
    //
    // 2. Otherwise, we might have the same underlying type but conflicting annotations, then we
    // take the first set of annotations and show an error for the argument/return type that
    // caused the second differing constraint.
    //
    // 3. Finally, we expect the following types to be involved in equality constraints:
    // AnnotatedDeclaredTypes, AnnotatedTypeVariables, and AnnotatedArrayTypes
    while (entryIterator.hasNext() && !missingAnnos.isEmpty()) {
      final Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> current = entryIterator.next();
      final AnnotatedTypeMirror currentType = current.getKey();
      final AnnotationMirrorSet currentHierarchies = current.getValue();

      AnnotationMirrorSet found = new AnnotationMirrorSet();
      for (AnnotationMirror top : missingAnnos) {
        if (currentHierarchies.contains(top)) {
          final AnnotationMirror newAnno = currentType.getAnnotationInHierarchy(top);
          if (newAnno != null) {
            mergedType.replaceAnnotation(newAnno);
            found.add(top);

          } else if (mergedType.getKind() == TypeKind.TYPEVAR
              && typeFactory.types.isSameType(
                  currentType.getUnderlyingType(), mergedType.getUnderlyingType())) {
            // the options here are we are merging with the same typevar, in which case
            // we can just remove the annotation from the missing list
            found.add(top);

          } else {
            // otherwise the other type is missing an annotation
            throw new BugInCF(
                "Missing annotation.%nmergedType=%s%ncurrentType=%s", mergedType, currentType);
          }
        }
      }

      missingAnnos.removeAll(found);
    }

    // add all the annotations from the primaries
    for (final AnnotationMirror top : missingAnnos) {
      final AnnotationMirror anno = primaries.get(top);
      if (anno != null) {
        mergedType.replaceAnnotation(anno);
      }
    }

    typesToHierarchies.clear();

    if (missingAnnos.isEmpty()) {
      return new InferredType(mergedType);
    }

    // TODO: we probably can do more with this information than just putting it back into the
    // TODO: ConstraintMap (which is what's happening here)
    final AnnotationMirrorSet hierarchies = new AnnotationMirrorSet(tops);
    hierarchies.removeAll(missingAnnos);
    typesToHierarchies.put(mergedType, hierarchies);

    return null;
  }

  public InferredValue mergeConstraints(
      final TypeVariable target,
      final Equalities equalities,
      final InferenceResult solution,
      ConstraintMap constraintMap,
      AnnotatedTypeFactory typeFactory) {
    final AnnotationMirrorSet tops =
        new AnnotationMirrorSet(typeFactory.getQualifierHierarchy().getTopAnnotations());
    InferredValue inferred = null;
    if (!equalities.types.isEmpty()) {
      inferred = mergeTypesAndPrimaries(equalities.types, equalities.primaries, tops, typeFactory);
    }

    if (inferred != null) {
      return inferred;
    } // else

    // We did not have enough information to infer an annotation in all hierarchies for one
    // concrete type.
    // However, we have a "partial solution", one in which we know the type in some but not all
    // qualifier hierarchies.
    // Update our set of constraints with this information
    dirty |= updateTargetsWithPartiallyInferredType(equalities, constraintMap, typeFactory);
    inferred = findEqualTarget(equalities, tops);

    if (inferred == null && equalities.types.size() == 1) {
      // Still could not find an inferred type in all hierarchies, so just use what type is known.
      AnnotatedTypeMirror type = equalities.types.keySet().iterator().next();
      inferred = new InferredType(type);
    }
    return inferred;
  }

  // If we determined that this target T1 is equal to a type ATM in hierarchies @A,@B,@C
  // for each of those hierarchies, if a target is equal to T1 in that hierarchy it is also equal
  // to ATM.
  // e.g.
  //   if : T1 == @A @B @C ATM in only the A,B hierarchies
  //    and T1 == T2 only in @A hierarchy
  //
  //   then T2 == @A @B @C only in the @A hierarchy
  //
  public boolean updateTargetsWithPartiallyInferredType(
      final Equalities equalities, ConstraintMap constraintMap, AnnotatedTypeFactory typeFactory) {

    boolean updated = false;

    if (!equalities.types.isEmpty()) {
      if (equalities.types.size() != 1) {
        throw new BugInCF("Equalities should have at most 1 constraint.");
      }

      Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> remainingTypeEquality;
      remainingTypeEquality = equalities.types.entrySet().iterator().next();

      final AnnotatedTypeMirror remainingType = remainingTypeEquality.getKey();
      final AnnotationMirrorSet remainingHierarchies = remainingTypeEquality.getValue();

      // update targets
      for (Map.Entry<TypeVariable, AnnotationMirrorSet> targetToHierarchies :
          equalities.targets.entrySet()) {
        final TypeVariable equalTarget = targetToHierarchies.getKey();
        final AnnotationMirrorSet hierarchies = targetToHierarchies.getValue();

        final AnnotationMirrorSet equalTypeHierarchies =
            new AnnotationMirrorSet(remainingHierarchies);
        equalTypeHierarchies.retainAll(hierarchies);

        final Map<AnnotatedTypeMirror, AnnotationMirrorSet> otherTargetsEqualTypes =
            constraintMap.getConstraints(equalTarget).equalities.types;

        AnnotationMirrorSet equalHierarchies = otherTargetsEqualTypes.get(remainingType);
        if (equalHierarchies == null) {
          equalHierarchies = new AnnotationMirrorSet(equalTypeHierarchies);
          otherTargetsEqualTypes.put(remainingType, equalHierarchies);
          updated = true;

        } else {
          final int size = equalHierarchies.size();
          equalHierarchies.addAll(equalTypeHierarchies);
          updated = size == equalHierarchies.size();
        }
      }
    }

    return updated;
  }

  /**
   * Attempt to find a target which is equal to this target.
   *
   * @return a target equal to this target in all hierarchies, or null
   */
  public InferredTarget findEqualTarget(final Equalities equalities, AnnotationMirrorSet tops) {
    for (Map.Entry<TypeVariable, AnnotationMirrorSet> targetToHierarchies :
        equalities.targets.entrySet()) {
      final TypeVariable equalTarget = targetToHierarchies.getKey();
      final AnnotationMirrorSet hierarchies = targetToHierarchies.getValue();

      // Now see if target is equal to equalTarget in all hierarchies
      boolean targetIsEqualInAllHierarchies = hierarchies.size() == tops.size();
      if (targetIsEqualInAllHierarchies) {
        return new InferredTarget(equalTarget, new AnnotationMirrorSet());

      } else {
        // annos in primaries that are not covered by the target's list of equal hierarchies
        final AnnotationMirrorSet requiredPrimaries =
            new AnnotationMirrorSet(equalities.primaries.keySet());
        requiredPrimaries.removeAll(hierarchies);

        boolean typeWithPrimariesIsEqual =
            (requiredPrimaries.size() + hierarchies.size()) == tops.size();
        if (typeWithPrimariesIsEqual) {
          return new InferredTarget(equalTarget, requiredPrimaries);
        }
      }
    }

    return null;
  }
}
