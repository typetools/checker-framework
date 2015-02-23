package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference.solver.InferredValue.InferredType;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ErrorReporter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by jburke on 2/18/15.
 */
public class SupertypesSolver {


    public SupertypesSolver() {


    }

    public InferenceResult solveFromArguments(final Set<TypeVariable> remainingTargets,
                                                               final ConstraintMap constraintMap,
                                                               final AnnotatedTypeFactory typeFactory) {
        final Lubs lubs = targetToTypeLubs(remainingTargets, constraintMap, typeFactory);

        final InferenceResult solution = new InferenceResult();
        for (final TypeVariable target : remainingTargets) {
            final AnnotatedTypeMirror lub = lubs.getType(target);
            Map<AnnotationMirror, AnnotationMirror> lubAnnos = lubs.getPrimaries(target);

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

    public InferredType mergeLubTypeWithEqualities(final TypeVariable target, final AnnotatedTypeMirror lub,
                                                   final ConstraintMap constraintMap, final AnnotatedTypeFactory typeFactory) {
        final Equalities equalities = constraintMap.getConstraints(target).equalities;
        final Set<? extends AnnotationMirror> tops = typeFactory.getQualifierHierarchy().getTopAnnotations();

        if (!equalities.types.isEmpty()) {
            //there should be only equality type if any at this point
            final Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> eqEntry = equalities.types.entrySet().iterator().next();
            final AnnotatedTypeMirror equalityType = eqEntry.getKey();
            final Set<AnnotationMirror> equalityAnnos = eqEntry.getValue();

            boolean failed = false;
            for (final AnnotationMirror top : tops) {
                if (!equalityAnnos.contains(top)) {
                    final AnnotationMirror lubAnno = lub.getAnnotationInHierarchy(top);
                    if (lubAnno == null) {
                        //If the LUB and the Equality were the SAME typevar, and the lub was unannotated
                        //then "NO ANNOTATION" is the correct choice
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

    public InferredType mergeLubAnnosWithEqualities(final TypeVariable target, final Map<AnnotationMirror, AnnotationMirror> lubAnnos,
                                                    final ConstraintMap constraintMap, final AnnotatedTypeFactory typeFactory) {
        final Equalities equalities = constraintMap.getConstraints(target).equalities;
        final Set<? extends AnnotationMirror> tops = typeFactory.getQualifierHierarchy().getTopAnnotations();

        if (!equalities.types.isEmpty()) {
            //there should be only equality type if any at this point
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

    private Lubs targetToTypeLubs(Set<TypeVariable> remainingTargets,
                                  ConstraintMap constraintMap,
                                  AnnotatedTypeFactory typeFactory) {
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        final Set<? extends AnnotationMirror> tops = qualifierHierarchy.getTopAnnotations();

        Lubs solution = new Lubs();

        Map<AnnotationMirror, AnnotationMirror> lubOfPrimaries = new HashMap<>(tops.size());

        List<TypeVariable> targetsSupertypesLast = new ArrayList<>(remainingTargets);

        final Types types = typeFactory.getProcessingEnv().getTypeUtils();
        //If we have two type variables <A, A extends B> order them B then A
        //this is required because we will use the fact that A must be above B
        //when determining the LUB of A
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

        for(final TypeVariable target : targetsSupertypesLast) {
            TargetConstraints targetRecord = constraintMap.getConstraints(target);
            final Map<AnnotationMirror, Set<AnnotationMirror>> subtypeAnnos = targetRecord.supertypes.primaries;
            final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> subtypesOfTarget = targetRecord.supertypes.types;

            //if this target is a supertype of other targets and those targets have already been lubbed
            //add that LUB to the list of lubs for this target (as it must be above this target)
            for (final Entry<TypeVariable, Set<AnnotationMirror>> supertypeTarget : targetRecord.supertypes.targets.entrySet()) {
                final AnnotatedTypeMirror supertargetLub = solution.getType(supertypeTarget.getKey());
                if (supertargetLub != null) {
                    Set<AnnotationMirror> supertargetTypeAnnos = subtypesOfTarget.get(supertargetLub);
                    if (supertargetTypeAnnos != null) {
                        //there is already an equivalent type in the list of subtypes, just add
                        //any hierarchies that are not in its list but are in the supertarget's list
                        supertargetTypeAnnos.addAll(supertypeTarget.getValue());
                    } else {
                        subtypesOfTarget.put(supertargetLub, supertypeTarget.getValue());
                    }
                }
            }

            lubOfPrimaries.clear();
            for(final AnnotationMirror top : tops) {
                final Set<AnnotationMirror> annosInHierarchy = subtypeAnnos.get(top);
                if (annosInHierarchy != null && !annosInHierarchy.isEmpty()) {
                    lubOfPrimaries.put(top, leastUpperBound(annosInHierarchy, qualifierHierarchy));
                }
            }

            solution.addPrimaries(target, lubOfPrimaries);

            if (subtypesOfTarget.keySet().size() > 0) {
                //TODO: THIS LUB NEEDS TO NOT LUB THE ONE'S IN WHICH THEY ARE NOT THE SAME, i.e. IF NONE OF THE TYPES
                //TODO: ARE SUBTYPES IN THE NULLNESS TYPE SYSTEM THEN THE LUB SHOULD NOT BE EITHER
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

    public static Map<AnnotationMirror, AnnotationMirror> createHierarchyMap(final Set<AnnotationMirror> annos,
                                                                             final QualifierHierarchy qualifierHierarchy) {
        Map<AnnotationMirror, AnnotationMirror> result = AnnotationUtils.createAnnotationMap();


        for (AnnotationMirror anno : annos) {
            result.put(qualifierHierarchy.getTopAnnotation(anno), anno);
        }

        return result;
    }

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
     * Successively calls least upper bound on the elements of types.  Unlike leastUpperBound,
     * this method will box primitives if necessary
     */
    public static AnnotatedTypeMirror leastUpperBound(final TypeVariable target, final AnnotatedTypeFactory typeFactory,
                                                      final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> types) {


        QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        AnnotatedTypeVariable targetsDeclaredType = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(target.asElement());
        final Map<AnnotationMirror, AnnotationMirror> lowerBoundAnnos =
                createHierarchyMap(targetsDeclaredType.getLowerBound().getEffectiveAnnotations(), qualifierHierarchy);

        final Iterator<Entry<AnnotatedTypeMirror, Set<AnnotationMirror>>> typesIter = types.entrySet().iterator();
        if (!typesIter.hasNext()) {
            ErrorReporter.errorAbort("Calling LUB on empty list!");
        }

        //TODO: EXPLAIN THIS BETTER
        //TODO: If a type is NOT equal to the type argument in any hierarchy than we use the BOTTOM
        //TODO: annotation of the type parameter because either there will be another type in the LUB
        //TODO: that will raise it, OR none of the arguments to the method will actually determine the
        //TODO: type of T.  In which case, we can infer the lowest possible and let the assignment context
        //TODO: raise it if needed
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

    private final AnnotationMirror leastUpperBound(final Iterable<? extends AnnotationMirror> annos,
                                                   QualifierHierarchy qualifierHierarchy) {
        Iterator<? extends AnnotationMirror> annoIter = annos.iterator();
        AnnotationMirror lub = annoIter.next();

        while(annoIter.hasNext()) {
            lub = qualifierHierarchy.leastUpperBound(lub, annoIter.next());
        }

        return lub;
    }
}
