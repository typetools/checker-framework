package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ConditionalExpressionTree extends ExpressionTree {
    @PolyRead ExpressionTree getCondition(@PolyRead ConditionalExpressionTree this);
    @PolyRead ExpressionTree getTrueExpression(@PolyRead ConditionalExpressionTree this);
    @PolyRead ExpressionTree getFalseExpression(@PolyRead ConditionalExpressionTree this);
}
