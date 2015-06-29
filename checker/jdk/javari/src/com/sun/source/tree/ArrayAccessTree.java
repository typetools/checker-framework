package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface ArrayAccessTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ArrayAccessTree this);
    @PolyRead ExpressionTree getIndex(@PolyRead ArrayAccessTree this);
}
