package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface NewArrayTree extends ExpressionTree {
    @PolyRead Tree getType() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getDimensions() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getInitializers() @PolyRead;
}
