package org.checkerframework.framework.util.typeinference.solver;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Equalities;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Subtypes;
import org.checkerframework.framework.util.typeinference.solver.TargetConstraints.Supertypes;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import java.util.*;

/**
 * ConstraintMap holds simplified versions of the TUConstraints for ALL type variable for which
 * we are inferring an argument.  The ConstraintMap is edited on the fly as the various solvers
 * work (unlike the AF/TU Constraints which are immutable).
 * This really consists of 2 things:
 *    a) a Map({@code target -> constraints for target})
 *    b) Methods to easily build up the constraints in the map
 *    c) A getter for the constraints of individual targets.
 *       Note: This class, along with TargetConstraints, uses a lot of mutable state and few setters/getters
 *       be careful.  This choice was made as it makes the resulting code more readable.
 */
public class ConstraintMap {

    private Map<TypeVariable, TargetConstraints> targetToRecords = new LinkedHashMap<>();

    public ConstraintMap(Set<TypeVariable> targets) {
        for(final TypeVariable target : targets) {
            targetToRecords.put(target, new TargetConstraints(target));
        }
    }

    public ConstraintMap(final ConstraintMap toCopy) {
        this.targetToRecords.putAll(toCopy.targetToRecords);
    }

    /**
     * Gets the equality, subtypes, and supertypes constraints for a particular target
     */
    public TargetConstraints getConstraints(final TypeVariable target) {
        return targetToRecords.get(target);
    }

    /**
     * @return The set of all targets passed to the constructor of this constraint map (a target will
     * appear in this list whether or not it has any cosntraints added)
     */
    public Set<TypeVariable> getTargets() {
        return targetToRecords.keySet();
    }

    public void addTargetEquality(final TypeVariable target, final TypeVariable equivalent, Set<AnnotationMirror> hierarchies) {
        Equalities equalities = targetToRecords.get(target).equalities;
        Set<AnnotationMirror> equivalentTops = equalities.targets.get(equivalent);
        if (equivalentTops == null) {
            equalities.targets.put(equivalent, new HashSet<>(hierarchies));
        } else {
            equivalentTops.addAll(hierarchies);
        }
    }

    public void addPrimaryEqualities(final TypeVariable target, QualifierHierarchy qualHierarchy, final Set<AnnotationMirror> annos) {
        Equalities equalities = targetToRecords.get(target).equalities;

        for (final AnnotationMirror anno : annos) {
            final AnnotationMirror top = qualHierarchy.getTopAnnotation(anno);
            if (!equalities.primaries.containsKey(top)) {
                equalities.primaries.put(top, anno);
            }
        }
    }

    public void addTargetSupertype(final TypeVariable target, final TypeVariable subtype, Set<AnnotationMirror> hierarchies) {
        Supertypes supertypes = targetToRecords.get(target).supertypes;
        Set<AnnotationMirror> supertypeTops = supertypes.targets.get(subtype);
        if (supertypeTops == null) {
            supertypes.targets.put(subtype, new HashSet<>(hierarchies));
        } else {
            supertypeTops.addAll(hierarchies);
        }
    }

    public void addTypeSupertype(final TypeVariable target, final AnnotatedTypeMirror subtype, Set<AnnotationMirror> hierarchies) {
        Supertypes supertypes = targetToRecords.get(target).supertypes;
        Set<AnnotationMirror> supertypeTops = supertypes.types.get(subtype);
        if (supertypeTops == null) {
            supertypes.types.put(subtype, new HashSet<>(hierarchies));
        } else {
            supertypeTops.addAll(hierarchies);
        }
    }

    public void addPrimarySupertype(final TypeVariable target, QualifierHierarchy qualifierHierarchy, final Set<AnnotationMirror> annos) {
        Supertypes supertypes = targetToRecords.get(target).supertypes;
        for( AnnotationMirror anno : annos) {
            final AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
            Set<AnnotationMirror> entries = supertypes.primaries.get(top);
            if (entries == null) {
                entries = new LinkedHashSet<>();
                supertypes.primaries.put(top, entries);
            }
            entries.add(anno);
        }
    }

    public void addTargetSubtype(final TypeVariable target, final TypeVariable supertype, Set<AnnotationMirror> hierarchies) {
        Subtypes subtypes = targetToRecords.get(target).subtypes;
        Set<AnnotationMirror> subtypesTops = subtypes.targets.get(supertype);
        if (subtypesTops == null) {
            subtypes.targets.put(supertype, new HashSet<>(hierarchies));
        } else {
            subtypesTops.addAll(hierarchies);
        }
    }

    public void addTypeSubtype(final TypeVariable target, final AnnotatedTypeMirror supertype, Set<AnnotationMirror> hierarchies) {
        Subtypes subtypes = targetToRecords.get(target).subtypes;
        Set<AnnotationMirror> subtypesTops = subtypes.targets.get(supertype);
        if (subtypesTops == null) {
            subtypes.types.put(supertype, new HashSet<>(hierarchies));
        } else {
            subtypesTops.addAll(hierarchies);
        }
    }

    public void addPrimarySubtypes(final TypeVariable target, QualifierHierarchy qualifierHierarchy, final Set<AnnotationMirror> annos) {
        Subtypes subtypes = targetToRecords.get(target).subtypes;
        for( AnnotationMirror anno : annos) {
            final AnnotationMirror top = qualifierHierarchy.getTopAnnotation(anno);
            Set<AnnotationMirror> entries = subtypes.primaries.get(top);
            if (entries == null) {
                entries = new LinkedHashSet<>();
                subtypes.primaries.put(top, entries);
            }
            entries.add(anno);
        }
    }

    public void addTypeEqualities(TypeVariable target, AnnotatedTypeMirror type, Set<AnnotationMirror> hierarchies) {
        Equalities equalities = targetToRecords.get(target).equalities;
        Set<AnnotationMirror> equalityTops = equalities.types.get(type);
        if (equalityTops == null) {
            equalities.types.put(type, new HashSet<>(hierarchies));
        } else {
            equalityTops.addAll(hierarchies);
        }
    }
}
