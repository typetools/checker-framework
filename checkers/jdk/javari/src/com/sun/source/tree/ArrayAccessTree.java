package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ArrayAccessTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ArrayAccessTree this);
    @PolyRead ExpressionTree getIndex(@PolyRead ArrayAccessTree this);
}
