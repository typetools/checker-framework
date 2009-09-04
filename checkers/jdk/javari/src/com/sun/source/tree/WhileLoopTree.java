package com.sun.source.tree;

import checkers.javari.quals.*;

public interface WhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead StatementTree getStatement() @PolyRead;
}
