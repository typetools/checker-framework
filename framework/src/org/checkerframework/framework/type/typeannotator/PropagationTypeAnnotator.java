package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;

/**
 * {@link PropagationTypeAnnotator} adds qualifiers to types where the qualifier
 * to add should be transferred from a different type.
 *
 * At the moment, the only function PropagationTypeAnnotator provides, is the
 * propagation of generic type parameter functions to unannotated wildcards
 * with missing bounds annotations.
 *
 * @see #visitWildcard(org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType, Object)
 *
 * PropagationTypeAnnotator traverses trees deeply by default.
 *
 */
public class PropagationTypeAnnotator extends TypeAnnotator {

    public PropagationTypeAnnotator(AnnotatedTypeFactory typeFactory) {
        super(typeFactory);
    }

    @Override
    public Void visitWildcard(AnnotatedWildcardType type, Void aVoid) {
        return super.visitWildcard(type, aVoid);
    }
}
