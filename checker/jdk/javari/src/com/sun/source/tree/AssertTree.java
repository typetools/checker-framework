package com.sun.source.tree;
import org.checkerframework.checker.javari.qual.*;

public interface AssertTree extends StatementTree {
    @PolyRead ExpressionTree getCondition(@PolyRead AssertTree this);
    @PolyRead ExpressionTree getDetail(@PolyRead AssertTree this);
}
