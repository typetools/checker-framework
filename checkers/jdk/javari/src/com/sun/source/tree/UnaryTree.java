package com.sun.source.tree;

import checkers.javari.quals.*;

public interface UnaryTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead UnaryTree this);
}
