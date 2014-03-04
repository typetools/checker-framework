package com.sun.source.tree;

import javax.lang.model.type.TypeKind;
import checkers.javari.quals.*;

public interface PrimitiveTypeTree extends Tree {
    @PolyRead TypeKind getPrimitiveTypeKind(@PolyRead PrimitiveTypeTree this);
}
