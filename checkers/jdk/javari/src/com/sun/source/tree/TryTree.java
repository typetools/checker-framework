package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface TryTree extends StatementTree {
    @PolyRead BlockTree getBlock() @PolyRead;
    @PolyRead List<? extends CatchTree> getCatches() @PolyRead;
    @PolyRead BlockTree getFinallyBlock() @PolyRead;
}
