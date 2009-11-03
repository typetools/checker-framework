package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface NewClassTree extends ExpressionTree {
    @PolyRead ExpressionTree getEnclosingExpression() @PolyRead;
    @PolyRead List<? extends Tree> getTypeArguments() @PolyRead;
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead ExpressionTree getIdentifier() @PolyRead;
    @PolyRead List<? extends ExpressionTree> getArguments() @PolyRead;
    @PolyRead ClassTree getClassBody() @PolyRead;
}
