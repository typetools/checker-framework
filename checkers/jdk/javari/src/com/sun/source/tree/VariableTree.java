package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface VariableTree extends StatementTree {
    @PolyRead ModifiersTree getModifiers(@PolyRead VariableTree this);
    @PolyRead Name getName(@PolyRead VariableTree this);
    @PolyRead Tree getType(@PolyRead VariableTree this);
    @PolyRead ExpressionTree getInitializer(@PolyRead VariableTree this);
}
