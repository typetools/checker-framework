package com.sun.source.tree;

import java.util.List;
import checkers.javari.quals.*;

public interface NewClassTree extends ExpressionTree {
    @PolyRead ExpressionTree getEnclosingExpression(@PolyRead NewClassTree this);
    @PolyRead List<? extends Tree> getTypeArguments(@PolyRead NewClassTree this);
    @PolyRead ExpressionTree getIdentifier(@PolyRead NewClassTree this);
    @PolyRead List<? extends ExpressionTree> getArguments(@PolyRead NewClassTree this);
    @PolyRead ClassTree getClassBody(@PolyRead NewClassTree this);
}
