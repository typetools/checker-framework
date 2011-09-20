package com.sun.source.tree;

import checkers.javari.quals.*;

public interface DoWhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead DoWhileLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead DoWhileLoopTree this);
}
