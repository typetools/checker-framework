package org.checkerframework.framework.util;

import java.util.List;

public interface ExtendedIntersectionType extends ExtendedTypeMirror {
    List<? extends ExtendedTypeMirror> getBounds();
}
