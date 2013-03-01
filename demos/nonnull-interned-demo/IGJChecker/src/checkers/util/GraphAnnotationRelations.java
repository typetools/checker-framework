package checkers.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import checkers.types.AnnotationRelations;

/**
 * A representation for annotations in a graph.
 */
public class GraphAnnotationRelations extends AnnotationRelations {
    /** map: qualifier --> supertypes of the qualifier**/
    private Map<String, List<String>> subtypes;
    
    /** the root of all the qualifiers **/
    private AnnotationMirror root;

    /**
     * Constructs an instance of {@code GraphAnnotationRelations} with 
     * no qualifier (i.e. {@code null}) being the root qualifier
     */
    public GraphAnnotationRelations() {
        this(null);
    }
    
    /**
     * Initialize an instance with the provided annotation being the root
     * annotation in the hierarchy
     * 
     * @param root
     */
    public GraphAnnotationRelations(AnnotationMirror root) {
        subtypes = new HashMap<String, List<String>>();
        this.root = root;
        subtypes.put(AnnotationUtils.annotationName(root), new LinkedList<String>());
    }
    
    /**
     * Adds a subtype relationship between two type qualifiers
     * 
     * @param subAnno   the sub type qualifier
     * @param superAnno the super type qualifier
     */
    public void addSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
        String superAnnotation = AnnotationUtils.annotationName(superAnno);
        String subAnnotation = AnnotationUtils.annotationName(subAnno);
        
        if (!subtypes.containsKey(subAnnotation))
            subtypes.put(subAnnotation, new LinkedList<String>());
        if (!subtypes.containsKey(superAnnotation))
            subtypes.put(superAnnotation, new LinkedList<String>());
        if (AnnotationUtils.isSame(subAnno, superAnno))
            return;
        
        subtypes.get(subAnnotation).add(superAnnotation);
        List<String> supSupers = subtypes.get(superAnnotation);
        
        if (supSupers != null) {
            for (String supSuper : supSupers)
                subtypes.get(subAnnotation).add(supSuper);
        }
    }
    
    @Override
    public AnnotationMirror getRootAnnotation() {
        return root;
    }

    /**
     * Set the root type qualifier of the represented hierarchy
     * @param root  the root (the ultimate super) type qualifier
     */
    public void setRootAnnotation(AnnotationMirror root) {
        this.root = root;
    }

    @Override
    public boolean isSubtype(AnnotationMirror sup, AnnotationMirror sub) {
        if (AnnotationUtils.isSame(sup, this.root) || AnnotationUtils.isSame(sup, sub))
            return true;
        String supAnno = AnnotationUtils.annotationName(sup);
        String subAnno = AnnotationUtils.annotationName(sub);
        
        return subtypes.get(subAnno).contains(supAnno);
    }
    
    @Override
    public Set<String> getTypeQualifiers() {
        return Collections.unmodifiableSet(this.subtypes.keySet());
    }
}
