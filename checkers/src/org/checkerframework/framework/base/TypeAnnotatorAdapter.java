package org.checkerframework.framework.base;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

import checkers.types.AnnotatedTypeMirror;

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
            // Sometimes we get an ATM that has already been processed.
            return null;
        }

        TypeMirror type = atm.getUnderlyingType();
        QualifiedTypeMirror<Q> qtm = underlying.visit(type, elt);
        converter.bindTypes(qtm, atm);
        return null;
    }
}
