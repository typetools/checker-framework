package org.checkerframework.checkers.tainting;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.QualifiedNameable;

import org.checkerframework.framework.base.AnnotationConverter;

import org.checkerframework.checkers.tainting.annotations.Tainted;
import org.checkerframework.checkers.tainting.annotations.Untainted;


public class TaintingAnnotationConverter implements AnnotationConverter<Tainting> {
    private String taintedName;
    private String untaintedName;

    public TaintingAnnotationConverter() {
        this.taintedName = Tainted.class.getName();
        this.untaintedName = Untainted.class.getName();
    }

    private String getAnnotationTypeName(AnnotationMirror anno) {
        Element elt = anno.getAnnotationType().asElement();
        if (elt instanceof QualifiedNameable) {
            @SuppressWarnings("unchecked")
            QualifiedNameable nameable = (QualifiedNameable)elt;
            return nameable.getQualifiedName().toString();
        } else {
            return null;
        }
    }

    @Override
    public Tainting fromAnnotations(Collection<? extends AnnotationMirror> annos) {
        for (AnnotationMirror anno : annos) {
            String name = getAnnotationTypeName(anno);
            if (taintedName.equals(name)) {
                return Tainting.TAINTED;
            } else if (untaintedName.equals(name)) {
                return Tainting.UNTAINTED;
            }
        }
        return null;
    }

    @Override
    public boolean isAnnotationSupported(AnnotationMirror anno) {
        String name = getAnnotationTypeName(anno);
        return (taintedName.equals(name) || untaintedName.equals(name));
    }
}

