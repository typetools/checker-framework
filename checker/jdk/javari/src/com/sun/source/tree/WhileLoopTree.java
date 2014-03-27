package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface WhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead WhileLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead WhileLoopTree this);
}
