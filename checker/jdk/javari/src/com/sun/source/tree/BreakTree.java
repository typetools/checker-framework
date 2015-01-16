package com.sun.source.tree;

import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface BreakTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead BreakTree this);
}
