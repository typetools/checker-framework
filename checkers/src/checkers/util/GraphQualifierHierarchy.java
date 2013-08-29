package checkers.util;

import java.util.Collection;
import java.util.Map;
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

    public GraphQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
        super(f, bottom);
        // this.bottom = bottom;
    }

    // private final AnnotationMirror bottom;

    @Override
    protected void finish(QualifierHierarchy qualHierarchy,
            Map<AnnotationMirror, Set<AnnotationMirror>> fullMap,
            Map<AnnotationMirror, AnnotationMirror> polyQualifiers,
            Set<AnnotationMirror> tops, Set<AnnotationMirror> bottoms,
            Object... args) {
        // Careful, when this method is called, a field this.bottom would not be set yet.
        if (args != null && args[0] != null) {
            AnnotationMirror thebottom = (AnnotationMirror) args[0];
            // A special bottom qualifier was provided; go through the existing
            // bottom qualifiers and tie them all to this bottom qualifier.
            // Set<AnnotationMirror> bottoms = findBottoms(supertypes);
            Set<AnnotationMirror> allQuals = AnnotationUtils.createAnnotationSet();
            allQuals.addAll(fullMap.keySet());
            allQuals.remove(thebottom);
            AnnotationUtils.updateMappingToImmutableSet(fullMap, thebottom, allQuals);
            // thebottom is initially a top qualifier
            tops.remove(thebottom);
            // thebottom is now the single bottom qualifier
            bottoms.clear();
            bottoms.add(thebottom);
        }
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
        if (lhs.size() > 1) {
            SourceChecker.errorAbort("GraphQualifierHierarchy: Type with more than one annotation found: " + lhs);
        }
        if (rhs.size() > 1) {
            SourceChecker.errorAbort("GraphQualifierHierarchy: Type with more than one annotation found: " + rhs);
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
