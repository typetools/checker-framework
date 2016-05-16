package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;

/**
 * Infers type arguments by using the Least Upper Bound computation on the supertype relationships
 * in a constraint map.
 */
public class SupertypesSolver {

    /**
     * Infers type arguments using supertype constraints.
     * @param remainingTargets targets for which we still need to infer a value
     * @param constraintMap the set of constraints for all targets
     * @return a mapping of ( {@code target -> inferred type} ), note this class always infers concrete types
     *         and will not infer that the target is equivalent to another target
     */
    public InferenceResult solveFromSupertypes(final Set<TypeVariable> remainingTargets,
                                               final ConstraintMap constraintMap,
                                               final AnnotatedTypeFactory typeFactory) {
        // infer a type for all targets that have supertype constraints
        final Lubs lubs = targetToTypeLubs(remainingTargets, constraintMap, typeFactory);

        // add the lub types to the outgoing solution
        final InferenceResult solution = new InferenceResult();
        for (final TypeVariable target : remainingTargets) {
            final AnnotatedTypeMirror lub = lubs.getType(target);
            Map<AnnotationMirror, AnnotationMirror> lubAnnos = lubs.getPrimaries(target);

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
     * We previously found a type that is equal to target but not in all hierarchies.  Use the primary
     * annotations from the lub type to fill in the missing annotations in this type.  Use that type
     * as the inferred argument.
     *
     * If we failed to infer any annotation for a given hierarchy, either previously from equalities or from the lub,
     * return null.
     */
    protected InferredType mergeLubTypeWithEqualities(final TypeVariable target, final AnnotatedTypeMirror lub,
                                                      final ConstraintMap constraintMap, final AnnotatedTypeFactory typeFactory) {
        final Equalities equalities = constraintMap.getConstraints(target).equalities;
        final Set<? extends AnnotationMirror> tops = typeFactory.getQualifierHierarchy().getTopAnnotations();

        if (!equalities.types.isEmpty()) {
            // there should be only one equality type if any at this point
            final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> eqEntry = equalities.types.entrySet().iterator().next();
            final AnnotatedTypeMirror equalityType = eqEntry.getKey();
            final Set<AnnotationMirror> equalityAnnos = eqEntry.getValue();

            boolean failed = false;
            for (final AnnotationMirror top : tops) {
                if (!equalityAnnos.contains(top)) {
                    final AnnotationMirror lubAnno = lub.getAnnotationInHierarchy(top);
                    if (lubAnno == null) {
                        // If the LUB and the Equality were the SAME typevar, and the lub was unannotated
                        // then "NO ANNOTATION" is the correct choice
                        if (lub.getKind() == TypeKind.TYPEVAR
                         && equalityType.getUnderlyingType().equals(lub.getUnderlyingType())) {
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
     * We previously found a type that is equal to target but not in all hierarchies.  Use the primary
     * annotations from the lub annos to fill in the missing annotations in this type.  Use that type
     * as the inferred argument.
     *
     * If we failed to infer any annotation for a given hierarchy, either previously from equalities or from the lub,
     * return null.
     */
    protected InferredType mergeLubAnnosWithEqualities(final TypeVariable target, final Map<AnnotationMirror, AnnotationMirror> lubAnnos,
                                                    final ConstraintMap constraintMap, final AnnotatedTypeFactory typeFactory) {
        final Equalities equalities = constraintMap.getConstraints(target).equalities;
        final Set<? extends AnnotationMirror> tops = typeFactory.getQualifierHierarchy().getTopAnnotations();

        if (!equalities.types.isEmpty()) {
            // there should be only equality type if any at this point
            final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> eqEntry = equalities.types.entrySet().iterator().next();
            final AnnotatedTypeMirror equalityType = eqEntry.getKey();
            final Set<AnnotationMirror> equalityAnnos = eqEntry.getValue();

            boolean failed = false;
            for (final AnnotationMirror top : tops) {
                if (!equalityAnnos.contains(top)) {
                    final AnnotationMirror lubAnno = lubAnnos.get(top);
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

    /**
     * Holds the least upper bounds for every target type parameter.
     */
    class Lubs {
        public final Map<TypeVariable, AnnotatedTypeMirror> types = new LinkedHashMap<>();
        public final Map<TypeVariable, Map<AnnotationMirror, AnnotationMirror>> primaries = new LinkedHashMap<>();

        public void addPrimaries(final TypeVariable target, Map<AnnotationMirror, AnnotationMirror> primaries) {
            this.primaries.put(target, new LinkedHashMap<>(primaries));
        }

        public void addType(final TypeVariable target, final AnnotatedTypeMirror type) {
            types.put(target, type);
        }

        public Map<AnnotationMirror, AnnotationMirror> getPrimaries(final TypeVariable target) {
            return primaries.get(target);
        }

        public AnnotatedTypeMirror getType(final TypeVariable target) {
            return types.get(target);
        }
    }

    /**
     * For each target, lub all of the types/annotations in its supertypes constraints and return the lubs
     * @param remainingTargets targets that do not already have an inferred type argument
     * @param constraintMap the set of constraints for all targets
     * @return the lub determined for each target that has at least 1 supertype constraint
     */
    private Lubs targetToTypeLubs(Set<TypeVariable> remainingTargets,
                                  ConstraintMap constraintMap,
                                  AnnotatedTypeFactory typeFactory) {
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        final Set<? extends AnnotationMirror> tops = qualifierHierarchy.getTopAnnotations();

        Lubs solution = new Lubs();

        Map<AnnotationMirror, AnnotationMirror> lubOfPrimaries = new HashMap<>(tops.size());

        List<TypeVariable> targetsSupertypesLast = new ArrayList<>(remainingTargets);

        final Types types = typeFactory.getProcessingEnv().getTypeUtils();
        // If we have two type variables <A, A extends B> order them B then A
        // this is required because we will use the fact that A must be above B
        // when determining the LUB of A
        Collections.sort(targetsSupertypesLast, new Comparator<TypeVariable>() {
            @Override
            public int compare(TypeVariable o1, TypeVariable o2) {
                if (types.isSubtype(o1, o2)) {
                    return -1;
                } else if (types.isSubtype(o2, o1)) {
                    return 1;
                }

                return 0;
            }
        });

        for (final TypeVariable target : targetsSupertypesLast) {
            TargetConstraints targetRecord = constraintMap.getConstraints(target);
            final Map<AnnotationMirror, Set<AnnotationMirror>> subtypeAnnos = targetRecord.supertypes.primaries;
            final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> subtypesOfTarget = targetRecord.supertypes.types;

            // if this target is a supertype of other targets and those targets have already been lubbed
            // add that LUB to the list of lubs for this target (as it must be above this target)
            propagatePreviousLubs(targetRecord, solution, subtypesOfTarget);

            // lub all the primary annotations and put them in lubOfPrimaries
            lubPrimaries(lubOfPrimaries, subtypeAnnos, tops, qualifierHierarchy);
            solution.addPrimaries(target, lubOfPrimaries);

            if (subtypesOfTarget.keySet().size() > 0) {
                final AnnotatedTypeMirror lub = leastUpperBound(target, typeFactory, subtypesOfTarget);
                final Set<AnnotationMirror> effectiveLubAnnos = lub.getEffectiveAnnotations();

                for (AnnotationMirror lubAnno : effectiveLubAnnos) {
                    final AnnotationMirror hierarchy = qualifierHierarchy.getTopAnnotation(lubAnno);
                    final AnnotationMirror primaryLub = lubOfPrimaries.get(hierarchy);

                    if (primaryLub != null) {
                        if (qualifierHierarchy.isSubtype(lubAnno, primaryLub) && !AnnotationUtils.areSame(lubAnno, primaryLub)) {
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
     * If the target corresponding to targetRecord must be a supertype of another target for which
     * we have already determined a lub, add that target's lub to this list.
     */
    protected static void propagatePreviousLubs(final TargetConstraints targetRecord, Lubs solution,
                                                final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> subtypesOfTarget ) {

        for (final Entry<TypeVariable, Set<AnnotationMirror>> supertypeTarget : targetRecord.supertypes.targets.entrySet()) {
            final AnnotatedTypeMirror supertargetLub = solution.getType(supertypeTarget.getKey());
            if (supertargetLub != null) {
                Set<AnnotationMirror> supertargetTypeAnnos = subtypesOfTarget.get(supertargetLub);
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
     * For each qualifier hierarchy in tops,
     *     take the lub of the annos in subtypeAnnos that correspond to that hierarchy
     *     place the lub in lubOfPrimaries
     */
    protected static void lubPrimaries(Map<AnnotationMirror, AnnotationMirror> lubOfPrimaries,
                                       Map<AnnotationMirror, Set<AnnotationMirror>> subtypeAnnos,
                                       Set<? extends AnnotationMirror> tops,
                                      QualifierHierarchy qualifierHierarchy) {

        lubOfPrimaries.clear();
        for (final AnnotationMirror top : tops) {
            final Set<AnnotationMirror> annosInHierarchy = subtypeAnnos.get(top);
            if (annosInHierarchy != null && !annosInHierarchy.isEmpty()) {
                lubOfPrimaries.put(top, leastUpperBound(annosInHierarchy, qualifierHierarchy));
            }
        }
    }

    /**
     * For each type in typeToHierarchies,
     *    if that type does not have a corresponding annotation for a given hierarchy
     *    replace it with the corresponding value in lowerBoundAnnos
     */
    public static AnnotatedTypeMirror groundMissingHierarchies(
                                    final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> typeToHierarchies,
                                    final Map<AnnotationMirror, AnnotationMirror> lowerBoundAnnos) {
        final Set<AnnotationMirror> presentHierarchies = typeToHierarchies.getValue();
        final Set<AnnotationMirror> missingAnnos = new LinkedHashSet<>();
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
     * Successively calls least upper bound on the elements of types.  Unlike AnnotatedTypes.leastUpperBound,
     * this method will box primitives if necessary
     */
    public static AnnotatedTypeMirror leastUpperBound(final TypeVariable target, final AnnotatedTypeFactory typeFactory,
                                                      final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> types) {


        QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        AnnotatedTypeVariable targetsDeclaredType = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(target.asElement());
        final Map<AnnotationMirror, AnnotationMirror> lowerBoundAnnos =
                TypeArgInferenceUtil.createHierarchyMap(targetsDeclaredType.getLowerBound().getEffectiveAnnotations(), qualifierHierarchy);

        final Iterator<Entry<AnnotatedTypeMirror, Set<AnnotationMirror>>> typesIter = types.entrySet().iterator();
        if (!typesIter.hasNext()) {
            ErrorReporter.errorAbort("Calling LUB on empty list!");
        }

        /**
         * If a constraint implies that a type parameter Ti is a supertype of an annotated type mirror Ai
         * but only in a subset of all qualifier hierarchies then for all other qualifier hierarchies replace
         * the primary annotation on Ai with the lowest possible annotation (ensuring that it won't be
         * the LUB unless there are no other constraints, or all other constraints imply the bottom
         * annotation is the LUB).
         * Note: Even if we choose bottom as the lub here, the assignment context may raise this annotation.
         */
        final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> head = typesIter.next();

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
            lubType = AnnotatedTypes.leastUpperBound(typeFactory.getProcessingEnv(), typeFactory, lubType, nextType);
        }

        return lubType;
    }

    /**
     * @param annos a set of annotations in the same annotation hierarchy
     * @param qualifierHierarchy the qualifier hierarchy that contains each annotation
     * @return the lub of all the annotations in annos
     */
    private final static AnnotationMirror leastUpperBound(final Iterable<? extends AnnotationMirror> annos,
                                                           QualifierHierarchy qualifierHierarchy) {
        Iterator<? extends AnnotationMirror> annoIter = annos.iterator();
        AnnotationMirror lub = annoIter.next();

        while (annoIter.hasNext()) {
            lub = qualifierHierarchy.leastUpperBound(lub, annoIter.next());
        }

        return lub;
    }
}
