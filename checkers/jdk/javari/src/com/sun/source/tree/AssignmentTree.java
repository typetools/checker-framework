package com.sun.source.tree;
import checkers.javari.quals.*;

public interface AssignmentTree extends ExpressionTree {
    @PolyRead ExpressionTree getVariable() @PolyRead;
    @PolyRead ExpressionTree getExpression() @PolyRead;
}
