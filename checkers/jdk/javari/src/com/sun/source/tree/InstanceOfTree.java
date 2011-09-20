package com.sun.source.tree;

import checkers.javari.quals.*;

public interface InstanceOfTree extends ExpressionTree {
    @PolyRead ExpressionTree getExpression(@PolyRead InstanceOfTree this);
    @PolyRead Tree getType(@PolyRead InstanceOfTree this);
}
