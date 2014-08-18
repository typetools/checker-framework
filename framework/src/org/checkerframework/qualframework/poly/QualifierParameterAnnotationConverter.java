package org.checkerframework.qualframework.poly;

import java.util.*;

import javax.lang.model.element.Element;

import org.checkerframework.qualframework.base.AnnotationConverter;

/** An {@link AnnotationConverter} that can also find the names of all declared
 * parameters on a class or method.
 */
public interface QualifierParameterAnnotationConverter<Q> extends AnnotationConverter<QualParams<Q>> {
    /** Get the names of all parameters declared on a class, interface, enum,
     * or method declaration {@link Element}.
     */
    Set<String> getDeclaredParameters(Element elt);
}
