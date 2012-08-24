package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface ContinueTree extends StatementTree {
    @PolyRead Name getLabel(@PolyRead ContinueTree this);
}
