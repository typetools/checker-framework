package com.sun.source.tree;

import checkers.javari.quals.*;

public interface TypeCastTree extends ExpressionTree {
    @PolyRead Tree getType(@PolyRead TypeCastTree this);
    @PolyRead ExpressionTree getExpression(@PolyRead TypeCastTree this);
}
