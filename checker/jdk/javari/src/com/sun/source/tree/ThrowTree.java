package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface ThrowTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ThrowTree this);
}
