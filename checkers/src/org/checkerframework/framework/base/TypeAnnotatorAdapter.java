package org.checkerframework.framework.base;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

import org.checkerframework.framework.util.ExtendedTypeMirror;
import org.checkerframework.framework.util.WrappedAnnotatedTypeMirror;

public class TypeAnnotatorAdapter<Q> extends checkers.types.TypeAnnotator {
    private TypeAnnotator<Q> underlying;
    private TypeMirrorConverter<Q> converter;

    public TypeAnnotatorAdapter(TypeAnnotator<Q> underlying,
            TypeMirrorConverter<Q> converter,
            QualifiedTypeFactoryAdapter<Q> factoryAdapter) {
        super(factoryAdapter);
        this.underlying = underlying;
        this.converter = converter;
    }

    public Q getExistingQualifier(ExtendedTypeMirror type) {
        if (type instanceof WrappedAnnotatedTypeMirror) {
            AnnotatedTypeMirror atm = ((WrappedAnnotatedTypeMirror)type).unwrap();
            if (atm.hasAnnotation(TypeMirrorConverter.Key.class)) {
                return converter.getQualifier(atm);
            }
        }

        return null;
    }

    @Override
    protected Void scan(AnnotatedTypeMirror atm, Element elt) {
        // Produce a qualified version of the ATM.
        WrappedAnnotatedTypeMirror watm = WrappedAnnotatedTypeMirror.wrap(atm);
        QualifiedTypeMirror<Q> qtm = underlying.visit(watm, elt);

        // Update the input ATM with the new qualifiers.
        converter.applyQualifiers(qtm, atm);

        return null;
    }
}
