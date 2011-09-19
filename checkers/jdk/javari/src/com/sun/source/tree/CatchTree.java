package com.sun.source.tree;

import checkers.javari.quals.*;

public interface CatchTree extends Tree {
    @PolyRead VariableTree getParameter(@PolyRead CatchTree this);
    @PolyRead BlockTree getBlock(@PolyRead CatchTree this);
}
