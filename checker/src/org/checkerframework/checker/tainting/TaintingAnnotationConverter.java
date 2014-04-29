package org.checkerframework.checker.tainting;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;

import org.checkerframework.qualframework.base.AnnotationConverter;

import org.checkerframework.checker.qualparam.BaseQual;
import org.checkerframework.checker.qualparam.ParamValue;
import org.checkerframework.checker.qualparam.QualParams;
import org.checkerframework.checker.qualparam.QualVar;
import org.checkerframework.checker.qualparam.WildcardQual;

import org.checkerframework.checker.tainting.qual.*;


public class TaintingAnnotationConverter implements AnnotationConverter<QualParams<Tainting>> {
    private Map<String, ParamValue<Tainting>> lookup;

    public TaintingAnnotationConverter() {
        lookup = new HashMap<>();
        lookup.put(Untainted.class.getName(), new BaseQual<>(Tainting.UNTAINTED));
        lookup.put(Tainted.class.getName(), new BaseQual<>(Tainting.TAINTED));
        lookup.put(UseMain.class.getName(), new QualVar<>("Main"));
        lookup.put(ExtendsUntainted.class.getName(), new WildcardQual<>(null, Tainting.UNTAINTED));
        lookup.put(ExtendsTainted.class.getName(), new WildcardQual<>(null, Tainting.TAINTED));
        lookup.put(ExtendsMain.class.getName(), new WildcardQual<>(null, new QualVar<>("Main")));
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
            ParamValue<Tainting> value = lookup.get(name);
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

