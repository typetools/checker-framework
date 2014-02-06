package org.checkerframework.framework.util;

import java.util.List;
import javax.lang.model.element.ExecutableElement;

public interface ExtendedExecutableType extends ExtendedTypeMirror {
    ExecutableElement asElement();
    List<? extends ExtendedTypeMirror> getParameterTypes();
    ExtendedTypeMirror getReceiverType();
    ExtendedTypeMirror getReturnType();
    List<? extends ExtendedTypeMirror> getThrownTypes();
    List<? extends ExtendedTypeVariable> getTypeVariables();
}
