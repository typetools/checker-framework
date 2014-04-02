package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface LiteralTree extends ExpressionTree {
    @PolyRead Object getValue(@PolyRead LiteralTree this);
}
