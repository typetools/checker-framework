package org.checkerframework.framework.util.typeinference.solver;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.AnnotationMirrorMap;
import org.checkerframework.framework.util.AnnotationMirrorSet;

/**
 * TargetConstraints represents the set of all TUConstraints for which target was the type
 * parameter, i.e. the T in the TUConstraint. Unlike AF/TU Constraints, this class holds multiple
 * constraints and is mutated during solving (where the TU/AF Constraints are immutable).
 *
 * @see org.checkerframework.framework.util.typeinference.solver.ConstraintMap
 */
public class TargetConstraints {
    /**
     * The type parameter for which we are inferring a type argument. All constraints in this object
     * are related to this target.
     */
    public final TypeVariable target;

    public final Equalities equalities;

    /**
     * The target is the supertype in this case, that these are supertype constraints in which
     * target is the supertype. These are NOT supertypes of the target.
     */
    public final Supertypes supertypes;

    /**
     * The target is the supertype in this case, that these are subtype constraints in which target
     * is the subtype. These are NOT subtypes of the target.
     */
    public final Subtypes subtypes;

    public TargetConstraints(final TypeVariable target) {
        this.target = target;
        this.equalities = new Equalities();
        this.supertypes = new Supertypes();
        this.subtypes = new Subtypes();
    }

    protected static class Equalities {
        // Map( hierarchy top -> exact annotation in hierarchy)
        public AnnotationMirrorMap<AnnotationMirror> primaries = new AnnotationMirrorMap<>();

        // Map( type -> hierarchy top for which the primary annotation of type is equal to the
        // primary annotation of the target)
        // note all components and underlying types are EXACTLY equal to the key to this map
        public final Map<AnnotatedTypeMirror, AnnotationMirrorSet> types = new LinkedHashMap<>();

        // Map( type -> hierarchy top for which the primary annotation of target is equal to the
        // primary annotaiton of the target)
        // note all components and underlying types are EXACTLY equal to the key to this map
        public final Map<TypeVariable, AnnotationMirrorSet> targets = new LinkedHashMap<>();

        public void clear() {
            primaries.clear();
            types.clear();
            targets.clear();
        }
    }

    // remember these are constraint in which target is the supertype
    protected static class Supertypes {
        // Map( hierarchy top -> annotations that are subtypes to target in hierarchy)
        public AnnotationMirrorMap<AnnotationMirrorSet> primaries = new AnnotationMirrorMap<>();

        // Map( type -> hierarchy tops for which the primary annotations of type are subtypes of the
        // primary annotations of the target)
        // note all components and underlying types must uphold the supertype relationship in all
        // hierarchies
        public final Map<AnnotatedTypeMirror, AnnotationMirrorSet> types = new LinkedHashMap<>();

        // Map( otherTarget -> hierarchy tops for which the primary annotations of otherTarget are
        // subtypes of the primary annotations of the target)
        // note all components and underlying types must uphold the subtype relationship in all
        // hierarchies
        public final Map<TypeVariable, AnnotationMirrorSet> targets = new LinkedHashMap<>();

        public void clear() {
            primaries.clear();
            types.clear();
            targets.clear();
        }
    }

    // remember these are constraint in which target is the subtype
    protected static class Subtypes {
        // Map( hierarchy top -> annotations that are supertypes to target in hierarchy)
        public AnnotationMirrorMap<AnnotationMirrorSet> primaries = new AnnotationMirrorMap<>();

        // Map( type -> hierarchy tops for which the primary annotations of type are supertypes of
        // the primary annotations of the target)
        // note all components and underlying types must uphold the supertype relationship in all
        // hierarchies
        public final Map<AnnotatedTypeMirror, AnnotationMirrorSet> types = new LinkedHashMap<>();

        // Map( otherTarget -> hierarchy tops for which the primary annotations of otherTarget are
        // supertypes of the primary annotations of the target)
        // note all components and underlying types must uphold the subtype relationship in all
        // hierarchies
        public final Map<TypeVariable, AnnotationMirrorSet> targets = new LinkedHashMap<>();

        public void clear() {
            primaries.clear();
            types.clear();
            targets.clear();
        }
    }
}
