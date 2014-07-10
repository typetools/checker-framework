package org.checkerframework.framework.type.visitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Replaces or adds all the annotations in the parameter with the annotations
 * from the visited type. An annotation is replaced if the parameter type
 * already has an annotation in the same hierarchy at the same location as the
 * visited type.
 * 
 * Example use: AnnotatedTypeMirror visitType = ...; AnnotatedTypeMirror
 * parameter - ...; visitType.accept(new AnnotatedTypesMerger(), parameter);
 * 
 * @author smillst
 * 
 */
public class AnnotatedTypeMerger extends AnnotatedTypeComparer<Void> {

    @Override
    protected Void compare(AnnotatedTypeMirror one, AnnotatedTypeMirror two) {
        if (one != null && two != null) {
            two.replaceAnnotations(one.getAnnotations());
        }
        return null;
    }

    @Override
    protected Void combineRs(Void r1, Void r2) {
        return r1;
    }

}
