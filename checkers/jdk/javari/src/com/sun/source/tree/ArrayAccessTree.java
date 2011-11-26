package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ArrayAccessTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression() @PolyRead;
    @PolyRead ExpressionTree getIndex() @PolyRead;
}
