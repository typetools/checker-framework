package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface IfTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead IfTree this);
    @PolyRead StatementTree getThenStatement(@PolyRead IfTree this);
    @PolyRead StatementTree getElseStatement(@PolyRead IfTree this);
}
