package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface TryTree extends StatementTree {
    @PolyRead BlockTree getBlock(@PolyRead TryTree this);
    @PolyRead List<? extends CatchTree> getCatches(@PolyRead TryTree this);
    @PolyRead BlockTree getFinallyBlock(@PolyRead TryTree this);
}
