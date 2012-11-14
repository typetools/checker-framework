package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface NewArrayTree extends ExpressionTree {
    @PolyRead Tree getType(@PolyRead NewArrayTree this);
    @PolyRead List<? extends ExpressionTree> getDimensions(@PolyRead NewArrayTree this);
    @PolyRead List<? extends ExpressionTree> getInitializers(@PolyRead NewArrayTree this);
}
