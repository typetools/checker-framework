package org.checkerframework.framework.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public interface ExtendedTypeMirror extends AnnotatedConstruct {
    TypeMirror getRaw();
    <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p);
    TypeKind getKind();
}
