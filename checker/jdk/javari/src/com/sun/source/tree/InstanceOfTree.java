package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface InstanceOfTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead InstanceOfTree this);
    @PolyRead Tree getType(@PolyRead InstanceOfTree this);
}
