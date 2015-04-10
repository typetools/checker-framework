package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;

/**
 * Compares AnnotatedTypeMirrors for subtype relationships.
 * See also QualifierHierarchy
 */
public interface TypeHierarchy {

    /**
     * @return true if subtype {@literal <:} supertype for all qualifier hierarchies in the type system
     */
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

    /**
     * @return true  if subtype {@literal <:} supertype in the qualifier hierarchy rooted by the top annotation
     */
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top);
}
