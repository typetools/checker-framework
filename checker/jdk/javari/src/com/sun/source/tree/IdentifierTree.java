package com.sun.source.tree;

import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface IdentifierTree extends ExpressionTree {
    @PolyRead Name getName(@PolyRead IdentifierTree this);
}
