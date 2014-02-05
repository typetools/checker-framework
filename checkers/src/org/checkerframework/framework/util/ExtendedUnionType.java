package org.checkerframework.framework.util;

import java.util.List;

public interface ExtendedUnionType extends ExtendedTypeMirror {
    List<? extends ExtendedTypeMirror> getAlternatives();
}
