package com.sun.source.tree;
import checkers.javari.quals.*;

public interface ExpressionStatementTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ExpressionStatementTree this);
}
