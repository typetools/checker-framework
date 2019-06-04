package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;

/**
 * {@link TypeAnnotator} is an abstract AnnotatedTypeScanner to be used with {@link
 * ListTypeAnnotator}.
 *
 * @see org.checkerframework.framework.type.typeannotator.ListTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator
 * @see DefaultForTypeAnnotator
 */
public abstract class TypeAnnotator extends AnnotatedTypeScanner<Void, Void> {

    protected final AnnotatedTypeFactory typeFactory;

    public TypeAnnotator(AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }
}
