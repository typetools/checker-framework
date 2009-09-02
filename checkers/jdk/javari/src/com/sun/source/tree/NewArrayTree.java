package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface NewArrayTree extends ExpressionTree {
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead Tree getType() @PolyRead;
    @PolyRead List<? extends ModifiersTree> getDimensionsModifiers() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getDimensions() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getInitializers() @PolyRead;
}
