package com.sun.source.tree;
import org.checkerframework.checker.javari.qual.*;

public interface AssignmentTree extends ExpressionTree {
    @PolyRead ExpressionTree getVariable(@PolyRead AssignmentTree this);
    @PolyRead ExpressionTree getExpression(@PolyRead AssignmentTree this);
}
