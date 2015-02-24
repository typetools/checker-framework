package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredTarget;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import java.util.*;
import java.util.Map.Entry;

/**
 *  EqualitiesSolver infers type arguments for targets using the equality constraints in ConstraintMap.  When
 *  a type is inferred, it rewrites the remaining equality/supertype constraints
 *
 *  //TODO: SEE IF THERE IS A SIMPLE WAY TO IMPROVE THIS
 */
public class EqualitiesSolver {
    private boolean dirty = false;

    public InferenceResult solveEqualities(Set<TypeVariable> targets, ConstraintMap constraintMap, AnnotatedTypeFactory typeFactory) {
        final InferenceResult solution = new InferenceResult();

        do {
            dirty = false;
            for (TypeVariable target : targets) {
                if (solution.containsKey(target)) {
                    continue;
                }

                Equalities equalities = constraintMap.getConstraints(target).equalities;

                InferredValue inferred = mergeConstraints(target, equalities, solution, constraintMap, typeFactory);
                if (inferred != null) {
                    if (inferred instanceof InferredType) {
                        rewriteWithInferredType(target, ((InferredType) inferred).type, constraintMap);
                    } else {
                        rewriteWithInferredTarget(target, ((InferredTarget) inferred).target, constraintMap, typeFactory);
                    }

                    solution.put(target, inferred);
                }
            }

        } while(dirty);

        solution.resolveChainedTargets();

        return solution;
    }

    private void rewriteWithInferredType(final TypeVariable target, final AnnotatedTypeMirror type, final ConstraintMap ptMap) {

        final TargetConstraints targetRecord = ptMap.getConstraints(target);
        final Map<TypeVariable, Set<AnnotationMirror>> equivalentTargets = targetRecord.equalities.targets;
        //each target that was equivalent to this one needs to be equivalent in the same hierarchies as the inferred type
        for(final Entry<TypeVariable, Set<AnnotationMirror>> eqEntry : equivalentTargets.entrySet()) {
            ptMap.addTypeEqualities(eqEntry.getKey(), type, eqEntry.getValue());
        }

        for(TypeVariable otherTarget : ptMap.getTargets()) {
            if (otherTarget != target) {
                final TargetConstraints record = ptMap.getConstraints(otherTarget);

                //each target that was equivalent to this one needs to be equivalent in the same hierarchies as the inferred type
                final Set<AnnotationMirror> hierarchies = record.equalities.targets.get(target);
                if (hierarchies != null) {
                    record.equalities.targets.remove(target);
                    ptMap.addTypeEqualities(otherTarget, type, hierarchies);
                }

                //otherTypes may have AnnotatedTypeVariables of type target, run substitution on these with type
                for (AnnotatedTypeMirror otherType : record.equalities.types.keySet()) {
                    final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, type, otherType);
                    final Set<AnnotationMirror> otherHierarchies = record.equalities.types.get(otherType);
                    record.equalities.types.remove(otherType);
                    record.equalities.types.put(copy, otherHierarchies);
                }
            }
        }

        for(TypeVariable otherTarget : ptMap.getTargets()) {
            if (otherTarget != target) {
                final TargetConstraints record = ptMap.getConstraints(otherTarget);

                //each target that was equivalent to this one needs to be equivalent in the same hierarchies as the inferred type
                final Set<AnnotationMirror> hierarchies = record.supertypes.targets.get(target);
                if (hierarchies != null) {
                    record.supertypes.targets.remove(target);
                    ptMap.addTypeEqualities(otherTarget, type, hierarchies);
                }

                //otherTypes may have AnnotatedTypeVariables of type target, run substitution on these with type
                for (AnnotatedTypeMirror otherType : record.supertypes.types.keySet()) {
                    final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, type, otherType);
                    final Set<AnnotationMirror> otherHierarchies = record.supertypes.types.get(otherType);
                    record.supertypes.types.remove(otherType);
                    record.supertypes.types.put(copy, otherHierarchies);
                }
            }
        }

        targetRecord.equalities.clear();
    }

    private void rewriteWithInferredTarget(final TypeVariable target, final TypeVariable inferredTarget, final ConstraintMap ptMap,
                                          final AnnotatedTypeFactory typeFactory) {
        final TargetConstraints targetRecord = ptMap.getConstraints(target);
        final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> equivalentTypes = targetRecord.equalities.types;
        final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> supertypes = targetRecord.supertypes.types;

        //each type that was equivalent to this one needs to be equivalent in the same hierarchies to the inferred target
        for(final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> eqEntry : equivalentTypes.entrySet()) {
            ptMap.addTypeEqualities(inferredTarget, eqEntry.getKey(), eqEntry.getValue());
        }

        for(final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> superEntry : supertypes.entrySet()) {
            ptMap.addTypeSupertype(inferredTarget, superEntry.getKey(), superEntry.getValue());
        }

        for(TypeVariable otherTarget : ptMap.getTargets()) {
            if (otherTarget != target && otherTarget != inferredTarget) {
                final TargetConstraints record = ptMap.getConstraints(otherTarget);

                //each target that was equivalent to this one needs to be equivalent in the same hierarchies as the inferred target
                final Set<AnnotationMirror> hierarchies = record.equalities.targets.get(target);
                if (hierarchies != null) {
                    record.equalities.targets.remove(target);
                    ptMap.addTargetEquality(otherTarget, inferredTarget, hierarchies);
                }

                //otherTypes may have AnnotatedTypeVariables of type target, run substitution on these with type
                for (AnnotatedTypeMirror otherType : record.equalities.types.keySet()) {
                    final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, createAnnotatedTypeVar(target, typeFactory), otherType);
                    final Set<AnnotationMirror> otherHierarchies = record.equalities.types.get(otherType);
                    record.equalities.types.remove(otherType);
                    record.equalities.types.put(copy, otherHierarchies);
                }
            }
        }

        for(TypeVariable otherTarget : ptMap.getTargets()) {
            if (otherTarget != target && otherTarget != inferredTarget) {
                final TargetConstraints record = ptMap.getConstraints(otherTarget);

                final Set<AnnotationMirror> hierarchies = record.supertypes.targets.get(target);
                if (hierarchies != null) {
                    record.supertypes.targets.remove(target);
                    ptMap.addTargetSupertype(otherTarget, inferredTarget, hierarchies);
                }

                //otherTypes may have AnnotatedTypeVariables of type target, run substitution on these with type
                for (AnnotatedTypeMirror otherType : record.supertypes.types.keySet()) {
                    final AnnotatedTypeMirror copy = TypeArgInferenceUtil.substitute(target, createAnnotatedTypeVar(target, typeFactory), otherType);
                    final Set<AnnotationMirror> otherHierarchies = record.supertypes.types.get(otherType);
                    record.supertypes.types.remove(otherType);
                    record.supertypes.types.put(copy, otherHierarchies);
                }
            }
        }

        targetRecord.equalities.clear();
        //don't need to include supertypes
    }


    private AnnotatedTypeVariable createAnnotatedTypeVar(final TypeVariable typeVariable, final AnnotatedTypeFactory typeFactory) {
        return (AnnotatedTypeVariable) typeFactory.getAnnotatedType(typeVariable.asElement());
    }


    //when this is complete we should have 1 typ
    private InferredType mergeTypesAndPrimaries(
            Map<AnnotatedTypeMirror, Set<AnnotationMirror>> typesToHierarchies,
            Map<AnnotationMirror, AnnotationMirror> primaries,
            final Set<? extends AnnotationMirror> tops) {
        final Set<AnnotationMirror> missingAnnos = new HashSet<>(tops);

        Iterator<Entry<AnnotatedTypeMirror, Set<AnnotationMirror>>> entryIterator = typesToHierarchies.entrySet().iterator();
        if (!entryIterator.hasNext()) {
            ErrorReporter.errorAbort("Merging a list of empty types!");
        }

        final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> head = entryIterator.next();

        AnnotatedTypeMirror mergedType = head.getKey();
        missingAnnos.removeAll(head.getValue());

        //1. if there are multiple equality constraints in a ConstraintMap then the types better have
        //the same underlying type or Javac will complain and we won't be here.  When building ConstraintMaps
        //constraints involving AnnotatedTypeMirrors that are exactly equal are combined so there must be some
        //difference between two types being merged here.
        //2. Otherwise, we might have the same underlying type but conflicting annotations, then we take
        //the first set of annotations and show an error for the argument/return type that caused the second
        //differing constraint
        //3. Finally, we expect the following types to be involved in equality constraints:
        //AnnotatedDeclaredTypes, AnnotatedTypeVariables, and AnnotatedArrayTypes
        while(entryIterator.hasNext() && !missingAnnos.isEmpty()) {
            final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> current = entryIterator.next();
            final AnnotatedTypeMirror currentType = current.getKey();
            final Set<AnnotationMirror> currentHierarchies = current.getValue();

            Set<AnnotationMirror> found = new HashSet<>();
            for (AnnotationMirror top : missingAnnos) {
                if (currentHierarchies.contains(top)) {
                    final AnnotationMirror newAnno = currentType.getAnnotationInHierarchy(top);
                    if (newAnno != null) {
                        mergedType.replaceAnnotation(newAnno);
                        found.add(top);

                    } else if (mergedType.getKind() == TypeKind.TYPEVAR
                            && currentType.getUnderlyingType().equals(mergedType.getUnderlyingType())) {
                        //the options here are we are merging with the same typevar, in which case
                        //we can just remove the annotation from the missing list
                        found.add(top);

                    } else {
                        //otherwise the other type is missing an annotation
                        ErrorReporter.errorAbort("REMOVE THIS, THIS IS FOR TESTING PURPOSE" + "\nmergedType=" + mergedType + "\ncurrentType=" + currentType);

                    }
                }
            }

            missingAnnos.removeAll(found);
        }

        //add all the annotations from the primaries
        final HashSet<AnnotationMirror> foundHierarchies = new HashSet<>();
        for (final AnnotationMirror top : missingAnnos) {
            final AnnotationMirror anno = primaries.get(top);
            if (anno != null) {
                foundHierarchies.add(top);
                mergedType.replaceAnnotation(anno);
            }
        }

        typesToHierarchies.clear();


        if (missingAnnos.isEmpty()) {
            return new InferredType(mergedType);
        }

        //TODO: DO SOMETHING SMARTER
        final Set<AnnotationMirror> hierarchies = new HashSet<>(tops);
        hierarchies.removeAll(missingAnnos);
        typesToHierarchies.put(mergedType, hierarchies);

        return null;
    }

    //If we determined that this target T1 is equal to a type ATM in hierarchies @A,@B,@C
    //for each of those hierarchy, if a target is equal to T1 in that hierarchy it is also equal to ATM
    // e.g.
    //   if : T1 == @A @B @C ATM in only the A,B hierarchies
    //    and T1 == T2 in @A hierarchy
    //   then T2 == @A @B @C only in the @A hierarchy
    // add this final constraint
    public boolean updateTargetsWithPartiallyInferredType( final Equalities equalities, ConstraintMap constraintMap,
                                                           AnnotatedTypeFactory typeFactory) {

        boolean updated = false;

        if (!equalities.types.isEmpty()) {
            if (equalities.types.size() != 1) {
                //TODO: ADD BETTER MESSAGE
                ErrorReporter.errorAbort("Equalities should be empty!");
            }

            Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> remainingTypeEquality;
            remainingTypeEquality = equalities.types.entrySet().iterator().next();
            final AnnotatedTypeMirror remainingType = remainingTypeEquality.getKey();
            final Set<AnnotationMirror> remainingHierarchies = remainingTypeEquality.getValue();

            //update targets
            for (Map.Entry<TypeVariable, Set<AnnotationMirror>> targetToHierarchies  : equalities.targets.entrySet()) {
                final TypeVariable equalTarget = targetToHierarchies.getKey();
                final Set<AnnotationMirror> hierarchies = targetToHierarchies.getValue();

                final Set<AnnotationMirror> equalTypeHierarchies = new HashSet<>(remainingHierarchies);
                equalTypeHierarchies.retainAll(hierarchies);

                final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> otherTargetsEqualTypes =
                        constraintMap.getConstraints(equalTarget).equalities.types;

                Set<AnnotationMirror> equalHierarchies = otherTargetsEqualTypes.get(remainingType);
                if (equalHierarchies == null) {
                    equalHierarchies = new HashSet<>();
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

    public InferredValue mergeConstraints(final TypeVariable target, final Equalities equalities,
                                          final InferenceResult solution, ConstraintMap constraintMap,
                                          AnnotatedTypeFactory typeFactory) {
        final Set<? extends AnnotationMirror> tops = typeFactory.getQualifierHierarchy().getTopAnnotations();
        InferredValue inferred = null;
        if (!equalities.types.isEmpty()) {
            inferred = mergeTypesAndPrimaries(equalities.types, equalities.primaries, tops);
        }

        if (inferred != null) {
            return inferred;
        } //else

        //TODO: gonna probably need to make sure that we don't update the equalities of this target
        //TODO: gonna probably need to make sure that we don't update the equalities of this target
        //or any target that has been solved
        dirty |= updateTargetsWithPartiallyInferredType(equalities, constraintMap, typeFactory);
        inferred = findEqualTarget(equalities, tops);

        return inferred;
    }

    public InferredTarget findEqualTarget(final Equalities equalities,  Set<? extends AnnotationMirror> tops) {
        for (Map.Entry<TypeVariable, Set<AnnotationMirror>> targetToHierarchies  : equalities.targets.entrySet()) {
            final TypeVariable equalTarget = targetToHierarchies.getKey();
            final Set<AnnotationMirror> hierarchies = targetToHierarchies.getValue();
            //No see if target is equal to equalTarget in all hierarchies
            boolean targetIsEqualInAllHierarchies = equalities.targets.get(equalTarget).size() == tops.size();
            if (targetIsEqualInAllHierarchies) {
                return new InferredTarget(equalTarget, new HashSet<AnnotationMirror>());

            } else {
                //annos in primaries that are not covered by the target's list of equal hierarchies
                final Set<AnnotationMirror> requiredPrimaries = new HashSet<AnnotationMirror>(equalities.primaries.keySet());
                requiredPrimaries.removeAll(hierarchies);

                boolean typeWithPrimariesIsEqual = (requiredPrimaries.size() + hierarchies.size()) == tops.size();
                if (typeWithPrimariesIsEqual) {
                    return new InferredTarget(equalTarget, requiredPrimaries);
                }
            }
        }

        return null;
    }
}
