package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ParenthesizedTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ParenthesizedTree this);
}
