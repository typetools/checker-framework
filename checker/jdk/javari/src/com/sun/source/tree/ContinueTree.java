package com.sun.source.tree;

import javax.lang.model.element.Name;
import org.checkerframework.checker.javari.qual.*;

public interface ContinueTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead ContinueTree this);
}
