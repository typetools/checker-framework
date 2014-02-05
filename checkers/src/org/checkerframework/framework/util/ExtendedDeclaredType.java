package org.checkerframework.framework.util;

import java.util.List;
import javax.lang.model.element.Element;

public interface ExtendedDeclaredType extends ExtendedReferenceType {
    Element asElement();
    ExtendedTypeMirror getEnclosingType();
    List<? extends ExtendedTypeMirror> getTypeArguments();
}

