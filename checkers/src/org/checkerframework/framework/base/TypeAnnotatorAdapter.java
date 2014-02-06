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

    @Override
    protected Void scan(AnnotatedTypeMirror atm, Element elt) {
        if (atm.hasAnnotation(atm.getAnnotation(TypeMirrorConverter.Key.class)) &&
                    converter.getQualifiedType(atm) != null) {
            // Sometimes we get an ATM that has already been fully processed.
            return null;
        }

        // We don't actually read any information from the input ATM aside from
        // its underlying type, so we don't need to convert it to a QTM.

        // Produce a qualified version of the ATM's underlying type.
        TypeMirror type = atm.getUnderlyingType();
        ExtendedTypeMirror wrappedType = converter.getWrapper().wrap(type, elt);
        QualifiedTypeMirror<Q> qtm = underlying.visit(wrappedType, elt);

        // Update the input ATM with the new qualifiers.
        converter.applyQualifiers(qtm, atm);

        return null;
    }
}
