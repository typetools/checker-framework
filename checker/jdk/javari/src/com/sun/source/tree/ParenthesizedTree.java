package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface ParenthesizedTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead ParenthesizedTree this);
}
