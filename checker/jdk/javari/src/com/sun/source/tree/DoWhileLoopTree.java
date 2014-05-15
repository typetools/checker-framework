package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface DoWhileLoopTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead DoWhileLoopTree this);
    @PolyRead StatementTree getStatement(@PolyRead DoWhileLoopTree this);
}
