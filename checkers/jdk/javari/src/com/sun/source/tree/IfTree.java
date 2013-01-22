package com.sun.source.tree;

import checkers.javari.quals.*;

public interface IfTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead IfTree this);
    @PolyRead StatementTree getThenStatement(@PolyRead IfTree this);
    @PolyRead StatementTree getElseStatement(@PolyRead IfTree this);
}
