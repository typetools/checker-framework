package com.sun.source.tree;

import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.javari.qual.*;

public interface PrimitiveTypeTree extends Tree {
    @PolyRead TypeKind getPrimitiveTypeKind(@PolyRead PrimitiveTypeTree this);
}
