package com.sun.source.tree;

import javax.lang.model.element.Name;
import checkers.javari.quals.*;

public interface VariableTree extends StatementTree {
    @PolyRead ModifiersTree getModifiers() @PolyRead;
    @PolyRead Name getName() @PolyRead;
    @PolyRead Tree getType() @PolyRead;
    @PolyRead ExpressionTree getInitializer() @PolyRead;
}
