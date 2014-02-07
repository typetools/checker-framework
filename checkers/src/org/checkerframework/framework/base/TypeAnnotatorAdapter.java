package org.checkerframework.framework.base;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;

import checkers.types.AnnotatedTypeMirror;

import org.checkerframework.framework.util.ExtendedTypeMirror;

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
        if (type instanceof ZippedTypeMirror) {
            AnnotatedTypeMirror atm = ((ZippedTypeMirror)type).getAnnotated();
            if (atm.hasAnnotation(TypeMirrorConverter.Key.class)) {
                return converter.getQualifier(atm);
            }
        }

        return null;
    }

    @Override
    protected Void scan(AnnotatedTypeMirror atm, Element elt) {
        // Produce a qualified version of the ATM's underlying type.
        TypeMirror type = atm.getUnderlyingType();
        ExtendedTypeMirror wrappedType = converter.getWrapper().wrap(type, elt);
        ExtendedTypeMirror zippedType = ZippedTypeMirror.zip(wrappedType, atm);
        QualifiedTypeMirror<Q> qtm = underlying.visit(zippedType, elt);

        // Update the input ATM with the new qualifiers.
        converter.applyQualifiers(qtm, atm);

        return null;
    }
}
