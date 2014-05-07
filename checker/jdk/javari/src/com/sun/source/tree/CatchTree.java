package com.sun.source.tree;

import org.checkerframework.checker.javari.qual.*;

public interface CatchTree extends Tree {
    @PolyRead VariableTree getParameter(@PolyRead CatchTree this);
    @PolyRead BlockTree getBlock(@PolyRead CatchTree this);
}
