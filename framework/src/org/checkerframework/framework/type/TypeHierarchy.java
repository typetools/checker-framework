package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;

/**
 * Compares AnnotatedTypeMirrors for subtype relationships.
 * See also QualifierHierarchy
 */
public interface TypeHierarchy {

    /**
     * Returns true if {@code subtype} is a subtype of or convertible to {@code supertype}
     * for all hierarchies present.  The underlying Java type of {@code subtype} must be a subtype of
     * or convertible to the underlying Java type of {@code supertype}.
     *
     * @param subtype   possible subtype
     * @param supertype possible supertype
     * @return true if {@code subtype} is a subtype of {@code supertype} for all hierarchies present.
     */
    boolean isSubtypeOrConvertible(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

    /**
     * Returns true if {@code subtype} is a subtype of {@code supertype} in the qualifier hierarchy
     * whose top is {@code top} The underlying Java type of {@code subtype} must be a subtype of
     * or convertible to the underlying Java type of {@code supertype}.
     *
     * @param subtype   possible subtype
     * @param supertype possible supertype
     * @param top       the qualifier at the top of the heirarchy for which the subtype check should be preformed.
     * @return Returns true if {@code subtype} is a subtype of {@code supertype} in the qualifier hierarchy
     * whose top is {@code top}
     */
    boolean isSubtypeOrConvertible(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top);

}
