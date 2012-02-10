package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ConditionalExpressionTree extends ExpressionTree {
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead ExpressionTree getTrueExpression() @PolyRead;
    @PolyRead ExpressionTree getFalseExpression() @PolyRead;
}
