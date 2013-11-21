package com.sun.source.tree;

import checkers.javari.quals.*;

public interface BinaryTree extends ExpressionTree {
    @PolyRead ExpressionTree getLeftOperand(@PolyRead BinaryTree this);
    @PolyRead ExpressionTree getRightOperand(@PolyRead BinaryTree this);
}
