package org.checkerframework.checker.qualparam;

import java.util.*;

import javax.lang.model.element.Element;

import org.checkerframework.qualframework.base.AnnotationConverter;

public interface QualifierParameterAnnotationConverter<Q> extends AnnotationConverter<QualParams<Q>> {
    Set<String> getDeclaredParameters(Element elt);
}
