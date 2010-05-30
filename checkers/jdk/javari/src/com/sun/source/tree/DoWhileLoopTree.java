package com.sun.source.tree;

import checkers.javari.quals.*;

public interface DoWhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead StatementTree getStatement() @PolyRead;
}
