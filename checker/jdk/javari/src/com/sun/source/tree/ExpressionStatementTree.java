package com.sun.source.tree;
import org.checkerframework.checker.javari.qual.*;

public interface ExpressionStatementTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ExpressionStatementTree this);
}
