package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface ErroneousTree extends ExpressionTree {
    @PolyRead List<? extends Tree> getErrorTrees(@PolyRead ErroneousTree this);
}
