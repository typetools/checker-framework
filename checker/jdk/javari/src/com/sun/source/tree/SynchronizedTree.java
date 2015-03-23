package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface SynchronizedTree extends StatementTree {
    @PolyRead ExpressionTree getExpression(@PolyRead SynchronizedTree this);
    @PolyRead BlockTree getBlock(@PolyRead SynchronizedTree this);
}
