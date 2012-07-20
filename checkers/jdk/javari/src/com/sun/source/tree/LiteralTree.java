package com.sun.source.tree;

import checkers.javari.quals.*;

public interface LiteralTree extends ExpressionTree {
    @PolyRead Object getValue(@PolyRead LiteralTree this);
}
