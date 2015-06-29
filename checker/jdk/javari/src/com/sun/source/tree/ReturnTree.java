package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface ReturnTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ReturnTree this);
}
