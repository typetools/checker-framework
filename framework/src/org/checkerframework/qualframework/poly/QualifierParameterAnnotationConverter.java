package org.checkerframework.qualframework.poly;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.checkerframework.qualframework.base.AnnotationConverter;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

/** An {@link AnnotationConverter} that can also find the names of all declared
 * parameters on a class or method.
 */
public interface QualifierParameterAnnotationConverter<Q> extends AnnotationConverter<QualParams<Q>> {
    /** Get the names of all parameters declared on a class, interface, enum,
     * or method declaration {@link Element}.
     *
     * @param elt the element to inspect for parameters
     * @param declAnnotations the declaration annotations on elt
     * @param type the ExtendedTypeMirror for the element, after stub file annotations have been applied
     * @return the list of declared parameters
     */
    Set<String> getDeclaredParameters(Element elt, Set<AnnotationMirror> declAnnotations, ExtendedTypeMirror type);
}
