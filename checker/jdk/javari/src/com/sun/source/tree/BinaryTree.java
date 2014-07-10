package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface BinaryTree extends ExpressionTree {
    @PolyRead ExpressionTree getLeftOperand(@PolyRead BinaryTree this);
    @PolyRead ExpressionTree getRightOperand(@PolyRead BinaryTree this);
}
