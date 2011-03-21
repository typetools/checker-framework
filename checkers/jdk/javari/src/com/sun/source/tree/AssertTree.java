package com.sun.source.tree;
import checkers.javari.quals.*;

public interface AssertTree extends StatementTree {
    @PolyRead ExpressionTree getCondition() @PolyRead;
    @PolyRead ExpressionTree getDetail() @PolyRead;
}
