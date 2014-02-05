package org.checkerframework.framework.util;

import java.util.List;
import javax.lang.model.element.Element;

public interface ExtendedExecutableType extends ExtendedTypeMirror {
    Element asElement();
    List<? extends ExtendedTypeMirror> getParameterTypes();
    ExtendedTypeMirror getReceiverType();
    ExtendedTypeMirror getReturnType();
    List<? extends ExtendedTypeMirror> getThrownTypes();
    List<? extends ExtendedTypeVariable> getTypeVariables();
}
