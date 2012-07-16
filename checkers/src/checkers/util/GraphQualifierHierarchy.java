package checkers.util;

import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.source.SourceChecker;
import checkers.types.QualifierHierarchy;

/**
* Represents the type qualifier hierarchy of a type system.
*
* This class is immutable and can be only created through {@link GraphFactory}.
*/
public class GraphQualifierHierarchy extends MultiGraphQualifierHierarchy {

    /**
     * We only need to make sure that "build" instantiates the right QualifierHierarchy. 
     */
    public static class GraphFactory extends MultiGraphFactory {
        private final AnnotationMirror bottom;

        public GraphFactory(SourceChecker checker) {
            super(checker);
            this.bottom = null;
        }

        public GraphFactory(SourceChecker checker, AnnotationMirror bottom) {
            super(checker);
            this.bottom = bottom;
        }

        @Override
        protected QualifierHierarchy createQualifierHierarchy() {
            if (this.bottom!=null) {
                // A special bottom qualifier was provided; go through the existing
                // bottom qualifiers and tie them all to this bottom qualifier.
                Set<AnnotationMirror> bottoms = findBottoms(supertypes);
                for (AnnotationMirror abot : bottoms) {
                    if (!AnnotationUtils.areSame(bottom, abot)) {
                        addSubtype(bottom, abot);
                    }
                }
            }

            return new GraphQualifierHierarchy(this);
        }
    }

    
    protected GraphQualifierHierarchy(GraphFactory f) {
        super(f);
    }

    protected GraphQualifierHierarchy(GraphQualifierHierarchy h) {
        super(h);
    }

    /**
     * Returns the top qualifier for this hierarchy.
     *
     * The top qualifier is inferred from the hierarchy, as being the only
     * one without any super qualifiers
     */
    @Override
    public Set<AnnotationMirror> getTopAnnotations() {
        if (tops.size() != 1) {
            SourceChecker.errorAbort("Expected 1 possible top qualifier, found "
                               + tops.size()
                               + " (does the checker know about all type qualifiers?): "
                               + tops);
        }
        return this.tops;
    }

    @Override
    public Set<AnnotationMirror> getBottomAnnotations() {
        // TODO: checks?
        return this.bottoms;
    }

    @Override
    public boolean isSubtype(Collection<AnnotationMirror> rhs, Collection<AnnotationMirror> lhs) {
        if (lhs.isEmpty() || rhs.isEmpty()) {
            SourceChecker.errorAbort("GraphQualifierHierarchy: Empty annotations in lhs: " + lhs + " or rhs: " + rhs);
        }
        for (AnnotationMirror lhsAnno : lhs) {
            for (AnnotationMirror rhsAnno : rhs) {
                if (isSubtype(rhsAnno, lhsAnno)) {
                    return true;
                }
            }
        }
        return false;
    }
}
