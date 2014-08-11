package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;

public interface TypeHierarchy {

    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);
    public boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror top);
    /*public boolean areSubtypes(Iterable<? extends AnnotatedTypeMirror> subtypes,
                               Iterable<? extends AnnotatedTypeMirror> supertypes);*/
}
