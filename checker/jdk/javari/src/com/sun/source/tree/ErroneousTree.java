package com.sun.source.tree;

import java.util.List;
import org.checkerframework.checker.javari.qual.*;

public interface ErroneousTree extends ExpressionTree {
    @PolyRead List<? extends Tree> getErrorTrees(@PolyRead ErroneousTree this);
}
