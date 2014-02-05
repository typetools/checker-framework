package org.checkerframework.framework.util;

import javax.lang.model.element.Element;

public interface ExtendedTypeVariable extends ExtendedReferenceType {
    Element asElement();
    ExtendedTypeMirror getLowerBound();
    ExtendedTypeMirror getUpperBound();
}
