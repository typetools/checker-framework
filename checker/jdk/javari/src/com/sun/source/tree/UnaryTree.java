package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface UnaryTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead UnaryTree this);
}
