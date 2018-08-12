package org.checkerframework.framework.type.typeannotator;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;

/**
 * {@link TypeAnnotator} is an abstract AnnotatedTypeScanner to be used with {@link
 * ListTypeAnnotator}.
 *
 * @see org.checkerframework.framework.type.typeannotator.ListTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.PropagationTypeAnnotator
 * @see org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator
 */
public abstract class TypeAnnotator extends AnnotatedTypeScanner<Void, Void> {

    protected final AnnotatedTypeFactory typeFactory;

    public TypeAnnotator(AnnotatedTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    @Override
    public Void visitExecutable(AnnotatedExecutableType t, Void p) {
        // skip the receiver
        scan(t.getReturnType(), p);
        scanAndReduce(t.getParameterTypes(), p, null);
        scanAndReduce(t.getThrownTypes(), p, null);
        scanAndReduce(t.getTypeVariables(), p, null);
        return null;
    }
}
