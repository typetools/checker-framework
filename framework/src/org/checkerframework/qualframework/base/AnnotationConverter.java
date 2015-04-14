package org.checkerframework.qualframework.base;

import java.util.Collection;

import javax.lang.model.element.AnnotationMirror;

/**
 * {@link DefaultQualifiedTypeFactory} component for converting annotations
 * written in source code into type qualifiers.  Each type system must provide
 * an implementation of this interface that converts the annotations relevant
 * to that type system into an instance of the type system's qualifier
 * representation.
 */
public interface AnnotationConverter<Q> {
    /**
     * Performs type system-specific annotation parsing to produce a type
     * qualifier.
     *
     * @param annos     the collection of type annotations to parse
     * @return          a type qualifier corresponding to the annotations
     */
    Q fromAnnotations(Collection<? extends AnnotationMirror> annos);

    /**
     * Checks if an annotation is supported by the current type system.  This
     * method should return true if the presence of the provided {@link
     * AnnotationMirror} could affect the result of {@link fromAnnotations}.
     *
     * @param anno      the annotation to check
     * @return          true if the annotation is supported
     */
    boolean isAnnotationSupported(AnnotationMirror anno);
}
