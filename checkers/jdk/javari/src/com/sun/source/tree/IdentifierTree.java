package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface IdentifierTree extends ExpressionTree {
    @PolyRead Name getName(@PolyRead IdentifierTree this);
}
