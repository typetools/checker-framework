package org.checkerframework.checker.tainting;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;

import org.checkerframework.qualframework.base.AnnotationConverter;

import org.checkerframework.checker.qualparam.PolyQual.GroundQual;
import org.checkerframework.checker.qualparam.PolyQual.QualVar;
import org.checkerframework.checker.qualparam.QualParams;
import org.checkerframework.checker.qualparam.Wildcard;

import org.checkerframework.checker.tainting.qual.*;


public class TaintingAnnotationConverter implements AnnotationConverter<QualParams<Tainting>> {
    private Map<String, Wildcard<Tainting>> lookup;

    public TaintingAnnotationConverter() {
        QualVar<Tainting> mainVar = new QualVar<>("Main", Tainting.UNTAINTED, Tainting.TAINTED);

        lookup = new HashMap<>();
        lookup.put(Untainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED));
        lookup.put(Tainted.class.getName(), new Wildcard<>(Tainting.TAINTED));
        lookup.put(UseMain.class.getName(), new Wildcard<>(mainVar));
        lookup.put(ExtendsUntainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.UNTAINTED));
        lookup.put(ExtendsTainted.class.getName(), new Wildcard<>(Tainting.UNTAINTED, Tainting.TAINTED));
        lookup.put(ExtendsMain.class.getName(), new Wildcard<>(
                    new GroundQual<>(Tainting.UNTAINTED), mainVar));
    }

    private String getAnnotationTypeName(AnnotationMirror anno) {
        Element elt = anno.getAnnotationType().asElement();
        if (elt instanceof QualifiedNameable) {
            QualifiedNameable nameable = (QualifiedNameable)elt;
            return nameable.getQualifiedName().toString();
        } else {
            return null;
        }
    }

    @Override
    public QualParams<Tainting> fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        for (AnnotationMirror anno : annos) {
            String name = getAnnotationTypeName(anno);
            Wildcard<Tainting> value = lookup.get(name);
            if (value != null) {
                return new QualParams<>("Main", value);
            }
        }
        return null;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = getAnnotationTypeName(anno);
        return lookup.containsKey(name);
    }
}

