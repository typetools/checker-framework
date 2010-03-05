package com.sun.source.tree;

import checkers.javari.quals.*;

public interface BinaryTree extends ExpressionTree {
    @PolyRead ExpressionTree getLeftOperand() @PolyRead;
    @PolyRead ExpressionTree getRightOperand() @PolyRead;
}
