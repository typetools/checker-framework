package checkers.types;

import java.util.Collection;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.util.AnnotationUtils;

/**
 * Represents the relationship hierarchy of some given type qualifiers.
 */
public abstract class AnnotationRelations {

    /**
     * Compares the type qualifiers
     * 
     * @param sup   the type qualifier tested to be the super qualifier
     * @param sub   the type qualifier tested to be the sub qualifier
     * @return  true iff sup is a super qualifier of sub
     */
    public abstract boolean isSubtype(AnnotationMirror sup, AnnotationMirror sub);

    /**
     * Check if the two annotations are in conflict.  Two qualifiers are in
     * conflict if both cannot annotate a type together.
     * 
     * @param anno1
     * @param anno2
     *      the two annotations to be checked
     * @return true iff anno1 and anno2 in conflict
     */
    public boolean isInConflict(AnnotationMirror anno1, AnnotationMirror anno2) {
        return isSubtype(anno1, anno2) || isSubtype(anno2, anno1);
    }

    /**
     * @return  the root (ultimate super) type qualifier in the hierarchy
     */
    public abstract AnnotationMirror getRootAnnotation();

    /**
     * Finds the first type qualifier in this hierachy in the given list of
     * qualifiers.
     * 
     * @param annotations   list of type qualifiers
     * @return  the first qualifier in annotations in this hierarchy
     */
    public AnnotationMirror getAnnotation(Collection<AnnotationMirror> annotations) {
        Set<String> typeQualifiers = getTypeQualifiers();
        for (AnnotationMirror anno : annotations) {
            if (typeQualifiers.contains(AnnotationUtils.annotationName(anno)))
                return anno;
        }
        return null;
    }
    
    /**
     * @return the fully qualified name represented in this hierarchy
     */
    public abstract Set<String> getTypeQualifiers();
    
}
