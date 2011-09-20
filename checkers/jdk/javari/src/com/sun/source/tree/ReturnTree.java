package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ReturnTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ReturnTree this);
}
