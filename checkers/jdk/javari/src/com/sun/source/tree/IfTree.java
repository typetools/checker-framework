package com.sun.source.tree;

import checkers.javari.quals.*;

public interface IfTree extends StatementTree {
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead StatementTree getThenStatement() @PolyRead;
    @PolyRead StatementTree getElseStatement() @PolyRead;
}
