package com.sun.source.tree;

import checkers.javari.quals.*;

public interface ThrowTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ThrowTree this);
}
