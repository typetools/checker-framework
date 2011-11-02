package com.sun.source.tree;
import checkers.javari.quals.*;

public interface AssertTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead AssertTree this);
    @PolyRead ExpressionTree getDetail(@PolyRead AssertTree this);
}
