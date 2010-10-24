package com.sun.source.tree;

import checkers.javari.quals.*;

public interface CatchTree extends Tree {
    @PolyRead VariableTree getParameter() @PolyRead;
    @PolyRead BlockTree getBlock() @PolyRead;
}
