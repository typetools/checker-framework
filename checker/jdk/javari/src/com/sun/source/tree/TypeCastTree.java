package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface TypeCastTree extends ExpressionTree {
    @PolyRead Tree getType(@PolyRead TypeCastTree this);
    @PolyRead ExpressionTree getExpression(@PolyRead TypeCastTree this);
}
