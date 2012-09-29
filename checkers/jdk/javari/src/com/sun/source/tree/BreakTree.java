package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface BreakTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead BreakTree this);
}
