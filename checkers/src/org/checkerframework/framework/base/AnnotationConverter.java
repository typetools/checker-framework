package org.checkerframework.framework.base;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;

public interface AnnotationConverter<Q> {
    Q fromAnnotations(Collection<? extends AnnotationMirror> annos);

    boolean isAnnotationSupported(AnnotationMirror anno);
}
