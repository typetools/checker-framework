package org.checkerframework.framework.util;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.type.TypeKind;

public interface ExtendedTypeMirror extends AnnotatedConstruct {
    <R,P> R accept(ExtendedTypeVisitor<R,P> v, P p);
    TypeKind getKind();
}
