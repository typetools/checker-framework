package com.sun.source.tree;

import checkers.javari.quals.*;

public interface WhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead WhileLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead WhileLoopTree this);
}
